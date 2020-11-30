/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync

import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.sync.tasks.CheckNewMessagesSyncTask
import com.flowcrypt.email.api.email.sync.tasks.LoadMessagesToCacheSyncTask
import com.flowcrypt.email.api.email.sync.tasks.RefreshMessagesSyncTask
import com.flowcrypt.email.api.email.sync.tasks.SearchMessagesSyncTask
import com.flowcrypt.email.jetpack.workmanager.ForwardedAttachmentsDownloaderWorker
import com.flowcrypt.email.jetpack.workmanager.MessagesSenderWorker
import com.flowcrypt.email.util.LogsUtil
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import javax.mail.Store

/**
 * This class describes a logic of work with [Store] for the single account. Via
 * this class we can retrieve a new information from the server and send a data to the server.
 * Here we open a new connection to the [Store] and keep it alive. This class does
 * all job to communicate with IMAP server.
 *
 * @author DenBond7
 * Date: 14.06.2017
 * Time: 10:31
 * E-mail: DenBond7@gmail.com
 */
class EmailSyncManager(val listener: SyncListener) {
  private val connectionExecutorService: ExecutorService = Executors.newSingleThreadExecutor()
  private val idleExecutorService: ExecutorService = Executors.newSingleThreadExecutor()
  private var connectionFuture: Future<*>? = null
  private var idleFuture: Future<*>? = null
  private val connectionRunnable = ConnectionSyncRunnable(listener)
  private var idleSyncRunnable: IdleSyncRunnable? = null

  /**
   * Start a synchronization.
   */
  fun beginSync() {
    LogsUtil.d(TAG, "beginSync")

    if (!isThreadAlreadyWorking(connectionFuture)) {
      connectionFuture = connectionExecutorService.submit(connectionRunnable)
    }

    runIdleInboxIfNeeded()

    ForwardedAttachmentsDownloaderWorker.enqueue(listener.context)
    MessagesSenderWorker.enqueue(listener.context)
  }

  /**
   * Stop a synchronization.
   */
  fun stopSync() {
    connectionFuture?.cancel(true)
    idleFuture?.cancel(true)
    idleSyncRunnable?.interruptIdle()
    connectionExecutorService.shutdown()
    idleExecutorService.shutdown()
  }

  /**
   * Run a thread where we will idle INBOX folder.
   */
  private fun runIdleInboxIfNeeded() {
    if (!isThreadAlreadyWorking(idleFuture)) {
      idleSyncRunnable = IdleSyncRunnable(listener, this)
      idleSyncRunnable?.let {
        idleFuture = idleExecutorService.submit(it)
      }
    }
  }

  /**
   * Check a sync thread state.
   *
   * @return true if already work, otherwise false.
   */
  private fun isThreadAlreadyWorking(future: Future<*>?): Boolean {
    return future != null && !future.isCancelled && !future.isDone
  }

  /**
   * Start loading new messages to the local cache. This method create a new
   * [CheckNewMessagesSyncTask] object and add it to the passive BlockingQueue.
   *
   * @param ownerKey    The name of the reply to [android.os.Messenger].
   * @param requestCode The unique request code for the reply to [android.os.Messenger].
   * @param localFolder A local implementation of the remote localFolder.
   */
  fun loadNewMsgs(ownerKey: String, requestCode: Int, localFolder: LocalFolder) {
    connectionRunnable.loadNewMsgs(ownerKey, requestCode, localFolder)
  }

  /**
   * Cancel loading details of some message
   *
   */
  fun cancelLoadMsgDetails(uniqueId: String) {
    connectionRunnable.cancelTask(uniqueId)
  }

  /**
   * Add the task of load information of the next messages. This method create a new
   * [LoadMessagesToCacheSyncTask] object and added it to the current synchronization
   * BlockingQueue.
   *
   * @param ownerKey               The name of the reply to [android.os.Messenger].
   * @param requestCode            The unique request code for the reply to
   * [android.os.Messenger].
   * @param localFolder            A local implementation of the remote localFolder.
   * @param alreadyLoadedMsgsCount The count of already cached messages in the localFolder.
   */
  fun loadNextMsgs(ownerKey: String, requestCode: Int, localFolder: LocalFolder, alreadyLoadedMsgsCount: Int) {
    connectionRunnable.loadNextMsgs(ownerKey, requestCode, localFolder, alreadyLoadedMsgsCount)
  }

  /**
   * Add load a new messages information task. This method create a new
   * [RefreshMessagesSyncTask] object and added it to the current synchronization
   * BlockingQueue.
   *
   * @param ownerKey          The name of the reply to [android.os.Messenger].
   * @param requestCode       The unique request code for the reply to [android.os.Messenger].
   * @param localFolder       A local implementation of the remote localFolder.
   */
  fun refreshMsgs(ownerKey: String, requestCode: Int, localFolder: LocalFolder) {
    connectionRunnable.refreshMsgs(ownerKey, requestCode, localFolder)
  }

  /**
   * Add the task of load information of the next searched messages. This method create a new
   * [SearchMessagesSyncTask] object and added it to the current synchronization
   * BlockingQueue.
   *
   * @param ownerKey               The name of the reply to [android.os.Messenger].
   * @param requestCode            The unique request code for the reply to [android.os.Messenger].
   * @param localFolder            A localFolder where we do a search.
   * @param alreadyLoadedMsgsCount The count of already cached messages in the database.
   */
  fun searchMsgs(ownerKey: String, requestCode: Int, localFolder: LocalFolder, alreadyLoadedMsgsCount: Int) {
    connectionRunnable.searchMsgs(ownerKey, requestCode, localFolder, alreadyLoadedMsgsCount)
  }

  companion object {
    private val TAG = EmailSyncManager::class.java.simpleName
  }
}
