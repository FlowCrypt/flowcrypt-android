/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync

import com.flowcrypt.email.api.email.sync.tasks.SyncTask
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.jetpack.viewmodel.AccountViewModel
import com.flowcrypt.email.jetpack.workmanager.sync.LoadContactsWorker
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.sun.mail.iap.ConnectionException
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue

/**
 * @author Denis Bondarenko
 *         Date: 10/17/19
 *         Time: 3:36 PM
 *         E-mail: DenBond7@gmail.com
 */
class ConnectionSyncRunnable(syncListener: SyncListener) : BaseSyncRunnable(syncListener) {
  private val tasksQueue: BlockingQueue<SyncTask> = LinkedBlockingQueue()
  private val tasksExecutorService: ExecutorService = Executors.newFixedThreadPool(MAX_RUNNING_TASKS_COUNT)
  private val tasksMap = ConcurrentHashMap<String, Future<*>>()

  init {
    loadContactsInfoIfNeeded()
  }

  override fun run() {
    LogsUtil.d(tag, " run!")
    Thread.currentThread().name = javaClass.simpleName
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(syncListener.context)
    var lastActiveAccount = AccountViewModel.getAccountEntityWithDecryptedInfo(roomDatabase.accountDao().getActiveAccount())
    if (lastActiveAccount != null) {
      while (!Thread.interrupted()) {
        try {
          LogsUtil.d(tag, "TasksQueue size = " + tasksQueue.size)
          val refreshedActiveAccount = AccountViewModel.getAccountEntityWithDecryptedInfo(roomDatabase.accountDao().getActiveAccount())
              ?: throw InterruptedException()
          val isResetConnectionNeeded = !refreshedActiveAccount.email.equals(lastActiveAccount?.email, true)
          lastActiveAccount = refreshedActiveAccount
          runSyncTask(lastActiveAccount, tasksQueue.take(), true, isResetConnectionNeeded)
        } catch (e: InterruptedException) {
          e.printStackTrace()
          tasksQueue.clear()
          tasksExecutorService.shutdown()
          break
        }
      }
    }

    closeConn()
    LogsUtil.d(tag, " stopped!")
  }

  private fun loadContactsInfoIfNeeded() {
    LoadContactsWorker.enqueue(syncListener.context)
  }

  private fun runSyncTask(accountEntity: AccountEntity, task: SyncTask?, isRetryEnabled: Boolean, isResetConnectionNeeded: Boolean) {
    task?.let {
      try {
        resetConnIfNeeded(accountEntity, isResetConnectionNeeded, task)

        if (sess == null || store == null) {
          openConnToStore(accountEntity)
          LogsUtil.d(tag, "Connected!")
        }

        val activeStore = store ?: return
        val activeSess = sess ?: return

        val iterator = tasksMap.iterator()
        while (iterator.hasNext()) {
          val entry = iterator.next()
          if (entry.value.isDone) {
            iterator.remove()
          }
        }

        if (!tasksExecutorService.isShutdown) {
          tasksMap[task.uniqueId] = tasksExecutorService.submit(
              SyncTaskRunnable(accountEntity, syncListener, task, activeStore, activeSess))
        }

      } catch (e: Exception) {
        e.printStackTrace()
        if (e is ConnectionException) {
          if (isRetryEnabled) {
            runSyncTask(accountEntity, task, false, isResetConnectionNeeded)
          } else {
            ExceptionUtil.handleError(e)
            task.handleException(accountEntity, e, syncListener)
          }
        } else {
          ExceptionUtil.handleError(e)
          task.handleException(accountEntity, e, syncListener)
        }
      }
    }
  }

  companion object {
    private const val MAX_RUNNING_TASKS_COUNT = 5
  }
}
