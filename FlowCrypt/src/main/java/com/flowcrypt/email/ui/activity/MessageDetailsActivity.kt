/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.test.espresso.idling.CountingIdlingResource
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.MsgsCacheManager
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.sync.SyncErrorTypes
import com.flowcrypt.email.api.retrofit.LoadingState
import com.flowcrypt.email.api.retrofit.Status
import com.flowcrypt.email.api.retrofit.response.model.node.Error
import com.flowcrypt.email.api.retrofit.response.node.NodeResponseWrapper
import com.flowcrypt.email.api.retrofit.response.node.ParseDecryptedMsgResult
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.dao.source.imap.AttachmentDaoSource
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.jetpack.viewmodel.DecryptMessageViewModel
import com.flowcrypt.email.jetpack.viewmodel.MsgDetailsViewModel
import com.flowcrypt.email.jetpack.viewmodel.factory.MsgDetailsViewModelFactory
import com.flowcrypt.email.service.EmailSyncService
import com.flowcrypt.email.ui.activity.base.BaseBackStackSyncActivity
import com.flowcrypt.email.ui.activity.fragment.MessageDetailsFragment
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.ManualHandledException
import com.flowcrypt.email.util.idling.SingleIdlingResources
import com.sun.mail.util.ASCIIUtility
import java.util.*

/**
 * This activity describe details of some message.
 *
 * @author DenBond7
 * Date: 03.05.2017
 * Time: 16:29
 * E-mail: DenBond7@gmail.com
 */
