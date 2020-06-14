/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.idling.CountingIdlingResource
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.MsgsCacheManager
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.sync.SyncErrorTypes
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AttachmentEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.jetpack.viewmodel.DecryptMessageViewModel
import com.flowcrypt.email.jetpack.viewmodel.MsgDetailsViewModel
import com.flowcrypt.email.jetpack.viewmodel.factory.MsgDetailsViewModelFactory
import com.flowcrypt.email.node.Node
import com.flowcrypt.email.service.EmailSyncService
import com.flowcrypt.email.ui.activity.base.BaseBackStackSyncActivity
import com.flowcrypt.email.ui.activity.fragment.MessageDetailsFragment
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.cache.DiskLruCache
import com.flowcrypt.email.util.exception.CorruptedMsgInCacheException
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.ManualHandledException
import com.flowcrypt.email.util.idling.SingleIdlingResources
import java.util.*

/**
 * This activity describe details of some message.
 *
 * @author DenBond7
 * Date: 03.05.2017
 * Time: 16:29
 * E-mail: DenBond7@gmail.com
 */
class MessageDetailsActivity : BaseBackStackSyncActivity(), MessageDetailsFragment.MessageDetailsListener {
  private lateinit var messageEntity: MessageEntity
  private lateinit var localFolder: LocalFolder
  private lateinit var msgDetailsViewModel: MsgDetailsViewModel
  private lateinit var decryptMsgViewModel: DecryptMessageViewModel
  private lateinit var label: String

  var idlingForDecryption: CountingIdlingResource? = null
    private set
  val idlingForWebView: SingleIdlingResources = SingleIdlingResources(false)

  private var isReceiveMsgBodyNeeded: Boolean = false
  private var isRequestMsgDetailsStarted: Boolean = false
  private var isRetrieveIncomingMsgNeeded = true
  private var rawMimeBytesOfOutgoingMsg: ByteArray? = null
  private var msgSnapshot: DiskLruCache.Snapshot? = null
  private val uniqueId = UUID.randomUUID().toString()

  override val rootView: View
    get() = View(this)

  override val contentViewResourceId: Int
    get() = R.layout.activity_message_details

  override fun onCreate(savedInstanceState: Bundle?) {
    initMsgDetailsViewModel()
    super.onCreate(savedInstanceState)
    setupDecryptMessageViewModel()

    idlingForDecryption = CountingIdlingResource(
        GeneralUtil.genIdlingResourcesName(MessageDetailsActivity::class.java), GeneralUtil.isDebugBuild())

    updateViews()
  }

  override fun onDestroy() {
    cancelLoadMsgDetails(uniqueId)
    super.onDestroy()
  }

  override fun onNodeStateChanged(nodeInitResult: Node.NodeInitResult) {
    super.onNodeStateChanged(nodeInitResult)
    if (nodeInitResult.isReady) {
      if (rawMimeBytesOfOutgoingMsg?.isNotEmpty() == true) {
        decryptMsg()
      }
    }
  }

  override fun onSyncServiceConnected() {
    super.onSyncServiceConnected()
    if (isReceiveMsgBodyNeeded) {
      loadMsgDetails()
    }
  }

  override fun onReplyReceived(requestCode: Int, resultCode: Int, obj: Any?) {
    when (requestCode) {
      R.id.syns_request_code_load_raw_mime_msg -> {
        isRequestMsgDetailsStarted = false
        when (resultCode) {
          EmailSyncService.REPLY_RESULT_CODE_ACTION_OK -> {
            msgDetailsViewModel.setSeenStatus(true)
          }

          EmailSyncService.REPLY_RESULT_CODE_ACTION_ERROR_MESSAGE_NOT_FOUND -> messageNotAvailableInFolder()
        }
      }

      R.id.syns_request_move_message_to_inbox -> {
        when (resultCode) {
          EmailSyncService.REPLY_RESULT_CODE_ACTION_ERROR_MESSAGE_NOT_EXISTS -> messageNotAvailableInFolder()
        }
      }
    }

    super.onReplyReceived(requestCode, resultCode, obj)
  }

