/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync

import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.sync.tasks.CheckNewMessagesSyncTask
import com.flowcrypt.email.api.email.sync.tasks.LoadMessagesToCacheSyncTask
import com.flowcrypt.email.api.email.sync.tasks.LoadPrivateKeysFromEmailBackupSyncTask
import com.flowcrypt.email.api.email.sync.tasks.RefreshMessagesSyncTask
import com.flowcrypt.email.api.email.sync.tasks.SearchMessagesSyncTask
import com.flowcrypt.email.api.email.sync.tasks.SendMessageWithBackupToKeyOwnerSynsTask
import com.flowcrypt.email.api.email.sync.tasks.SyncTask
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.jetpack.viewmodel.AccountViewModel
import com.flowcrypt.email.jetpack.workmanager.sync.LoadContactsSyncTask
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

  private fun removeOldTasks(cls: Class<*>, queue: BlockingQueue<SyncTask>, syncTask: SyncTask? = null) {
    val iterator = queue.iterator()
    while (iterator.hasNext()) {
      val item = iterator.next()
      if (cls.isInstance(item)) {
        if (syncTask == null || (item.requestCode == syncTask.requestCode && item.ownerKey == syncTask.ownerKey)) {
          item.isCancelled = true
          iterator.remove()
          //todo-denbond7 Need to improve this code to use an account. it will help us to manage accounts requests
          syncTask ?: continue
          syncListener.onActionCanceled(null, syncTask.ownerKey, syncTask.requestCode, -1)
        }
      }
    }
  }

  fun loadNewMsgs(ownerKey: String, requestCode: Int, localFolder: LocalFolder) {
    try {
      removeOldTasks(CheckNewMessagesSyncTask::class.java, tasksQueue)
      tasksQueue.put(CheckNewMessagesSyncTask(ownerKey, requestCode, localFolder))
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  fun loadNextMsgs(ownerKey: String, requestCode: Int, localFolder: LocalFolder, alreadyLoadedMsgsCount: Int) {
    try {
      syncListener.onActionProgress(null, ownerKey, requestCode, R.id.progress_id_adding_task_to_queue)
      removeOldTasks(LoadMessagesToCacheSyncTask::class.java, tasksQueue)
      tasksQueue.put(LoadMessagesToCacheSyncTask(ownerKey, requestCode, localFolder, alreadyLoadedMsgsCount))
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  fun refreshMsgs(ownerKey: String, requestCode: Int, localFolder: LocalFolder) {
    try {
      val syncTaskBlockingQueue = tasksQueue
      val task = RefreshMessagesSyncTask(ownerKey, requestCode, localFolder)
      removeOldTasks(RefreshMessagesSyncTask::class.java, syncTaskBlockingQueue, task)
      syncTaskBlockingQueue.put(task)
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  fun loadPrivateKeys(ownerKey: String, requestCode: Int) {
    try {
      tasksQueue.put(LoadPrivateKeysFromEmailBackupSyncTask(ownerKey, requestCode))
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  fun sendMsgWithBackup(ownerKey: String, requestCode: Int) {
    try {
      tasksQueue.put(SendMessageWithBackupToKeyOwnerSynsTask(ownerKey, requestCode))
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  fun searchMsgs(ownerKey: String, requestCode: Int, localFolder: LocalFolder, alreadyLoadedMsgsCount: Int) {
    try {
      syncListener.onActionProgress(null, ownerKey, requestCode, R.id.progress_id_adding_task_to_queue)
      removeOldTasks(SearchMessagesSyncTask::class.java, tasksQueue)
      tasksQueue.put(SearchMessagesSyncTask(ownerKey, requestCode, localFolder, alreadyLoadedMsgsCount))
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  fun cancelTask(uniqueId: String) {
    val future = tasksMap[uniqueId]
    if (future?.isDone == false) {
      future.cancel(true)
    }
  }

  private fun loadContactsInfoIfNeeded() {
    LoadContactsSyncTask.enqueue(syncListener.context)
  }

  private fun runSyncTask(accountEntity: AccountEntity, task: SyncTask?, isRetryEnabled: Boolean, isResetConnectionNeeded: Boolean) {
    task?.let {
      try {
        syncListener.onActionProgress(accountEntity, task.ownerKey, task.requestCode, R.id.progress_id_running_task)

        resetConnIfNeeded(accountEntity, isResetConnectionNeeded, task)

        if (sess == null || store == null) {
          syncListener.onActionProgress(accountEntity, task.ownerKey, task.requestCode, R.id.progress_id_connecting_to_email_server)
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
