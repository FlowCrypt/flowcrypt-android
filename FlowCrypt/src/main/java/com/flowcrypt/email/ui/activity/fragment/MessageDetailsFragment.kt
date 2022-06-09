/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.Manifest
import android.accounts.AuthenticatorException
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.format.DateUtils
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.text.toSpannable
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.model.ServiceInfo
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.DecryptErrorMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.DecryptedAttMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.MsgBlock
import com.flowcrypt.email.api.retrofit.response.model.OrgRules
import com.flowcrypt.email.api.retrofit.response.model.PublicKeyMsgBlock
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.databinding.FragmentMessageDetailsBinding
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.jakarta.mail.internet.getFormattedString
import com.flowcrypt.email.extensions.jakarta.mail.internet.personalOrEmail
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.showChoosePublicKeyDialogFragment
import com.flowcrypt.email.extensions.showInfoDialog
import com.flowcrypt.email.extensions.showNeedPassphraseDialog
import com.flowcrypt.email.extensions.showTwoWayDialog
import com.flowcrypt.email.extensions.supportActionBar
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.extensions.visibleOrGone
import com.flowcrypt.email.jetpack.viewmodel.LabelsViewModel
import com.flowcrypt.email.jetpack.viewmodel.MsgDetailsViewModel
import com.flowcrypt.email.jetpack.viewmodel.RecipientsViewModel
import com.flowcrypt.email.jetpack.workmanager.sync.ArchiveMsgsWorker
import com.flowcrypt.email.jetpack.workmanager.sync.DeleteMessagesPermanentlyWorker
import com.flowcrypt.email.jetpack.workmanager.sync.DeleteMessagesWorker
import com.flowcrypt.email.jetpack.workmanager.sync.MovingToInboxWorker
import com.flowcrypt.email.jetpack.workmanager.sync.UpdateMsgsSeenStateWorker
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import com.flowcrypt.email.service.attachment.AttachmentDownloadManagerService
import com.flowcrypt.email.ui.activity.CreateMessageActivity
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.ui.activity.fragment.dialog.ChoosePublicKeyDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.DownloadAttachmentDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.TwoWayDialogFragment
import com.flowcrypt.email.ui.adapter.AttachmentsRecyclerViewAdapter
import com.flowcrypt.email.ui.adapter.MsgDetailsRecyclerViewAdapter
import com.flowcrypt.email.ui.adapter.PgpBadgeListAdapter
import com.flowcrypt.email.ui.adapter.recyclerview.itemdecoration.MarginItemDecoration
import com.flowcrypt.email.ui.adapter.recyclerview.itemdecoration.VerticalSpaceMarginItemDecoration
import com.flowcrypt.email.ui.widget.EmailWebView
import com.flowcrypt.email.util.CacheManager
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.FileAndDirectoryUtils
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.exception.CommonConnectionException
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.ManualHandledException
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.material.snackbar.Snackbar
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import jakarta.mail.AuthenticationFailedException
import jakarta.mail.internet.InternetAddress
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
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
class MessageDetailsFragment : BaseFragment<FragmentMessageDetailsBinding>(), ProgressBehaviour,
  View.OnClickListener {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentMessageDetailsBinding.inflate(inflater, container, false)

  override val progressView: View?
    get() = binding?.progress?.root
  override val contentView: View?
    get() = binding?.layoutContent
  override val statusView: View?
    get() = binding?.status?.root

  private val args by navArgs<MessageDetailsFragmentArgs>()
  private val msgDetailsViewModel: MsgDetailsViewModel by viewModels {
    object : ViewModelProvider.AndroidViewModelFactory(requireActivity().application) {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MsgDetailsViewModel(
          args.localFolder, args.messageEntity, requireActivity().application
        ) as T
      }
    }
  }

  private val attachmentsRecyclerViewAdapter = AttachmentsRecyclerViewAdapter(
    object : AttachmentsRecyclerViewAdapter.AttachmentActionListener {
      override fun onDownloadClick(attachmentInfo: AttachmentInfo) {
        lastClickedAtt = attachmentInfo
        lastClickedAtt?.orderNumber = GeneralUtil.genAttOrderId(requireContext())

        if (SecurityUtils.isPossiblyEncryptedData(attachmentInfo.name)) {
          for (block in msgInfo?.msgBlocks ?: emptyList()) {
            if (block.type == MsgBlock.Type.DECRYPT_ERROR) {
              val decryptErrorMsgBlock = block as? DecryptErrorMsgBlock ?: continue
              val decryptErrorDetails = decryptErrorMsgBlock.decryptErr?.details ?: continue
              if (decryptErrorDetails.type == PgpDecryptAndOrVerify.DecryptionErrorType.NEED_PASSPHRASE) {
                val fingerprints = decryptErrorMsgBlock.decryptErr.fingerprints ?: continue
                showNeedPassphraseDialog(
                  fingerprints
                )
                return
              }
            }
          }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
          ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE
          ) == PackageManager.PERMISSION_GRANTED
        ) {
          downloadAttachment()
        } else {
          requestPermissions(
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            REQUEST_CODE_REQUEST_WRITE_EXTERNAL_STORAGE
          )
        }
      }

      override fun onAttachmentClick(attachmentInfo: AttachmentInfo) {
        if (account?.isRuleExist(OrgRules.DomainRule.RESTRICT_ANDROID_ATTACHMENT_HANDLING) == true) {
          if (attachmentInfo.uri != null || attachmentInfo.rawData?.isNotEmpty() == true) {
            previewAttachment(
              attachmentInfo = attachmentInfo,
              useEnterpriseBehaviour = true
            )
          }
        }
      }

      override fun onAttachmentPreviewClick(attachmentInfo: AttachmentInfo) {
        if (account?.isRuleExist(OrgRules.DomainRule.RESTRICT_ANDROID_ATTACHMENT_HANDLING) == true) {
          if (attachmentInfo.uri != null || attachmentInfo.rawData?.isNotEmpty() == true) {
            previewAttachment(
              attachmentInfo = attachmentInfo,
              useEnterpriseBehaviour = true
            )
          } else {
            navController?.navigate(
              MessageDetailsFragmentDirections
                .actionMessageDetailsFragmentToDownloadAttachmentDialogFragment(
                  attachmentInfo, REQUEST_CODE_PREVIEW_ATTACHMENT
                )
            )
          }
        }
      }
    })

  private var msgInfo: IncomingMessageInfo? = null
  private var folderType: FoldersManager.FolderType? = null
  private val labelsViewModel: LabelsViewModel by viewModels()
  private val recipientsViewModel: RecipientsViewModel by viewModels()
  private val msgDetailsAdapter = MsgDetailsRecyclerViewAdapter()
  private val pgpBadgeListAdapter = PgpBadgeListAdapter()

  private var isAdditionalActionEnabled: Boolean = false
  private var isDeleteActionEnabled: Boolean = false
  private var isArchiveActionEnabled: Boolean = false
  private var isMoveToInboxActionEnabled: Boolean = false
  private var lastClickedAtt: AttachmentInfo? = null
  private var msgEncryptType = MessageEncryptionType.STANDARD

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
    subscribeToDownloadAttachmentViaDialog()
    subscribeToImportingAdditionalPrivateKeys()
    updateActionsVisibility(args.localFolder, null)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    updateViews()

    setupLabelsViewModel()
    setupMsgDetailsViewModel()
    setupRecipientsViewModel()
    collectReVerifySignaturesStateFlow()
    subscribeToTwoWayDialog()
    subscribeToChoosePublicKeyDialogFragment()
  }

  override fun onDestroy() {
    super.onDestroy()
    FileAndDirectoryUtils.cleanDir(CacheManager.getCurrentMsgTempDir())
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
    menuActionMarkUnread?.isVisible =
      !JavaEmailConstants.FOLDER_OUTBOX.equals(args.messageEntity.folder, ignoreCase = true)

    menuItemArchiveMsg?.isEnabled = isAdditionalActionEnabled
    menuItemDeleteMsg?.isEnabled = isAdditionalActionEnabled
    menuActionMoveToInbox?.isEnabled = isAdditionalActionEnabled
    menuActionMarkUnread?.isEnabled = isAdditionalActionEnabled

    args.localFolder.searchQuery?.let {
      menuItemArchiveMsg?.isVisible = false
      menuItemDeleteMsg?.isVisible = false
      menuActionMoveToInbox?.isVisible = false
      menuActionMarkUnread?.isVisible = false
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menuActionArchiveMessage -> {
        msgDetailsViewModel.changeMsgState(MessageState.PENDING_ARCHIVING)
        true
      }

      R.id.menuActionDeleteMessage -> {
        if (JavaEmailConstants.FOLDER_OUTBOX.equals(args.messageEntity.folder, ignoreCase = true)) {
          val msgEntity = args.messageEntity

          msgEntity.let {
            if (msgEntity.msgState === MessageState.SENDING) {
              Toast.makeText(context, R.string.can_not_delete_sending_message, Toast.LENGTH_LONG)
                .show()
            } else {
              msgDetailsViewModel.deleteMsg()
              Toast.makeText(context, R.string.message_was_deleted, Toast.LENGTH_SHORT).show()
            }
          }
        } else {
          if (args.localFolder.getFolderType() == FoldersManager.FolderType.TRASH) {
            showTwoWayDialog(
              requestCode = REQUEST_CODE_DELETE_MESSAGE_DIALOG,
              dialogTitle = "",
              dialogMsg = requireContext().resources.getQuantityString(
                R.plurals.delete_msg_question,
                1,
                1
              ),
              positiveButtonTitle = getString(android.R.string.ok),
              negativeButtonTitle = getString(android.R.string.cancel),
            )
          } else {
            msgDetailsViewModel.changeMsgState(MessageState.PENDING_DELETING)
          }
        }
        true
      }

      R.id.menuActionMoveToInbox -> {
        msgDetailsViewModel.changeMsgState(MessageState.PENDING_MOVE_TO_INBOX)
        true
      }

      R.id.menuActionMarkUnread -> {
        msgDetailsViewModel.changeMsgState(MessageState.PENDING_MARK_UNREAD)
        true
      }

      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.layoutReplyButton -> {
        startActivity(
          CreateMessageActivity.generateIntent(
            context, MessageType.REPLY, msgEncryptType, prepareMsgInfoForReply()
          )
        )
      }

      R.id.imageButtonReplyAll, R.id.layoutReplyAllButton -> {
        startActivity(
          CreateMessageActivity.generateIntent(
            context, MessageType.REPLY_ALL, msgEncryptType, prepareMsgInfoForReply()
          )
        )
      }

      R.id.imageButtonMoreOptions -> {
        val popup = PopupMenu(requireContext(), v)
        popup.menuInflater.inflate(R.menu.popup_reply_actions, popup.menu)
        popup.setOnMenuItemClickListener {
          when (it.itemId) {
            R.id.menuActionReply -> {
              binding?.layoutReplyButtons?.layoutReplyButton?.let { view -> onClick(view) }
              true
            }

            R.id.menuActionForward -> {
              binding?.layoutReplyButtons?.layoutFwdButton?.let { view -> onClick(view) }
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
          msgInfo?.atts =
            attachmentsRecyclerViewAdapter.currentList.map {
              it.copy(
                isForwarded = true,
                name = if (it.isPossiblyEncrypted()) FilenameUtils.removeExtension(it.name) else it.name,
                decryptWhenForward = it.isPossiblyEncrypted()
              )
            }
        } else {
          msgInfo?.atts =
            attachmentsRecyclerViewAdapter.currentList.map { it.copy(isForwarded = true) }
        }

        startActivity(
          CreateMessageActivity.generateIntent(
            context, MessageType.FORWARD, msgEncryptType, prepareMsgInfoForReply()
          )
        )
      }
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    when (requestCode) {
      REQUEST_CODE_REQUEST_WRITE_EXTERNAL_STORAGE -> {
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
          downloadAttachment()
        } else {
          Toast.makeText(
            activity,
            R.string.cannot_save_attachment_without_permission,
            Toast.LENGTH_LONG
          ).show()
        }
      }

      else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
  }

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    super.onAccountInfoRefreshed(accountEntity)
    attachmentsRecyclerViewAdapter.isPreviewEnabled =
      account?.isRuleExist(OrgRules.DomainRule.RESTRICT_ANDROID_ATTACHMENT_HANDLING) == true
  }

  /**
   * Show an incoming message info.
   *
   * @param msgInfo An incoming message info
   */
  private fun showIncomingMsgInfo(msgInfo: IncomingMessageInfo) {
    this.msgInfo = msgInfo
    this.msgEncryptType = msgInfo.encryptionType
    binding?.imageButtonReplyAll?.visibility = View.VISIBLE
    binding?.imageButtonMoreOptions?.visibility = View.VISIBLE
    isAdditionalActionEnabled = true
    activity?.invalidateOptionsMenu()
    msgInfo.localFolder = args.localFolder

    msgInfo.inlineSubject?.let { binding?.textViewSubject?.text = it }

    updateMsgBody()
    showContent()
  }

  private fun setActionProgress(progress: Int, message: String? = null) {
    if (progress > 0) {
      binding?.progressBarActionProgress?.progress = progress
    }

    if (progress != 100) {
      binding?.textViewActionProgress?.text =
        getString(R.string.progress_message, progress, message)
      binding?.textViewActionProgress?.visibility = View.VISIBLE
    } else {
      binding?.textViewActionProgress?.text = null
      binding?.layoutActionProgress?.visibility = View.GONE
    }
  }

  private fun updateMsgBody() {
    if (msgInfo != null) {
      updatePgpBadges()
      updateMsgView()
    }
  }

  private fun updatePgpBadges() {
    msgInfo?.verificationResult?.let { verificationResult ->
      val badges = mutableListOf<PgpBadgeListAdapter.PgpBadge>()

      if (msgInfo?.encryptionType == MessageEncryptionType.ENCRYPTED) {
        badges.add(PgpBadgeListAdapter.PgpBadge(PgpBadgeListAdapter.PgpBadge.Type.ENCRYPTED))
      } else {
        badges.add(PgpBadgeListAdapter.PgpBadge(PgpBadgeListAdapter.PgpBadge.Type.NOT_ENCRYPTED))
      }

      if (verificationResult.hasSignedParts) {
        val badge = when {
          verificationResult.hasBadSignatures -> {
            PgpBadgeListAdapter.PgpBadge(PgpBadgeListAdapter.PgpBadge.Type.BAD_SIGNATURE)
          }

          verificationResult.hasUnverifiedSignatures -> {
            if (recipientsViewModel.recipientsFromLiveData.value?.data != null) {
              PgpBadgeListAdapter.PgpBadge(PgpBadgeListAdapter.PgpBadge.Type.CAN_NOT_VERIFY_SIGNATURE)
            } else {
              val fromAddresses = msgInfo?.getFrom()
              if (fromAddresses?.isNotEmpty() == true) {
                recipientsViewModel.fetchAndUpdateInfoAboutRecipients(
                  RecipientsViewModel.FROM,
                  fromAddresses.map { it.address.lowercase() })
                PgpBadgeListAdapter.PgpBadge(PgpBadgeListAdapter.PgpBadge.Type.VERIFYING_SIGNATURE)
              } else {
                PgpBadgeListAdapter.PgpBadge(PgpBadgeListAdapter.PgpBadge.Type.CAN_NOT_VERIFY_SIGNATURE)
              }
            }
          }

          verificationResult.isPartialSigned -> {
            PgpBadgeListAdapter.PgpBadge(PgpBadgeListAdapter.PgpBadge.Type.ONLY_PARTIALLY_SIGNED)
          }

          !verificationResult.isPartialSigned && verificationResult.hasMixedSignatures -> {
            PgpBadgeListAdapter.PgpBadge(PgpBadgeListAdapter.PgpBadge.Type.MIXED_SIGNED)
          }

          else -> PgpBadgeListAdapter.PgpBadge(PgpBadgeListAdapter.PgpBadge.Type.SIGNED)
        }
        badges.add(badge)
      } else {
        badges.add(PgpBadgeListAdapter.PgpBadge(PgpBadgeListAdapter.PgpBadge.Type.NOT_SIGNED))
      }
      pgpBadgeListAdapter.submitList(badges)
    }
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
    showChoosePublicKeyDialogFragment(
      args.messageEntity.email,
      ListView.CHOICE_MODE_SINGLE, R.plurals.tell_sender_to_update_their_settings
    )
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

    startActivity(
      CreateMessageActivity.generateIntent(
        context, MessageType.REPLY,
        MessageEncryptionType.STANDARD, prepareMsgInfoForReply(),
        ServiceInfo(
          isToFieldEditable = false,
          isFromFieldEditable = false,
          isMsgEditable = false,
          isSubjectEditable = false,
          isMsgTypeSwitchable = false,
          hasAbilityToAddNewAtt = false,
          systemMsg = getString(R.string.message_was_encrypted_for_wrong_key),
          atts = atts
        )
      )
    )
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
          isDeleteActionEnabled = true
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

    when (args.messageEntity.msgState) {
      MessageState.PENDING_ARCHIVING -> isArchiveActionEnabled = false
      else -> {
      }
    }

    activity?.invalidateOptionsMenu()
  }

  private fun updateActionBar(messageEntity: MessageEntity) {
    var actionBarTitle: String? = null
    var actionBarSubTitle: String? = null

    if (JavaEmailConstants.FOLDER_OUTBOX.equals(messageEntity.folder, ignoreCase = true)) {
      actionBarTitle = getString(R.string.outgoing)
      actionBarSubTitle = when (messageEntity.msgState) {
        MessageState.NEW,
        MessageState.NEW_FORWARDED,
        MessageState.NEW_PASSWORD_PROTECTED -> getString(R.string.preparing)

        MessageState.QUEUED, MessageState.QUEUED_MAKE_COPY_IN_SENT_FOLDER -> getString(R.string.queued)

        MessageState.SENDING -> getString(R.string.sending)

        MessageState.SENT, MessageState.SENT_WITHOUT_LOCAL_COPY -> getString(R.string.sent)

        MessageState.ERROR_CACHE_PROBLEM,
        MessageState.ERROR_DURING_CREATION,
        MessageState.ERROR_ORIGINAL_MESSAGE_MISSING,
        MessageState.ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND,
        MessageState.ERROR_SENDING_FAILED,
        MessageState.ERROR_PRIVATE_KEY_NOT_FOUND,
        MessageState.ERROR_COPY_NOT_SAVED_IN_SENT_FOLDER,
        MessageState.ERROR_PASSWORD_PROTECTED -> getString(R.string.an_error_has_occurred)

        else -> null
      }
    } else when (messageEntity.msgState) {
      MessageState.PENDING_ARCHIVING -> actionBarTitle = getString(R.string.pending)
      else -> {
      }
    }

    supportActionBar?.title = actionBarTitle
    supportActionBar?.subtitle = actionBarSubTitle
  }

  private fun updateViews() {
    binding?.imageButtonReplyAll?.setOnClickListener(this)
    binding?.imageButtonMoreOptions?.setOnClickListener(this)

    binding?.iBShowDetails?.setOnClickListener {
      binding?.rVMsgDetails?.visibleOrGone(!(binding?.rVMsgDetails?.isVisible ?: false))
      binding?.textViewDate?.visibleOrGone(!(binding?.rVMsgDetails?.isVisible ?: false))
    }

    updateMsgDetails()

    binding?.rVAttachments?.apply {
      layoutManager = LinearLayoutManager(context)
      addItemDecoration(
        MarginItemDecoration(
          marginBottom = resources.getDimensionPixelSize(R.dimen.default_margin_content_small)
        )
      )
      adapter = attachmentsRecyclerViewAdapter
    }

    val subject =
      if (TextUtils.isEmpty(args.messageEntity.subject)) getString(R.string.no_subject) else
        args.messageEntity.subject

    if (folderType === FoldersManager.FolderType.SENT) {
      binding?.textViewSenderAddress?.text = EmailUtil.getFirstAddressString(args.messageEntity.to)
    } else {
      binding?.textViewSenderAddress?.text =
        EmailUtil.getFirstAddressString(args.messageEntity.from)
    }
    binding?.textViewSubject?.text = subject
    if (JavaEmailConstants.FOLDER_OUTBOX.equals(args.messageEntity.folder, ignoreCase = true)) {
      binding?.textViewDate?.text =
        DateTimeUtil.formatSameDayTime(context, args.messageEntity.sentDate ?: 0)
    } else {
      binding?.textViewDate?.text =
        DateTimeUtil.formatSameDayTime(context, args.messageEntity.receivedDate ?: 0)
    }

    updateMsgBody()
  }

  private fun updateMsgDetails() {
    binding?.rVMsgDetails?.apply {
      layoutManager = LinearLayoutManager(context)
      addItemDecoration(
        VerticalSpaceMarginItemDecoration(
          marginTop = 0,
          marginBottom = 0,
          marginInternal = resources.getDimensionPixelSize(R.dimen.default_margin_content_small)
        )
      )
      adapter = msgDetailsAdapter
    }

    binding?.rVPgpBadges?.apply {
      layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
      addItemDecoration(
        MarginItemDecoration(
          marginRight = resources.getDimensionPixelSize(R.dimen.default_margin_small)
        )
      )
      adapter = pgpBadgeListAdapter
    }

    binding?.tVTo?.text = prepareToText()

    val headers = mutableListOf<MsgDetailsRecyclerViewAdapter.Header>().apply {
      add(
        MsgDetailsRecyclerViewAdapter.Header(
          name = getString(R.string.from),
          value = formatAddresses(args.messageEntity.from)
        )
      )

      if (args.messageEntity.replyToAddress.isNotEmpty()) {
        add(
          MsgDetailsRecyclerViewAdapter.Header(
            name = getString(R.string.reply_to),
            value = formatAddresses(args.messageEntity.replyToAddress)
          )
        )
      }

      add(
        MsgDetailsRecyclerViewAdapter.Header(
          name = getString(R.string.to),
          value = formatAddresses(args.messageEntity.to)
        )
      )

      if (args.messageEntity.cc.isNotEmpty()) {
        add(
          MsgDetailsRecyclerViewAdapter.Header(
            name = getString(R.string.cc),
            value = formatAddresses(args.messageEntity.cc)
          )
        )
      }

      add(
        MsgDetailsRecyclerViewAdapter.Header(
          name = getString(R.string.date),
          value = prepareDateHeaderValue()
        )
      )
    }

    msgDetailsAdapter.submitList(headers)
  }

  private fun prepareDateHeaderValue(): String {
    val dateInMilliseconds: Long =
      if (JavaEmailConstants.FOLDER_OUTBOX.equals(args.messageEntity.folder, ignoreCase = true)) {
        args.messageEntity.sentDate ?: 0
      } else {
        args.messageEntity.receivedDate ?: 0
      }

    val flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or
        DateUtils.FORMAT_SHOW_YEAR
    return DateUtils.formatDateTime(context, dateInMilliseconds, flags)
  }

  private fun prepareToText(): String {
    val stringBuilder = SpannableStringBuilder()
    val meAddress = args.messageEntity.to.firstOrNull {
      it.address.equals(args.messageEntity.email, true)
    }
    val leftAddresses: List<InternetAddress>
    if (meAddress == null) {
      leftAddresses = args.messageEntity.to
    } else {
      stringBuilder.append(getString(R.string.me))
      leftAddresses = ArrayList(args.messageEntity.to) - meAddress
      if (leftAddresses.isNotEmpty()) {
        stringBuilder.append(", ")
      }
    }

    val to = leftAddresses.foldIndexed(stringBuilder) { index, builder, it ->
      builder.append(it.personalOrEmail)
      if (index != leftAddresses.size - 1) {
        builder.append(",")
      }
      builder
    }.toSpannable()

    return getString(R.string.to_receiver, to)
  }

  private fun formatAddresses(addresses: List<InternetAddress>) =
    addresses.foldIndexed(SpannableStringBuilder()) { index, builder, it ->
      if (index < MAX_ALLOWED_RECEPIENTS_IN_HEADER_VALUE) {
        builder.append(it.getFormattedString())
        if (index != addresses.size - 1) {
          builder.append("\n")
        }
      } else if (index == MAX_ALLOWED_RECEPIENTS_IN_HEADER_VALUE + 1) {
        builder.append(getString(R.string.and_others))
      }
      builder
    }.toSpannable()

  private fun updateMsgView() {
    val inlineEncryptedAtts = mutableListOf<AttachmentInfo>()
    binding?.emailWebView?.loadUrl("about:blank")
    binding?.layoutMessageParts?.removeAllViews()

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
          binding?.layoutMessageParts?.addView(genDecryptedTextPart(block, layoutInflater))
        }

        MsgBlock.Type.PLAIN_TEXT -> {
          binding?.layoutMessageParts?.addView(genTextPart(block, layoutInflater))
          if (isFirstMsgPartText) {
            binding?.layoutFooterOfHeader?.visibility = View.VISIBLE
          }
        }

        MsgBlock.Type.PUBLIC_KEY ->
          binding?.layoutMessageParts?.addView(
            genPublicKeyPart(
              block as PublicKeyMsgBlock,
              layoutInflater
            )
          )

        MsgBlock.Type.DECRYPT_ERROR -> {
          msgEncryptType = MessageEncryptionType.ENCRYPTED
          binding?.layoutMessageParts?.addView(
            genDecryptErrorPart(
              block as DecryptErrorMsgBlock,
              layoutInflater
            )
          )
        }

        MsgBlock.Type.DECRYPTED_ATT -> {
          val decryptAtt = block as? DecryptedAttMsgBlock
          if (decryptAtt != null) {
            inlineEncryptedAtts.add(decryptAtt.toAttachmentInfo())
          } else {
            handleOtherBlock(block, layoutInflater)
          }
        }

        else -> handleOtherBlock(block, layoutInflater)
      }
      isFirstMsgPartText = false
    }

    if (!isHtmlDisplayed) {
      updateReplyButtons()
    }

    if (inlineEncryptedAtts.isNotEmpty()) {
      inlineEncryptedAtts.addAll(attachmentsRecyclerViewAdapter.currentList)
      attachmentsRecyclerViewAdapter.submitList(inlineEncryptedAtts)
    }
  }

  private fun handleOtherBlock(
    block: @JvmSuppressWildcards MsgBlock,
    layoutInflater: LayoutInflater
  ) {
    binding?.layoutMessageParts?.addView(
      genDefPart(
        block, layoutInflater,
        R.layout.message_part_other, binding?.layoutMessageParts
      )
    )
  }

  private fun setupWebView(block: MsgBlock) {
    binding?.emailWebView?.configure()

    val text = clipLargeText(block.content) ?: ""

    binding?.emailWebView?.loadDataWithBaseURL(
      null,
      text,
      "text/html",
      StandardCharsets.UTF_8.displayName(),
      null
    )
    binding?.emailWebView?.setOnPageLoadingListener(object : EmailWebView.OnPageLoadingListener {
      override fun onPageLoading(newProgress: Int) {
        when (newProgress) {
          0 -> countingIdlingResource?.incrementSafely()

          100 -> {
            setActionProgress(100, null)
            updateReplyButtons()
            countingIdlingResource?.decrementSafely()
          }
        }
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
    if (binding?.layoutReplyButtons != null) {
      val imageViewReply = binding?.layoutReplyButtons?.imageViewReply
      val imageViewReplyAll = binding?.layoutReplyButtons?.imageViewReplyAll
      val imageViewFwd = binding?.layoutReplyButtons?.imageViewFwd

      val textViewReply = binding?.layoutReplyButtons?.textViewReply
      val textViewReplyAll = binding?.layoutReplyButtons?.textViewReplyAll
      val textViewFwd = binding?.layoutReplyButtons?.textViewFwd

      if (msgEncryptType === MessageEncryptionType.ENCRYPTED) {
        imageViewReply?.setImageResource(R.mipmap.ic_reply_green)
        imageViewReplyAll?.setImageResource(R.mipmap.ic_reply_all_green)
        binding?.imageButtonReplyAll?.setImageResource(R.mipmap.ic_reply_all_green)
        imageViewFwd?.setImageResource(R.mipmap.ic_forward_green)

        textViewReply?.setText(R.string.reply_encrypted)
        textViewReplyAll?.setText(R.string.reply_all_encrypted)
        textViewFwd?.setText(R.string.forward_encrypted)
      } else {
        imageViewReply?.setImageResource(R.mipmap.ic_reply_red)
        imageViewReplyAll?.setImageResource(R.mipmap.ic_reply_all_red)
        binding?.imageButtonReplyAll?.setImageResource(R.mipmap.ic_reply_all_red)
        imageViewFwd?.setImageResource(R.mipmap.ic_forward_red)

        textViewReply?.setText(R.string.reply)
        textViewReplyAll?.setText(R.string.reply_all)
        textViewFwd?.setText(R.string.forward)
      }

      binding?.layoutReplyButtons?.layoutReplyButton?.setOnClickListener(this)
      binding?.layoutReplyButtons?.layoutFwdButton?.setOnClickListener(this)
      binding?.layoutReplyButtons?.layoutReplyAllButton?.setOnClickListener(this)
      binding?.layoutReplyButtons?.root?.visibility = View.VISIBLE
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
    if (block.error?.errorMsg?.isNotEmpty() == true) {
      return getView(
        clipLargeText(block.content),
        getString(R.string.msg_contains_not_valid_pub_key, block.error.errorMsg),
        layoutInflater
      )
    }

    val pubKeyView = inflater.inflate(
      R.layout.message_part_public_key,
      binding?.layoutMessageParts,
      false
    ) as ViewGroup
    val textViewPgpPublicKey = pubKeyView.findViewById<TextView>(R.id.textViewPgpPublicKey)
    val switchShowPublicKey = pubKeyView.findViewById<CompoundButton>(R.id.switchShowPublicKey)

    switchShowPublicKey.setOnCheckedChangeListener { buttonView, isChecked ->
      TransitionManager.beginDelayedTransition(pubKeyView)
      textViewPgpPublicKey.visibility = if (isChecked) View.VISIBLE else View.GONE
      buttonView.setText(if (isChecked) R.string.hide_the_public_key else R.string.show_the_public_key)
    }

    val keyDetails = block.keyDetails
    val userIds = keyDetails?.getUserIdsAsSingleString()

    if (userIds?.isNotEmpty() == true) {
      val keyOwner = pubKeyView.findViewById<TextView>(R.id.textViewKeyOwnerTemplate)
      keyOwner.text = getString(R.string.template_message_part_public_key_owner, userIds)
    }

    val fingerprint = pubKeyView.findViewById<TextView>(R.id.textViewFingerprintTemplate)
    UIUtil.setHtmlTextToTextView(
      getString(
        R.string.template_message_part_public_key_fingerprint,
        GeneralUtil.doSectionsInText(" ", keyDetails?.fingerprint, 4)
      ), fingerprint
    )

    textViewPgpPublicKey.text = clipLargeText(block.keyDetails?.publicKey ?: block.content)

    val existingRecipientWithPubKeys = block.existingRecipientWithPubKeys
    val button = pubKeyView.findViewById<Button>(R.id.buttonKeyAction)
    val textViewStatus = pubKeyView.findViewById<TextView>(R.id.textViewStatus)
    val textViewManualImportWarning =
      pubKeyView.findViewById<TextView>(R.id.textViewManualImportWarning)
    if (keyDetails?.usableForEncryption == true) {
      if (button != null) {
        if (existingRecipientWithPubKeys == null) {
          initImportPubKeyButton(keyDetails, button)
        } else {
          val matchingKeyByFingerprint = existingRecipientWithPubKeys.publicKeys.firstOrNull {
            it.fingerprint.equals(keyDetails.fingerprint, true)
          }
          if (matchingKeyByFingerprint != null) {
            if (keyDetails.isNewerThan(matchingKeyByFingerprint.pgpKeyDetails)) {
              textViewManualImportWarning?.visible()
              initUpdatePubKeyButton(matchingKeyByFingerprint, keyDetails, button)
            } else {
              textViewManualImportWarning?.gone()
              textViewStatus?.text = getString(R.string.already_imported)
              textViewStatus.visible()
              button.gone()
            }
          } else {
            button.gone()
          }
        }
      }
    } else {
      textViewStatus.text = getString(R.string.cannot_be_used_for_encryption)
      textViewStatus.setTextColor(UIUtil.getColor(requireContext(), R.color.red))
      textViewManualImportWarning?.gone()
      textViewStatus.visible()
      button?.gone()
    }

    return pubKeyView
  }

  private fun initImportPubKeyButton(pgpKeyDetails: PgpKeyDetails?, button: Button) {
    button.setText(R.string.import_pub_key)
    button.setOnClickListener { v ->
      if (pgpKeyDetails != null) {
        recipientsViewModel.addRecipientsBasedOnPgpKeyDetails(pgpKeyDetails)
        toast(R.string.pub_key_successfully_imported)
        v.gone()
      } else {
        toast(R.string.pub_key_for_saving_not_found)
      }
    }
  }

  private fun initUpdatePubKeyButton(
    publicKeyEntity: PublicKeyEntity,
    pgpKeyDetails: PgpKeyDetails?,
    button: Button
  ) {
    button.setText(R.string.update_pub_key)
    button.setOnClickListener { v ->
      if (pgpKeyDetails != null) {
        recipientsViewModel.updateExistingPubKey(publicKeyEntity, pgpKeyDetails)
        toast(R.string.pub_key_successfully_updated)
        v.gone()
      } else {
        toast(R.string.pub_key_for_updating_is_not_found)
      }
    }
  }

  private fun genDefPart(
    block: MsgBlock,
    inflater: LayoutInflater,
    res: Int,
    viewGroup: ViewGroup?
  ): View {
    val errorMsg = block.error?.errorMsg
    return if (errorMsg?.isNotEmpty() == true) {
      getView(
        null,
        getString(R.string.msg_contains_not_valid_block, block.type.toString(), errorMsg),
        layoutInflater
      )
    } else {
      val textViewMsgPartOther = inflater.inflate(res, viewGroup, false) as TextView
      textViewMsgPartOther.text = clipLargeText(block.content)
      textViewMsgPartOther
    }
  }

  private fun genTextPart(block: MsgBlock, layoutInflater: LayoutInflater): View {
    return genDefPart(
      block,
      layoutInflater,
      R.layout.message_part_text,
      binding?.layoutMessageParts
    )
  }

  private fun genDecryptedTextPart(block: MsgBlock, layoutInflater: LayoutInflater): View {
    return genDefPart(
      block,
      layoutInflater,
      R.layout.message_part_pgp_message,
      binding?.layoutMessageParts
    )
  }

  private fun genDecryptErrorPart(
    block: DecryptErrorMsgBlock,
    layoutInflater: LayoutInflater
  ): View {
    val decryptError = block.decryptErr ?: return View(context)

    when (decryptError.details?.type) {
      PgpDecryptAndOrVerify.DecryptionErrorType.KEY_MISMATCH -> return generateMissingPrivateKeyLayout(
        clipLargeText(
          block.content
        ), layoutInflater
      )

      PgpDecryptAndOrVerify.DecryptionErrorType.FORMAT -> {
        val formatErrorMsg = (getString(
          R.string.decrypt_error_message_badly_formatted,
          getString(R.string.app_name)
        ) + "\n\n"
            + decryptError.details.type + ": " + decryptError.details.message)
        return getView(clipLargeText(block.content), formatErrorMsg, layoutInflater)
      }

      PgpDecryptAndOrVerify.DecryptionErrorType.OTHER -> {
        val otherErrorMsg =
          getString(R.string.decrypt_error_could_not_open_message, getString(R.string.app_name)) +
              "\n\n" + getString(
            R.string.decrypt_error_please_write_me,
            getString(R.string.support_email)
          ) +
              "\n\n" + decryptError.details.type + ": " + decryptError.details.message
        return getView(clipLargeText(block.content), otherErrorMsg, layoutInflater)
      }

      else -> {
        var btText: String? = null
        var onClickListener: View.OnClickListener? = null
        if (decryptError.details?.type == PgpDecryptAndOrVerify.DecryptionErrorType.NEED_PASSPHRASE) {
          btText = getString(R.string.fix)
          onClickListener = View.OnClickListener {
            val fingerprints = decryptError.fingerprints ?: return@OnClickListener
            showNeedPassphraseDialog(fingerprints)
          }
        }

        val detailedMessage = when (decryptError.details?.type) {
          PgpDecryptAndOrVerify.DecryptionErrorType.NO_MDC -> getString(R.string.decrypt_error_message_no_mdc)
          PgpDecryptAndOrVerify.DecryptionErrorType.BAD_MDC -> getString(R.string.decrypt_error_message_bad_mdc)
          else -> decryptError.details?.message
        }

        return getView(
          originalMsg = clipLargeText(block.content),
          errorMsg = getString(
            R.string.could_not_decrypt_message_due_to_error,
            decryptError.details?.type.toString() + ": " + detailedMessage
          ),
          layoutInflater = layoutInflater,
          buttonText = btText,
          onClickListener = onClickListener
        )
      }
    }
  }

  private fun getView(
    originalMsg: String?,
    errorMsg: String,
    layoutInflater: LayoutInflater,
    buttonText: String? = null,
    onClickListener: View.OnClickListener? = null
  ): View {
    val viewGroup = layoutInflater.inflate(
      R.layout.message_part_pgp_message_error,
      binding?.layoutMessageParts, false
    ) as ViewGroup
    val textViewErrorMsg = viewGroup.findViewById<TextView>(R.id.textViewErrorMessage)
    ExceptionUtil.handleError(ManualHandledException(errorMsg))
    textViewErrorMsg.text = errorMsg
    if (originalMsg != null) {
      viewGroup.addView(genShowOrigMsgLayout(originalMsg, layoutInflater, viewGroup))
      onClickListener?.let {
        val btAction = viewGroup.findViewById<TextView>(R.id.btAction)
        btAction.text = buttonText
        btAction.visible()
        btAction.setOnClickListener(it)
      }
    }
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
      R.layout.message_part_pgp_message_missing_private_key, binding?.layoutMessageParts, false
    ) as ViewGroup
    val buttonImportPrivateKey = viewGroup.findViewById<Button>(R.id.buttonImportPrivateKey)
    buttonImportPrivateKey?.setOnClickListener {
      account?.let { accountEntity ->
        navController?.navigate(
          MessageDetailsFragmentDirections
            .actionMessageDetailsFragmentToImportAdditionalPrivateKeysFragment(
              accountEntity
            )
        )
      }
    }

    val buttonSendOwnPublicKey = viewGroup.findViewById<Button>(R.id.buttonSendOwnPublicKey)
    buttonSendOwnPublicKey?.setOnClickListener { showSendersPublicKeyDialog() }

    val textViewErrorMsg = viewGroup.findViewById<TextView>(R.id.textViewErrorMessage)
    if (account?.clientConfiguration?.usesKeyManager() == true) {
      textViewErrorMsg?.text = getString(R.string.your_keys_cannot_open_this_message)
      buttonImportPrivateKey?.gone()
      buttonSendOwnPublicKey?.text = getString(R.string.inform_sender)
    } else {
      textViewErrorMsg?.text = getString(R.string.decrypt_error_current_key_cannot_open_message)
    }

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
  private fun genShowOrigMsgLayout(
    msg: String?, layoutInflater: LayoutInflater,
    rootView: ViewGroup
  ): ViewGroup {
    val viewGroup =
      layoutInflater.inflate(R.layout.pgp_show_original_message, rootView, false) as ViewGroup
    val textViewOrigPgpMsg = viewGroup.findViewById<TextView>(R.id.textViewOrigPgpMsg)
    textViewOrigPgpMsg.text = msg

    val switchShowOrigMsg = viewGroup.findViewById<CompoundButton>(R.id.switchShowOrigMsg)

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
    labelsViewModel.foldersManagerLiveData.observe(viewLifecycleOwner) {
      updateActionsVisibility(args.localFolder, it)
    }
  }

  private fun setupMsgDetailsViewModel() {
    observeFreshMsgLiveData()
    observerIncomingMessageInfoLiveData()
    observeAttsLiveData()
    observerMsgStatesLiveData()
    observerPassphraseNeededLiveData()
  }

  private fun setupRecipientsViewModel() {
    recipientsViewModel.recipientsFromLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource?.incrementSafely()
        }

        Result.Status.SUCCESS -> {
          val recipientsWithPubKeys = it.data
          val keyIdOfSigningKeys = msgInfo?.verificationResult?.keyIdOfSigningKeys ?: emptyList()
          if (recipientsWithPubKeys?.isNotEmpty() == true) {
            var isReVerificationNeeded = false
            for (recipientWithPubKeys in recipientsWithPubKeys) {
              for (publicKeyEntity in recipientWithPubKeys.publicKeys) {
                if (publicKeyEntity.pgpKeyDetails?.primaryKeyId in keyIdOfSigningKeys) {
                  isReVerificationNeeded = true
                }
              }
            }

            if (isReVerificationNeeded) {
              msgDetailsViewModel.reVerifySignatures()
            } else {
              updatePgpBadges()
            }
          } else {
            updatePgpBadges()
          }
          countingIdlingResource?.decrementSafely()
        }

        Result.Status.EXCEPTION -> {
          updatePgpBadges()
          countingIdlingResource?.decrementSafely()
        }

        else -> {
        }
      }
    }
  }

  private fun observeFreshMsgLiveData() {
    msgDetailsViewModel.freshMsgLiveData.observe(viewLifecycleOwner) {
      it?.let { messageEntity -> updateActionBar(messageEntity) }
    }
  }

  private fun observerIncomingMessageInfoLiveData() {
    msgDetailsViewModel.incomingMessageInfoLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          showProgress()

          val value = it.progress?.toInt() ?: 0

          if (value == 0) {
            countingIdlingResource?.incrementSafely()
          }

          when (it.resultCode) {
            R.id.progress_id_connecting -> setActionProgress(value, "Connecting")

            R.id.progress_id_fetching_message -> setActionProgress(value, "Fetching message")

            R.id.progress_id_processing -> setActionProgress(value, "Processing")

            R.id.progress_id_rendering -> setActionProgress(value, "Rendering")
          }
        }

        Result.Status.SUCCESS -> {
          showContent()
          it.data?.let { incomingMsgInfo ->
            showIncomingMsgInfo(incomingMsgInfo)
          }
          countingIdlingResource?.decrementSafely()
        }

        Result.Status.EXCEPTION -> {
          setActionProgress(100)

          when (it.exception) {
            is CommonConnectionException -> {
              showStatus(getString(R.string.connection_lost))
            }

            is UserRecoverableAuthException -> {
              showAuthIssueHint(it.exception.intent, duration = Snackbar.LENGTH_INDEFINITE)
              showStatus(msg = it.exception.message ?: it.exception.javaClass.simpleName)
            }

            is UserRecoverableAuthIOException -> {
              showAuthIssueHint(it.exception.intent, duration = Snackbar.LENGTH_INDEFINITE)
              showStatus(msg = it.exception.message ?: it.exception.javaClass.simpleName)
            }

            is AuthenticationFailedException -> {
              showAuthIssueHint(duration = Snackbar.LENGTH_INDEFINITE)
              showStatus(msg = it.exception.message ?: it.exception.javaClass.simpleName)
            }

            is AuthenticatorException -> {
              showAuthIssueHint(duration = Snackbar.LENGTH_INDEFINITE)
              showStatus(msg = it.exception.message ?: it.exception.javaClass.simpleName)
            }

            else -> {
              showStatus(
                msg = it.exception?.message ?: it.exception?.javaClass?.simpleName
                ?: getString(R.string.unknown_error)
              )
            }
          }
          countingIdlingResource?.decrementSafely()
        }

        else -> {
          setActionProgress(100)
          countingIdlingResource?.decrementSafely()
        }
      }
    }
  }

  private fun observeAttsLiveData() {
    msgDetailsViewModel.attsLiveData.observe(viewLifecycleOwner) { list ->
      val attachmentInfoList = list.map {
        if (args.localFolder.searchQuery.isNullOrEmpty()) {
          it.toAttInfo()
        } else {
          it.toAttInfo().copy(folder = args.localFolder.fullName)
        }
      }.filterNot { it.isHidden() }.toMutableList()

      attachmentsRecyclerViewAdapter.submitList(attachmentInfoList)
      if (args.messageEntity.hasAttachments == true && attachmentInfoList.isEmpty()) {
        msgDetailsViewModel.fetchAttachments()
      }
    }
  }

  private fun observerMsgStatesLiveData() {
    msgDetailsViewModel.msgStatesLiveData.observe(viewLifecycleOwner) { newState ->
      var navigateUp = true
      when (newState) {
        MessageState.PENDING_ARCHIVING -> ArchiveMsgsWorker.enqueue(requireContext())
        MessageState.PENDING_DELETING -> DeleteMessagesWorker.enqueue(requireContext())
        MessageState.PENDING_DELETING_PERMANENTLY -> DeleteMessagesPermanentlyWorker.enqueue(
          requireContext()
        )
        MessageState.PENDING_MOVE_TO_INBOX -> MovingToInboxWorker.enqueue(requireContext())
        MessageState.PENDING_MARK_UNREAD -> UpdateMsgsSeenStateWorker.enqueue(requireContext())
        MessageState.PENDING_MARK_READ -> {
          UpdateMsgsSeenStateWorker.enqueue(requireContext())
          navigateUp = false
        }
        else -> {}
      }

      if (navigateUp) {
        navController?.navigateUp()
      }
    }
  }

  private fun observerPassphraseNeededLiveData() {
    msgDetailsViewModel.passphraseNeededLiveData.observe(viewLifecycleOwner) { fingerprintList ->
      if (fingerprintList.isNotEmpty()) {
        showNeedPassphraseDialog(fingerprintList)
      }
    }
  }

  private fun collectReVerifySignaturesStateFlow() {
    lifecycleScope.launchWhenStarted {
      msgDetailsViewModel.reVerifySignaturesStateFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource?.incrementSafely()
          }

          Result.Status.SUCCESS -> {
            val verificationResult = it.data
            if (verificationResult != null) {
              msgInfo = msgInfo?.copy(verificationResult = verificationResult)
            }
            updatePgpBadges()
            countingIdlingResource?.decrementSafely()
          }

          Result.Status.EXCEPTION, Result.Status.ERROR -> {
            updatePgpBadges()
            countingIdlingResource?.decrementSafely()
          }

          else -> {
          }
        }
      }
    }
  }

  private fun subscribeToTwoWayDialog() {
    setFragmentResultListener(TwoWayDialogFragment.REQUEST_KEY_BUTTON_CLICK) { _, bundle ->
      val requestCode = bundle.getInt(TwoWayDialogFragment.KEY_REQUEST_CODE)
      val result = bundle.getInt(TwoWayDialogFragment.KEY_RESULT)

      when (requestCode) {
        REQUEST_CODE_DELETE_MESSAGE_DIALOG -> if (result == TwoWayDialogFragment.RESULT_OK) {
          msgDetailsViewModel.changeMsgState(MessageState.PENDING_DELETING_PERMANENTLY)
        }
      }
    }
  }

  private fun subscribeToChoosePublicKeyDialogFragment() {
    setFragmentResultListener(ChoosePublicKeyDialogFragment.REQUEST_KEY_RESULT) { _, bundle ->
      val keyList = bundle.getParcelableArrayList<AttachmentInfo>(
        ChoosePublicKeyDialogFragment.KEY_ATTACHMENT_INFO_LIST
      ) ?: return@setFragmentResultListener

      if (keyList.isNotEmpty()) {
        makeAttsProtected(keyList)
        sendTemplateMsgWithPublicKey(keyList[0])
      }
    }
  }

  private fun downloadAttachment() {
    lastClickedAtt?.let { attInfo ->
      if (attInfo.rawData?.isNotEmpty() == true) {
        downloadInlinedAtt(attInfo)
      } else {
        if (account?.isRuleExist(OrgRules.DomainRule.RESTRICT_ANDROID_ATTACHMENT_HANDLING) == true) {
          navController?.navigate(
            MessageDetailsFragmentDirections
              .actionMessageDetailsFragmentToDownloadAttachmentDialogFragment(
                attInfo, REQUEST_CODE_SAVE_ATTACHMENT
              )
          )
        } else {
          context?.startService(AttachmentDownloadManagerService.newIntent(context, attInfo))
        }
      }
    }
  }

  private fun previewAttachment(
    attachmentInfo: AttachmentInfo,
    useEnterpriseBehaviour: Boolean = false
  ) {
    val intent = if (attachmentInfo.uri != null) {
      GeneralUtil.genViewAttachmentIntent(requireNotNull(attachmentInfo.uri), attachmentInfo)
    } else {
      val (_, uri) = useFileProviderToGenerateUri(attachmentInfo)
      GeneralUtil.genViewAttachmentIntent(uri, attachmentInfo)
    }

    if (useEnterpriseBehaviour) {
      try {
        //ask Tom about receiving package as a parameter
        startActivity(Intent(intent).setPackage("com.airwatch.contentlocker"))
      } catch (e: ActivityNotFoundException) {
        //We don't have the required app
        showInfoDialog(
          dialogTitle = "",
          dialogMsg = getString(R.string.warning_don_not_have_content_app)
        )
      }
    } else {
      try {
        startActivity(intent)
      } catch (e: ActivityNotFoundException) {
        toast(getString(R.string.no_apps_that_can_handle_intent))
      }
    }
  }

  private fun downloadInlinedAtt(attInfo: AttachmentInfo) = try {
    val (file, uri) = useFileProviderToGenerateUri(attInfo)
    context?.let {
      attInfo.uri = uri
      it.startService(
        AttachmentDownloadManagerService.newIntent(
          context,
          attInfo.copy(rawData = null, name = file.name)
        )
      )
    }
  } catch (e: Exception) {
    e.printStackTrace()
    ExceptionUtil.handleError(e)
  }

  private fun useFileProviderToGenerateUri(attInfo: AttachmentInfo): Pair<File, Uri> {
    val tempDir = CacheManager.getCurrentMsgTempDir()
    val fileName = FileAndDirectoryUtils.normalizeFileName(attInfo.name)
    val file = if (fileName.isNullOrEmpty()) {
      File.createTempFile("tmp", null, tempDir)
    } else {
      val fileCandidate = File(tempDir, fileName)
      if (!fileCandidate.exists()) {
        FileUtils.writeByteArrayToFile(fileCandidate, attInfo.rawData)
      }
      fileCandidate
    }
    val uri = FileProvider.getUriForFile(requireContext(), Constants.FILE_PROVIDER_AUTHORITY, file)
    return Pair(file, uri)
  }

  private fun subscribeToDownloadAttachmentViaDialog() {
    setFragmentResultListener(DownloadAttachmentDialogFragment.REQUEST_KEY_ATTACHMENT_DATA) { _, bundle ->
      val requestCode = bundle.getInt(DownloadAttachmentDialogFragment.KEY_REQUEST_CODE)
      val attachmentInfo =
        bundle.getParcelable<AttachmentInfo>(DownloadAttachmentDialogFragment.KEY_ATTACHMENT)
      val data = bundle.getByteArray(DownloadAttachmentDialogFragment.KEY_ATTACHMENT_DATA)
      attachmentInfo?.rawData = data

      for (att in attachmentsRecyclerViewAdapter.currentList) {
        if (attachmentInfo == att) {
          att.rawData = data
          att.name = if (SecurityUtils.isPossiblyEncryptedData(att.name)) {
            FilenameUtils.getBaseName(att.name)
          } else {
            att.name
          }
          attachmentsRecyclerViewAdapter.notifyItemRangeChanged(
            0,
            attachmentsRecyclerViewAdapter.currentList.size
          )
          break
        }
      }

      when (requestCode) {
        REQUEST_CODE_PREVIEW_ATTACHMENT -> {
          attachmentInfo?.let {
            previewAttachment(
              attachmentInfo = it,
              useEnterpriseBehaviour =
              account?.isRuleExist(OrgRules.DomainRule.RESTRICT_ANDROID_ATTACHMENT_HANDLING) == true
            )
          }
        }

        REQUEST_CODE_SAVE_ATTACHMENT -> {
          downloadAttachment()
        }
      }
    }
  }

  private fun subscribeToImportingAdditionalPrivateKeys() {
    setFragmentResultListener(
      ImportAdditionalPrivateKeysFragment.REQUEST_KEY_IMPORT_ADDITIONAL_PRIVATE_KEYS
    ) { _, bundle ->
      val keys = bundle.getParcelableArrayList<PgpKeyDetails>(
        ImportAdditionalPrivateKeysFragment.KEY_IMPORTED_PRIVATE_KEYS
      )
      if (keys?.isNotEmpty() == true) {
        toast(R.string.key_successfully_imported)
      }
    }
  }

  companion object {
    private const val REQUEST_CODE_REQUEST_WRITE_EXTERNAL_STORAGE = 100
    private const val REQUEST_CODE_DELETE_MESSAGE_DIALOG = 103
    private const val CONTENT_MAX_ALLOWED_LENGTH = 50000
    private const val MAX_ALLOWED_RECEPIENTS_IN_HEADER_VALUE = 10

    private const val REQUEST_CODE_SAVE_ATTACHMENT = 1000
    private const val REQUEST_CODE_PREVIEW_ATTACHMENT = 1001
  }
}