class MessageDetailsActivity : BaseBackStackSyncActivity(), LoaderManager.LoaderCallbacks<Cursor>,
    Observer<NodeResponseWrapper<*>>, MessageDetailsFragment.MessageDetailsListener {

  private lateinit var messageEntity: MessageEntity
  private lateinit var localFolder: LocalFolder
  private lateinit var msgDetailsViewModel: MsgDetailsViewModel
  private lateinit var decryptMsgViewModel: DecryptMessageViewModel

  @get:VisibleForTesting
  var idlingForDecryption: CountingIdlingResource? = null
    private set
  val idlingForWebView: SingleIdlingResources = SingleIdlingResources(false)

  private var isReceiveMsgBodyNeeded: Boolean = false
  private var isRequestMsgDetailsStarted: Boolean = false
  private var isRetrieveIncomingMsgNeeded = true
  private var rawMimeBytes: ByteArray? = null
  private lateinit var label: String
  private val uniqueId = UUID.randomUUID().toString()

  override val rootView: View
    get() = View(this)

  override val contentViewResourceId: Int
    get() = R.layout.activity_message_details

  override fun onCreate(savedInstanceState: Bundle?) {
    initMsgDetailsViewModel()
    super.onCreate(savedInstanceState)
    decryptMsgViewModel = ViewModelProvider(this).get(DecryptMessageViewModel::class.java)
    decryptMsgViewModel.responsesLiveData.observe(this, this)

    idlingForDecryption = CountingIdlingResource(
        GeneralUtil.genIdlingResourcesName(MessageDetailsActivity::class.java), GeneralUtil.isDebugBuild())

    updateViews()
  }

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
    return when (id) {
      R.id.loader_id_load_attachments -> {
        val uriAtt = AttachmentDaoSource().baseContentUri
        val selectionAtt = (AttachmentDaoSource.COL_EMAIL + " = ?" + " AND "
            + AttachmentDaoSource.COL_FOLDER + " = ? AND " + AttachmentDaoSource.COL_UID + " = ?")
        val selectionArgsAtt = arrayOf(messageEntity.email, label, messageEntity.uid.toString())
        CursorLoader(this, uriAtt, null, selectionAtt, selectionArgsAtt, null)
      }

      else -> Loader(this)
    }
  }

  override fun onDestroy() {
    cancelLoadMsgDetails(uniqueId)
    super.onDestroy()
  }

  override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor?) {
    when (loader.id) {
      R.id.loader_id_load_attachments -> if (cursor != null) {
        val atts = ArrayList<AttachmentInfo>()
        while (cursor.moveToNext()) {
          atts.add(AttachmentDaoSource.getAttInfo(cursor))
        }

        if (atts.isNotEmpty()) {
          updateAtts(atts)
          LoaderManager.getInstance(this).destroyLoader(R.id.loader_id_load_attachments)
        } else if (messageEntity.hasAttachments == true) {
          loadAttsInfo(R.id.syns_request_code_load_atts_info, localFolder, messageEntity.uid.toInt())
        }
      }
    }
  }

  override fun onLoaderReset(loader: Loader<Cursor>) {
    when (loader.id) {
      R.id.loader_id_load_attachments -> updateAtts(ArrayList())
    }
  }

  override fun onNodeStateChanged(isReady: Boolean) {
    super.onNodeStateChanged(isReady)
    if (isReady) {
      if (rawMimeBytes?.isNotEmpty() == true) {
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

  override fun onChanged(nodeResponseWrapper: NodeResponseWrapper<*>) {
    when (nodeResponseWrapper.requestCode) {
      R.id.live_data_id_parse_and_decrypt_msg -> when (nodeResponseWrapper.status) {
        Status.LOADING -> {
          nodeResponseWrapper.loadingState?.let {
            when (it) {
              LoadingState.PREPARE_REQUEST -> {
                onProgressReplyReceived(R.id.syns_request_code_load_raw_mime_msg, R.id
                    .progress_id_processing, 75)
              }

              LoadingState.PREPARE_SERVICE -> {
                onProgressReplyReceived(R.id.syns_request_code_load_raw_mime_msg, R.id
                    .progress_id_processing, 80)
              }

              LoadingState.RUN_REQUEST -> {
                onProgressReplyReceived(R.id.syns_request_code_load_raw_mime_msg, R.id
                    .progress_id_processing, 85)
              }

              else -> {
              }
            }
          }
        }

        Status.SUCCESS -> {
          onProgressReplyReceived(R.id.syns_request_code_load_raw_mime_msg, R.id
              .progress_id_processing, 90)
          val result = nodeResponseWrapper.result as ParseDecryptedMsgResult?
          if (result == null) {
            Toast.makeText(this, getString(R.string.unknown_error), Toast.LENGTH_LONG).show()
            if (!idlingForDecryption!!.isIdleNow) {
              idlingForDecryption!!.decrement()
            }
            return
          } else {
            val msgInfo = IncomingMessageInfo(messageEntity, result.text, result.msgBlocks!!,
                EmailUtil.getHeadersFromRawMIME(ASCIIUtility.toString(rawMimeBytes)), result.getMsgEncryptionType())
            val fragment = supportFragmentManager
                .findFragmentById(R.id.messageDetailsFragment) as MessageDetailsFragment?

            if (fragment != null) {
              fragment.showIncomingMsgInfo(msgInfo)
              LoaderManager.getInstance(this).initLoader(R.id.loader_id_load_attachments, null, this)
            }
            if (!idlingForDecryption!!.isIdleNow) {
              idlingForDecryption!!.decrement()
            }
          }
        }

        Status.ERROR -> {
          idlingForWebView.setIdleState(true)
          updateActionProgressState(100, null)
          showErrorInfo(nodeResponseWrapper.result?.error, null)
          if (!idlingForDecryption!!.isIdleNow) {
            idlingForDecryption!!.decrement()
          }
          ExceptionUtil.handleError(ManualHandledException("" + nodeResponseWrapper.result?.error))
        }

        Status.EXCEPTION -> {
          idlingForWebView.setIdleState(true)
          updateActionProgressState(100, null)
          showErrorInfo(null, nodeResponseWrapper.exception)
          if (!idlingForDecryption!!.isIdleNow) {
            idlingForDecryption!!.decrement()
          }
          ExceptionUtil.handleError(nodeResponseWrapper.exception!!)
        }
      }
    }
  }

  override fun getMsgDetailsViewModel(): MsgDetailsViewModel? {
    return if (::msgDetailsViewModel.isInitialized) {
      msgDetailsViewModel
    } else null
  }

  fun decryptMsg() {
    if (rawMimeBytes?.isNotEmpty() == true) {
      idlingForDecryption!!.increment()
      onProgressReplyReceived(R.id.syns_request_code_load_raw_mime_msg, R.id
          .progress_id_processing, 65)
      decryptMsgViewModel.decryptMessage(rawMimeBytes!!)
      onProgressReplyReceived(R.id.syns_request_code_load_raw_mime_msg, R.id
          .progress_id_processing, 70)
    }
  }

  fun loadMsgDetails() {
    loadMsgDetails(R.id.syns_request_code_load_raw_mime_msg, uniqueId, localFolder, messageEntity
        .uid.toInt(), messageEntity.id?.toInt() ?: -1)
  }

  private fun updateActionProgressState(progress: Int, message: String?) {
    val fragment = supportFragmentManager
        .findFragmentById(R.id.messageDetailsFragment) as MessageDetailsFragment?

    fragment?.setActionProgress(progress, message)
  }

  private fun showErrorInfo(error: Error?, e: Throwable?) {
    val fragment = supportFragmentManager
        .findFragmentById(R.id.messageDetailsFragment) as MessageDetailsFragment?

    fragment?.showErrorInfo(error, e)
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
          this@MessageDetailsActivity.rawMimeBytes = if (JavaEmailConstants.FOLDER_OUTBOX.equals(messageEntity.folder,
                  ignoreCase = true)) {
            it.rawMessageWithoutAttachments?.toByteArray()
          } else {
            MsgsCacheManager.getMsgAsByteArray(messageEntity.id.toString())
          }

          onMsgDetailsUpdated()

          if (rawMimeBytes?.isNotEmpty() == true) {
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
              this@MessageDetailsActivity.isRequestMsgDetailsStarted = true
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

    this.localFolder = intent.getParcelableExtra(EXTRA_KEY_FOLDER)
    this.messageEntity = intent.getParcelableExtra(EXTRA_KEY_MSG)

    label = if (localFolder.searchQuery.isNullOrEmpty()) {
      localFolder.fullName
    } else {
      SearchMessagesActivity.SEARCH_FOLDER_NAME
    }

    msgDetailsViewModel = ViewModelProvider(this, MsgDetailsViewModelFactory(localFolder,
        messageEntity, application)).get(MsgDetailsViewModel::class.java)
    msgDetailsViewModel.msgLiveData.observe(this, genMsgObserver())
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

  private fun updateAtts(atts: ArrayList<AttachmentInfo>) {
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
