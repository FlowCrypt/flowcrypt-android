/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.service

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.isAppForegrounded
import com.flowcrypt.email.extensions.kotlin.toHex
import com.flowcrypt.email.jetpack.lifecycle.ConnectionLifecycleObserver
import com.flowcrypt.email.jetpack.workmanager.sync.InboxIdleMsgsAddedWorker
import com.flowcrypt.email.jetpack.workmanager.sync.InboxIdleMsgsRemovedWorker
import com.flowcrypt.email.jetpack.workmanager.sync.InboxIdleSyncWorker
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import jakarta.mail.Flags
import jakarta.mail.event.MessageChangedEvent
import jakarta.mail.event.MessageCountEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.angus.mail.imap.IMAPFolder
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor

/**
 *
 * @author Denys Bondarenko
 */
class IdleService : LifecycleService() {
  private val binder = LocalBinder()
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

  override fun onBind(intent: Intent): IBinder {
    super.onBind(intent)
    LogsUtil.d(TAG, "onBind")
    return binder
  }

  override fun onRebind(intent: Intent) {
    super.onRebind(intent)
    LogsUtil.d(TAG, "onRebind:$intent")
  }

  override fun onUnbind(intent: Intent): Boolean {
    LogsUtil.d(TAG, "onUnbind:$intent")
    return super.onUnbind(intent)
  }

  private fun stopIdleThread() {
    idleFuture?.cancel(true)
    idleSyncRunnable?.interruptIdle()
    idleSyncRunnable = null
  }

  private fun setupConnectionObserver() {
    lifecycle.addObserver(connectionLifecycleObserver)
    connectionLifecycleObserver.connectionLiveData.observe(this) { isConnected ->
      if (isConnected) {
        cachedAccountEntity?.let { accountEntity -> submitIdle(accountEntity) }
      }
    }
  }

  private fun observeActiveAccountChanges() {
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(this)
    val activeAccountLiveData: LiveData<AccountEntity?> =
      roomDatabase.accountDao().getActiveAccountLD().switchMap { accountEntity ->
        liveData {
          emit(accountEntity?.withDecryptedInfo())
        }
      }

    activeAccountLiveData.observe(this) {
      cachedAccountEntity = it
      cachedAccountEntity?.let { accountEntity -> submitIdle(accountEntity) }
    }
  }

  private fun submitIdle(accountEntity: AccountEntity) {
    cleanPool()
    stopIdleThread()

    idleSyncRunnable = IdleSyncRunnable(
      applicationContext,
      accountEntity,
      object : IdleSyncRunnable.ActionsListener {
        override fun syncFolderState() {
          InboxIdleSyncWorker.enqueue(applicationContext)
        }

        override fun messageChanged(
          accountEntity: AccountEntity,
          localFolder: LocalFolder,
          remoteFolder: IMAPFolder,
          e: MessageChangedEvent?
        ) {
          LogsUtil.d(TAG, "messageChanged")
          lifecycleScope.launch {
            handleMsgChangedEvent(e, accountEntity, localFolder, remoteFolder)
          }
        }

        override fun messagesAdded(
          accountEntity: AccountEntity,
          localFolder: LocalFolder,
          remoteFolder: IMAPFolder,
          e: MessageCountEvent?
        ) {
          LogsUtil.d(TAG, "messagesAdded: " + e?.messages?.size)
          InboxIdleMsgsAddedWorker.enqueue(applicationContext)
        }

        override fun messagesRemoved(
          accountEntity: AccountEntity,
          localFolder: LocalFolder,
          remoteFolder: IMAPFolder,
          e: MessageCountEvent?
        ) {
          LogsUtil.d(TAG, "messagesRemoved")
          InboxIdleMsgsRemovedWorker.enqueue(applicationContext)
        }
      })

    idleSyncRunnable?.let { runnable ->
      idleFuture = if (accountEntity.useAPI) null else idleExecutorService.submit(runnable)
    }
  }

  private suspend fun handleMsgChangedEvent(
    e: MessageChangedEvent?, accountEntity: AccountEntity,
    localFolder: LocalFolder, remoteFolder: IMAPFolder
  ) = withContext(Dispatchers.IO) {
    val msg = e?.message
    if (msg != null && e.messageChangeType == MessageChangedEvent.FLAGS_CHANGED) {
      try {
        val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)
        roomDatabase.msgDao().updateLocalMsgFlags(
          accountEntity.email,
          localFolder.fullName,
          remoteFolder.getUID(msg),
          msg.flags
        )

        if (!applicationContext.isAppForegrounded()) {
          if (msg.flags.contains(Flags.Flag.SEEN)) {
            MessagesNotificationManager(applicationContext).cancel(remoteFolder.getUID(msg).toHex())
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
      }
    }
  }

  private fun cleanPool() {
    val iterator = idleExecutorService.queue.iterator()
    while (iterator.hasNext()) {
      iterator.next()
      iterator.remove()
    }
  }

  /**
   * We use an instance of [Binder] to prevent the system kills a service if the app will go to the
   * background
   */
  inner class LocalBinder : Binder()

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

    fun bind(context: Context, serviceConnection: ServiceConnection) {
      Intent(context, IdleService::class.java).also { intent ->
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
      }
    }
  }
}
