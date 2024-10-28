/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.navigation.NavDirections
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.model.ServiceInfo
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.databinding.FragmentThreadDetailsBinding
import com.flowcrypt.email.extensions.android.os.getParcelableArrayListViaExt
import com.flowcrypt.email.extensions.android.os.getParcelableViaExt
import com.flowcrypt.email.extensions.androidx.fragment.app.launchAndRepeatWithViewLifecycle
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.extensions.androidx.fragment.app.setFragmentResultListener
import com.flowcrypt.email.extensions.androidx.fragment.app.setFragmentResultListenerForTwoWayDialog
import com.flowcrypt.email.extensions.androidx.fragment.app.showChoosePublicKeyDialogFragment
import com.flowcrypt.email.extensions.androidx.fragment.app.showInfoDialog
import com.flowcrypt.email.extensions.androidx.fragment.app.showNeedPassphraseDialog
import com.flowcrypt.email.extensions.androidx.fragment.app.showTwoWayDialog
import com.flowcrypt.email.extensions.androidx.fragment.app.supportActionBar
import com.flowcrypt.email.extensions.androidx.fragment.app.toast
import com.flowcrypt.email.extensions.exceptionMsg
import com.flowcrypt.email.extensions.showDialogFragment
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
import com.flowcrypt.email.jetpack.viewmodel.RecipientsViewModel
import com.flowcrypt.email.jetpack.viewmodel.ThreadDetailsViewModel
import com.flowcrypt.email.jetpack.workmanager.sync.ArchiveMsgsWorker
import com.flowcrypt.email.jetpack.workmanager.sync.DeleteDraftsWorker
import com.flowcrypt.email.jetpack.workmanager.sync.DeleteMessagesPermanentlyWorker
import com.flowcrypt.email.jetpack.workmanager.sync.DeleteMessagesWorker
import com.flowcrypt.email.jetpack.workmanager.sync.MarkAsNotSpamWorker
import com.flowcrypt.email.jetpack.workmanager.sync.MovingToInboxWorker
import com.flowcrypt.email.jetpack.workmanager.sync.MovingToSpamWorker
import com.flowcrypt.email.jetpack.workmanager.sync.UpdateMsgsSeenStateWorker
import com.flowcrypt.email.model.MessageAction
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.providers.EmbeddedAttachmentsProvider
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.service.attachment.AttachmentDownloadManagerService
import com.flowcrypt.email.ui.activity.CreateMessageActivity
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.ui.activity.fragment.dialog.ChangeGmailLabelsDialogFragmentArgs
import com.flowcrypt.email.ui.activity.fragment.dialog.ChoosePublicKeyDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.DecryptAttachmentDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.DecryptAttachmentDialogFragmentArgs
import com.flowcrypt.email.ui.activity.fragment.dialog.DeleteDraftDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.DeleteDraftDialogFragmentArgs
import com.flowcrypt.email.ui.activity.fragment.dialog.DownloadAttachmentDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.DownloadAttachmentDialogFragmentArgs
import com.flowcrypt.email.ui.activity.fragment.dialog.FixNeedPassphraseIssueDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.ProcessMessageDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.ProcessMessageDialogFragmentArgs
import com.flowcrypt.email.ui.activity.fragment.dialog.TwoWayDialogFragment
import com.flowcrypt.email.ui.adapter.GmailApiLabelsListAdapter
import com.flowcrypt.email.ui.adapter.MessagesInThreadListAdapter
import com.flowcrypt.email.ui.adapter.recyclerview.itemdecoration.SkipFirstAndLastDividerItemDecoration
import com.flowcrypt.email.util.GeneralUtil
import org.apache.commons.io.FilenameUtils

/**
 * @author Denys Bondarenko
 */
class ThreadDetailsFragment : BaseFragment<FragmentThreadDetailsBinding>(), ProgressBehaviour {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentThreadDetailsBinding.inflate(inflater, container, false)

  override val progressView: View?
    get() = binding?.progress?.root
  override val contentView: View?
    get() = binding?.groupContent
  override val statusView: View?
    get() = binding?.status?.root

  private var isActive: Boolean = false
  private val isAdditionalActionEnabled: Boolean
    get() = threadDetailsViewModel.messagesInThreadFlow.value.status == Result.Status.SUCCESS
  private val threadMessageEntity: MessageEntity?
    get() = threadDetailsViewModel.threadMessageEntityFlow.value
  private val localFolder: LocalFolder?
    get() = threadDetailsViewModel.localFolderFlow.value

