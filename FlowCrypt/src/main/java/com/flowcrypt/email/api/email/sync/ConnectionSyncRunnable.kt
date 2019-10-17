/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync

import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.sync.tasks.CheckIsLoadedMessagesEncryptedSyncTask
import com.flowcrypt.email.api.email.sync.tasks.CheckNewMessagesSyncTask
import com.flowcrypt.email.api.email.sync.tasks.LoadAttsInfoSyncTask
import com.flowcrypt.email.api.email.sync.tasks.LoadContactsSyncTask
import com.flowcrypt.email.api.email.sync.tasks.LoadMessageDetailsSyncTask
import com.flowcrypt.email.api.email.sync.tasks.LoadMessagesSyncTask
import com.flowcrypt.email.api.email.sync.tasks.LoadMessagesToCacheSyncTask
import com.flowcrypt.email.api.email.sync.tasks.LoadPrivateKeysFromEmailBackupSyncTask
import com.flowcrypt.email.api.email.sync.tasks.MoveMessagesSyncTask
import com.flowcrypt.email.api.email.sync.tasks.RefreshMessagesSyncTask
import com.flowcrypt.email.api.email.sync.tasks.SearchMessagesSyncTask
import com.flowcrypt.email.api.email.sync.tasks.SendMessageWithBackupToKeyOwnerSynsTask
import com.flowcrypt.email.api.email.sync.tasks.SyncTask
import com.flowcrypt.email.api.email.sync.tasks.UpdateLabelsSyncTask
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.sun.mail.iap.ConnectionException
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

/**
 * @author Denis Bondarenko
 *         Date: 10/17/19
 *         Time: 3:36 PM
 *         E-mail: DenBond7@gmail.com
 */
