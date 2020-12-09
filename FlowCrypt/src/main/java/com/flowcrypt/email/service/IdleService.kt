/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.sync.IdleSyncRunnable
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.jetpack.lifecycle.ConnectionLifecycleObserver
import com.flowcrypt.email.jetpack.viewmodel.AccountViewModel
import com.flowcrypt.email.jetpack.workmanager.sync.InboxIdleMsgsAddedWorker
import com.flowcrypt.email.jetpack.workmanager.sync.InboxIdleMsgsRemovedWorker
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.sun.mail.imap.IMAPFolder
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor
import javax.mail.event.MessageChangedEvent
import javax.mail.event.MessageCountEvent

/**
 *
 * @author DenBond7
 * Date: 14.06.2017
 * Time: 12:18
 * E-mail: DenBond7@gmail.com
 */
class IdleService : LifecycleService() {
  private val idleExecutorService = Executors.newFixedThreadPool(1) as ThreadPoolExecutor
  private var idleFuture: Future<*>? = null
  private var idleSyncRunnable: IdleSyncRunnable? = null

  private val connectionLifecycleObserver: ConnectionLifecycleObserver by lazy {
    ConnectionLifecycleObserver(this)
  }

  private var cachedAccountEntity: AccountEntity? = null

  override fun onCreate() {
    super.onCreate()
    LogsUtil.d(TAG, "onCreate")
    setupConnectionObserver()
    observeActiveAccountChanges()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    LogsUtil.d(TAG, "onStartCommand |intent =$intent|flags = $flags|startId = $startId")
    return super.onStartCommand(intent, flags, startId)
  }

  override fun onDestroy() {
    super.onDestroy()
    LogsUtil.d(TAG, "onDestroy")
    lifecycle.removeObserver(connectionLifecycleObserver)

    stopIdleThread()
    idleExecutorService.shutdown()
  }

  private fun stopIdleThread() {
    idleFuture?.cancel(true)
    idleSyncRunnable?.interruptIdle()
    idleSyncRunnable = null
  }

  private fun setupConnectionObserver() {
    lifecycle.addObserver(connectionLifecycleObserver)
    connectionLifecycleObserver.connectionLiveData.observe(this, { isConnected ->
      if (isConnected) {
        cachedAccountEntity?.let { accountEntity -> submitIdle(accountEntity) }
      }
    })
  }

  private fun observeActiveAccountChanges() {
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(this)
    val activeAccountLiveData: LiveData<AccountEntity?> = roomDatabase.accountDao().getActiveAccountLD().switchMap { accountEntity ->
      liveData {
        emit(AccountViewModel.getAccountEntityWithDecryptedInfoSuspend(accountEntity))
      }
    }

    activeAccountLiveData.observe(this, {
      cachedAccountEntity = it
      cachedAccountEntity?.let { accountEntity -> submitIdle(accountEntity) }
    })
  }

  private fun submitIdle(accountEntity: AccountEntity) {
    cleanPool()
    stopIdleThread()

    idleSyncRunnable = IdleSyncRunnable(applicationContext, accountEntity, object : IdleSyncRunnable.ActionsListener {
      override fun syncFolderState() {
        //RefreshMessagesSyncTask
      }

      override fun messageChanged(accountEntity: AccountEntity, localFolder: LocalFolder, remoteFolder: IMAPFolder, e: MessageChangedEvent?) {
        LogsUtil.d(TAG, "messageChanged")
        lifecycleScope.launch {
          val msg = e?.message
          if (msg != null && e.messageChangeType == MessageChangedEvent.FLAGS_CHANGED) {
            try {
              val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)
              roomDatabase.msgDao().updateLocalMsgFlags(accountEntity.email, localFolder.fullName, remoteFolder.getUID(msg), msg.flags)
            } catch (e: Exception) {
              e.printStackTrace()
              ExceptionUtil.handleError(e)
            }
          }
        }
      }

      override fun messagesAdded(accountEntity: AccountEntity, localFolder: LocalFolder, remoteFolder: IMAPFolder, e: MessageCountEvent?) {
        LogsUtil.d(TAG, "messagesAdded: " + e?.messages?.size)
        InboxIdleMsgsAddedWorker.enqueue(applicationContext)
      }

      override fun messagesRemoved(accountEntity: AccountEntity, localFolder: LocalFolder, remoteFolder: IMAPFolder, e: MessageCountEvent?) {
        LogsUtil.d(TAG, "messagesRemoved")
        InboxIdleMsgsRemovedWorker.enqueue(applicationContext)
      }
    })

    idleSyncRunnable?.let { runnable ->
      idleFuture = idleExecutorService.submit(runnable)
    }
  }

  private fun cleanPool() {
    val iterator = idleExecutorService.queue.iterator()
    while (iterator.hasNext()) {
      iterator.remove()
    }
  }

  companion object {
    private val TAG = IdleService::class.java.simpleName

    /**
     * This method can bu used to start [IdleService].
     *
     * @param context Interface to global information about an application environment.
     */
    fun start(context: Context) {
      val startEmailServiceIntent = Intent(context, IdleService::class.java)
      context.startService(startEmailServiceIntent)
    }

    /**
     * Restart [IdleService].
     *
     * @param context Interface to global information about an application environment.
     */
    fun restart(context: Context) {
      NotificationManagerCompat.from(context).cancelAll()
      val intent = Intent(context, IdleService::class.java)
      context.stopService(intent)
      context.startService(intent)
    }
  }
}
