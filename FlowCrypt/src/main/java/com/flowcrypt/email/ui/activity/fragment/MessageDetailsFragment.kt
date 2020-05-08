/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import android.text.format.DateFormat
import android.text.format.Formatter
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.model.ServiceInfo
import com.flowcrypt.email.api.email.sync.SyncErrorTypes
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.model.node.DecryptErrorDetails
import com.flowcrypt.email.api.retrofit.response.model.node.DecryptErrorMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.DecryptedAttMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.PublicKeyMsgBlock
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.jetpack.viewmodel.ContactsViewModel
import com.flowcrypt.email.jetpack.viewmodel.LabelsViewModel
import com.flowcrypt.email.jetpack.viewmodel.MsgDetailsViewModel
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.service.attachment.AttachmentDownloadManagerService
import com.flowcrypt.email.ui.activity.CreateMessageActivity
import com.flowcrypt.email.ui.activity.ImportPrivateKeyActivity
import com.flowcrypt.email.ui.activity.MessageDetailsActivity
import com.flowcrypt.email.ui.activity.fragment.base.BaseSyncFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.ChoosePublicKeyDialogFragment
import com.flowcrypt.email.ui.widget.EmailWebView
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.ManualHandledException
import com.google.android.gms.common.util.CollectionUtils
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * This fragment describe msgEntity of some message.
 *
 * @author DenBond7
 * Date: 03.05.2017
 * Time: 16:29
 * E-mail: DenBond7@gmail.com
 */
class MessageDetailsFragment : BaseSyncFragment(), View.OnClickListener {

  private lateinit var onMsgDetailsListener: MessageDetailsListener

  private var textViewSenderAddress: TextView? = null
  private var textViewDate: TextView? = null
  private var textViewSubject: TextView? = null
  private var viewFooterOfHeader: View? = null
  private var layoutMsgParts: ViewGroup? = null
  private var layoutContent: View? = null
  private var imageBtnReplyAll: ImageButton? = null
  private var imageBtnMoreOptions: View? = null
  private var progressBarActionRunning: View? = null
  override var contentView: View? = null
    private set
  private var layoutReplyButton: View? = null
  private var layoutFwdButton: View? = null
  private var layoutReplyBtns: View? = null
  private var emailWebView: EmailWebView? = null
  private var layoutActionProgress: View? = null
  private var textViewActionProgress: TextView? = null
  private var progressBarActionProgress: ProgressBar? = null

  private var dateFormat: java.text.DateFormat? = null
  private var msgInfo: IncomingMessageInfo? = null
  private var folderType: FoldersManager.FolderType? = null
  private val labelsViewModel: LabelsViewModel by viewModels()
  private val contactsViewModel: ContactsViewModel by viewModels()
  private var atts = mutableListOf<AttachmentInfo>()

  private var isAdditionalActionEnabled: Boolean = false
  private var isDeleteActionEnabled: Boolean = false
  private var isArchiveActionEnabled: Boolean = false
  private var isMoveToInboxActionEnabled: Boolean = false
  private var lastClickedAtt: AttachmentInfo? = null
  private var msgEncryptType = MessageEncryptionType.STANDARD

  private val msgDetailsViewModel: MsgDetailsViewModel?
    get() = onMsgDetailsListener.getMsgDetailsViewModel()

  private val msgEntity: MessageEntity?
    get() = msgDetailsViewModel?.msgEntity

  private val localFolder: LocalFolder?
    get() = msgDetailsViewModel?.localFolder

  override val contentResourceId: Int = R.layout.fragment_message_details

