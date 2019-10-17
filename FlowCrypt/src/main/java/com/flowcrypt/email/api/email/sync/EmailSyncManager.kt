/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync

import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.sync.tasks.CheckNewMessagesSyncTask
import com.flowcrypt.email.api.email.sync.tasks.LoadAttsInfoSyncTask
import com.flowcrypt.email.api.email.sync.tasks.LoadMessagesSyncTask
import com.flowcrypt.email.api.email.sync.tasks.LoadMessagesToCacheSyncTask
import com.flowcrypt.email.api.email.sync.tasks.RefreshMessagesSyncTask
import com.flowcrypt.email.api.email.sync.tasks.SearchMessagesSyncTask
import com.flowcrypt.email.api.email.sync.tasks.UpdateLabelsSyncTask
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.jobscheduler.ForwardedAttachmentsDownloaderJobService
import com.flowcrypt.email.jobscheduler.MessagesSenderJobService
import com.flowcrypt.email.util.LogsUtil
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import javax.mail.Message
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

class EmailSyncManager(val account: AccountDao, val listener: SyncListener) {
  private val connectionExecutorService: ExecutorService = Executors.newSingleThreadExecutor()
  private val idleExecutorService: ExecutorService = Executors.newSingleThreadExecutor()
  private var connectionFuture: Future<*>? = null
  private var idleFuture: Future<*>? = null
  private val connectionRunnable = ConnectionSyncRunnable(account, listener)

  /**
   * Start a synchronization.
   */
  fun beginSync() {
    LogsUtil.d(TAG, "beginSync")

    if (!isThreadAlreadyWorking(connectionFuture)) {
      connectionFuture = connectionExecutorService.submit(connectionRunnable)
    }

    runIdleInboxIfNeeded()

    ForwardedAttachmentsDownloaderJobService.schedule(listener.context)
    MessagesSenderJobService.schedule(listener.context)
  }

  /**
   * Stop a synchronization.
   */
  fun stopSync() {
    connectionFuture?.cancel(true)
    idleFuture?.cancel(true)
    connectionExecutorService.shutdown()
    idleExecutorService.shutdown()
  }

  /**
   * Run a thread where we will idle INBOX folder.
   */
  private fun runIdleInboxIfNeeded() {
    if (!isThreadAlreadyWorking(idleFuture)) {
      idleFuture = idleExecutorService.submit(IdleSyncRunnable(account, listener, this))
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
   * Run update a folders list.
   *
   * @param ownerKey    The name of the reply to [android.os.Messenger].
   * @param requestCode The unique request code for the reply to [android.os.Messenger].
   * @param queue       The queue where [UpdateLabelsSyncTask] will be run.
   */
  fun updateLabels(ownerKey: String, requestCode: Int) {
    connectionRunnable.updateLabels(ownerKey, requestCode)
  }

  /**
   * Run update a folders list.
   *
   * @param ownerKey       The name of the reply to [android.os.Messenger].
   * @param requestCode    The unique request code for the reply to [android.os.Messenger].
   * @param isInBackground if true we will run this task using the passive queue, else we will use the active queue.
   */
  fun updateLabels(ownerKey: String, requestCode: Int, isInBackground: Boolean) {
    connectionRunnable.updateLabels(ownerKey, requestCode)
  }

  /**
   * Add load a messages information task. This method create a new
   * [LoadMessagesSyncTask] object and added it to the current synchronization
   * BlockingQueue.
   *
   * @param ownerKey    The name of the reply to [android.os.Messenger].
   * @param requestCode The unique request code for the reply to [android.os.Messenger].
   * @param localFolder A local implementation of the remote localFolder.
   * @param start       The position of the start.
   * @param end         The position of the end.
   */
  fun loadMsgs(ownerKey: String, requestCode: Int, localFolder: LocalFolder, start: Int, end: Int) {
    connectionRunnable.loadMsgs(ownerKey, requestCode, localFolder, start, end)
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
   * Add load a messages information task. This method create a new
   * [LoadMessagesSyncTask] object and added it to the current synchronization
   * BlockingQueue.
   *
   * @param ownerKey    The name of the reply to [android.os.Messenger].
   * @param requestCode The unique request code for the reply to [android.os.Messenger].
   * @param localFolder The local implementation of the remote localFolder.
   * @param uid         The [com.sun.mail.imap.protocol.UID] of [Message]
   * @param id          A unique id of the row in the local database which identifies a message
   */

  fun loadMsgDetails(ownerKey: String, requestCode: Int, localFolder: LocalFolder, uid: Int, id: Int,
                     resetConnection: Boolean) {
    connectionRunnable.loadMsgDetails(ownerKey, requestCode, localFolder, uid, id, resetConnection)
  }

  /**
   * This method create a new [LoadAttsInfoSyncTask] object and added it to the current synchronization [BlockingQueue].
   *
   * @param ownerKey    The name of the reply to [android.os.Messenger].
   * @param requestCode The unique request code for the reply to [android.os.Messenger].
   * @param localFolder The local implementation of the remote localFolder.
   * @param uid         The [com.sun.mail.imap.protocol.UID] of [).][Message]
   */
  fun loadAttsInfo(ownerKey: String, requestCode: Int, localFolder: LocalFolder, uid: Int) {
    connectionRunnable.loadAttsInfo(ownerKey, requestCode, localFolder, uid)
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
   * Move the message to an another folder.
   *
   * @param ownerKey    The name of the reply to [android.os.Messenger].
   * @param requestCode The unique request code for identify the current action.
   * @param srcFolder   A local implementation of the remote folder which is the source.
   * @param destFolder  A local implementation of the remote folder which is the destination.
   * @param uid         The [com.sun.mail.imap.protocol.UID] of [                    .Message ).][javax.mail]
   */
  fun moveMsg(ownerKey: String, requestCode: Int, srcFolder: LocalFolder, destFolder: LocalFolder, uid: Int) {
    connectionRunnable.moveMsg(ownerKey, requestCode, srcFolder, destFolder, uid)
  }

  /**
   * Load the private keys from the INBOX folder.
   *
   * @param ownerKey    The name of the reply to [android.os.Messenger].
   * @param requestCode The unique request code for identify the current action.
   */
  fun loadPrivateKeys(ownerKey: String, requestCode: Int) {
    connectionRunnable.loadPrivateKeys(ownerKey, requestCode)
  }

  /**
   * Send a message with a backup to the key owner.
   *
   * @param ownerKey    The name of the reply to [android.os.Messenger].
   * @param requestCode The unique request code for identify the current action.
   */
  fun sendMsgWithBackup(ownerKey: String, requestCode: Int) {
    connectionRunnable.sendMsgWithBackup(ownerKey, requestCode)
  }

  /**
   * Identify encrypted messages.
   *
   * @param ownerKey    The name of the reply to [android.os.Messenger].
   * @param requestCode The unique request code for identify the current action.
   * @param localFolder The local implementation of the remote folder
   */
  fun identifyEncryptedMsgs(ownerKey: String, requestCode: Int, localFolder: LocalFolder) {
    connectionRunnable.identifyEncryptedMsgs(ownerKey, requestCode, localFolder)
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
    private const val MAX_RUNNING_TASKS_COUNT = 5
    private val TAG = EmailSyncManager::class.java.simpleName
  }
}
