/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.test.espresso.idling.CountingIdlingResource
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.GeneralMessageDetails
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.sync.SyncErrorTypes
import com.flowcrypt.email.api.retrofit.Status
import com.flowcrypt.email.api.retrofit.node.NodeRepository
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
    MessageDetailsFragment.OnActionListener, Observer<NodeResponseWrapper<*>> {

  private var details: GeneralMessageDetails? = null
  private var localFolder: LocalFolder? = null
  @get:VisibleForTesting
  var idlingForDecryption: CountingIdlingResource? = null
    private set

  private var isReceiveMsgBodyNeeded: Boolean = false
  private var isBackEnabled = true
  private var isRequestMsgDetailsStarted: Boolean = false
  private var isRetrieveIncomingMsgNeeded = true
  private var viewModel: DecryptMessageViewModel? = null

  override val rootView: View
    get() = View(this)

  override val contentViewResourceId: Int
    get() = R.layout.activity_message_details

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = ViewModelProviders.of(this).get(DecryptMessageViewModel::class.java)
    viewModel!!.init(NodeRepository())
    viewModel!!.responsesLiveData.observe(this, this)

    if (intent != null) {
      this.localFolder = intent.getParcelableExtra(EXTRA_KEY_FOLDER)
      this.details = intent.getParcelableExtra(EXTRA_KEY_GENERAL_MESSAGE_DETAILS)
    }

    idlingForDecryption = CountingIdlingResource(
        GeneralUtil.genIdlingResourcesName(MessageDetailsActivity::class.java), GeneralUtil.isDebugBuild())

    updateViews()

    LoaderManager.getInstance(this).initLoader(R.id.loader_id_load_message_info_from_database, null, this)
  }

  override fun onBackPressed() {
    if (isBackEnabled) {
      super.onBackPressed()
    } else {
      Toast.makeText(this, R.string.please_wait_while_action_will_be_completed, Toast.LENGTH_SHORT).show()
    }
  }

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
    when (id) {
      R.id.loader_id_load_message_info_from_database, R.id.loader_id_subscribe_to_message_changes -> {
        val uri = MessageDaoSource().baseContentUri
        val selection = (MessageDaoSource.COL_EMAIL + "= ? AND " + MessageDaoSource.COL_FOLDER + " = ? AND "
            + MessageDaoSource.COL_UID + " = ? ")
        val selectionArgs = arrayOf(details!!.email, localFolder!!.folderAlias, details!!.uid.toString())
        return CursorLoader(this, uri, null, selection, selectionArgs, null)
      }

      R.id.loader_id_load_attachments -> {
        val uriAtt = AttachmentDaoSource().baseContentUri
        val selectionAtt = (AttachmentDaoSource.COL_EMAIL + " = ?" + " AND "
            + AttachmentDaoSource.COL_FOLDER + " = ? AND " + AttachmentDaoSource.COL_UID + " = ?")
        val selectionArgsAtt = arrayOf(details!!.email, localFolder!!.fullName, details!!.uid.toString())
        return CursorLoader(this, uriAtt, null, selectionAtt, selectionArgsAtt, null)
      }

      else -> return Loader(this)
    }
  }

  override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor?) {
    val messageDaoSource = MessageDaoSource()

    when (loader.id) {
      R.id.loader_id_load_message_info_from_database -> if (cursor != null && cursor.moveToFirst()) {
        this.details = messageDaoSource.getMsgInfo(cursor)
        updateMsgDetails(details!!)

        if (TextUtils.isEmpty(details!!.rawMsgWithoutAtts)) {
          if (isSyncServiceBound && !isRequestMsgDetailsStarted) {
            this.isRequestMsgDetailsStarted = true
            loadMsgDetails(R.id.syns_request_code_load_message_details, localFolder!!, details!!.uid)
          } else {
            isReceiveMsgBodyNeeded = true
          }
        } else if (isRetrieveIncomingMsgNeeded) {
          isRetrieveIncomingMsgNeeded = false
          isReceiveMsgBodyNeeded = false
          messageDaoSource.setSeenStatus(this, details!!.email, localFolder!!.folderAlias, details!!.uid.toLong())
          setResult(RESULT_CODE_UPDATE_LIST, null)
          decryptMsg()
        }
      }

      R.id.loader_id_subscribe_to_message_changes -> if (cursor != null && cursor.moveToFirst()) {
        details = messageDaoSource.getMsgInfo(cursor)
        updateViews()
      }

      R.id.loader_id_load_attachments -> if (cursor != null) {
        val attInfolist = ArrayList<AttachmentInfo>()
        while (cursor.moveToNext()) {
          attInfolist.add(AttachmentDaoSource.getAttInfo(cursor))
        }

        updateAtts(attInfolist)
      }
    }
  }

  override fun onLoaderReset(loader: Loader<Cursor>) {
    when (loader.id) {
      R.id.loader_id_load_message_info_from_database -> {
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
      if (TextUtils.isEmpty(details!!.rawMsgWithoutAtts)) {
        LoaderManager.getInstance(this).initLoader(R.id.loader_id_load_message_info_from_database, null, this)
      } else {
        decryptMsg()
      }
    }
  }

  override fun onSyncServiceConnected() {
    super.onSyncServiceConnected()
    if (isReceiveMsgBodyNeeded) {
      loadMsgDetails(R.id.syns_request_code_load_message_details, localFolder!!, details!!.uid)
    }
  }

  override fun onReplyReceived(requestCode: Int, resultCode: Int, obj: Any?) {
    when (requestCode) {
      R.id.syns_request_code_load_message_details -> {
        isRequestMsgDetailsStarted = false
        when (resultCode) {
          EmailSyncService.REPLY_RESULT_CODE_ACTION_OK -> {
            val folderAlias = localFolder!!.folderAlias
            MessageDaoSource().setSeenStatus(this, details!!.email, folderAlias, details!!.uid.toLong())
            setResult(RESULT_CODE_UPDATE_LIST, null)
            LoaderManager.getInstance(this).restartLoader(R.id.loader_id_load_message_info_from_database, null, this)
          }

          EmailSyncService.REPLY_RESULT_CODE_ACTION_ERROR_MESSAGE_NOT_FOUND -> messageNotAvailableInFolder()
        }
      }

      R.id.syns_request_archive_message, R.id.syns_request_delete_message, R.id.syns_request_move_message_to_inbox -> {
        isBackEnabled = true
        when (resultCode) {
          EmailSyncService.REPLY_RESULT_CODE_ACTION_OK -> {
            var toastMsgResId = 0

            when (requestCode) {
              R.id.syns_request_archive_message -> toastMsgResId = R.string.message_was_archived

              R.id.syns_request_delete_message -> toastMsgResId = R.string.message_was_deleted

              R.id.syns_request_move_message_to_inbox -> toastMsgResId = R.string.message_was_moved_to_inbox
            }

            Toast.makeText(this, toastMsgResId, Toast.LENGTH_SHORT).show()
            val folderAlias = localFolder!!.folderAlias
            MessageDaoSource().deleteMsg(this, details!!.email, folderAlias, details!!.uid.toLong())
            AttachmentDaoSource().deleteAtts(this, details!!.email, folderAlias, details!!.uid.toLong())
            setResult(RESULT_CODE_UPDATE_LIST, null)
            finish()
          }

          EmailSyncService.REPLY_RESULT_CODE_ACTION_ERROR_MESSAGE_NOT_EXISTS -> messageNotAvailableInFolder()
        }
      }
    }
  }

  override fun onErrorHappened(requestCode: Int, errorType: Int, e: Exception) {
    when (requestCode) {
      R.id.syns_request_code_load_message_details -> {
        isRequestMsgDetailsStarted = false
        onErrorOccurred(requestCode, errorType, e)
      }

      R.id.syns_request_archive_message, R.id.syns_request_delete_message, R.id.syns_request_move_message_to_inbox -> {
        isBackEnabled = true
        onErrorOccurred(requestCode, errorType, e)
      }

      else -> onErrorOccurred(requestCode, errorType, e)
    }
  }

  override fun onArchiveMsgClicked() {
    isBackEnabled = false
    val foldersManager = FoldersManager.fromDatabase(this, details!!.email)
    val archive = foldersManager.folderArchive
    if (archive == null) {
      ExceptionUtil.handleError(IllegalArgumentException("Folder 'All Mail' not found"))
    }
    moveMsg(R.id.syns_request_archive_message, localFolder!!, archive!!, details!!.uid)
  }

  override fun onDeleteMsgClicked() {
    isBackEnabled = false
    if (JavaEmailConstants.FOLDER_OUTBOX.equals(details!!.label, ignoreCase = true)) {
      val msgDaoSource = MessageDaoSource()
      val details = msgDaoSource.getMsg(this, this.details!!.email,
          this.details!!.label, this.details!!.uid.toLong())

      if (details == null || details.msgState === MessageState.SENDING) {
        Toast.makeText(this, if (details == null)
          R.string.can_not_delete_sent_message
        else
          R.string.can_not_delete_sending_message, Toast.LENGTH_LONG).show()
      } else {
        val deletedRows = MessageDaoSource().deleteOutgoingMsg(this, details)
        if (deletedRows > 0) {
          Toast.makeText(this, R.string.message_was_deleted, Toast.LENGTH_SHORT).show()
        } else {
          Toast.makeText(this, R.string.can_not_delete_sent_message, Toast.LENGTH_LONG).show()
        }
      }

      setResult(RESULT_CODE_UPDATE_LIST, null)
      finish()
    } else {
      val foldersManager = FoldersManager.fromDatabase(this, details!!.email)
      val trash = foldersManager.folderTrash
      if (trash == null) {
        ExceptionUtil.handleError(IllegalArgumentException("Folder 'Trash' not found, provider: "
            + EmailUtil.getDomain(details!!.email)))
      } else {
        moveMsg(R.id.syns_request_delete_message, localFolder!!, trash, details!!.uid)
      }
    }
  }

  override fun onMoveMsgToInboxClicked() {
    isBackEnabled = false
    val foldersManager = FoldersManager.fromDatabase(this, details!!.email)
    val folderInbox = foldersManager.folderInbox
    if (folderInbox == null) {
      ExceptionUtil.handleError(IllegalArgumentException("Folder 'Inbox' not found"))
    }
    moveMsg(R.id.syns_request_move_message_to_inbox, localFolder!!, folderInbox!!, details!!.uid)
  }

  override fun onChanged(nodeResponseWrapper: NodeResponseWrapper<*>) {
    when (nodeResponseWrapper.requestCode) {
      R.id.live_data_id_parse_and_decrypt_msg -> when (nodeResponseWrapper.status) {
        Status.SUCCESS -> {
          val result = nodeResponseWrapper.result as ParseDecryptedMsgResult?
          if (result == null) {
            Toast.makeText(this, getString(R.string.unknown_error), Toast.LENGTH_LONG).show()
            if (!idlingForDecryption!!.isIdleNow) {
              idlingForDecryption!!.decrement()
            }
            return
          } else {
            val msgInfo = IncomingMessageInfo(details!!, result.msgBlocks!!, result.getMsgEncryptionType())
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
          showErrorInfo(nodeResponseWrapper.result!!.error, null)
          if (!idlingForDecryption!!.isIdleNow) {
            idlingForDecryption!!.decrement()
          }
          ExceptionUtil.handleError(ManualHandledException("" + nodeResponseWrapper.result?.error!!))
        }

        Status.EXCEPTION -> {
          showErrorInfo(null, nodeResponseWrapper.exception)
          if (!idlingForDecryption!!.isIdleNow) {
            idlingForDecryption!!.decrement()
          }
          ExceptionUtil.handleError(nodeResponseWrapper.exception!!)
        }

        else -> {
        }
      }
    }
  }

  fun decryptMsg() {
    idlingForDecryption!!.increment()
    viewModel!!.decryptMessage(details!!.rawMsgWithoutAtts!!)
  }

  private fun showErrorInfo(error: Error?, e: Throwable?) {
    val fragment = supportFragmentManager
        .findFragmentById(R.id.messageDetailsFragment) as MessageDetailsFragment?

    fragment?.showErrorInfo(error, e)
  }

  private fun messageNotAvailableInFolder() {
    val folderAlias = localFolder!!.folderAlias
    MessageDaoSource().deleteMsg(this, details!!.email, folderAlias, details!!.uid.toLong())
    AttachmentDaoSource().deleteAtts(this, details!!.email, folderAlias, details!!.uid.toLong())
    setResult(RESULT_CODE_UPDATE_LIST, null)
    Toast.makeText(this, R.string.email_does_not_available_in_this_folder, Toast.LENGTH_LONG).show()
    finish()
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
    if (supportActionBar != null) {
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
        }
      }

      supportActionBar!!.title = actionBarTitle
      supportActionBar!!.subtitle = actionBarSubTitle
    }
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