class ConnectionSyncRunnable(account: AccountDao, syncListener: SyncListener)
  : BaseSyncRunnable(account, syncListener) {
  private val tasksQueue: BlockingQueue<SyncTask> = LinkedBlockingQueue()
  private val tasksExecutorService: ExecutorService = Executors.newFixedThreadPool(MAX_RUNNING_TASKS_COUNT)

  init {
    updateLabels("", 0)
    loadContactsInfoIfNeeded()
  }

  override fun run() {
    LogsUtil.d(tag, " run!")
    Thread.currentThread().name = javaClass.simpleName
    while (!Thread.interrupted()) {
      try {
        LogsUtil.d(tag, "TasksQueue size = " + tasksQueue.size)
        runSyncTask(tasksQueue.take(), true)
      } catch (e: InterruptedException) {
        e.printStackTrace()
      }
    }

    closeConn()
    LogsUtil.d(tag, " stopped!")
  }

  private fun removeOldTasks(cls: Class<*>, queue: BlockingQueue<SyncTask>) {
    val iterator = queue.iterator()
    while (iterator.hasNext()) {
      val item = iterator.next()
      if (cls.isInstance(item)) {
        item.isCancelled = true
        iterator.remove()
      }
    }
  }

  fun updateLabels(ownerKey: String, requestCode: Int) {
    try {
      removeOldTasks(UpdateLabelsSyncTask::class.java, tasksQueue)
      tasksQueue.put(UpdateLabelsSyncTask(ownerKey, requestCode))
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  fun loadMsgs(ownerKey: String, requestCode: Int, localFolder: LocalFolder, start: Int, end: Int) {
    try {
      tasksQueue.put(LoadMessagesSyncTask(ownerKey, requestCode, localFolder, start, end))
    } catch (e: InterruptedException) {
      e.printStackTrace()
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

  fun loadMsgDetails(ownerKey: String, requestCode: Int, localFolder: LocalFolder, uid: Int, id: Int,
                     resetConnection: Boolean) {
    try {
      removeOldTasks(LoadMessageDetailsSyncTask::class.java, tasksQueue)
      tasksQueue.put(LoadMessageDetailsSyncTask(ownerKey, requestCode, localFolder, uid.toLong(), id.toLong(),
          resetConnection))

    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  fun loadAttsInfo(ownerKey: String, requestCode: Int, localFolder: LocalFolder, uid: Int) {
    try {
      removeOldTasks(LoadAttsInfoSyncTask::class.java, tasksQueue)
      tasksQueue.put(LoadAttsInfoSyncTask(ownerKey, requestCode, localFolder, uid.toLong()))
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  fun loadNextMsgs(ownerKey: String, requestCode: Int, localFolder: LocalFolder, alreadyLoadedMsgsCount: Int) {
    try {
      syncListener.onActionProgress(account, ownerKey, requestCode, R.id.progress_id_adding_task_to_queue)
      removeOldTasks(LoadMessagesToCacheSyncTask::class.java, tasksQueue)
      tasksQueue.put(LoadMessagesToCacheSyncTask(ownerKey, requestCode, localFolder, alreadyLoadedMsgsCount))
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  fun refreshMsgs(ownerKey: String, requestCode: Int, localFolder: LocalFolder) {
    try {
      val syncTaskBlockingQueue = tasksQueue
      removeOldTasks(RefreshMessagesSyncTask::class.java, syncTaskBlockingQueue)
      syncTaskBlockingQueue.put(RefreshMessagesSyncTask(ownerKey, requestCode, localFolder))
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  fun moveMsg(ownerKey: String, requestCode: Int, srcFolder: LocalFolder, destFolder: LocalFolder, uid: Int) {
    try {
      tasksQueue.put(MoveMessagesSyncTask(ownerKey, requestCode, srcFolder, destFolder, longArrayOf(uid.toLong())))
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

  fun identifyEncryptedMsgs(ownerKey: String, requestCode: Int, localFolder: LocalFolder) {
    try {
      removeOldTasks(CheckIsLoadedMessagesEncryptedSyncTask::class.java, tasksQueue)
      tasksQueue.put(CheckIsLoadedMessagesEncryptedSyncTask(ownerKey, requestCode, localFolder))
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  fun searchMsgs(ownerKey: String, requestCode: Int, localFolder: LocalFolder, alreadyLoadedMsgsCount: Int) {
    try {
      removeOldTasks(SearchMessagesSyncTask::class.java, tasksQueue)
      tasksQueue.put(SearchMessagesSyncTask(ownerKey, requestCode, localFolder, alreadyLoadedMsgsCount))
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  private fun loadContactsInfoIfNeeded() {
    if (!account.areContactsLoaded) {
      //we need to update labels before we can use the SENT folder for retrieve contacts
      updateLabels("", 0)
      try {
        tasksQueue.put(LoadContactsSyncTask())
      } catch (e: InterruptedException) {
        e.printStackTrace()
      }
    }
  }

  private fun runSyncTask(task: SyncTask?, isRetryEnabled: Boolean) {

    task?.let {
      try {
        syncListener.onActionProgress(account, task.ownerKey, task.requestCode, R.id.progress_id_running_task)

        resetConnIfNeeded(task)

        if (!isConnected) {
          LogsUtil.d(tag, "Not connected. Start a reconnection ...")
          syncListener.onActionProgress(account, task.ownerKey, task.requestCode, R.id.progress_id_connecting_to_email_server)
          openConnToStore()
          LogsUtil.d(tag, "Reconnection done")
        }

        val activeStore = store ?: return
        val activeSess = sess ?: return
        tasksExecutorService.execute(SyncTaskRunnable(account, syncListener, task, activeStore, activeSess))

      } catch (e: Exception) {
        e.printStackTrace()
        if (e is ConnectionException) {
          if (isRetryEnabled) {
            runSyncTask(task, false)
          } else {
            ExceptionUtil.handleError(e)
            task.handleException(account, e, syncListener)
          }
        } else {
          ExceptionUtil.handleError(e)
          task.handleException(account, e, syncListener)
        }
      }
    }
  }

  companion object {
    private const val MAX_RUNNING_TASKS_COUNT = 5
  }
}
