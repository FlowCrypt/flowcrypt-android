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
import com.flowcrypt.email.api.email.model.GeneralMessageDetails
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
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource
import com.flowcrypt.email.jetpack.viewmodel.DecryptMessageViewModel
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
    Observer<NodeResponseWrapper<*>> {

  private var details: GeneralMessageDetails? = null
  private var localFolder: LocalFolder? = null
  @get:VisibleForTesting
  var idlingForDecryption: CountingIdlingResource? = null
    private set
  val idlingForWebView: SingleIdlingResources = SingleIdlingResources(false)

  private var isReceiveMsgBodyNeeded: Boolean = false
  private var isRequestMsgDetailsStarted: Boolean = false
  private var isRetrieveIncomingMsgNeeded = true
  private lateinit var viewModel: DecryptMessageViewModel
  private var rawMimeBytes: ByteArray? = null
  private lateinit var label: String
  private val uniqueId = UUID.randomUUID().toString()

  override val rootView: View
    get() = View(this)

  override val contentViewResourceId: Int
    get() = R.layout.activity_message_details

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = ViewModelProvider(this).get(DecryptMessageViewModel::class.java)
    viewModel.responsesLiveData.observe(this, this)

    if (intent != null) {
      this.localFolder = intent.getParcelableExtra(EXTRA_KEY_FOLDER)
      this.details = intent.getParcelableExtra(EXTRA_KEY_GENERAL_MESSAGE_DETAILS)

      label = if (localFolder?.searchQuery.isNullOrEmpty()) {
        localFolder?.fullName ?: ""
      } else {
        SearchMessagesActivity.SEARCH_FOLDER_NAME
      }
    }

    idlingForDecryption = CountingIdlingResource(
        GeneralUtil.genIdlingResourcesName(MessageDetailsActivity::class.java), GeneralUtil.isDebugBuild())

    updateViews()

    LoaderManager.getInstance(this).initLoader(R.id.loader_id_subscribe_to_message_changes, null, this)
  }

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
    when (id) {
      R.id.loader_id_load_raw_mime_msg_from_db, R.id.loader_id_subscribe_to_message_changes -> {
        val uri = MessageDaoSource().baseContentUri
        val selection = (MessageDaoSource.COL_EMAIL + "= ? AND " + MessageDaoSource.COL_FOLDER + " = ? AND "
            + MessageDaoSource.COL_UID + " = ? ")
        val selectionArgs = arrayOf(details?.email ?: "", label, details?.uid?.toString() ?: "")
        return CursorLoader(this, uri, null, selection, selectionArgs, null)
      }

      R.id.loader_id_load_attachments -> {
        val uriAtt = AttachmentDaoSource().baseContentUri
        val selectionAtt = (AttachmentDaoSource.COL_EMAIL + " = ?" + " AND "
            + AttachmentDaoSource.COL_FOLDER + " = ? AND " + AttachmentDaoSource.COL_UID + " = ?")
        val selectionArgsAtt = arrayOf(details!!.email, label, details!!.uid.toString())
        return CursorLoader(this, uriAtt, null, selectionAtt, selectionArgsAtt, null)
      }

      else -> return Loader(this)
    }
  }

  override fun onDestroy() {
    cancelLoadMsgDetails(uniqueId)
    super.onDestroy()
  }

  override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor?) {
    val msgDaoSource = MessageDaoSource()

    when (loader.id) {
      R.id.loader_id_load_raw_mime_msg_from_db -> if (cursor?.moveToFirst() == true) {
        this.rawMimeBytes = if (JavaEmailConstants.FOLDER_OUTBOX.equals(details?.label, ignoreCase = true)) {
          cursor.getBlob(cursor.getColumnIndex(MessageDaoSource.COL_RAW_MESSAGE_WITHOUT_ATTACHMENTS))
        } else {
          MsgsCacheManager.getMsgAsByteArray(details!!.id.toString())
        }

        updateMsgDetails(details!!)

        if (rawMimeBytes?.isNotEmpty() == true) {
          if (isRetrieveIncomingMsgNeeded) {
            isRetrieveIncomingMsgNeeded = false
            isReceiveMsgBodyNeeded = false

            if (!JavaEmailConstants.FOLDER_OUTBOX.equals(details?.label, ignoreCase = true)
                && details?.isSeen() == false) {
              msgDaoSource.setSeenStatus(this, details!!.email, label, details!!.uid.toLong())
              //todo-denbond7 #793
              /*msgDaoSource.updateMsgState(this, details?.email ?: "", details?.label ?: "",
                  details?.uid?.toLong() ?: 0, MessageState.PENDING_MARK_READ)*/
              changeMsgsReadState()
              setResult(RESULT_CODE_UPDATE_LIST, null)
            }

            decryptMsg()
          }
        } else {
          if (isSyncServiceBound && !isRequestMsgDetailsStarted) {
            this.isRequestMsgDetailsStarted = true
            loadMsgDetails()
          } else {
            isReceiveMsgBodyNeeded = true
          }
        }
      } else {
        messageNotAvailableInFolder()
      }

      R.id.loader_id_subscribe_to_message_changes -> if (cursor != null && cursor.moveToFirst()) {
        details = msgDaoSource.getMsgInfo(cursor)
        updateViews()
      }

      R.id.loader_id_load_attachments -> if (cursor != null) {
        val atts = ArrayList<AttachmentInfo>()
        while (cursor.moveToNext()) {
          atts.add(AttachmentDaoSource.getAttInfo(cursor))
        }

        if (atts.isNotEmpty()) {
          updateAtts(atts)
          LoaderManager.getInstance(this).destroyLoader(R.id.loader_id_load_attachments)
        } else if (details?.hasAtts == true) {
          loadAttsInfo(R.id.syns_request_code_load_atts_info, localFolder!!, details!!.uid)
        }
      }
    }
  }

  override fun onLoaderReset(loader: Loader<Cursor>) {
    when (loader.id) {
      R.id.loader_id_load_raw_mime_msg_from_db -> {
      }

      R.id.loader_id_subscribe_to_message_changes -> {
        details = null
        updateViews()
      }

      R.id.loader_id_load_attachments -> updateAtts(ArrayList())
    }
  }

  override fun onNodeStateChanged(isReady: Boolean) {
    super.onNodeStateChanged(isReady)
    if (isReady) {
      if (rawMimeBytes?.isNotEmpty() == true) {
        decryptMsg()
      } else {
        LoaderManager.getInstance(this).initLoader(R.id.loader_id_load_raw_mime_msg_from_db, null, this)
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
            MessageDaoSource().setSeenStatus(this, details!!.email, label, details!!.uid.toLong())
            setResult(RESULT_CODE_UPDATE_LIST, null)
            LoaderManager.getInstance(this).restartLoader(R.id.loader_id_load_raw_mime_msg_from_db, null, this)
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
            val msgInfo = IncomingMessageInfo(details!!, result.text, result.msgBlocks!!,
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

  fun decryptMsg() {
    if (rawMimeBytes?.isNotEmpty() == true) {
      idlingForDecryption!!.increment()
      onProgressReplyReceived(R.id.syns_request_code_load_raw_mime_msg, R.id
          .progress_id_processing, 65)
      viewModel.decryptMessage(rawMimeBytes!!)
      onProgressReplyReceived(R.id.syns_request_code_load_raw_mime_msg, R.id
          .progress_id_processing, 70)
    }
  }

  fun loadMsgDetails() {
    loadMsgDetails(R.id.syns_request_code_load_raw_mime_msg, uniqueId, localFolder!!,
        details!!.uid, details!!.id)
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

  private fun messageNotAvailableInFolder() {
    //todo-denbond7 #793
    /*MessageDaoSource().deleteMsg(this, details!!.email, label, details!!.uid.toLong())
    AttachmentDaoSource().deleteAtts(this, details!!.email, label, details!!.uid.toLong())
    setResult(RESULT_CODE_UPDATE_LIST, null)
    Toast.makeText(this, R.string.email_does_not_available_in_this_folder, Toast.LENGTH_LONG).show()
    finish()*/
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

  private fun updateMsgDetails(generalMsgDetails: GeneralMessageDetails) {
    val fragment = supportFragmentManager
        .findFragmentById(R.id.messageDetailsFragment) as MessageDetailsFragment?

    fragment?.updateMsgDetails(generalMsgDetails)
  }

  private fun updateAtts(atts: ArrayList<AttachmentInfo>) {
    val fragment = supportFragmentManager
        .findFragmentById(R.id.messageDetailsFragment) as MessageDetailsFragment?

    fragment?.updateAttInfos(atts)
  }

  private fun updateViews() {
    var actionBarTitle: String? = null
    var actionBarSubTitle: String? = null

    if (details != null) {
      if (JavaEmailConstants.FOLDER_OUTBOX.equals(details!!.label, ignoreCase = true)) {
        actionBarTitle = getString(R.string.outgoing)

        when (details!!.msgState) {
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
      } else when (details?.msgState) {
        MessageState.PENDING_ARCHIVING -> actionBarTitle = getString(R.string.pending)
        else -> {
        }
      }
    }

    supportActionBar?.title = actionBarTitle
    supportActionBar?.subtitle = actionBarSubTitle
  }

  companion object {
    const val RESULT_CODE_UPDATE_LIST = 100

    val EXTRA_KEY_FOLDER = GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_FOLDER",
        MessageDetailsActivity::class.java)
    val EXTRA_KEY_GENERAL_MESSAGE_DETAILS =
        GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_GENERAL_MESSAGE_DETAILS", MessageDetailsActivity::class.java)

    fun getIntent(context: Context?, localFolder: LocalFolder?, details: GeneralMessageDetails?): Intent {
      val intent = Intent(context, MessageDetailsActivity::class.java)
      intent.putExtra(EXTRA_KEY_FOLDER, localFolder)
      intent.putExtra(EXTRA_KEY_GENERAL_MESSAGE_DETAILS, details)
      return intent
    }
  }
}
