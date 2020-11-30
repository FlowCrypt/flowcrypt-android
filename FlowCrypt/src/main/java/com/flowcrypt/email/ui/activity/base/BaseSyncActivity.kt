/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import androidx.test.espresso.idling.CountingIdlingResource
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.shutdown
import com.flowcrypt.email.jetpack.workmanager.sync.ArchiveMsgsSyncTask
import com.flowcrypt.email.jetpack.workmanager.sync.ChangeMsgsReadStateSyncTask
import com.flowcrypt.email.jetpack.workmanager.sync.DeleteMessagesPermanentlySyncTask
import com.flowcrypt.email.jetpack.workmanager.sync.DeleteMessagesSyncTask
import com.flowcrypt.email.jetpack.workmanager.sync.EmptyTrashSyncTask
import com.flowcrypt.email.jetpack.workmanager.sync.MovingToInboxSyncTask
import com.flowcrypt.email.jetpack.workmanager.sync.UpdateLabelsSyncTask
import com.flowcrypt.email.service.BaseService
import com.flowcrypt.email.service.EmailSyncService
import com.flowcrypt.email.ui.activity.BaseNodeActivity
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil

/**
 * This class describes a bind to the email sync service logic.
 *
 * @author DenBond7
 * Date: 16.06.2017
 * Time: 11:30
 * E-mail: DenBond7@gmail.com
 */
//todo-denbond7 need to refactor this class, too many duplicate code
abstract class BaseSyncActivity : BaseNodeActivity() {
  // Messengers for communicating with the service.
  protected var syncMessenger: Messenger? = null
  protected val syncReplyMessenger: Messenger = Messenger(ReplyHandler(this))

  val syncServiceCountingIdlingResource: CountingIdlingResource = CountingIdlingResource(GeneralUtil.genIdlingResourcesName(this::class.java), GeneralUtil.isDebugBuild())

  /**
   * Flag indicating whether we have called bind on the [EmailSyncService].
   */
  @JvmField
  var isSyncServiceBound: Boolean = false

