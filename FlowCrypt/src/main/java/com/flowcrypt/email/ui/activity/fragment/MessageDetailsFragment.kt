/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.Manifest
import android.accounts.AuthenticatorException
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.Html
import android.text.SpannableStringBuilder
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.text.toSpannable
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
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
import com.flowcrypt.email.api.retrofit.response.model.PublicKeyMsgBlock
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.databinding.FragmentMessageDetailsBinding
import com.flowcrypt.email.extensions.android.os.getParcelableArrayListViaExt
import com.flowcrypt.email.extensions.android.os.getParcelableViaExt
import com.flowcrypt.email.extensions.android.widget.useGlideToApplyImageFromSource
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.exceptionMsg
import com.flowcrypt.email.extensions.exceptionMsgWithStack
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.jakarta.mail.internet.getFormattedString
import com.flowcrypt.email.extensions.jakarta.mail.internet.personalOrEmail
import com.flowcrypt.email.extensions.launchAndRepeatWithViewLifecycle
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.setFragmentResultListenerForTwoWayDialog
import com.flowcrypt.email.extensions.showChoosePublicKeyDialogFragment
import com.flowcrypt.email.extensions.showInfoDialog
import com.flowcrypt.email.extensions.showNeedPassphraseDialog
import com.flowcrypt.email.extensions.showTwoWayDialog
import com.flowcrypt.email.extensions.supportActionBar
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.extensions.useFileProviderToGenerateUri
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.extensions.visibleOrGone
import com.flowcrypt.email.extensions.visibleOrInvisible
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
import com.flowcrypt.email.jetpack.viewmodel.LabelsViewModel
import com.flowcrypt.email.jetpack.viewmodel.MsgDetailsViewModel
import com.flowcrypt.email.jetpack.viewmodel.RecipientsViewModel
import com.flowcrypt.email.jetpack.workmanager.sync.ArchiveMsgsWorker
import com.flowcrypt.email.jetpack.workmanager.sync.DeleteDraftsWorker
import com.flowcrypt.email.jetpack.workmanager.sync.DeleteMessagesPermanentlyWorker
import com.flowcrypt.email.jetpack.workmanager.sync.DeleteMessagesWorker
import com.flowcrypt.email.jetpack.workmanager.sync.MarkAsNotSpamWorker
import com.flowcrypt.email.jetpack.workmanager.sync.MovingToInboxWorker
import com.flowcrypt.email.jetpack.workmanager.sync.MovingToSpamWorker
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
import com.flowcrypt.email.ui.adapter.GmailApiLabelsListAdapter
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
import com.flowcrypt.email.util.graphics.glide.AvatarModelLoader
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.material.snackbar.Snackbar
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import jakarta.mail.AuthenticationFailedException
import jakarta.mail.internet.InternetAddress
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.apache.commons.io.FilenameUtils
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * This fragment describe msgEntity of some message.
 *
 * @author Denys Bondarenko
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
    object : CustomAndroidViewModelFactory(requireActivity().application) {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MsgDetailsViewModel(
          args.localFolder, args.messageEntity, requireActivity().application
        ) as T
      }
    }
  }

  val requestPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
      if (isGranted) {
        downloadAttachment()
      } else {
        toast(R.string.cannot_save_attachment_without_permission, Toast.LENGTH_LONG)
      }
    }

  private val attachmentsRecyclerViewAdapter = AttachmentsRecyclerViewAdapter(
    isDeleteEnabled = false,
    attachmentActionListener = object : AttachmentsRecyclerViewAdapter.AttachmentActionListener {
      override fun onDownloadClick(attachmentInfo: AttachmentInfo) {
        lastClickedAtt =
          attachmentInfo.copy(orderNumber = GeneralUtil.genAttOrderId(requireContext()))

        if (SecurityUtils.isPossiblyEncryptedData(attachmentInfo.name)) {
          for (block in msgInfo?.msgBlocks ?: emptyList()) {
            if (block.type == MsgBlock.Type.DECRYPT_ERROR) {
              val decryptErrorMsgBlock = block as? DecryptErrorMsgBlock ?: continue
              val decryptErrorDetails = decryptErrorMsgBlock.decryptErr?.details ?: continue
              if (decryptErrorDetails.type == PgpDecryptAndOrVerify.DecryptionErrorType.NEED_PASSPHRASE) {
                val fingerprints = decryptErrorMsgBlock.decryptErr.fingerprints ?: continue
                showNeedPassphraseDialog(
                  requestKey = REQUEST_KEY_FIX_MISSING_PASSPHRASE,
                  fingerprints = fingerprints
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
          requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
      }

      override fun onAttachmentClick(attachmentInfo: AttachmentInfo) {
        if (attachmentInfo.uri != null || attachmentInfo.rawData?.isNotEmpty() == true) {
          previewAttachment(
            attachmentInfo = attachmentInfo,
            useContentApp = account?.isHandlingAttachmentRestricted() == true
          )
        }
      }

      override fun onPreviewClick(attachmentInfo: AttachmentInfo) {
        if (attachmentInfo.uri != null || attachmentInfo.rawData?.isNotEmpty() == true) {
          previewAttachment(
            attachmentInfo = attachmentInfo,
            useContentApp = account?.isHandlingAttachmentRestricted() == true
          )
        } else {
          navController?.navigate(
            MessageDetailsFragmentDirections
              .actionMessageDetailsFragmentToDownloadAttachmentDialogFragment(
                attachmentInfo = attachmentInfo,
                requestKey = REQUEST_KEY_DOWNLOAD_ATTACHMENT,
                requestCode = REQUEST_CODE_PREVIEW_ATTACHMENT
              )
          )
        }
      }

      override fun onDeleteClick(attachmentInfo: AttachmentInfo) {}
    })

  private var msgInfo: IncomingMessageInfo? = null
  private var folderType: FoldersManager.FolderType? = null
  private val labelsViewModel: LabelsViewModel by viewModels()
  private val recipientsViewModel: RecipientsViewModel by viewModels()
  private val msgDetailsAdapter = MsgDetailsRecyclerViewAdapter()
  private val pgpBadgeListAdapter = PgpBadgeListAdapter()
  private val gmailApiLabelsListAdapter = GmailApiLabelsListAdapter(
    object : GmailApiLabelsListAdapter.OnLabelClickListener {
      override fun onLabelClick(label: GmailApiLabelsListAdapter.Label) {
        if (args.localFolder.searchQuery == null) {
          changeGmailLabels()
        }
      }
    })

  private var isAdditionalActionEnabled: Boolean = false
  private var isDeleteActionEnabled: Boolean = false
  private var isMoveToSpamActionEnabled: Boolean = false
  private var lastClickedAtt: AttachmentInfo? = null
  private var msgEncryptType = MessageEncryptionType.STANDARD
  private var downloadAttachmentsProgressJob: Job? = null

  private val downloadAttachmentsServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(className: ComponentName, service: IBinder) {
      val binder = service as AttachmentDownloadManagerService.LocalBinder
      val attachmentDownloadManagerService = binder.getService()

      downloadAttachmentsProgressJob = lifecycleScope.launch {
        attachmentDownloadManagerService.attachmentDownloadProgressStateFlow.collect { map ->
          updateProgress(map)
        }
      }
    }

    override fun onServiceDisconnected(arg0: ComponentName) {}
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    subscribeToDownloadAttachmentViaDialog()
    subscribeToImportingAdditionalPrivateKeys()
    updateActionsVisibility(args.localFolder, null)
  }

  override fun onStart() {
    super.onStart()
    bindToAttachmentDownloadManagerService()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    setupLabelsViewModel()
    setupMsgDetailsViewModel()
    setupRecipientsViewModel()
    collectReVerifySignaturesStateFlow()
    subscribeToTwoWayDialog()
    subscribeToChoosePublicKeyDialogFragment()
    collectMessageActionsVisibilityStateFlow()
  }

  override fun onStop() {
    super.onStop()
    context?.unbindService(downloadAttachmentsServiceConnection)
    downloadAttachmentsProgressJob?.cancel()
  }

  override fun onDestroy() {
    super.onDestroy()
    FileAndDirectoryUtils.cleanDir(CacheManager.getCurrentMsgTempDirectory(requireContext()))
  }

  override fun onSetupActionBarMenu(menuHost: MenuHost) {
    super.onSetupActionBarMenu(menuHost)
    menuHost.addMenuProvider(object : MenuProvider {
      override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.fragment_message_details, menu)
      }

      override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        val menuItemArchiveMsg = menu.findItem(R.id.menuActionArchiveMessage)
        val menuItemDeleteMsg = menu.findItem(R.id.menuActionDeleteMessage)
        val menuActionMoveToInbox = menu.findItem(R.id.menuActionMoveToInbox)
        val menuActionMarkUnread = menu.findItem(R.id.menuActionMarkUnread)
        val menuActionMoveToSpam = menu.findItem(R.id.menuActionMoveToSpam)
        val menuActionMarkAsNotSpam = menu.findItem(R.id.menuActionMarkAsNotSpam)
        val menuActionChangeLabels = menu.findItem(R.id.menuActionChangeLabels)

        menuItemArchiveMsg?.isVisible = msgDetailsViewModel.getMessageActionAvailability(
          MsgDetailsViewModel.MessageAction.ARCHIVE
        )
        menuItemDeleteMsg?.isVisible = isDeleteActionEnabled
        menuActionMoveToInbox?.isVisible = msgDetailsViewModel.getMessageActionAvailability(
          MsgDetailsViewModel.MessageAction.MOVE_TO_INBOX
        )
        menuActionMarkUnread?.isVisible =
          !JavaEmailConstants.FOLDER_OUTBOX.equals(args.messageEntity.folder, ignoreCase = true)
        menuActionMoveToSpam?.isVisible = isMoveToSpamActionEnabled
        menuActionMarkAsNotSpam?.isVisible = msgDetailsViewModel.getMessageActionAvailability(
          MsgDetailsViewModel.MessageAction.MARK_AS_NOT_SPAM
        )
        menuActionChangeLabels?.isVisible = msgDetailsViewModel.getMessageActionAvailability(
          MsgDetailsViewModel.MessageAction.CHANGE_LABELS
        )

        menuItemArchiveMsg?.isEnabled = isAdditionalActionEnabled
        menuItemDeleteMsg?.isEnabled = isAdditionalActionEnabled
        menuActionMoveToInbox?.isEnabled = isAdditionalActionEnabled
        menuActionMarkUnread?.isEnabled = isAdditionalActionEnabled
        menuActionMoveToSpam?.isEnabled = isAdditionalActionEnabled
        menuActionMarkAsNotSpam?.isEnabled = isAdditionalActionEnabled
        menuActionChangeLabels?.isEnabled = isAdditionalActionEnabled

        args.localFolder.searchQuery?.let {
          menuItemArchiveMsg?.isVisible = false
          menuItemDeleteMsg?.isVisible = false
          menuActionMoveToInbox?.isVisible = false
          menuActionMarkUnread?.isVisible = false
          menuActionMoveToSpam?.isVisible = false
          menuActionMarkAsNotSpam?.isVisible = false
          menuActionChangeLabels?.isVisible = false
        }
      }

      override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
          R.id.menuActionArchiveMessage -> {
            msgDetailsViewModel.changeMsgState(MessageState.PENDING_ARCHIVING)
            true
          }

          R.id.menuActionDeleteMessage -> {
            if (args.messageEntity.isOutboxMsg) {
              val msgEntity = args.messageEntity

              msgEntity.let {
                if (msgEntity.msgState === MessageState.SENDING) {
                  toast(R.string.can_not_delete_sending_message, Toast.LENGTH_LONG)
                } else {
                  msgDetailsViewModel.deleteMsg()
                  toast(R.string.message_was_deleted)
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
                msgDetailsViewModel.changeMsgState(
                  if (args.messageEntity.isDraft) {
                    MessageState.PENDING_DELETING_DRAFT
                  } else {
                    MessageState.PENDING_DELETING
                  }
                )
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

          R.id.menuActionMoveToSpam -> {
            msgDetailsViewModel.changeMsgState(MessageState.PENDING_MOVE_TO_SPAM)
            true
          }

          R.id.menuActionMarkAsNotSpam -> {
            msgDetailsViewModel.changeMsgState(MessageState.PENDING_MARK_AS_NOT_SPAM)
            true
          }

          R.id.menuActionChangeLabels -> {
            changeGmailLabels()
            true
          }

          else -> false
        }
      }
    }, viewLifecycleOwner, Lifecycle.State.RESUMED)
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

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    super.onAccountInfoRefreshed(accountEntity)
    if (folderType == FoldersManager.FolderType.SENT) {
      val senderAddress = EmailUtil.getFirstAddressString(args.messageEntity.from)
      binding?.imageViewAvatar?.useGlideToApplyImageFromSource(
        account?.avatarResource ?: (AvatarModelLoader.SCHEMA_AVATAR + senderAddress),
        applyCircleTransformation = account?.avatarResource != null
      )
    }
  }

  /**
   * Show an incoming message info.
   *
   * @param msgInfo An incoming message info
   */
  private fun showIncomingMsgInfo(msgInfo: IncomingMessageInfo) {
    this.msgInfo = msgInfo
    this.msgEncryptType = msgInfo.encryptionType
    isAdditionalActionEnabled = true
    activity?.invalidateOptionsMenu()
    msgInfo.localFolder = args.localFolder

    if ("..." == msgInfo.getSubject()) {
      msgInfo.inlineSubject?.let {
        binding?.textViewSubject?.text = Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY)
      }
    }

    updatePgpBadges()
    updateMsgView()
    showContent()

    if (args.messageEntity.isDraft) {
      binding?.imageButtonEditDraft?.visibleOrGone(args.messageEntity.isDraft)
    }
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

  /**
   * Show a dialog where the user can select some public key which will be attached to a message.
   */
  private fun showSendersPublicKeyDialog() {
    showChoosePublicKeyDialogFragment(
      requestKey = REQUEST_KEY_CHOOSE_PUBLIC_KEY,
      email = args.messageEntity.email,
      choiceMode = ListView.CHOICE_MODE_SINGLE,
      titleResourceId = R.plurals.tell_sender_to_update_their_settings
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
      atts.add(att.copy(isProtected = true))
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
          isDeleteActionEnabled = true
          isMoveToSpamActionEnabled = true
        }

        FoldersManager.FolderType.SENT -> {
          isDeleteActionEnabled = true
          isMoveToSpamActionEnabled = AccountEntity.ACCOUNT_TYPE_GOOGLE == account?.accountType
        }

        FoldersManager.FolderType.TRASH -> {
          isDeleteActionEnabled = true
          isMoveToSpamActionEnabled = true
        }

        FoldersManager.FolderType.DRAFTS, FoldersManager.FolderType.OUTBOX -> {
          isMoveToSpamActionEnabled = false
          isDeleteActionEnabled = true
        }

        FoldersManager.FolderType.JUNK, FoldersManager.FolderType.SPAM -> {
          isMoveToSpamActionEnabled = false
          isDeleteActionEnabled = true
        }

        else -> {
          isDeleteActionEnabled = true
          isMoveToSpamActionEnabled = true
        }
      }
    } else {
      isMoveToSpamActionEnabled = false
      isDeleteActionEnabled = true
    }

    if (foldersManager != null) {
      if (foldersManager.folderTrash == null) {
        isDeleteActionEnabled = false
      }
    } else {
      isDeleteActionEnabled = false
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

  private fun initViews() {
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

    binding?.recyclerViewLabels?.apply {
      layoutManager = FlexboxLayoutManager(context).apply {
        flexDirection = FlexDirection.ROW
        justifyContent = JustifyContent.FLEX_START
      }
      addItemDecoration(
        MarginItemDecoration(
          marginRight = resources.getDimensionPixelSize(R.dimen.default_margin_small),
          marginTop = resources.getDimensionPixelSize(R.dimen.default_margin_small)
        )
      )
      adapter = gmailApiLabelsListAdapter
    }

    binding?.imageButtonReplyAll?.setOnClickListener(this)
    binding?.imageButtonMoreOptions?.setOnClickListener(this)

    binding?.imageButtonEditDraft?.setOnClickListener {
      val fingerprintList = msgDetailsViewModel.passphraseNeededLiveData.value
      if (fingerprintList?.isNotEmpty() == true) {
        showNeedPassphraseDialog(
          requestKey = REQUEST_KEY_FIX_MISSING_PASSPHRASE,
          fingerprints = fingerprintList
        )
      } else {
        startActivity(
          CreateMessageActivity.generateIntent(
            context,
            MessageType.DRAFT,
            msgEncryptType,
            msgInfo?.copy(
              msgBlocks = emptyList(),
              text = clipLargeText(msgInfo?.text),
            )
          )
        )
      }
    }

    binding?.iBShowDetails?.setOnClickListener {
      binding?.rVMsgDetails?.visibleOrGone(!(binding?.rVMsgDetails?.isVisible ?: false))
      binding?.textViewDate?.visibleOrInvisible(!(binding?.rVMsgDetails?.isVisible ?: false))
    }

    binding?.rVAttachments?.apply {
      layoutManager = LinearLayoutManager(context)
      addItemDecoration(
        MarginItemDecoration(
          marginBottom = resources.getDimensionPixelSize(R.dimen.default_margin_content_small)
        )
      )
      adapter = attachmentsRecyclerViewAdapter
    }
  }

  private fun updateViews(messageEntity: MessageEntity) {
    updateActionBar(messageEntity)

    binding?.imageButtonReplyAll?.visibleOrInvisible(
      !messageEntity.isOutboxMsg && !messageEntity.isDraft
    )
    binding?.imageButtonMoreOptions?.visibleOrInvisible(
      !messageEntity.isOutboxMsg && !messageEntity.isDraft
    )
    updateMsgDetails(messageEntity)

    val senderAddress = EmailUtil.getFirstAddressString(messageEntity.from)
    binding?.textViewSenderAddress?.text = senderAddress
    binding?.imageViewAvatar?.useGlideToApplyImageFromSource(
      source = when (folderType) {
        FoldersManager.FolderType.DRAFTS -> R.drawable.avatar_draft
        FoldersManager.FolderType.SENT -> account?.avatarResource
          ?: (AvatarModelLoader.SCHEMA_AVATAR + senderAddress)

        else -> AvatarModelLoader.SCHEMA_AVATAR + senderAddress
      }
    )

    if (binding?.textViewSubject?.text.isNullOrEmpty()) {
      binding?.textViewSubject?.text =
        (messageEntity.subject ?: "").ifEmpty { getString(R.string.no_subject) }
    }

    if (JavaEmailConstants.FOLDER_OUTBOX.equals(messageEntity.folder, ignoreCase = true)) {
      binding?.textViewDate?.text =
        DateTimeUtil.formatSameDayTime(context, messageEntity.sentDate ?: 0)
    } else {
      binding?.textViewDate?.text =
        DateTimeUtil.formatSameDayTime(context, messageEntity.receivedDate ?: 0)
    }
  }

  private fun updateMsgDetails(messageEntity: MessageEntity) {
    binding?.tVTo?.text = prepareToText()

    val headers = mutableListOf<MsgDetailsRecyclerViewAdapter.Header>().apply {
      add(
        MsgDetailsRecyclerViewAdapter.Header(
          name = getString(R.string.from),
          value = formatAddresses(messageEntity.from)
        )
      )

      if (messageEntity.replyToAddress.isNotEmpty()) {
        add(
          MsgDetailsRecyclerViewAdapter.Header(
            name = getString(R.string.reply_to),
            value = formatAddresses(messageEntity.replyToAddress)
          )
        )
      }

      add(
        MsgDetailsRecyclerViewAdapter.Header(
          name = getString(R.string.to),
          value = formatAddresses(messageEntity.to).ifEmpty { getString(R.string.no_recipients) }
        )
      )

      if (messageEntity.cc.isNotEmpty()) {
        add(
          MsgDetailsRecyclerViewAdapter.Header(
            name = getString(R.string.cc),
            value = formatAddresses(messageEntity.cc)
          )
        )
      }

      add(
        MsgDetailsRecyclerViewAdapter.Header(
          name = getString(R.string.date),
          value = prepareDateHeaderValue(messageEntity)
        )
      )
    }

    msgDetailsAdapter.submitList(headers)
  }

  private fun prepareDateHeaderValue(messageEntity: MessageEntity): String {
    val dateInMilliseconds: Long =
      if (JavaEmailConstants.FOLDER_OUTBOX.equals(messageEntity.folder, ignoreCase = true)) {
        messageEntity.sentDate ?: 0
      } else {
        messageEntity.receivedDate ?: 0
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
      if (index < MAX_ALLOWED_RECIPIENTS_IN_HEADER_VALUE) {
        builder.append(it.getFormattedString())
        if (index != addresses.size - 1) {
          builder.append("\n")
        }
      } else if (index == MAX_ALLOWED_RECIPIENTS_IN_HEADER_VALUE + 1) {
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
            inlineEncryptedAtts.add(decryptAtt.toAttachmentInfo().copy(email = account?.email))
          } else {
            handleOtherBlock(block, layoutInflater)
          }
        }

        MsgBlock.Type.ENCRYPTED_SUBJECT -> {}// we should skip such blocks here

        else -> handleOtherBlock(block, layoutInflater)
      }
      isFirstMsgPartText = false
    }

    if (!isHtmlDisplayed) {
      updateReplyButtons()
    }

    if (inlineEncryptedAtts.isNotEmpty()) {
      msgDetailsViewModel.updateInlinedAttachments(inlineEncryptedAtts)
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
          0 -> countingIdlingResource?.incrementSafely(this@MessageDetailsFragment)

          100 -> {
            setActionProgress(100, null)
            updateReplyButtons()
            countingIdlingResource?.decrementSafely(this@MessageDetailsFragment)
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
      binding?.layoutReplyButtons?.root?.visibleOrGone(
        !args.messageEntity.isOutboxMsg && !args.messageEntity.isDraft
      )
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
            when {
              matchingKeyByFingerprint.pgpKeyDetails?.isRevoked == true -> {
                textViewStatus?.text = getString(R.string.key_is_revoked_unable_to_update)
                textViewStatus?.setTextColor(UIUtil.getColor(requireContext(), R.color.red))
                textViewStatus?.visible()
                textViewManualImportWarning?.gone()
                button.gone()
              }

              keyDetails.isNewerThan(matchingKeyByFingerprint.pgpKeyDetails) -> {
                textViewManualImportWarning?.visible()
                initUpdatePubKeyButton(matchingKeyByFingerprint, keyDetails, button)
              }

              else -> {
                textViewStatus?.text = getString(R.string.already_imported)
                textViewStatus.visible()
                textViewManualImportWarning?.gone()
                button.gone()
              }
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
      button?.gone()
      textViewStatus.visible()
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
        val otherErrorMsg = when (decryptError.details.message) {
          "Symmetric-Key algorithm TRIPLE_DES is not acceptable for message decryption." -> {
            getString(R.string.message_was_not_decrypted_due_to_triple_des, "TRIPLE_DES")
          }

          else -> getString(
            R.string.decrypt_error_could_not_open_message,
            getString(R.string.app_name)
          ) + "\n\n" + getString(
            R.string.decrypt_error_please_write_me,
            getString(R.string.support_email)
          ) + "\n\n" + decryptError.details.type +
              ": " + decryptError.details.message +
              "\n\n" + decryptError.details.stack
        }

        return getView(clipLargeText(block.content), otherErrorMsg, layoutInflater)
      }

      else -> {
        var btText: String? = null
        var onClickListener: View.OnClickListener? = null
        if (decryptError.details?.type == PgpDecryptAndOrVerify.DecryptionErrorType.NEED_PASSPHRASE) {
          btText = getString(R.string.fix)
          onClickListener = View.OnClickListener {
            val fingerprints = decryptError.fingerprints ?: return@OnClickListener
            showNeedPassphraseDialog(
              requestKey = REQUEST_KEY_FIX_MISSING_PASSPHRASE,
              fingerprints = fingerprints
            )
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
              requestKey = REQUEST_KEY_IMPORT_ADDITIONAL_PRIVATE_KEYS,
              accountEntity = accountEntity
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
    collectAttachmentsFlow()
    observerMsgStatesLiveData()
    observerPassphraseNeededLiveData()

    launchAndRepeatWithViewLifecycle {
      msgDetailsViewModel.messageGmailApiLabelsFlow.collect {
        gmailApiLabelsListAdapter.submitList(it)
      }
    }
  }

  private fun setupRecipientsViewModel() {
    recipientsViewModel.recipientsFromLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource?.incrementSafely(this@MessageDetailsFragment)
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
          countingIdlingResource?.decrementSafely(this@MessageDetailsFragment)
        }

        Result.Status.EXCEPTION -> {
          updatePgpBadges()
          countingIdlingResource?.decrementSafely(this@MessageDetailsFragment)
        }

        else -> {
        }
      }
    }
  }

  private fun observeFreshMsgLiveData() {
    msgDetailsViewModel.mediatorMsgLiveData.observe(viewLifecycleOwner) { messageEntity ->
      if (messageEntity != null) {
        updateViews(messageEntity)
      } else {
        if (!args.messageEntity.isDraft) {
          toast(R.string.message_was_deleted_or_labels_changed)
        }
        navController?.navigateUp()
      }
    }
  }

  private fun observerIncomingMessageInfoLiveData() {
    msgDetailsViewModel.incomingMessageInfoLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          showProgress()

          val value = it.progress?.toInt() ?: 0

          if (value == 0) {
            countingIdlingResource?.incrementSafely(this@MessageDetailsFragment)
          }

          when (it.resultCode) {
            R.id.progress_id_connecting -> setActionProgress(value, "Connecting")

            R.id.progress_id_fetching_message -> setActionProgress(value, "Fetching message")

            R.id.progress_id_processing -> setActionProgress(value, "Processing")

            R.id.progress_id_rendering -> setActionProgress(value, "Rendering")
          }
        }

        Result.Status.SUCCESS -> {
          it.data?.let { incomingMsgInfo ->
            showIncomingMsgInfo(incomingMsgInfo)
          }
          countingIdlingResource?.decrementSafely(this@MessageDetailsFragment)
        }

        Result.Status.EXCEPTION -> {
          setActionProgress(100)

          when (it.exception) {
            is CommonConnectionException -> {
              showStatus(getString(R.string.connection_lost))
            }

            is UserRecoverableAuthException -> {
              showAuthIssueHint(it.exception.intent, duration = Snackbar.LENGTH_INDEFINITE)
              showStatus(msg = it.exceptionMsg)
            }

            is UserRecoverableAuthIOException -> {
              showAuthIssueHint(it.exception.intent, duration = Snackbar.LENGTH_INDEFINITE)
              showStatus(msg = it.exceptionMsg)
            }

            is AuthenticationFailedException -> {
              showAuthIssueHint(duration = Snackbar.LENGTH_INDEFINITE)
              showStatus(msg = it.exceptionMsg)
            }

            is AuthenticatorException -> {
              showAuthIssueHint(duration = Snackbar.LENGTH_INDEFINITE)
              showStatus(msg = it.exceptionMsg)
            }

            else -> showStatus(msg = it.exceptionMsgWithStack)
          }
          countingIdlingResource?.decrementSafely(this@MessageDetailsFragment)
        }

        else -> {
          setActionProgress(100)
          countingIdlingResource?.decrementSafely(this@MessageDetailsFragment)
        }
      }
    }
  }

  private fun collectAttachmentsFlow() {
    launchAndRepeatWithViewLifecycle {
      msgDetailsViewModel.attachmentsFlow.collect { attachmentInfoList ->
        attachmentsRecyclerViewAdapter.submitList(attachmentInfoList)
        if (args.messageEntity.hasAttachments == true && attachmentInfoList.isEmpty()) {
          msgDetailsViewModel.fetchAttachments()
        }
      }
    }
  }

  private fun observerMsgStatesLiveData() {
    msgDetailsViewModel.msgStatesLiveData.observe(viewLifecycleOwner) { newState ->
      var navigateUp = true
      when (newState) {
        MessageState.PENDING_ARCHIVING -> ArchiveMsgsWorker.enqueue(requireContext())
        MessageState.PENDING_DELETING -> DeleteMessagesWorker.enqueue(requireContext())
        MessageState.PENDING_DELETING_DRAFT -> DeleteDraftsWorker.enqueue(requireContext())
        MessageState.PENDING_DELETING_PERMANENTLY -> DeleteMessagesPermanentlyWorker.enqueue(
          requireContext()
        )

        MessageState.PENDING_MOVE_TO_INBOX -> MovingToInboxWorker.enqueue(requireContext())
        MessageState.PENDING_MOVE_TO_SPAM -> MovingToSpamWorker.enqueue(requireContext())
        MessageState.PENDING_MARK_AS_NOT_SPAM -> MarkAsNotSpamWorker.enqueue(requireContext())
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
        showNeedPassphraseDialog(
          requestKey = REQUEST_KEY_FIX_MISSING_PASSPHRASE,
          fingerprints = fingerprintList
        )
      }
    }
  }

  private fun collectReVerifySignaturesStateFlow() {
    launchAndRepeatWithViewLifecycle {
      msgDetailsViewModel.reVerifySignaturesStateFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource?.incrementSafely(this@MessageDetailsFragment)
          }

          Result.Status.SUCCESS -> {
            val verificationResult = it.data
            if (verificationResult != null) {
              msgInfo = msgInfo?.copy(verificationResult = verificationResult)
            }
            updatePgpBadges()
            countingIdlingResource?.decrementSafely(this@MessageDetailsFragment)
          }

          Result.Status.EXCEPTION, Result.Status.ERROR -> {
            updatePgpBadges()
            countingIdlingResource?.decrementSafely(this@MessageDetailsFragment)
          }

          else -> {
          }
        }
      }
    }
  }

  private fun collectMessageActionsVisibilityStateFlow() {
    launchAndRepeatWithViewLifecycle {
      msgDetailsViewModel.messageActionsAvailabilityStateFlow.collect {
        activity?.invalidateOptionsMenu()
      }
    }
  }

  private fun subscribeToTwoWayDialog() {
    setFragmentResultListenerForTwoWayDialog { _, bundle ->
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
    setFragmentResultListener(REQUEST_KEY_CHOOSE_PUBLIC_KEY) { _, bundle ->
      val keyList = bundle.getParcelableArrayListViaExt<AttachmentInfo>(
        ChoosePublicKeyDialogFragment.KEY_ATTACHMENT_INFO_LIST
      )?.map { attachmentInfo ->
        attachmentInfo.copy(isProtected = true)
      } ?: return@setFragmentResultListener

      if (keyList.isNotEmpty()) {
        sendTemplateMsgWithPublicKey(keyList[0])
      }
    }
  }

  private fun downloadAttachment() {
    lastClickedAtt?.let { attInfo ->
      if (attInfo.rawData?.isNotEmpty() == true) {
        downloadInlinedAtt(attInfo)
      } else {
        if (account?.isHandlingAttachmentRestricted() == true) {
          navController?.navigate(
            MessageDetailsFragmentDirections
              .actionMessageDetailsFragmentToDownloadAttachmentDialogFragment(
                attachmentInfo = attInfo,
                requestKey = REQUEST_KEY_DOWNLOAD_ATTACHMENT,
                requestCode = REQUEST_CODE_SAVE_ATTACHMENT
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
    useContentApp: Boolean = false
  ) {
    val intent = if (attachmentInfo.uri != null) {
      GeneralUtil.genViewAttachmentIntent(requireNotNull(attachmentInfo.uri), attachmentInfo)
    } else {
      val (_, uri) = attachmentInfo.useFileProviderToGenerateUri(requireContext())
      GeneralUtil.genViewAttachmentIntent(uri, attachmentInfo)
    }

    if (useContentApp) {
      try {
        startActivity(Intent(intent).setPackage(Constants.APP_PACKAGE_CONTENT_LOCKER))
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
    val (file, uri) = attInfo.useFileProviderToGenerateUri(requireContext())
    context?.startService(
      AttachmentDownloadManagerService.newIntent(
        context,
        attInfo.copy(rawData = null, name = file.name, uri = uri)
      )
    )
  } catch (e: Exception) {
    e.printStackTrace()
    ExceptionUtil.handleError(e)
  }

  private fun subscribeToDownloadAttachmentViaDialog() {
    setFragmentResultListener(REQUEST_KEY_DOWNLOAD_ATTACHMENT) { _, bundle ->
      val requestCode = bundle.getInt(DownloadAttachmentDialogFragment.KEY_REQUEST_CODE)
      val attachmentInfo =
        bundle.getParcelableViaExt<AttachmentInfo>(DownloadAttachmentDialogFragment.KEY_ATTACHMENT)

      val existingList = attachmentsRecyclerViewAdapter.currentList.toMutableList()
      existingList.replaceAll {
        if (it.id == attachmentInfo?.id) {
          attachmentInfo
        } else {
          it
        }
      }

      lastClickedAtt = attachmentInfo
      attachmentsRecyclerViewAdapter.submitList(existingList.toList())

      when (requestCode) {
        REQUEST_CODE_PREVIEW_ATTACHMENT -> {
          attachmentInfo?.let {
            previewAttachment(
              attachmentInfo = it,
              useContentApp =
              account?.isHandlingAttachmentRestricted() == true
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
    setFragmentResultListener(REQUEST_KEY_IMPORT_ADDITIONAL_PRIVATE_KEYS) { _, bundle ->
      val keys = bundle.getParcelableArrayListViaExt<PgpKeyDetails>(
        ImportAdditionalPrivateKeysFragment.KEY_IMPORTED_PRIVATE_KEYS
      )
      if (keys?.isNotEmpty() == true) {
        toast(R.string.key_successfully_imported)
      }
    }
  }

  private fun bindToAttachmentDownloadManagerService() {
    Intent(requireContext(), AttachmentDownloadManagerService::class.java).also { intent ->
      context?.bindService(intent, downloadAttachmentsServiceConnection, Context.BIND_AUTO_CREATE)
    }
  }

  private fun updateProgress(
    mapWithLatestProgress: Map<String, AttachmentDownloadManagerService.DownloadProgress>
  ) {
    val idsToBeUpdated = mutableSetOf<String>()
    val existingMap = attachmentsRecyclerViewAdapter.progressMap
    idsToBeUpdated.addAll(mapWithLatestProgress.keys - existingMap.keys)
    existingMap.forEach { (attachmentId, existingProgress) ->
      val newProgress = mapWithLatestProgress[attachmentId]
      if (newProgress == null || existingProgress != newProgress) {
        idsToBeUpdated.add(attachmentId)
      }
    }

    attachmentsRecyclerViewAdapter.progressMap.clear()
    attachmentsRecyclerViewAdapter.progressMap.putAll(mapWithLatestProgress)
    val currentList = attachmentsRecyclerViewAdapter.currentList
    idsToBeUpdated.forEach { uniqueStringId ->
      currentList.firstOrNull { it.uniqueStringId == uniqueStringId }?.let { attachmentInfo ->
        attachmentsRecyclerViewAdapter.notifyItemChanged(currentList.indexOf(attachmentInfo))
      }
    }
  }

  private fun changeGmailLabels() {
    if (AccountEntity.ACCOUNT_TYPE_GOOGLE == account?.accountType) {
      navController?.navigate(
        MessageDetailsFragmentDirections
          .actionMessageDetailsFragmentToChangeGmailLabelsForSingleMessageDialogFragment(
            requestKey = UUID.randomUUID().toString(),
            messageEntity = args.messageEntity
          )
      )
    }
  }

  companion object {
    private const val REQUEST_CODE_DELETE_MESSAGE_DIALOG = 103
    private const val CONTENT_MAX_ALLOWED_LENGTH = 50000
    private const val MAX_ALLOWED_RECIPIENTS_IN_HEADER_VALUE = 10

    private const val REQUEST_CODE_SAVE_ATTACHMENT = 1000
    private const val REQUEST_CODE_PREVIEW_ATTACHMENT = 1001

    private val REQUEST_KEY_CHOOSE_PUBLIC_KEY = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_CHOOSE_PUBLIC_KEY",
      MessageDetailsFragment::class.java
    )

    private val REQUEST_KEY_DOWNLOAD_ATTACHMENT = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_DOWNLOAD_ATTACHMENT",
      MessageDetailsFragment::class.java
    )

    private val REQUEST_KEY_FIX_MISSING_PASSPHRASE = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_FIX_MISSING_PASSPHRASE",
      MessageDetailsFragment::class.java
    )

    private val REQUEST_KEY_IMPORT_ADDITIONAL_PRIVATE_KEYS = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_IMPORT_ADDITIONAL_PRIVATE_KEYS",
      MessageDetailsFragment::class.java
    )
  }
}