  override fun onErrorHappened(requestCode: Int, errorType: Int, e: Exception) {
    when (requestCode) {
      R.id.syns_request_code_load_raw_mime_msg -> {
        isRequestMsgDetailsStarted = false
        updateActionProgressState(100, null)
        onErrorOccurred(requestCode, errorType, e)
      }

      else -> onErrorOccurred(requestCode, errorType, e)
    }

    super.onErrorHappened(requestCode, errorType, e)
  }

  override fun onProgressReplyReceived(requestCode: Int, resultCode: Int, obj: Any?) {
    when (requestCode) {
      R.id.syns_request_code_load_raw_mime_msg -> {
        val value = obj as? Int ?: -1
        when (resultCode) {
          R.id.progress_id_connecting -> updateActionProgressState(value, "Connecting")

          R.id.progress_id_fetching_message -> updateActionProgressState(value, "Fetching message")

          R.id.progress_id_processing -> updateActionProgressState(value, "Processing")

          R.id.progress_id_rendering -> updateActionProgressState(value, "Rendering")
        }
      }

      else -> super.onProgressReplyReceived(requestCode, resultCode, obj)
    }
  }

  override fun getMsgDetailsViewModel(): MsgDetailsViewModel? {
    return if (::msgDetailsViewModel.isInitialized) {
      msgDetailsViewModel
    } else null
  }

  fun decryptMsg() {
    idlingForDecryption?.incrementSafely()
    onProgressReplyReceived(R.id.syns_request_code_load_raw_mime_msg, R.id.progress_id_processing, 65)
    when {
      rawMimeBytesOfOutgoingMsg?.isNotEmpty() == true -> rawMimeBytesOfOutgoingMsg?.let { decryptMsgViewModel.decryptMessage(it) }
      msgSnapshot != null -> msgSnapshot?.let {
        decryptMsgViewModel.decryptMessage(this@MessageDetailsActivity, it)
      }
    }
    onProgressReplyReceived(R.id.syns_request_code_load_raw_mime_msg, R.id.progress_id_processing, 70)
  }

  fun loadMsgDetails() {
    if (!JavaEmailConstants.FOLDER_OUTBOX.equals(localFolder.fullName, ignoreCase = true)) {
      loadMsgDetails(R.id.syns_request_code_load_raw_mime_msg, uniqueId, localFolder, messageEntity
          .uid.toInt(), messageEntity.id?.toInt() ?: -1)
    }
  }

  private fun updateActionProgressState(progress: Int, message: String?) {
    val fragment = supportFragmentManager
        .findFragmentById(R.id.messageDetailsFragment) as MessageDetailsFragment?

    fragment?.setActionProgress(progress, message)
  }

  private fun showErrorInfo(apiError: ApiError?, e: Throwable?) {
    val fragment = supportFragmentManager
        .findFragmentById(R.id.messageDetailsFragment) as MessageDetailsFragment?

    fragment?.showErrorInfo(apiError, e)
  }

  private fun messageNotAvailableInFolder(showToast: Boolean = true) {
    msgDetailsViewModel.deleteMsg()
    if (showToast) {
      Toast.makeText(this, R.string.email_does_not_available_in_this_folder, Toast.LENGTH_LONG).show()
    }
    finish()
  }

  private fun genMsgObserver(): Observer<MessageEntity?> {
    return object : Observer<MessageEntity?> {
      private var isFirstCall = true

      override fun onChanged(it: MessageEntity?) {
        if (it != null) {
          if (JavaEmailConstants.FOLDER_OUTBOX.equals(messageEntity.folder, ignoreCase = true)) {
            rawMimeBytesOfOutgoingMsg = it.rawMessageWithoutAttachments?.toByteArray()
          } else {
            msgSnapshot = MsgsCacheManager.getMsgSnapshot(it.id.toString())
          }

          onMsgDetailsUpdated()

          if (rawMimeBytesOfOutgoingMsg?.isNotEmpty() == true || msgSnapshot != null) {
            if (isRetrieveIncomingMsgNeeded) {
              isRetrieveIncomingMsgNeeded = false
              isReceiveMsgBodyNeeded = false

              if (!JavaEmailConstants.FOLDER_OUTBOX.equals(messageEntity.folder, ignoreCase = true) && !messageEntity.isSeen) {
                msgDetailsViewModel.changeMsgState(MessageState.PENDING_MARK_READ)
                changeMsgsReadState()
              }

              decryptMsg()
            }
          } else {
            if (isSyncServiceBound && !isRequestMsgDetailsStarted) {
              isRequestMsgDetailsStarted = true
              loadMsgDetails()
            } else {
              isReceiveMsgBodyNeeded = true
            }
          }
        } else {
          messageNotAvailableInFolder(isFirstCall)
        }

        isFirstCall = false
      }
    }
  }