  private val args by navArgs<ThreadDetailsFragmentArgs>()
  private val recipientsViewModel: RecipientsViewModel by viewModels()
  private val threadDetailsViewModel: ThreadDetailsViewModel by viewModels {
    object : CustomAndroidViewModelFactory(requireActivity().application) {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ThreadDetailsViewModel(
          threadMessageEntityId = args.messageEntityId,
          localFolder = args.localFolder,
          application = requireActivity().application
        ) as T
      }
    }
  }
  private val requestPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
      toast(
        if (isGranted) {
          R.string.permissions_granted_and_now_you_can_download_attachments
        } else {
          R.string.cannot_save_attachment_without_permission
        }, Toast.LENGTH_LONG
      )
    }

  private val messagesInThreadListAdapter = MessagesInThreadListAdapter(
    object : MessagesInThreadListAdapter.OnMessageActionsListener {
      override fun onMessageClick(position: Int, message: MessagesInThreadListAdapter.Message) {
        processMessageClick(message)
      }

      override fun onHeadersDetailsClick(
        position: Int,
        message: MessagesInThreadListAdapter.Message
      ) {
        threadDetailsViewModel.onHeadersDetailsClick(message)
      }

      override fun onMessageChanged(position: Int, message: MessagesInThreadListAdapter.Message) {
        threadDetailsViewModel.onMessageChanged(message)
      }

      override fun onLabelClicked(label: GmailApiLabelsListAdapter.Label) {
        if (args.localFolder.searchQuery == null) {
          changeGmailLabels()
        }
      }

      override fun onAttachmentDownloadClick(
        attachmentInfo: AttachmentInfo,
        message: MessagesInThreadListAdapter.Message
      ) {
        when {
          FilenameUtils.getExtension(attachmentInfo.getSafeName())
            ?.lowercase() in AttachmentInfo.DANGEROUS_FILE_EXTENSIONS -> {
            showTwoWayDialog(
              requestKey = REQUEST_KEY_TWO_WAY_DIALOG_BASE + args.messageEntityId.toString(),
              requestCode = REQUEST_CODE_SHOW_WARNING_DIALOG_FOR_DOWNLOADING_DANGEROUS_FILE,
              dialogTitle = "",
              dialogMsg = getString(R.string.download_dangerous_file_warning),
              positiveButtonTitle = getString(R.string.continue_),
              negativeButtonTitle = getString(android.R.string.cancel),
              bundle = Bundle().apply {
                putLong(KEY_EXTRA_MESSAGE_ID, message.id)
                putString(KEY_EXTRA_ATTACHMENT_ID, attachmentInfo.uniqueStringId)
              }
            )
          }

          EmbeddedAttachmentsProvider.Cache.getInstance().getDocumentId(attachmentInfo) == null
              && attachmentInfo.isDecrypted
              && attachmentInfo.uri == null -> {
            //need to process message again and store attachment in RAM cache
            showDialogFragment(navController) {
              object : NavDirections {
                override val actionId = R.id.process_message_dialog_graph
                override val arguments = ProcessMessageDialogFragmentArgs(
                  requestKey = REQUEST_KEY_PROCESS_MESSAGE + args.messageEntityId.toString(),
                  requestCode = REQUEST_CODE_PROCESS_ATTACHMENT_DOWNLOAD,
                  message = message,
                  attachmentId = attachmentInfo.path
                ).toBundle()
              }
            }
          }

          else -> {
            processDownloadAttachment(attachmentInfo, message)
          }
        }
      }

      override fun onAttachmentPreviewClick(
        attachmentInfo: AttachmentInfo,
        message: MessagesInThreadListAdapter.Message
      ) {
        handleAttachmentPreviewClick(attachmentInfo, message)
      }

      override fun onReply(message: MessagesInThreadListAdapter.Message) {
        replyTo(message)
      }

      override fun onReplyAll(message: MessagesInThreadListAdapter.Message) {
        replyAllTo(message)
      }

      override fun onForward(message: MessagesInThreadListAdapter.Message) {
        forward(message)
      }

      override fun onEditDraft(message: MessagesInThreadListAdapter.Message) {
        editDraft(message)
      }

      override fun onDeleteDraft(message: MessagesInThreadListAdapter.Message) {
        deleteDraft(message)
      }

      override fun addRecipientsBasedOnPgpKeyDetails(pgpKeyRingDetails: PgpKeyRingDetails) {
        recipientsViewModel.addRecipientsBasedOnPgpKeyDetails(pgpKeyRingDetails)
        toast(R.string.pub_key_successfully_imported)
      }

      override fun updateExistingPubKey(
        publicKeyEntity: PublicKeyEntity,
        pgpKeyRingDetails: PgpKeyRingDetails
      ) {
        recipientsViewModel.updateExistingPubKey(publicKeyEntity, pgpKeyRingDetails)
        toast(R.string.pub_key_successfully_updated)
      }

      override fun importAdditionalPrivateKeys(message: MessagesInThreadListAdapter.Message) {
        account?.let { accountEntity ->
          navController?.navigate(
            object : NavDirections {
              override val actionId = R.id.import_additional_private_keys_graph
              override val arguments = ImportAdditionalPrivateKeysFragmentArgs(
                requestKey = REQUEST_KEY_IMPORT_ADDITIONAL_PRIVATE_KEYS + args.messageEntityId.toString(),
                accountEntity = accountEntity,
                bundle = Bundle().apply { putLong(KEY_EXTRA_MESSAGE_ID, message.id) }
              ).toBundle()
            }
          )
        }
      }

      override fun fixMissingPassphraseIssue(
        message: MessagesInThreadListAdapter.Message,
        fingerprints: List<String>
      ) {
        showNeedPassphraseDialog(
          requestKey = REQUEST_KEY_FIX_MISSING_PASSPHRASE + args.messageEntityId.toString(),
          requestCode = REQUEST_CODE_FIX_MISSING_PASSPHRASE_AFTER_PROCESS_MESSAGE,
          fingerprints = fingerprints,
          bundle = Bundle().apply { putLong(KEY_EXTRA_MESSAGE_ID, message.id) }
        )
      }

      override fun showSendersPublicKeyDialog(message: MessagesInThreadListAdapter.Message) {
        showChoosePublicKeyDialogFragment(
          requestKey = REQUEST_KEY_CHOOSE_PUBLIC_KEY + args.messageEntityId.toString(),
          email = message.messageEntity.account,
          choiceMode = ListView.CHOICE_MODE_SINGLE,
          titleResourceId = R.plurals.tell_sender_to_update_their_settings,
          bundle = Bundle().apply { putLong(KEY_EXTRA_MESSAGE_ID, message.id) }
        )
      }

      override fun getAccount(): AccountEntity? = account
    })

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    updateActionBar()

    initViews()
    setupThreadDetailsViewModel()
    subscribeToTwoWayDialog()
    subscribeToProcessMessageDialogFragment()
    subscribeToFixNeedPassphraseIssueDialogFragment()
    subscribeToDownloadAttachmentViaDialog()
    subscribeToDeleteDraftDialog()
    subscribeToImportingAdditionalPrivateKeys()
    subscribeToChoosePublicKeyDialogFragment()
    subscribeToDecryptAttachmentViaDialog()
  }

  override fun onSetupActionBarMenu(menuHost: MenuHost) {
    super.onSetupActionBarMenu(menuHost)

    menuHost.addMenuProvider(object : MenuProvider {
      override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if (!args.isViewPagerMode) {
          menuInflater.inflate(R.menu.fragment_thread_details, menu)
        }
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

        menuItemArchiveMsg?.isVisible = threadDetailsViewModel.getMessageActionAvailability(
          MessageAction.ARCHIVE
        )
        menuItemDeleteMsg?.isVisible = threadDetailsViewModel.getMessageActionAvailability(
          MessageAction.DELETE
        )
        menuActionMoveToInbox?.isVisible = threadDetailsViewModel.getMessageActionAvailability(
          MessageAction.MOVE_TO_INBOX
        )
        menuActionMarkUnread?.isVisible = threadDetailsViewModel.getMessageActionAvailability(
          MessageAction.MARK_UNREAD
        )
        menuActionMoveToSpam?.isVisible = threadDetailsViewModel.getMessageActionAvailability(
          MessageAction.MOVE_TO_SPAM
        )
        menuActionMarkAsNotSpam?.isVisible = threadDetailsViewModel.getMessageActionAvailability(
          MessageAction.MARK_AS_NOT_SPAM
        )
        menuActionChangeLabels?.isVisible = threadDetailsViewModel.getMessageActionAvailability(
          MessageAction.CHANGE_LABELS
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
            threadDetailsViewModel.changeMsgState(MessageState.PENDING_ARCHIVING)
            true
          }

          R.id.menuActionDeleteMessage -> {
            val messageEntity = threadMessageEntity ?: return true
            if (messageEntity.isOutboxMsg) {
              if (messageEntity.msgState == MessageState.SENDING) {
                toast(R.string.can_not_delete_sending_message, Toast.LENGTH_LONG)
              } else {
                threadDetailsViewModel.deleteThread()
                toast(R.string.thread_was_deleted)
              }
            } else {
              if (localFolder?.getFolderType() == FoldersManager.FolderType.TRASH) {
                showTwoWayDialog(
                  requestKey = REQUEST_KEY_TWO_WAY_DIALOG_BASE + args.messageEntityId.toString(),
                  requestCode = REQUEST_CODE_DELETE_MESSAGE_DIALOG,
                  dialogTitle = "",
                  dialogMsg = requireContext().resources.getQuantityString(
                    R.plurals.delete_thread_question,
                    1,
                    1
                  ),
                  positiveButtonTitle = getString(android.R.string.ok),
                  negativeButtonTitle = getString(android.R.string.cancel),
                )
              } else {
                threadDetailsViewModel.changeMsgState(
                  if (messageEntity.isDraft) {
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
            threadDetailsViewModel.changeMsgState(MessageState.PENDING_MOVE_TO_INBOX)
            true
          }

          R.id.menuActionMarkUnread -> {
            threadDetailsViewModel.changeMsgState(MessageState.PENDING_MARK_UNREAD)
            true
          }

          R.id.menuActionMoveToSpam -> {
            threadDetailsViewModel.changeMsgState(MessageState.PENDING_MOVE_TO_SPAM)
            true
          }

          R.id.menuActionMarkAsNotSpam -> {
            threadDetailsViewModel.changeMsgState(MessageState.PENDING_MARK_AS_NOT_SPAM)
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

  fun changeActiveState(isActive: Boolean) {
    this.isActive = isActive

    if (isActive) {
      threadDetailsViewModel.messagesInThreadFlow.value.data?.let {
        tryToOpenTheFreshestMessage(it)
      }
    }
  }

  private fun updateActionBar() {
    supportActionBar?.title = null
    supportActionBar?.subtitle = null
  }

  private fun initViews() {
    binding?.recyclerViewMessages?.apply {
      val linearLayoutManager = LinearLayoutManager(context)
      layoutManager = linearLayoutManager
      addItemDecoration(
        SkipFirstAndLastDividerItemDecoration(
          context, linearLayoutManager.orientation
        )
      )
      adapter = messagesInThreadListAdapter
    }
  }

  private fun setupThreadDetailsViewModel() {
    launchAndRepeatWithViewLifecycle {
      threadDetailsViewModel.messagesInThreadFlow.collect {
        activity?.invalidateOptionsMenu()

        when (it.status) {
          Result.Status.LOADING -> {
            showProgress()
          }

          Result.Status.SUCCESS -> {
            val data = it.data
            if (data.isNullOrEmpty()) {
              navController?.navigateUp()
              toast("Fix me")
            } else {
              messagesInThreadListAdapter.submitList(data)
              (data.getOrNull(1) as? MessagesInThreadListAdapter.Message)?.let { message ->
                updateThreadReplyButtons(message)
              }
              showContent()
              tryToOpenTheFreshestMessage(data)
            }
          }

          Result.Status.EXCEPTION -> {
            showStatus(it.exceptionMsg) {
              threadDetailsViewModel.loadMessages()
            }
          }

          else -> {}
        }
      }
    }

    launchAndRepeatWithViewLifecycle {
      threadDetailsViewModel.localFolderFlow.collect {
        //do nothing. Just subscribe for updates to have the latest value async
      }
    }

    launchAndRepeatWithViewLifecycle {
      threadDetailsViewModel.messageActionsAvailabilityStateFlow.collect {
        activity?.invalidateOptionsMenu()
      }
    }

    launchAndRepeatWithViewLifecycle {
      threadDetailsViewModel.sessionMessageStateStateFlow.collect { newState ->
        newState ?: return@collect //skip initial value

        activity?.invalidateOptionsMenu()

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
  }

  private fun subscribeToTwoWayDialog() {
    setFragmentResultListenerForTwoWayDialog(
      requestKey = REQUEST_KEY_TWO_WAY_DIALOG_BASE + args.messageEntityId.toString(),
      useSuperParentFragmentManagerIfPossible = args.isViewPagerMode
    ) { _, bundle ->
      val requestCode = bundle.getInt(TwoWayDialogFragment.KEY_REQUEST_CODE)
      val result = bundle.getInt(TwoWayDialogFragment.KEY_RESULT)

      when (requestCode) {
        REQUEST_CODE_DELETE_MESSAGE_DIALOG -> if (result == TwoWayDialogFragment.RESULT_OK) {
          threadDetailsViewModel.changeMsgState(MessageState.PENDING_DELETING_PERMANENTLY)
        }

        REQUEST_CODE_SHOW_WARNING_DIALOG_FOR_DOWNLOADING_DANGEROUS_FILE ->
          if (result == TwoWayDialogFragment.RESULT_OK) {
            bundle.getBundle(TwoWayDialogFragment.KEY_REQUEST_INCOMING_BUNDLE)
              ?.let {
                processActionForMessageAndAttachmentBasedOnIncomingBundle(it) { attachmentInfo, message ->
                  processDownloadAttachment(attachmentInfo, message)
                }
              }
          }

        REQUEST_CODE_SHOW_TWO_WAY_DIALOG_FOR_DELETING_DRAFT ->
          if (result == TwoWayDialogFragment.RESULT_OK) {
            bundle.getBundle(TwoWayDialogFragment.KEY_REQUEST_INCOMING_BUNDLE)?.let {
              processActionForMessageBasedOnIncomingBundle(it) { message ->
                if (message.messageEntity.id != null) {
                  showDialogFragment(navController) {
                    object : NavDirections {
                      override val actionId = R.id.delete_draft_dialog_graph
                      override val arguments = DeleteDraftDialogFragmentArgs(
                        requestKey = REQUEST_KEY_DELETE_DRAFT + args.messageEntityId.toString(),
                        uniqueId = message.id,
                        messageEntityId = message.messageEntity.id,
                      ).toBundle()
                    }
                  }
                }
              }
            }
          }
      }
    }
  }

  private fun subscribeToProcessMessageDialogFragment() {
    setFragmentResultListener(
      requestKey = REQUEST_KEY_PROCESS_MESSAGE + args.messageEntityId.toString(),
      useSuperParentFragmentManagerIfPossible = args.isViewPagerMode
    ) { _, bundle ->
      val message: MessagesInThreadListAdapter.Message? = bundle.getParcelableViaExt(
        ProcessMessageDialogFragment.KEY_RESULT
      ) as? MessagesInThreadListAdapter.Message?
      val requestCode = bundle.getInt(ProcessMessageDialogFragment.KEY_REQUEST_CODE)
      if (message != null) {
        val updatedMessage = message.copy(
          type = MessagesInThreadListAdapter.Type.MESSAGE_EXPANDED,
          attachments = message.attachments.map { attachmentInfo ->
            if (attachmentInfo.rawData != null) {
              EmbeddedAttachmentsProvider.Cache.getInstance().addAndGet(attachmentInfo)
            } else {
              attachmentInfo
            }
          }
        )

        threadDetailsViewModel.onMessageChanged(updatedMessage)

        when (requestCode) {
          REQUEST_CODE_PROCESS_ATTACHMENT_PREVIEW -> {
            val attachmentId = bundle.getString(ProcessMessageDialogFragment.KEY_ATTACHMENT_ID)
            updatedMessage.attachments.firstOrNull { it.path == attachmentId }
              ?.let { attachmentInfo ->
                handleAttachmentPreviewClick(attachmentInfo, updatedMessage)
              }
          }

          REQUEST_CODE_PROCESS_ATTACHMENT_DOWNLOAD -> {
            val attachmentId = bundle.getString(ProcessMessageDialogFragment.KEY_ATTACHMENT_ID)
            updatedMessage.attachments.firstOrNull { it.path == attachmentId }
              ?.let { attachmentInfo ->
                processDownloadAttachment(attachmentInfo, updatedMessage)
              }
          }
        }
      } else {
        toast(R.string.unknown_error)
      }
    }
  }

  private fun subscribeToFixNeedPassphraseIssueDialogFragment() {
    setFragmentResultListener(
      requestKey = REQUEST_KEY_FIX_MISSING_PASSPHRASE + args.messageEntityId.toString(),
      useSuperParentFragmentManagerIfPossible = args.isViewPagerMode
    ) { _, bundle ->
      val requestCode = bundle.getInt(
        FixNeedPassphraseIssueDialogFragment.KEY_REQUEST_CODE, Int.MIN_VALUE
      )

      when (requestCode) {
        REQUEST_CODE_FIX_MISSING_PASSPHRASE_BEFORE_PROCESS_MESSAGE -> {
          bundle.getBundle(FixNeedPassphraseIssueDialogFragment.KEY_REQUEST_INCOMING_BUNDLE)?.let {
            processActionForMessageBasedOnIncomingBundle(it) { message ->
              processMessageClick(message)
            }
          }
        }

        REQUEST_CODE_FIX_MISSING_PASSPHRASE_AFTER_PROCESS_MESSAGE -> {
          bundle.getBundle(FixNeedPassphraseIssueDialogFragment.KEY_REQUEST_INCOMING_BUNDLE)?.let {
            processActionForMessageBasedOnIncomingBundle(it) { message ->
              processMessageClick(message = message, forceProcess = true)
            }
          }
        }

        REQUEST_CODE_FIX_MISSING_PASSPHRASE_BEFORE_PREVIEW_ATTACHMENT -> {
          bundle.getBundle(FixNeedPassphraseIssueDialogFragment.KEY_REQUEST_INCOMING_BUNDLE)?.let {
            processActionForMessageAndAttachmentBasedOnIncomingBundle(it) { attachmentInfo, message ->
              handleAttachmentPreviewClick(attachmentInfo, message)
            }
          }
        }

        REQUEST_CODE_FIX_MISSING_PASSPHRASE_BEFORE_DOWNLOAD_ATTACHMENT -> {
          bundle.getBundle(FixNeedPassphraseIssueDialogFragment.KEY_REQUEST_INCOMING_BUNDLE)?.let {
            processActionForMessageAndAttachmentBasedOnIncomingBundle(it) { attachmentInfo, message ->
              downloadAttachment(attachmentInfo, message)
            }
          }
        }

        REQUEST_CODE_FIX_MISSING_PASSPHRASE_BEFORE_EDITING_DRAFT -> {
          bundle.getBundle(FixNeedPassphraseIssueDialogFragment.KEY_REQUEST_INCOMING_BUNDLE)?.let {
            processActionForMessageBasedOnIncomingBundle(it) { message ->
              editDraft(message)
            }
          }
        }

        else -> toast(R.string.unknown_error)
      }
    }
  }

  private fun subscribeToDownloadAttachmentViaDialog() {
    setFragmentResultListener(
      REQUEST_KEY_DOWNLOAD_ATTACHMENT + args.messageEntityId.toString(),
      args.isViewPagerMode
    ) { _, bundle ->
      val requestCode = bundle.getInt(DownloadAttachmentDialogFragment.KEY_REQUEST_CODE)

      val attachmentInfo = bundle.getParcelableViaExt<AttachmentInfo>(
        DownloadAttachmentDialogFragment.KEY_ATTACHMENT
      )?.let { EmbeddedAttachmentsProvider.Cache.getInstance().addAndGet(it) }
        ?: return@setFragmentResultListener

      val message = bundle.getBundle(DownloadAttachmentDialogFragment.KEY_INCOMING_BUNDLE)?.getLong(
        KEY_EXTRA_MESSAGE_ID
      )?.let { messageId ->
        messagesInThreadListAdapter.currentList.firstOrNull {
          it.id == messageId
        } as? MessagesInThreadListAdapter.Message
      } ?: return@setFragmentResultListener

      threadDetailsViewModel.onMessageChanged(
        message.copy(
          attachments = message.attachments.toMutableList().apply {
            replaceAll {
              if (it.id == attachmentInfo.id) {
                attachmentInfo
              } else {
                it
              }
            }
          }
        )
      )

      when (requestCode) {
        REQUEST_CODE_PREVIEW_ATTACHMENT -> {
          previewAttachment(attachmentInfo)
        }

        REQUEST_CODE_SAVE_ATTACHMENT -> {
          downloadAttachment(attachmentInfo = attachmentInfo, message = message)
        }
      }
    }
  }

  private fun subscribeToDeleteDraftDialog() {
    setFragmentResultListener(
      REQUEST_KEY_DELETE_DRAFT + args.messageEntityId.toString(),
      args.isViewPagerMode
    ) { _, bundle ->
      val uniqueMessageId = bundle.getLong(
        DeleteDraftDialogFragment.KEY_RESULT
      )

      messagesInThreadListAdapter.deleteMessageById(uniqueMessageId)
    }
  }

  private fun subscribeToImportingAdditionalPrivateKeys() {
    setFragmentResultListener(
      REQUEST_KEY_IMPORT_ADDITIONAL_PRIVATE_KEYS + args.messageEntityId.toString(),
      args.isViewPagerMode
    ) { _, bundle ->
      val keys = bundle.getParcelableArrayListViaExt<PgpKeyRingDetails>(
        ImportAdditionalPrivateKeysFragment.KEY_IMPORTED_PRIVATE_KEYS
      )
      if (keys?.isNotEmpty() == true) {
        toast(R.string.key_successfully_imported)
        val message =
          bundle.getBundle(ImportAdditionalPrivateKeysFragment.KEY_INCOMING_BUNDLE)?.getLong(
            KEY_EXTRA_MESSAGE_ID
          )?.let { messageId ->
            messagesInThreadListAdapter.currentList.firstOrNull {
              it.id == messageId
            } as? MessagesInThreadListAdapter.Message
          } ?: return@setFragmentResultListener

        processMessageClick(message = message, forceProcess = true)
      }
    }
  }

  private fun subscribeToChoosePublicKeyDialogFragment() {
    setFragmentResultListener(
      REQUEST_KEY_CHOOSE_PUBLIC_KEY + args.messageEntityId.toString(),
      args.isViewPagerMode
    ) { _, bundle ->
      val keyList = bundle.getParcelableArrayListViaExt<AttachmentInfo>(
        ChoosePublicKeyDialogFragment.KEY_ATTACHMENT_INFO_LIST
      )?.map { attachmentInfo ->
        attachmentInfo.copy(isProtected = true)
      } ?: return@setFragmentResultListener
      if (keyList.isNotEmpty()) {
        val message =
          bundle.getBundle(ChoosePublicKeyDialogFragment.KEY_INCOMING_BUNDLE)?.getLong(
            KEY_EXTRA_MESSAGE_ID
          )?.let { messageId ->
            messagesInThreadListAdapter.currentList.firstOrNull {
              it.id == messageId
            } as? MessagesInThreadListAdapter.Message
          } ?: return@setFragmentResultListener
        sendTemplateMsgWithPublicKey(message, keyList[0])
      } else {
        toast(R.string.account_has_no_associated_keys)
      }
    }
  }

  private fun subscribeToDecryptAttachmentViaDialog() {
    setFragmentResultListener(
      REQUEST_KEY_DECRYPT_ATTACHMENT + args.messageEntityId.toString(),
      args.isViewPagerMode
    ) { _, bundle ->
      val attachmentInfo =
        bundle.getParcelableViaExt<AttachmentInfo>(DecryptAttachmentDialogFragment.KEY_ATTACHMENT)
          ?: return@setFragmentResultListener

      previewAttachment(EmbeddedAttachmentsProvider.Cache.getInstance().addAndGet(attachmentInfo))
    }
  }

  private fun replyTo(message: MessagesInThreadListAdapter.Message) {
    startActivity(
      prepareReply(message, MessageType.REPLY)
    )
  }

  private fun replyAllTo(message: MessagesInThreadListAdapter.Message) {
    startActivity(
      prepareReply(message, MessageType.REPLY_ALL)
    )
  }

  private fun prepareReply(message: MessagesInThreadListAdapter.Message, replyType: Int) =
    CreateMessageActivity.generateIntent(
      context = context,
      messageType = replyType,
      msgEncryptionType = message.messageEntity.getMessageEncryptionType(),
      msgInfo = message.incomingMessageInfo?.toReplyVersion(
        requireContext(),
        CONTENT_MAX_ALLOWED_LENGTH
      )
    )

  private fun forward(message: MessagesInThreadListAdapter.Message) {
    if (message.attachments.none { it.isEmbeddedAndPossiblyEncrypted() }) {
      startActivity(
        CreateMessageActivity.generateIntent(
          context = context,
          messageType = MessageType.FORWARD,
          msgEncryptionType = message.messageEntity.getMessageEncryptionType(),
          msgInfo = message.incomingMessageInfo?.toReplyVersion(
            requireContext(),
            CONTENT_MAX_ALLOWED_LENGTH
          ),
          //attachments = prepareAttachmentsForForwarding().toTypedArray()
        )
      )
    } else {
      /*navController?.navigate(
        object : NavDirections {
          override val actionId =
            R.id.prepare_downloaded_attachments_for_forwarding_dialog_graph
          override val arguments =
            DecryptDownloadedAttachmentsBeforeForwardingDialogFragmentArgs(
              requestKey = REQUEST_KEY_PREPARE_DOWNLOADED_ATTACHMENTS_FOR_FORWARDING + args.messageEntityId.toString(),
              attachments = attachmentsRecyclerViewAdapter.currentList.filter {
                it.isEmbeddedAndPossiblyEncrypted()
              }.toTypedArray()
            ).toBundle()
        }
      )*/
    }
  }

  private fun tryToOpenTheFreshestMessage(data: List<MessagesInThreadListAdapter.Item>) {
    if (isActive && data.size > 1) {
      val existing = messagesInThreadListAdapter.currentList.getOrNull(1)
          as? MessagesInThreadListAdapter.Message
      val firstMessage = data[1] as? MessagesInThreadListAdapter.Message

      if (existing != null && existing.type == MessagesInThreadListAdapter.Type.MESSAGE_COLLAPSED
        && existing.incomingMessageInfo == null
      ) {
        if (firstMessage != null && firstMessage.incomingMessageInfo == null) {
          processMessageClick(firstMessage)
        }
      }
    }
  }

  private fun processMessageClick(
    message: MessagesInThreadListAdapter.Message,
    forceProcess: Boolean = false
  ) {
    if (message.messageEntity.hasPgp == true) {
      val keysStorage = KeysStorageImpl.getInstance(requireContext())
      val fingerprints = keysStorage.getFingerprintsWithEmptyPassphrase()
      if (fingerprints.isNotEmpty()) {
        showNeedPassphraseDialog(
          requestKey = REQUEST_KEY_FIX_MISSING_PASSPHRASE + args.messageEntityId.toString(),
          requestCode = REQUEST_CODE_FIX_MISSING_PASSPHRASE_BEFORE_PROCESS_MESSAGE,
          fingerprints = fingerprints,
          bundle = Bundle().apply { putLong(KEY_EXTRA_MESSAGE_ID, message.id) }
        )
        return
      }
    }

    if (message.type == MessagesInThreadListAdapter.Type.MESSAGE_COLLAPSED || forceProcess) {
      if (message.incomingMessageInfo == null || forceProcess) {
        showDialogFragment(navController) {
          return@showDialogFragment object : NavDirections {
            override val actionId = R.id.process_message_dialog_graph
            override val arguments = ProcessMessageDialogFragmentArgs(
              requestKey = REQUEST_KEY_PROCESS_MESSAGE + args.messageEntityId.toString(),
              requestCode = REQUEST_CODE_PROCESS_WHOLE_MESSAGE,
              message = message
            ).toBundle()
          }
        }
      } else {
        threadDetailsViewModel.onMessageClicked(message)
      }
    } else {
      threadDetailsViewModel.onMessageClicked(message)
    }
  }

  private fun changeGmailLabels() {
    if (AccountEntity.ACCOUNT_TYPE_GOOGLE == account?.accountType) {
      showDialogFragment(navController) {
        object : NavDirections {
          override val actionId = R.id.change_gmail_labels_for_single_message_dialog_graph
          override val arguments = ChangeGmailLabelsDialogFragmentArgs(
            requestKey = REQUEST_KEY_CHANGE_LABELS + args.messageEntityId.toString(),
            messageEntityIds = arrayOf(args.messageEntityId).toLongArray()
          ).toBundle()
        }
      }
    }
  }

  private fun updateThreadReplyButtons(message: MessagesInThreadListAdapter.Message) {
    val replyButton = binding?.layoutReplyButtons?.replyButton
    val replyAllButton = binding?.layoutReplyButtons?.replyAllButton
    val forwardButton = binding?.layoutReplyButtons?.forwardButton

    val buttonsColorId: Int

    if (message.messageEntity.hasPgp == true) {
      buttonsColorId = R.color.colorPrimary
      replyButton?.setText(R.string.reply_encrypted)
      replyAllButton?.setText(R.string.reply_all_encrypted)
      forwardButton?.setText(R.string.forward_encrypted)
    } else {
      buttonsColorId = R.color.red
      replyButton?.setText(R.string.reply)
      replyAllButton?.setText(R.string.reply_all)
      forwardButton?.setText(R.string.forward)
    }

    val colorStateList =
      ColorStateList.valueOf(ContextCompat.getColor(requireContext(), buttonsColorId))

    replyButton?.iconTint = colorStateList
    replyAllButton?.iconTint = colorStateList
    forwardButton?.iconTint = colorStateList

    replyButton?.setOnClickListener {
      replyTo(message)
    }
    replyAllButton?.setOnClickListener {
      replyAllTo(message)
    }
    forwardButton?.setOnClickListener {
      forward(message)
    }
  }

  private fun handleAttachmentPreviewClick(
    attachmentInfo: AttachmentInfo,
    message: MessagesInThreadListAdapter.Message
  ) {
    val documentId = EmbeddedAttachmentsProvider.Cache.getInstance()
      .getDocumentId(attachmentInfo)

    if (attachmentInfo.uri != null && documentId != null) {
      if (attachmentInfo.isPossiblyEncrypted) {
        val embeddedAttachmentsCache = EmbeddedAttachmentsProvider.Cache.getInstance()
        val existingDocumentIdForDecryptedVersion = embeddedAttachmentsCache
          .getDocumentId(attachmentInfo.copy(name = FilenameUtils.getBaseName(attachmentInfo.name)))

        if (existingDocumentIdForDecryptedVersion != null) {
          embeddedAttachmentsCache.getUriVersion(existingDocumentIdForDecryptedVersion)?.let {
            previewAttachment(it)
          }
        } else {
          if (!checkAndShowNeedPassphraseDialogIfNeeded(
              attachmentInfo = attachmentInfo,
              message = message,
              requestCode = REQUEST_CODE_FIX_MISSING_PASSPHRASE_BEFORE_PREVIEW_ATTACHMENT
            )
          ) {
            navController?.navigate(
              object : NavDirections {
                override val actionId = R.id.decrypt_attachment_dialog_graph
                override val arguments = DecryptAttachmentDialogFragmentArgs(
                  attachmentInfo = attachmentInfo.copy(),
                  requestKey = REQUEST_KEY_DECRYPT_ATTACHMENT + args.messageEntityId.toString(),
                  requestCode = REQUEST_CODE_DECRYPT_ATTACHMENT,
                  bundle = Bundle().apply {
                    putLong(KEY_EXTRA_MESSAGE_ID, message.id)
                    putString(KEY_EXTRA_ATTACHMENT_ID, attachmentInfo.uniqueStringId)
                  }
                ).toBundle()
              }
            )
          }
        }
      } else {
        previewAttachment(attachmentInfo)
      }
    } else {
      when {
        checkAndShowNeedPassphraseDialogIfNeeded(
          attachmentInfo = attachmentInfo,
          message = message,
          requestCode = REQUEST_CODE_FIX_MISSING_PASSPHRASE_BEFORE_PREVIEW_ATTACHMENT
        ) -> return

        attachmentInfo.isDecrypted && attachmentInfo.uri == null -> {
          //need to process message again and store attachment in RAM cache
          showDialogFragment(navController) {
            object : NavDirections {
              override val actionId = R.id.process_message_dialog_graph
              override val arguments = ProcessMessageDialogFragmentArgs(
                requestKey = REQUEST_KEY_PROCESS_MESSAGE + args.messageEntityId.toString(),
                requestCode = REQUEST_CODE_PROCESS_ATTACHMENT_PREVIEW,
                message = message,
                attachmentId = attachmentInfo.path
              ).toBundle()
            }
          }
        }

        else -> {
          showDialogFragment(navController) {
            object : NavDirections {
              override val actionId = R.id.download_attachment_dialog_graph
              override val arguments = DownloadAttachmentDialogFragmentArgs(
                attachmentInfo = attachmentInfo,
                requestKey = REQUEST_KEY_DOWNLOAD_ATTACHMENT + args.messageEntityId.toString(),
                requestCode = REQUEST_CODE_PREVIEW_ATTACHMENT,
                bundle = Bundle().apply {
                  putLong(KEY_EXTRA_MESSAGE_ID, message.id)
                }
              ).toBundle()
            }
          }
        }
      }
    }
  }

  private fun previewAttachment(attachmentInfo: AttachmentInfo) {
    val intent = if (attachmentInfo.uri != null) {
      GeneralUtil.genViewAttachmentIntent(requireNotNull(attachmentInfo.uri), attachmentInfo)
    } else {
      toast(getString(R.string.preview_is_not_available))
      return
    }

    if (account?.isHandlingAttachmentRestricted() == true) {
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

  private fun processDownloadAttachment(
    attachmentInfo: AttachmentInfo,
    message: MessagesInThreadListAdapter.Message
  ) {
    if (
      checkAndShowNeedPassphraseDialogIfNeeded(
        attachmentInfo = attachmentInfo,
        message = message,
        requestCode = REQUEST_CODE_FIX_MISSING_PASSPHRASE_BEFORE_DOWNLOAD_ATTACHMENT
      )
    ) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
      ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.WRITE_EXTERNAL_STORAGE
      ) == PackageManager.PERMISSION_GRANTED
    ) {
      downloadAttachment(attachmentInfo, message)
    } else {
      requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
  }

  private fun downloadAttachment(
    attachmentInfo: AttachmentInfo,
    message: MessagesInThreadListAdapter.Message
  ) {
    val documentId = EmbeddedAttachmentsProvider.Cache.getInstance()
      .getDocumentId(attachmentInfo)

    when {
      attachmentInfo.uri != null && documentId != null -> {
        requireContext().startService(
          AttachmentDownloadManagerService.newIntent(context = context, attInfo = attachmentInfo)
        )
      }

      account?.isHandlingAttachmentRestricted() == true -> {
        showDialogFragment(navController) {
          object : NavDirections {
            override val actionId = R.id.download_attachment_dialog_graph
            override val arguments = DownloadAttachmentDialogFragmentArgs(
              attachmentInfo = attachmentInfo,
              requestKey = REQUEST_KEY_DOWNLOAD_ATTACHMENT + args.messageEntityId.toString(),
              requestCode = REQUEST_CODE_SAVE_ATTACHMENT,
              bundle = Bundle().apply {
                putLong(KEY_EXTRA_MESSAGE_ID, message.id)
              }
            ).toBundle()
          }
        }
      }

      else -> {
        requireContext().startService(
          AttachmentDownloadManagerService.newIntent(context = context, attInfo = attachmentInfo)
        )
      }
    }
  }

  private fun checkAndShowNeedPassphraseDialogIfNeeded(
    attachmentInfo: AttachmentInfo,
    message: MessagesInThreadListAdapter.Message,
    requestCode: Int
  ): Boolean {
    if (attachmentInfo.isPossiblyEncrypted || attachmentInfo.isDecrypted) {
      val keysStorage = KeysStorageImpl.getInstance(requireContext())
      val fingerprints = keysStorage.getFingerprintsWithEmptyPassphrase()
      if (fingerprints.isNotEmpty()) {
        showNeedPassphraseDialog(
          requestKey = REQUEST_KEY_FIX_MISSING_PASSPHRASE + args.messageEntityId.toString(),
          requestCode = requestCode,
          fingerprints = fingerprints,
          bundle = Bundle().apply {
            putLong(KEY_EXTRA_MESSAGE_ID, message.id)
            putString(KEY_EXTRA_ATTACHMENT_ID, attachmentInfo.uniqueStringId)
          }
        )
        return true
      }
    }
    return false
  }

  private fun processActionForMessageAndAttachmentBasedOnIncomingBundle(
    bundle: Bundle,
    action: (AttachmentInfo, MessagesInThreadListAdapter.Message) -> Unit
  ) {
    val messageId = bundle.getLong(KEY_EXTRA_MESSAGE_ID, Long.MIN_VALUE)
    val attachmentId = bundle.getString(KEY_EXTRA_ATTACHMENT_ID)
    if (messageId > 0 && !attachmentId.isNullOrEmpty()) {
      (messagesInThreadListAdapter.currentList.firstOrNull { item ->
        item.id == messageId
      } as? MessagesInThreadListAdapter.Message)?.let { message ->
        val attachmentInfo = message.attachments.firstOrNull { attachmentInfo ->
          attachmentInfo.uniqueStringId == attachmentId
        } ?: return
        action.invoke(attachmentInfo, message)
      }
    }
  }

  private fun processActionForMessageBasedOnIncomingBundle(
    bundle: Bundle,
    action: (MessagesInThreadListAdapter.Message) -> Unit
  ) {
    val messageId = bundle.getLong(KEY_EXTRA_MESSAGE_ID, Long.MIN_VALUE)
    if (messageId > 0) {
      (messagesInThreadListAdapter.currentList.firstOrNull { item ->
        item.id == messageId
      } as? MessagesInThreadListAdapter.Message)?.let { message ->
        action.invoke(message)
      }
    }
  }

  private fun editDraft(message: MessagesInThreadListAdapter.Message) {
    if (message.messageEntity.hasPgp == true) {
      val keysStorage = KeysStorageImpl.getInstance(requireContext())
      val fingerprints = keysStorage.getFingerprintsWithEmptyPassphrase()
      if (fingerprints.isNotEmpty()) {
        showNeedPassphraseDialog(
          requestKey = REQUEST_KEY_FIX_MISSING_PASSPHRASE + args.messageEntityId.toString(),
          requestCode = REQUEST_CODE_FIX_MISSING_PASSPHRASE_BEFORE_EDITING_DRAFT,
          fingerprints = fingerprints,
          bundle = Bundle().apply {
            putLong(KEY_EXTRA_MESSAGE_ID, message.id)
          }
        )
        return
      }
    }

    startActivity(
      CreateMessageActivity.generateIntent(
        context = context,
        messageType = MessageType.DRAFT,
        msgEncryptionType = message.messageEntity.getMessageEncryptionType(),
        msgInfo = message.incomingMessageInfo?.toReplyVersion(
          requireContext(),
          CONTENT_MAX_ALLOWED_LENGTH
        )
      )
    )
  }

  private fun deleteDraft(message: MessagesInThreadListAdapter.Message) {
    showTwoWayDialog(
      requestKey = REQUEST_KEY_TWO_WAY_DIALOG_BASE + args.messageEntityId.toString(),
      requestCode = REQUEST_CODE_SHOW_TWO_WAY_DIALOG_FOR_DELETING_DRAFT,
      dialogTitle = "",
      dialogMsg = getString(R.string.delete_draft),
      positiveButtonTitle = getString(R.string.delete),
      negativeButtonTitle = getString(android.R.string.cancel),
      bundle = Bundle().apply { putLong(KEY_EXTRA_MESSAGE_ID, message.id) }
    )
  }

  /**
   * Send a template message with a sender public key.
   */
  private fun sendTemplateMsgWithPublicKey(
    message: MessagesInThreadListAdapter.Message,
    attachmentInfo: AttachmentInfo
  ) {
    startActivity(
      CreateMessageActivity.generateIntent(
        context = context,
        messageType = MessageType.REPLY,
        msgEncryptionType = MessageEncryptionType.STANDARD,
        msgInfo = message.incomingMessageInfo?.toReplyVersion(
          requireContext(),
          CONTENT_MAX_ALLOWED_LENGTH
        ),
        serviceInfo = ServiceInfo(
          isToFieldEditable = false,
          isFromFieldEditable = false,
          isMsgEditable = false,
          isSubjectEditable = false,
          isMsgTypeSwitchable = false,
          hasAbilityToAddNewAtt = false,
          systemMsg = getString(R.string.message_was_encrypted_for_wrong_key),
          atts = listOf(attachmentInfo.copy(isProtected = true))
        )
      )
    )
  }

  companion object {
    private const val KEY_EXTRA_MESSAGE_ID = "MESSAGE_ID"
    private const val KEY_EXTRA_ATTACHMENT_ID = "ATTACHMENT_ID"

    private val REQUEST_KEY_PROCESS_MESSAGE = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_PROCESS_MESSAGE",
      ThreadDetailsFragment::class.java
    )

    private val REQUEST_KEY_FIX_MISSING_PASSPHRASE = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_FIX_MISSING_PASSPHRASE",
      ThreadDetailsFragment::class.java
    )

    private val REQUEST_KEY_CHANGE_LABELS = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_CHANGE_LABELS",
      ThreadDetailsFragment::class.java
    )

    private val REQUEST_KEY_DOWNLOAD_ATTACHMENT = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_DOWNLOAD_ATTACHMENT",
      ThreadDetailsFragment::class.java
    )

    private val REQUEST_KEY_TWO_WAY_DIALOG_BASE = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_TWO_WAY_DIALOG_BASE",
      ThreadDetailsFragment::class.java
    )

    private val REQUEST_KEY_DELETE_DRAFT = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_DELETE_DRAFT",
      ThreadDetailsFragment::class.java
    )

    private val REQUEST_KEY_IMPORT_ADDITIONAL_PRIVATE_KEYS = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_IMPORT_ADDITIONAL_PRIVATE_KEYS",
      ThreadDetailsFragment::class.java
    )

    private val REQUEST_KEY_CHOOSE_PUBLIC_KEY = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_CHOOSE_PUBLIC_KEY",
      ThreadDetailsFragment::class.java
    )

    private val REQUEST_KEY_DECRYPT_ATTACHMENT = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_DECRYPT_ATTACHMENT",
      ThreadDetailsFragment::class.java
    )

    private const val REQUEST_CODE_FIX_MISSING_PASSPHRASE_BEFORE_PROCESS_MESSAGE = 1000
    private const val REQUEST_CODE_FIX_MISSING_PASSPHRASE_BEFORE_PREVIEW_ATTACHMENT = 1001
    private const val REQUEST_CODE_FIX_MISSING_PASSPHRASE_BEFORE_DOWNLOAD_ATTACHMENT = 1002
    private const val REQUEST_CODE_DELETE_MESSAGE_DIALOG = 1003
    private const val REQUEST_CODE_SAVE_ATTACHMENT = 1004
    private const val REQUEST_CODE_PREVIEW_ATTACHMENT = 1005
    private const val REQUEST_CODE_DECRYPT_ATTACHMENT = 1006
    private const val REQUEST_CODE_SHOW_WARNING_DIALOG_FOR_DOWNLOADING_DANGEROUS_FILE = 1007
    private const val REQUEST_CODE_FIX_MISSING_PASSPHRASE_BEFORE_EDITING_DRAFT = 1008
    private const val REQUEST_CODE_SHOW_TWO_WAY_DIALOG_FOR_DELETING_DRAFT = 1009
    private const val REQUEST_CODE_PROCESS_WHOLE_MESSAGE = 1010
    private const val REQUEST_CODE_PROCESS_ATTACHMENT_DOWNLOAD = 1011
    private const val REQUEST_CODE_PROCESS_ATTACHMENT_PREVIEW = 1012
    private const val REQUEST_CODE_FIX_MISSING_PASSPHRASE_AFTER_PROCESS_MESSAGE = 1013

    private const val CONTENT_MAX_ALLOWED_LENGTH = 50000
  }
}