  private val syncConn = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName, service: IBinder) {
      LogsUtil.d(tag, "Activity connected to " + name.className)
      syncMessenger = Messenger(service)
      isSyncServiceBound = true

      registerReplyMessenger(EmailSyncService.MESSAGE_ADD_REPLY_MESSENGER, syncMessenger!!, syncReplyMessenger)
      onSyncServiceConnected()
    }

    override fun onServiceDisconnected(name: ComponentName) {
      LogsUtil.d(tag, "Activity disconnected from " + name.className)
      syncMessenger = null
      isSyncServiceBound = false
    }
  }

  /**
   * Check is a sync enable.
   *
   * @return true - if sync enable, false - otherwise.
   */
  abstract val isSyncEnabled: Boolean

  abstract fun onSyncServiceConnected()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (isSyncEnabled) {
      bindService(EmailSyncService::class.java, syncConn)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    disconnectFromSyncService()
    syncServiceCountingIdlingResource.shutdown()
  }

  override fun onReplyReceived(requestCode: Int, resultCode: Int, obj: Any?) {
    syncServiceCountingIdlingResource.decrementSafely(requestCode.toString())
  }

  override fun onProgressReplyReceived(requestCode: Int, resultCode: Int, obj: Any?) {

  }

  override fun onErrorHappened(requestCode: Int, errorType: Int, e: Exception) {
    syncServiceCountingIdlingResource.decrementSafely(requestCode.toString())
  }

  override fun onCanceled(requestCode: Int, resultCode: Int, obj: Any?) {
    syncServiceCountingIdlingResource.decrementSafely(requestCode.toString())
  }

  protected fun disconnectFromSyncService() {
    if (isSyncEnabled && isSyncServiceBound) {
      if (syncMessenger != null) {
        unregisterReplyMessenger(EmailSyncService.MESSAGE_REMOVE_REPLY_MESSENGER, syncMessenger!!, syncReplyMessenger)
      }

      unbindService(EmailSyncService::class.java, syncConn)
      isSyncServiceBound = false
    }
  }

  /**
   * Send a message with a backup to the key owner.
   *
   * @param requestCode The unique request code for identify the current action.
   */
  fun sendMsgWithPrivateKeyBackup(requestCode: Int) {
    if (checkServiceBound(isSyncServiceBound)) return
    syncServiceCountingIdlingResource.incrementSafely(requestCode.toString())

    val action = BaseService.Action(replyMessengerName, requestCode, null)

    val msg = Message.obtain(null, EmailSyncService.MESSAGE_SEND_MESSAGE_WITH_BACKUP, action)
    msg.replyTo = syncReplyMessenger
    try {
      syncMessenger?.send(msg)
    } catch (e: RemoteException) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }
  }

  /**
   * Load the user private keys.
   *
   * @param requestCode The unique request code for identify the current action.
   */
  fun loadPrivateKeys(requestCode: Int) {
    if (checkServiceBound(isSyncServiceBound)) return
    syncServiceCountingIdlingResource.incrementSafely(requestCode.toString())

    try {
      val action = BaseService.Action(replyMessengerName, requestCode, null)

      val msg = Message.obtain(null, EmailSyncService.MESSAGE_LOAD_PRIVATE_KEYS, action)
      msg.replyTo = syncReplyMessenger

      syncMessenger?.send(msg)
    } catch (e: RemoteException) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }
  }

  /**
   * Start a job to load message to cache.
   *
   * @param requestCode            The unique request code for identify the current action.
   * @param localFolder            [LocalFolder] object.
   * @param alreadyLoadedMsgsCount The count of already loaded messages in the localFolder.
   */
  open fun loadNextMsgs(requestCode: Int, localFolder: LocalFolder, alreadyLoadedMsgsCount: Int) {
    if (checkServiceBound(isSyncServiceBound)) return
    syncServiceCountingIdlingResource.incrementSafely(requestCode.toString())

    onProgressReplyReceived(requestCode, R.id.progress_id_start_of_loading_new_messages, Any())

    val action = BaseService.Action(replyMessengerName, requestCode, localFolder)

    val msg = Message.obtain(null, EmailSyncService.MESSAGE_LOAD_NEXT_MESSAGES, alreadyLoadedMsgsCount, 0, action)
    msg.replyTo = syncReplyMessenger
    try {
      syncMessenger?.send(msg)
    } catch (e: RemoteException) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }
  }

  /**
   * Start a job to load searched messages to the cache.
   *
   * @param requestCode            The unique request code for identify the current action.
   * @param localFolder            [LocalFolder] object which contains the search query.
   * @param alreadyLoadedMsgsCount The count of already loaded messages in the localFolder.
   */
  fun searchNextMsgs(requestCode: Int, localFolder: LocalFolder, alreadyLoadedMsgsCount: Int) {
    if (checkServiceBound(isSyncServiceBound)) return
    onProgressReplyReceived(requestCode, R.id.progress_id_start_of_loading_new_messages, Any())
    syncServiceCountingIdlingResource.incrementSafely(requestCode.toString())

    val action = BaseService.Action(replyMessengerName, requestCode, localFolder)

    val msg = Message.obtain(null, EmailSyncService.MESSAGE_SEARCH_MESSAGES, alreadyLoadedMsgsCount, 0, action)
    msg.replyTo = syncReplyMessenger
    try {
      syncMessenger?.send(msg)
    } catch (e: RemoteException) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }
  }

  /**
   * Run update a folders list.
   */
  fun updateLabels() {
    UpdateLabelsSyncTask.enqueue(this)
  }

  /**
   * Delete marked messages
   */
  fun deleteMsgs(deletePermanently: Boolean = false) {
    if (deletePermanently) {
      DeleteMessagesPermanentlySyncTask.enqueue(this)
    } else {
      DeleteMessagesSyncTask.enqueue(this)
    }
  }

  /**
   * Empty trash
   */
  fun emptyTrash() {
    EmptyTrashSyncTask.enqueue(this)
  }

  /**
   * Archive marked messages
   */
  fun archiveMsgs() {
    ArchiveMsgsSyncTask.enqueue(this)
  }

  /**
   * Change messages read state.
   */
  fun changeMsgsReadState() {
    ChangeMsgsReadStateSyncTask.enqueue(this)
  }

  /**
   * Move messages back to inbox
   *
   */
  fun moveMsgsToINBOX() {
    MovingToInboxSyncTask.enqueue(this)
  }

  /**
   * Load the last messages which not exist in the database.
   *
   * @param requestCode        The unique request code for identify the current action.
   * @param currentLocalFolder [LocalFolder] object.
   */
  open fun refreshMsgs(requestCode: Int, currentLocalFolder: LocalFolder) {
    if (checkServiceBound(isSyncServiceBound)) return
    syncServiceCountingIdlingResource.incrementSafely(requestCode.toString())

    val action = BaseService.Action(replyMessengerName, requestCode, currentLocalFolder)

    val msg = Message.obtain(null, EmailSyncService.MESSAGE_REFRESH_MESSAGES, action)
    msg.replyTo = syncReplyMessenger
    try {
      syncMessenger?.send(msg)
    } catch (e: RemoteException) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }
  }

  /**
   * Cancel a job which load the current message details
   *
   * @param uniqueId    The task unique id. This parameter helps identify which tasks should be
   * stopped
   */
  fun cancelLoadMsgDetails(uniqueId: String) {
    if (checkServiceBound(isSyncServiceBound)) return
    syncServiceCountingIdlingResource.incrementSafely()

    val action = BaseService.Action(replyMessengerName, -1, null, false, uniqueId)
    val msg = Message.obtain(null, EmailSyncService.MESSAGE_CANCEL_LOAD_MESSAGE_DETAILS, action)
    msg.replyTo = syncReplyMessenger
    try {
      syncMessenger?.send(msg)
    } catch (e: RemoteException) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }
  }

  /**
   * Start a job to load attachments info.
   *
   * @param requestCode The unique request code for identify the current action.
   * @param localFolder [LocalFolder] object.
   * @param uid         The [com.sun.mail.imap.protocol.UID] of [javax.mail.Message]
   */
  fun loadAttsInfo(requestCode: Int, localFolder: LocalFolder, uid: Int) {
    if (checkServiceBound(isSyncServiceBound)) return
    syncServiceCountingIdlingResource.incrementSafely(requestCode.toString())

    val action = BaseService.Action(replyMessengerName, requestCode, localFolder)

    val msg = Message.obtain(null, EmailSyncService.MESSAGE_LOAD_ATTS_INFO, uid, 0, action)
    msg.replyTo = syncReplyMessenger
    try {
      syncMessenger?.send(msg)
    } catch (e: RemoteException) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }
  }
}