  private fun initMsgDetailsViewModel() {
    if (intent?.extras?.containsKey(EXTRA_KEY_FOLDER) == false
        || intent?.extras?.containsKey(EXTRA_KEY_MSG) == false) {
      finish()
    }

    this.localFolder = intent.getParcelableExtra(EXTRA_KEY_FOLDER) ?: throw NullPointerException()
    this.messageEntity = intent.getParcelableExtra(EXTRA_KEY_MSG) ?: throw NullPointerException()

    label = if (localFolder.searchQuery.isNullOrEmpty()) {
      localFolder.fullName
    } else {
      SearchMessagesActivity.SEARCH_FOLDER_NAME
    }

    msgDetailsViewModel = ViewModelProvider(this, MsgDetailsViewModelFactory(localFolder,
        messageEntity, application)).get(MsgDetailsViewModel::class.java)
    msgDetailsViewModel.msgLiveData.observe(this, genMsgObserver())
    msgDetailsViewModel.attsLiveData.observe(this, object : Observer<List<AttachmentEntity>> {
      var isUpdateEnabled = true

      override fun onChanged(list: List<AttachmentEntity>) {
        val attachmentInfoList = list.map {
          if (localFolder.searchQuery.isNullOrEmpty()) {
            it.toAttInfo()
          } else {
            it.toAttInfo().copy(folder = localFolder.fullName)
          }
        }.toMutableList()
        if (isUpdateEnabled) {
          if (attachmentInfoList.isNotEmpty()) {
            updateAtts(attachmentInfoList)
            isUpdateEnabled = false
          } else if (messageEntity.hasAttachments == true) {
            loadAttsInfo(R.id.syns_request_code_load_atts_info, localFolder, messageEntity.uid.toInt())
          }
        }
      }
    })
    msgDetailsViewModel.msgStatesLiveData.observe(this, Observer {
      var finishActivity = true
      when (it) {
        MessageState.PENDING_ARCHIVING -> archiveMsgs()
        MessageState.PENDING_DELETING -> deleteMsgs()
        MessageState.PENDING_MOVE_TO_INBOX -> moveMsgsToINBOX()
        MessageState.PENDING_MARK_UNREAD -> changeMsgsReadState()
        MessageState.PENDING_MARK_READ -> {
          changeMsgsReadState()
          finishActivity = false
        }
        else -> {
        }
      }

      if (finishActivity) {
        finish()
      }
    })
  }

  /**
   * Handle an error from the sync service.
   *
   * @param requestCode The unique request code for the reply to [android.os.Messenger].
   * @param errorType   The [SyncErrorTypes]
   * @param e           The exception which happened.
   */
  private fun onErrorOccurred(requestCode: Int, errorType: Int, e: Exception) {
    val fragment = supportFragmentManager.findFragmentById(R.id.messageDetailsFragment) as MessageDetailsFragment?

    fragment?.onErrorOccurred(requestCode, errorType, e)
  }

  private fun onMsgDetailsUpdated() {
    val fragment = supportFragmentManager
        .findFragmentById(R.id.messageDetailsFragment) as MessageDetailsFragment?

    fragment?.onMsgDetailsUpdated()
  }

  private fun updateAtts(atts: MutableList<AttachmentInfo>) {
    val fragment = supportFragmentManager
        .findFragmentById(R.id.messageDetailsFragment) as MessageDetailsFragment?
    fragment?.updateAttInfos(atts)
  }

