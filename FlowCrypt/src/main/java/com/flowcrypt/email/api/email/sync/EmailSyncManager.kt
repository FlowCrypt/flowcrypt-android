/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
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
import com.flowcrypt.email.jetpack.workmanager.ForwardedAttachmentsDownloaderWorker
import com.flowcrypt.email.jetpack.workmanager.MessagesSenderWorker
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

    //run the tasks which maybe not completed last time
    archiveMsgs()
    changeMsgsReadState()
    deleteMsgs()
    deleteMsgs(deletePermanently = true)
    moveMsgsToINBOX()

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
   * Delete marked messages.
   *
   * @param ownerKey    The name of the reply to [android.os.Messenger].
   * @param requestCode The unique request code for the reply to [android.os.Messenger].
   * @param deletePermanently if true we will delete messages permanently
   */
  fun deleteMsgs(ownerKey: String = "", requestCode: Int = -1, deletePermanently: Boolean = false) {
    connectionRunnable.deleteMsgs(ownerKey, requestCode, deletePermanently)
  }

  /**
   * Archive marked messages.
   *
   * @param ownerKey    The name of the reply to [android.os.Messenger].
   * @param requestCode The unique request code for the reply to [android.os.Messenger].
   */
  fun archiveMsgs(ownerKey: String = "", requestCode: Int = -1) {

  }

  /**
   * Change messages read state
   *
   * @param ownerKey    The name of the reply to [android.os.Messenger].
   * @param requestCode The unique request code for the reply to [android.os.Messenger].
   */
  fun changeMsgsReadState(ownerKey: String = "", requestCode: Int = -1) {

  }

  /**
   * Move messages back to INBOX
   *
   * @param ownerKey    The name of the reply to [android.os.Messenger].
   * @param requestCode The unique request code for the reply to [android.os.Messenger].
   */
  fun moveMsgsToINBOX(ownerKey: String = "", requestCode: Int = -1) {
    connectionRunnable.moveMsgsToINBOX(ownerKey, requestCode)
  }

  /**
   * Run update a folders list.
   *
   * @param ownerKey       The name of the reply to [android.os.Messenger].
   * @param requestCode    The unique request code for the reply to [android.os.Messenger]
   */
  fun updateLabels(ownerKey: String, requestCode: Int) {
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
   * @param uniqueId    The task unique id.
   * @param localFolder The local implementation of the remote localFolder.
   * @param uid         The [com.sun.mail.imap.protocol.UID] of [Message]
   * @param id          A unique id of the row in the local database which identifies a message
   */

  fun loadMsgDetails(ownerKey: String, requestCode: Int, uniqueId: String, localFolder: LocalFolder,
                     uid: Int, id: Int, resetConnection: Boolean) {
    connectionRunnable.loadMsgDetails(ownerKey, requestCode, uniqueId, localFolder, uid, id,
        resetConnection)
  }

  /**
   * Cancel loading details of some message
   *
   */
  fun cancelLoadMsgDetails(uniqueId: String) {
    connectionRunnable.cancelTask(uniqueId)
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

  /**
   * Empty trash
   *
   * @param ownerKey    The name of the reply to [android.os.Messenger].
   * @param requestCode The unique request code for the reply to [android.os.Messenger].
   */
  fun emptyTrash(ownerKey: String = "", requestCode: Int = -1) {
    connectionRunnable.emptyTrash(ownerKey, requestCode)
  }

  companion object {
    private val TAG = EmailSyncManager::class.java.simpleName
  }
}