  override fun onAttach(context: Context) {
    super.onAttach(context)
    if (context is MessageDetailsListener) {
      this.onMsgDetailsListener = context
    } else
      throw IllegalArgumentException(context.toString() + " must implement " +
          MessageDetailsListener::class.java.simpleName)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
    dateFormat = DateFormat.getTimeFormat(context)

    updateActionsVisibility(localFolder, null)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews(view)
    updateViews()

    setupLabelsViewModel()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_START_IMPORT_KEY_ACTIVITY -> when (resultCode) {
        Activity.RESULT_OK -> {
          Toast.makeText(context, R.string.key_successfully_imported, Toast.LENGTH_SHORT).show()
          UIUtil.exchangeViewVisibility(true, progressView, contentView)

          val activity = baseActivity as MessageDetailsActivity
          activity.decryptMsg()
        }
      }

      REQUEST_CODE_SHOW_DIALOG_WITH_SEND_KEY_OPTION -> when (resultCode) {
        Activity.RESULT_OK -> {
          val atts: List<AttachmentInfo> = data?.getParcelableArrayListExtra(ChoosePublicKeyDialogFragment
              .KEY_ATTACHMENT_INFO_LIST) ?: emptyList()

          if (!CollectionUtils.isEmpty(atts)) {
            makeAttsProtected(atts)
            sendTemplateMsgWithPublicKey(atts[0])
          }
        }
      }

      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.fragment_message_details, menu)
  }

  override fun onPrepareOptionsMenu(menu: Menu) {
    super.onPrepareOptionsMenu(menu)

    val menuItemArchiveMsg = menu.findItem(R.id.menuActionArchiveMessage)
    val menuItemDeleteMsg = menu.findItem(R.id.menuActionDeleteMessage)
    val menuActionMoveToInbox = menu.findItem(R.id.menuActionMoveToInbox)
    val menuActionMarkUnread = menu.findItem(R.id.menuActionMarkUnread)

    menuItemArchiveMsg?.isVisible = isArchiveActionEnabled
    menuItemDeleteMsg?.isVisible = isDeleteActionEnabled
    menuActionMoveToInbox?.isVisible = isMoveToInboxActionEnabled
    menuActionMarkUnread?.isVisible = !JavaEmailConstants.FOLDER_OUTBOX.equals(msgEntity?.folder,
        ignoreCase = true)

    menuItemArchiveMsg?.isEnabled = isAdditionalActionEnabled
    menuItemDeleteMsg?.isEnabled = isAdditionalActionEnabled
    menuActionMoveToInbox?.isEnabled = isAdditionalActionEnabled
    menuActionMarkUnread?.isEnabled = isAdditionalActionEnabled

    localFolder?.searchQuery?.let {
      menuItemArchiveMsg?.isVisible = false
      menuItemDeleteMsg?.isVisible = false
      menuActionMoveToInbox?.isVisible = false
      menuActionMarkUnread?.isVisible = false
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menuActionArchiveMessage -> {
        msgDetailsViewModel?.changeMsgState(MessageState.PENDING_ARCHIVING)
        true
      }

      R.id.menuActionDeleteMessage -> {
        if (JavaEmailConstants.FOLDER_OUTBOX.equals(msgEntity?.folder, ignoreCase = true)) {
          val msgEntity = msgDetailsViewModel?.msgLiveData?.value

          msgEntity?.let {
            if (msgEntity.msgState === MessageState.SENDING) {
              Toast.makeText(context, R.string.can_not_delete_sending_message, Toast.LENGTH_LONG).show()
            } else {
              msgDetailsViewModel?.deleteMsg()
              Toast.makeText(context, R.string.message_was_deleted, Toast.LENGTH_SHORT).show()
            }
          }
        } else {
          msgDetailsViewModel?.changeMsgState(MessageState.PENDING_DELETING)
        }
        true
      }

      R.id.menuActionMoveToInbox -> {
        msgDetailsViewModel?.changeMsgState(MessageState.PENDING_MOVE_TO_INBOX)
        true
      }

      R.id.menuActionMarkUnread -> {
        msgDetailsViewModel?.changeMsgState(MessageState.PENDING_MARK_UNREAD)
        true
      }

      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.layoutReplyButton -> {
        startActivity(CreateMessageActivity.generateIntent(
            context, prepareMsgInfoForReply(), MessageType.REPLY, msgEncryptType))
      }

      R.id.imageButtonReplyAll, R.id.layoutReplyAllButton -> {
        startActivity(CreateMessageActivity.generateIntent(
            context, prepareMsgInfoForReply(), MessageType.REPLY_ALL, msgEncryptType))
      }

      R.id.imageButtonMoreOptions -> {
        val popup = PopupMenu(requireContext(), v)
        popup.menuInflater.inflate(R.menu.popup_reply_actions, popup.menu)
        popup.setOnMenuItemClickListener {
          when (it.itemId) {
            R.id.menuActionReply -> {
              layoutReplyButton?.let { view -> onClick(view) }
              true
            }

            R.id.menuActionForward -> {
              layoutFwdButton?.let { view -> onClick(view) }
              true
            }
            else -> {
              true
            }
          }
        }

        popup.show()
      }


      R.id.layoutFwdButton -> {
        if (msgEncryptType === MessageEncryptionType.ENCRYPTED) {
          if (atts.isNotEmpty()) {
            Toast.makeText(context, R.string.cannot_forward_encrypted_attachments, Toast.LENGTH_LONG).show()
          }
        } else {
          for (att in atts) {
            att.isForwarded = true
          }
          msgInfo?.atts = atts
        }

        startActivity(CreateMessageActivity.generateIntent(
            context, prepareMsgInfoForReply(), MessageType.FORWARD, msgEncryptType))
      }
    }
  }

  override fun onErrorOccurred(requestCode: Int, errorType: Int, e: Exception?) {
    super.onErrorOccurred(requestCode, errorType, e)
    isAdditionalActionEnabled = true
    UIUtil.exchangeViewVisibility(false, progressBarActionRunning, layoutContent)
    activity?.invalidateOptionsMenu()

    when (requestCode) {
      R.id.syns_request_code_load_raw_mime_msg -> when (errorType) {
        SyncErrorTypes.CONNECTION_TO_STORE_IS_LOST -> {
          showConnLostHint()
          return
        }
      }
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    when (requestCode) {
      REQUEST_CODE_REQUEST_WRITE_EXTERNAL_STORAGE -> {
        if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          AttachmentDownloadManagerService.newIntent(context, lastClickedAtt)?.let {
            context?.startService(it)
          }
        } else {
          Toast.makeText(activity, R.string.cannot_save_attachment_without_permission, Toast.LENGTH_LONG).show()
        }
      }

      else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
  }

  /**
   * Show an incoming message info.
   *
   * @param msgInfo An incoming message info
   */
  fun showIncomingMsgInfo(msgInfo: IncomingMessageInfo) {
    this.msgInfo = msgInfo
    this.msgEncryptType = msgInfo.encryptionType
    imageBtnReplyAll?.visibility = View.VISIBLE
    imageBtnMoreOptions?.visibility = View.VISIBLE
    isAdditionalActionEnabled = true
    activity?.invalidateOptionsMenu()
    msgInfo.localFolder = localFolder

    msgInfo.inlineSubject?.let { textViewSubject?.text = it }

    updateMsgBody()
    UIUtil.exchangeViewVisibility(false, progressView, contentView)
  }

  /**
   * Show info about an error.
   */
  fun showErrorInfo(apiError: ApiError?, e: Throwable?) {
    when {
      apiError != null -> textViewStatusInfo?.text = apiError.msg
      e != null -> textViewStatusInfo?.text = e.message
      else -> textViewStatusInfo?.setText(R.string.unknown_error)
    }

    UIUtil.exchangeViewVisibility(false, progressView!!, statusView!!)
  }

  fun onMsgDetailsUpdated() {
    updateViews()
  }

  fun updateAttInfos(attInfoList: List<AttachmentInfo>) {
    this.atts.addAll(attInfoList)
    showAttsIfTheyExist()
  }

  fun setActionProgress(progress: Int, message: String?) {
    if (progress > 0) {
      progressBarActionProgress?.progress = progress
    }

    if (progress != 100) {
      textViewActionProgress?.text = getString(R.string.progress_message, progress, message)
      textViewActionProgress?.visibility = View.VISIBLE
    } else {
      textViewActionProgress?.text = null
      layoutActionProgress?.visibility = View.GONE
    }
  }

  private fun updateMsgBody() {
    if (msgInfo != null) {
      updateMsgView()
      showAttsIfTheyExist()
    }
  }

  private fun showConnLostHint() {
    showSnackbar(requireView(), getString(R.string.failed_load_message_from_email_server),
        getString(R.string.retry), View.OnClickListener {
      UIUtil.exchangeViewVisibility(true, progressView, statusView)
      (baseActivity as MessageDetailsActivity).loadMsgDetails()
    })
  }

  private fun makeAttsProtected(atts: List<AttachmentInfo>) {
    for (att in atts) {
      att.isProtected = true
    }
  }

  /**
   * Show a dialog where the user can select some public key which will be attached to a message.
   */
  private fun showSendersPublicKeyDialog() {
    val fragment = ChoosePublicKeyDialogFragment.newInstance(msgEntity!!.email,
        ListView.CHOICE_MODE_SINGLE, R.plurals.tell_sender_to_update_their_settings)
    fragment.setTargetFragment(this@MessageDetailsFragment, REQUEST_CODE_SHOW_DIALOG_WITH_SEND_KEY_OPTION)
    fragment.show(parentFragmentManager, ChoosePublicKeyDialogFragment::class.java.simpleName)
  }

  /**
   * Send a template message with a sender public key.
   *
   * @param att An [AttachmentInfo] object which contains information about a sender public key.
   */
  private fun sendTemplateMsgWithPublicKey(att: AttachmentInfo?) {
    var atts: MutableList<AttachmentInfo>? = null
    if (att != null) {
      atts = ArrayList()
      att.isProtected = true
      atts.add(att)
    }

    startActivity(CreateMessageActivity.generateIntent(context, prepareMsgInfoForReply(), MessageType.REPLY,
        MessageEncryptionType.STANDARD,
        ServiceInfo(isToFieldEditable = false,
            isFromFieldEditable = false,
            isMsgEditable = false,
            isSubjectEditable = false,
            isMsgTypeSwitchable = false,
            hasAbilityToAddNewAtt = false,
            systemMsg = getString(R.string.message_was_encrypted_for_wrong_key),
            atts = atts)))
  }

  /**
   * Update actions visibility using [FoldersManager.FolderType]
   *
   * @param localFolder The localFolder where current message exists.
   */
  private fun updateActionsVisibility(localFolder: LocalFolder?, foldersManager: FoldersManager?) {
    folderType = FoldersManager.getFolderType(localFolder)
    if (folderType != null) {
      when (folderType) {
        FoldersManager.FolderType.INBOX -> {
          if (AccountEntity.ACCOUNT_TYPE_GOOGLE == account?.accountType) {
            isArchiveActionEnabled = true
          }
          isDeleteActionEnabled = true
        }

        FoldersManager.FolderType.SENT -> isDeleteActionEnabled = true

        FoldersManager.FolderType.TRASH -> {
          isMoveToInboxActionEnabled = true
          isDeleteActionEnabled = false
        }

        FoldersManager.FolderType.DRAFTS, FoldersManager.FolderType.OUTBOX -> {
          isMoveToInboxActionEnabled = false
          isArchiveActionEnabled = false
          isDeleteActionEnabled = true
        }

        else -> {
          isMoveToInboxActionEnabled = true
          isArchiveActionEnabled = false
          isDeleteActionEnabled = true
        }
      }
    } else {
      isArchiveActionEnabled = false
      isMoveToInboxActionEnabled = false
      isDeleteActionEnabled = true
    }

    if (foldersManager != null) {
      if (foldersManager.folderAll == null) {
        isArchiveActionEnabled = false
      }

      if (foldersManager.folderTrash == null) {
        isDeleteActionEnabled = false
      }
    } else {
      isArchiveActionEnabled = false
      isDeleteActionEnabled = false
    }

    when (msgEntity?.msgState) {
      MessageState.PENDING_ARCHIVING -> isArchiveActionEnabled = false
      else -> {
      }
    }

    activity?.invalidateOptionsMenu()
  }

  private fun initViews(view: View) {
    layoutActionProgress = view.findViewById(R.id.layoutActionProgress)
    textViewActionProgress = view.findViewById(R.id.textViewActionProgress)
    progressBarActionProgress = view.findViewById(R.id.progressBarActionProgress)

    textViewSenderAddress = view.findViewById(R.id.textViewSenderAddress)
    textViewDate = view.findViewById(R.id.textViewDate)
    textViewSubject = view.findViewById(R.id.textViewSubject)
    viewFooterOfHeader = view.findViewById(R.id.layoutFooterOfHeader)
    layoutMsgParts = view.findViewById(R.id.layoutMessageParts)
    contentView = view.findViewById(R.id.layoutMessageContainer)
    layoutReplyBtns = view.findViewById(R.id.layoutReplyButtons)
    progressBarActionRunning = view.findViewById(R.id.progressBarActionRunning)
    emailWebView = view.findViewById(R.id.emailWebView)

    layoutContent = view.findViewById(R.id.layoutContent)
    imageBtnReplyAll = view.findViewById(R.id.imageButtonReplyAll)
    imageBtnReplyAll?.setOnClickListener(this)
    imageBtnMoreOptions = view.findViewById(R.id.imageButtonMoreOptions)
    imageBtnMoreOptions?.setOnClickListener(this)
  }

  private fun updateViews() {
    msgEntity?.let {
      val subject = if (TextUtils.isEmpty(it.subject)) getString(R.string.no_subject) else
        it.subject

      if (folderType === FoldersManager.FolderType.SENT) {
        textViewSenderAddress?.text = EmailUtil.getFirstAddressString(it.to)
      } else {
        textViewSenderAddress?.text = EmailUtil.getFirstAddressString(it.from)
      }
      textViewSubject?.text = subject
      if (JavaEmailConstants.FOLDER_OUTBOX.equals(it.folder, ignoreCase = true)) {
        textViewDate?.text = DateTimeUtil.formatSameDayTime(context, it.sentDate ?: 0)
      } else {
        textViewDate?.text = DateTimeUtil.formatSameDayTime(context, it.receivedDate ?: 0)
      }
    }

    updateMsgBody()
  }

  private fun showAttsIfTheyExist() {
    if (atts.size > 0) {
      val layoutInflater = LayoutInflater.from(context)

      for (att in atts) {
        val rootView = layoutInflater.inflate(R.layout.attachment_item, layoutMsgParts, false)

        if (att.isDecrypted) {
          rootView.setBackgroundResource(R.drawable.bg_att_decrypted)
        }

        val textViewAttName = rootView.findViewById<TextView>(R.id.textViewAttachmentName)
        textViewAttName.text = att.name

        val textViewAttSize = rootView.findViewById<TextView>(R.id.textViewAttSize)
        textViewAttSize.text = Formatter.formatFileSize(context, att.encodedSize)

        val button = rootView.findViewById<View>(R.id.imageButtonDownloadAtt)
        button.setOnClickListener(getDownloadAttClickListener(att))

        if (att.uri != null) {
          val layoutAtt = rootView.findViewById<View>(R.id.layoutAtt)
          layoutAtt.setOnClickListener(getOpenFileClickListener(att, button))
        }

        layoutMsgParts?.addView(rootView)
      }
    }
  }

  private fun getOpenFileClickListener(att: AttachmentInfo, button: View): View.OnClickListener {
    return View.OnClickListener {
      if (att.uri?.lastPathSegment?.endsWith(Constants.PGP_FILE_EXT) == true) {
        button.performClick()
      } else {
        val intentOpenFile = Intent(Intent.ACTION_VIEW, att.uri)
        intentOpenFile.action = Intent.ACTION_VIEW
        intentOpenFile.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intentOpenFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (intentOpenFile.resolveActivity(requireContext().packageManager) != null) {
          startActivity(intentOpenFile)
        }
      }
    }
  }

  private fun getDownloadAttClickListener(att: AttachmentInfo): View.OnClickListener {
    return View.OnClickListener {
      lastClickedAtt = att
      lastClickedAtt?.orderNumber = GeneralUtil.genAttOrderId(requireContext())
      val isPermissionGranted = ContextCompat.checkSelfPermission(requireContext(),
          Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
      if (isPermissionGranted) {
        requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            REQUEST_CODE_REQUEST_WRITE_EXTERNAL_STORAGE)
      } else {
        context?.startService(AttachmentDownloadManagerService.newIntent(context, lastClickedAtt!!))
      }
    }
  }

  private fun updateMsgView() {
    emailWebView?.loadUrl("about:blank")
    val childCount = layoutMsgParts?.childCount ?: 0
    if (childCount > 1) {
      layoutMsgParts?.removeViews(1, childCount - 1)
    }

    var isFirstMsgPartText = true
    var isHtmlDisplayed = false

    for (block in msgInfo?.msgBlocks ?: emptyList()) {
      val layoutInflater = LayoutInflater.from(context)
      when (block.type) {
        MsgBlock.Type.DECRYPTED_HTML, MsgBlock.Type.PLAIN_HTML -> {
          if (!isHtmlDisplayed) {
            setupWebView(block)
            isHtmlDisplayed = true
          }
        }

        MsgBlock.Type.DECRYPTED_TEXT -> {
          msgEncryptType = MessageEncryptionType.ENCRYPTED
          layoutMsgParts?.addView(genDecryptedTextPart(block, layoutInflater))
        }

        MsgBlock.Type.PLAIN_TEXT -> {
          layoutMsgParts?.addView(genTextPart(block, layoutInflater))
          if (isFirstMsgPartText) {
            viewFooterOfHeader?.visibility = View.VISIBLE
          }
        }

        MsgBlock.Type.PUBLIC_KEY ->
          layoutMsgParts?.addView(genPublicKeyPart(block as PublicKeyMsgBlock, layoutInflater))

        MsgBlock.Type.DECRYPT_ERROR -> {
          msgEncryptType = MessageEncryptionType.ENCRYPTED
          layoutMsgParts?.addView(genDecryptErrorPart(block as DecryptErrorMsgBlock, layoutInflater))
        }

        MsgBlock.Type.DECRYPTED_ATT -> {
          val decryptAtt: DecryptedAttMsgBlock = block as DecryptedAttMsgBlock
          val att = EmailUtil.getAttInfoFromUri(activity, decryptAtt.fileUri)
          if (att != null) {
            att.isDecrypted = true
            att.uri?.path?.let {
              att.uri = FileProvider.getUriForFile(requireContext(), Constants.FILE_PROVIDER_AUTHORITY, File(it))
            }
            atts.add(att)
          }
        }

        else -> layoutMsgParts?.addView(genDefPart(block, layoutInflater,
            R.layout.message_part_other, layoutMsgParts))
      }
      isFirstMsgPartText = false
    }

    if (!isHtmlDisplayed) {
      updateReplyButtons()
    }
  }

  private fun setupWebView(block: MsgBlock) {
    emailWebView?.configure()

    val text = clipLargeText(block.content)

    emailWebView?.loadDataWithBaseURL(null, text, "text/html", StandardCharsets.UTF_8.displayName(), null)
    emailWebView?.setOnPageFinishedListener(object : EmailWebView.OnPageFinishedListener {
      override fun onPageFinished() {
        setActionProgress(100, null)
        updateReplyButtons()
        (activity as? MessageDetailsActivity)?.idlingForWebView?.setIdleState(true)
      }
    })
  }

  private fun clipLargeText(text: String?): String? {
    text?.let {
      return if (it.length > CONTENT_MAX_ALLOWED_LENGTH) {
        it.take(CONTENT_MAX_ALLOWED_LENGTH) + "\n\n" + getString(R.string.clipped_message_too_large)
      } else text
    }

    return text
  }

  /**
   * Update the reply buttons layout depending on the [MessageEncryptionType]
   */
  private fun updateReplyButtons() {
    if (layoutReplyBtns != null) {
      val imageViewReply = layoutReplyBtns!!.findViewById<ImageView>(R.id.imageViewReply)
      val imageViewReplyAll = layoutReplyBtns!!.findViewById<ImageView>(R.id.imageViewReplyAll)
      val imageViewFwd = layoutReplyBtns!!.findViewById<ImageView>(R.id.imageViewFwd)

      val textViewReply = layoutReplyBtns!!.findViewById<TextView>(R.id.textViewReply)
      val textViewReplyAll = layoutReplyBtns!!.findViewById<TextView>(R.id.textViewReplyAll)
      val textViewFwd = layoutReplyBtns!!.findViewById<TextView>(R.id.textViewFwd)

      if (msgEncryptType === MessageEncryptionType.ENCRYPTED) {
        imageViewReply.setImageResource(R.mipmap.ic_reply_green)
        imageViewReplyAll.setImageResource(R.mipmap.ic_reply_all_green)
        imageBtnReplyAll?.setImageResource(R.mipmap.ic_reply_all_green)
        imageViewFwd.setImageResource(R.mipmap.ic_forward_green)

        textViewReply.setText(R.string.reply_encrypted)
        textViewReplyAll.setText(R.string.reply_all_encrypted)
        textViewFwd.setText(R.string.forward_encrypted)
      } else {
        imageViewReply.setImageResource(R.mipmap.ic_reply_red)
        imageViewReplyAll.setImageResource(R.mipmap.ic_reply_all_red)
        imageBtnReplyAll?.setImageResource(R.mipmap.ic_reply_all_red)
        imageViewFwd.setImageResource(R.mipmap.ic_forward_red)

        textViewReply.setText(R.string.reply)
        textViewReplyAll.setText(R.string.reply_all)
        textViewFwd.setText(R.string.forward)
      }

      layoutReplyButton = layoutReplyBtns?.findViewById(R.id.layoutReplyButton)
      layoutReplyButton?.setOnClickListener(this)
      layoutFwdButton = layoutReplyBtns?.findViewById(R.id.layoutFwdButton)
      layoutFwdButton?.setOnClickListener(this)
      layoutReplyBtns?.findViewById<View>(R.id.layoutReplyAllButton)?.setOnClickListener(this)

      layoutReplyBtns?.visibility = View.VISIBLE
    }
  }

  /**
   * Generate the public key block. There we can see the public key msgEntity and save/update the
   * key owner information to the local database.
   *
   * @param block    The [PublicKeyMsgBlock] object which contains information about a public key and his owner.
   * @param inflater The [LayoutInflater] instance.
   * @return The generated view.
   */
  private fun genPublicKeyPart(block: PublicKeyMsgBlock, inflater: LayoutInflater): View {

    val pubKeyView = inflater.inflate(R.layout.message_part_public_key, layoutMsgParts, false) as ViewGroup
    val textViewPgpPublicKey = pubKeyView.findViewById<TextView>(R.id.textViewPgpPublicKey)
    val switchShowPublicKey = pubKeyView.findViewById<Switch>(R.id.switchShowPublicKey)

    switchShowPublicKey.setOnCheckedChangeListener { buttonView, isChecked ->
      TransitionManager.beginDelayedTransition(pubKeyView)
      textViewPgpPublicKey.visibility = if (isChecked) View.VISIBLE else View.GONE
      buttonView.setText(if (isChecked) R.string.hide_the_public_key else R.string.show_the_public_key)
    }

    val keyDetails = block.keyDetails
    val pgpContact = keyDetails?.primaryPgpContact ?: PgpContact(email = "")

    if (!TextUtils.isEmpty(pgpContact.email)) {
      val keyOwner = pubKeyView.findViewById<TextView>(R.id.textViewKeyOwnerTemplate)
      keyOwner.text = getString(R.string.template_message_part_public_key_owner, pgpContact.email)
    }

    val keyWords = pubKeyView.findViewById<TextView>(R.id.textViewKeyWordsTemplate)
    UIUtil.setHtmlTextToTextView(getString(R.string.template_message_part_public_key_key_words,
        keyDetails?.keywords), keyWords)

    val fingerprint = pubKeyView.findViewById<TextView>(R.id.textViewFingerprintTemplate)
    UIUtil.setHtmlTextToTextView(getString(R.string.template_message_part_public_key_fingerprint,
        GeneralUtil.doSectionsInText(" ", keyDetails?.fingerprint, 4)), fingerprint)

    textViewPgpPublicKey.text = block.content

    val existingPgpContact = block.existingPgpContact
    val button = pubKeyView.findViewById<Button>(R.id.buttonKeyAction)
    if (button != null) {
      if (existingPgpContact == null) {
        initSaveContactButton(block, button)
      } else if (TextUtils.isEmpty(existingPgpContact.longid)
          || keyDetails?.longId?.equals(existingPgpContact.longid!!, ignoreCase = true) == true) {
        initUpdateContactButton(block, button)
      } else {
        initReplaceContactButton(block, button)
      }
    }

    return pubKeyView
  }

  /**
   * Init the save contact button. When we press this button a new contact will be saved to the
   * local database.
   *
   * @param block  The [PublicKeyMsgBlock] object which contains information about a public key and his owner.
   * @param button The key action button.
   */
  private fun initSaveContactButton(block: PublicKeyMsgBlock, button: Button) {
    button.setText(R.string.save_contact)
    button.setOnClickListener { v ->
      val pgpContact = block.keyDetails?.primaryPgpContact
      if (pgpContact != null) {
        contactsViewModel.addContact(pgpContact)
        v.visibility = View.GONE
      } else {
        Toast.makeText(context, getString(R.string.contact_for_saving_not_found), Toast.LENGTH_SHORT).show()
      }
    }
  }

  /**
   * Init the update contact button. When we press this button the contact will be updated in the
   * local database.
   *
   * @param block  The [PublicKeyMsgBlock] object which contains information about a public key and his owner.
   * @param button The key action button.
   */
  private fun initUpdateContactButton(block: PublicKeyMsgBlock, button: Button) {
    button.setText(R.string.update_contact)
    button.setOnClickListener { v ->
      val pgpContact = block.keyDetails?.primaryPgpContact
      if (pgpContact != null) {
        contactsViewModel.updateContact(pgpContact)
        Toast.makeText(context, R.string.contact_successfully_updated, Toast.LENGTH_SHORT).show()
        v.visibility = View.GONE
      } else {
        Toast.makeText(context, getString(R.string.contact_for_updating_is_not_found), Toast.LENGTH_SHORT).show()
      }
    }
  }

  /**
   * Init the replace contact button. When we press this button the contact will be replaced in the
   * local database.
   *
   * @param block  The [PublicKeyMsgBlock] object which contains information about a public key and his owner.
   * @param button The key action button.
   */
  private fun initReplaceContactButton(block: PublicKeyMsgBlock, button: Button) {
    button.setText(R.string.replace_contact)
    button.setOnClickListener { v ->
      val pgpContact = block.keyDetails?.primaryPgpContact
      if (pgpContact != null) {
        contactsViewModel.updateContact(pgpContact)
        Toast.makeText(context, R.string.contact_successfully_replaced, Toast.LENGTH_SHORT).show()
        v.visibility = View.GONE
      } else {
        Toast.makeText(context, getString(R.string.contact_for_replacing_is_not_found), Toast.LENGTH_SHORT).show()
      }
    }
  }

  private fun genDefPart(block: MsgBlock, inflater: LayoutInflater, res: Int, viewGroup: ViewGroup?): TextView {
    val textViewMsgPartOther = inflater.inflate(res, viewGroup, false) as TextView
    textViewMsgPartOther.text = block.content
    return textViewMsgPartOther
  }

  private fun genTextPart(block: MsgBlock, layoutInflater: LayoutInflater): TextView {
    return genDefPart(block, layoutInflater, R.layout.message_part_text, layoutMsgParts)
  }

  private fun genDecryptedTextPart(block: MsgBlock, layoutInflater: LayoutInflater): View {
    return genDefPart(block, layoutInflater, R.layout.message_part_pgp_message, layoutMsgParts)
  }

  private fun genDecryptErrorPart(block: DecryptErrorMsgBlock, layoutInflater: LayoutInflater): View {
    val decryptError = block.error ?: return View(context)

    when (decryptError.details?.type) {
      DecryptErrorDetails.Type.KEY_MISMATCH -> return generateMissingPrivateKeyLayout(block.content, layoutInflater)

      DecryptErrorDetails.Type.FORMAT -> {
        val formatErrorMsg = (getString(R.string.decrypt_error_message_badly_formatted,
            getString(R.string.app_name)) + "\n\n"
            + decryptError.details.type + ": " + decryptError.details.message)
        return getView(block.content, formatErrorMsg, layoutInflater)
      }

      DecryptErrorDetails.Type.OTHER -> {
        val otherErrorMsg = getString(R.string.decrypt_error_could_not_open_message, getString(R.string.app_name)) +
            "\n\n" + getString(R.string.decrypt_error_please_write_me, getString(R.string.support_email)) +
            "\n\n" + decryptError.details.type + ": " + decryptError.details.message
        return getView(block.content, otherErrorMsg, layoutInflater)
      }

      else -> return getView(block.content, getString(R.string.could_not_decrypt_message_due_to_error,
          decryptError.details?.type.toString() + ": " + decryptError.details?.message),
          layoutInflater)
    }
  }

  private fun getView(originalMsg: String?, errorMsg: String, layoutInflater: LayoutInflater): View {
    val viewGroup = layoutInflater.inflate(R.layout.message_part_pgp_message_error,
        layoutMsgParts, false) as ViewGroup
    val textViewErrorMsg = viewGroup.findViewById<TextView>(R.id.textViewErrorMessage)
    ExceptionUtil.handleError(ManualHandledException(errorMsg))
    textViewErrorMsg.text = errorMsg
    viewGroup.addView(genShowOrigMsgLayout(originalMsg, layoutInflater, viewGroup))
    return viewGroup
  }

  /**
   * Generate a layout which describes the missing private keys situation.
   *
   * @param pgpMsg   The pgp message.
   * @param inflater The [LayoutInflater] instance.
   * @return Generated layout.
   */
  private fun generateMissingPrivateKeyLayout(pgpMsg: String?, inflater: LayoutInflater): View {
    val viewGroup = inflater.inflate(
        R.layout.message_part_pgp_message_missing_private_key, layoutMsgParts, false) as ViewGroup
    val textViewErrorMsg = viewGroup.findViewById<TextView>(R.id.textViewErrorMessage)
    textViewErrorMsg.text = getString(R.string.decrypt_error_current_key_cannot_open_message)

    val buttonImportPrivateKey = viewGroup.findViewById<Button>(R.id.buttonImportPrivateKey)
    buttonImportPrivateKey.setOnClickListener {
      startActivityForResult(ImportPrivateKeyActivity.getIntent(
          context = context,
          title = getString(R.string.import_private_key),
          throwErrorIfDuplicateFoundEnabled = true,
          cls = ImportPrivateKeyActivity::class.java,
          isUseExistingKeysEnabled = false,
          isSubmittingPubKeysEnabled = false,
          accountEntity = account),
          REQUEST_CODE_START_IMPORT_KEY_ACTIVITY)
    }

    val buttonSendOwnPublicKey = viewGroup.findViewById<Button>(R.id.buttonSendOwnPublicKey)
    buttonSendOwnPublicKey.setOnClickListener { showSendersPublicKeyDialog() }

    viewGroup.addView(genShowOrigMsgLayout(pgpMsg, inflater, viewGroup))
    return viewGroup
  }

  /**
   * Generate a layout with switch button which will be regulate visibility of original message info.
   *
   * @param msg            The original pgp message info.
   * @param layoutInflater The [LayoutInflater] instance.
   * @param rootView       The root view which will be used while we create a new layout using
   * [LayoutInflater].
   * @return A generated layout.
   */
  private fun genShowOrigMsgLayout(msg: String?, layoutInflater: LayoutInflater,
                                   rootView: ViewGroup): ViewGroup {
    val viewGroup = layoutInflater.inflate(R.layout.pgp_show_original_message, rootView, false) as ViewGroup
    val textViewOrigPgpMsg = viewGroup.findViewById<TextView>(R.id.textViewOrigPgpMsg)
    textViewOrigPgpMsg.text = msg

    val switchShowOrigMsg = viewGroup.findViewById<Switch>(R.id.switchShowOrigMsg)

    switchShowOrigMsg.setOnCheckedChangeListener { buttonView, isChecked ->
      TransitionManager.beginDelayedTransition(rootView)
      textViewOrigPgpMsg.visibility = if (isChecked) View.VISIBLE else View.GONE
      buttonView.setText(if (isChecked) R.string.hide_original_message else R.string.show_original_message)
    }
    return viewGroup
  }

  /**
   * To prevent TransactionTooLargeException we have to remove large objects from [IncomingMessageInfo]
   */
  private fun prepareMsgInfoForReply(): IncomingMessageInfo? {
    return msgInfo?.copy(msgBlocks = emptyList(), text = clipLargeText(msgInfo?.text))
  }

  private fun setupLabelsViewModel() {
    labelsViewModel.foldersManagerLiveData.observe(viewLifecycleOwner, Observer {
      updateActionsVisibility(localFolder, it)
    })
  }

  interface MessageDetailsListener {
    fun getMsgDetailsViewModel(): MsgDetailsViewModel?
  }

  companion object {
    private const val REQUEST_CODE_REQUEST_WRITE_EXTERNAL_STORAGE = 100
    private const val REQUEST_CODE_START_IMPORT_KEY_ACTIVITY = 101
    private const val REQUEST_CODE_SHOW_DIALOG_WITH_SEND_KEY_OPTION = 102
    private const val CONTENT_MAX_ALLOWED_LENGTH = 50000
  }
}