  private fun updateViews() {
    var actionBarTitle: String? = null
    var actionBarSubTitle: String? = null

    if (JavaEmailConstants.FOLDER_OUTBOX.equals(messageEntity.folder, ignoreCase = true)) {
      actionBarTitle = getString(R.string.outgoing)

      when (messageEntity.msgState) {
        MessageState.NEW, MessageState.NEW_FORWARDED -> actionBarSubTitle = getString(R.string.preparing)

        MessageState.QUEUED -> actionBarSubTitle = getString(R.string.queued)

        MessageState.SENDING -> actionBarSubTitle = getString(R.string.sending)

        MessageState.SENT, MessageState.SENT_WITHOUT_LOCAL_COPY -> actionBarSubTitle = getString(R.string.sent)

        MessageState.ERROR_CACHE_PROBLEM,
        MessageState.ERROR_DURING_CREATION,
        MessageState.ERROR_ORIGINAL_MESSAGE_MISSING,
        MessageState.ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND,
        MessageState.ERROR_SENDING_FAILED,
        MessageState.ERROR_PRIVATE_KEY_NOT_FOUND -> actionBarSubTitle = getString(R.string.an_error_has_occurred)

        else -> {
        }
      }
    } else when (messageEntity.msgState) {
      MessageState.PENDING_ARCHIVING -> actionBarTitle = getString(R.string.pending)
      else -> {
      }
    }

    supportActionBar?.title = actionBarTitle
    supportActionBar?.subtitle = actionBarSubTitle
  }

  private fun setupDecryptMessageViewModel() {
    decryptMsgViewModel = ViewModelProvider(this).get(DecryptMessageViewModel::class.java)
    decryptMsgViewModel.decryptLiveData.observe(this, Observer {
      when (it.status) {
        Result.Status.LOADING -> {
          onProgressReplyReceived(R.id.syns_request_code_load_raw_mime_msg, R.id.progress_id_processing, 75)
        }

        Result.Status.SUCCESS -> {
          onProgressReplyReceived(R.id.syns_request_code_load_raw_mime_msg, R.id.progress_id_processing, 90)
          val result = it.data
          if (result == null) {
            Toast.makeText(this, getString(R.string.internal_api_error), Toast.LENGTH_LONG).show()
            idlingForDecryption?.decrementSafely()
          } else {
            val msgInfo = IncomingMessageInfo(messageEntity, result.text,
                result.subject, result.msgBlocks!!, decryptMsgViewModel.headersLiveData.value, result.getMsgEncryptionType())
            val fragment = supportFragmentManager
                .findFragmentById(R.id.messageDetailsFragment) as MessageDetailsFragment?

            fragment?.showIncomingMsgInfo(msgInfo)

            idlingForDecryption?.decrementSafely()
          }
        }

        Result.Status.ERROR -> {
          idlingForWebView.setIdleState(true)
          updateActionProgressState(100, null)
          showErrorInfo(it.data?.apiError, null)
          idlingForDecryption?.decrementSafely()
          ExceptionUtil.handleError(ManualHandledException("" + it.data?.apiError))
        }

        Result.Status.EXCEPTION -> {
          if (it.exception is CorruptedMsgInCacheException) {
            idlingForDecryption?.decrementSafely()
            isRetrieveIncomingMsgNeeded = true
            isReceiveMsgBodyNeeded = true
            msgSnapshot = null
            loadMsgDetails()
          } else {
            idlingForWebView.setIdleState(true)
            updateActionProgressState(100, null)
            showErrorInfo(null, it.exception)
            idlingForDecryption?.decrementSafely()
            ExceptionUtil.handleError(it.exception)
          }
        }
      }
    })
  }

  companion object {
    val EXTRA_KEY_FOLDER = GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_FOLDER",
        MessageDetailsActivity::class.java)
    val EXTRA_KEY_MSG = GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_MSG",
        MessageDetailsActivity::class.java)

    fun getIntent(context: Context?, localFolder: LocalFolder?, msgEntity: MessageEntity?): Intent {
      val intent = Intent(context, MessageDetailsActivity::class.java)
      intent.putExtra(EXTRA_KEY_FOLDER, localFolder)
      intent.putExtra(EXTRA_KEY_MSG, msgEntity)
      return intent
    }
  }
}
