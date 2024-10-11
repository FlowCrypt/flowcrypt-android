/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.databinding.FragmentThreadDetailsBinding
import com.flowcrypt.email.extensions.android.os.getParcelableViaExt
import com.flowcrypt.email.extensions.androidx.fragment.app.launchAndRepeatWithViewLifecycle
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.extensions.androidx.fragment.app.setFragmentResultListener
import com.flowcrypt.email.extensions.androidx.fragment.app.setFragmentResultListenerForTwoWayDialog
import com.flowcrypt.email.extensions.androidx.fragment.app.showInfoDialog
import com.flowcrypt.email.extensions.androidx.fragment.app.showNeedPassphraseDialog
import com.flowcrypt.email.extensions.androidx.fragment.app.showTwoWayDialog
import com.flowcrypt.email.extensions.androidx.fragment.app.supportActionBar
import com.flowcrypt.email.extensions.androidx.fragment.app.toast
import com.flowcrypt.email.extensions.exceptionMsg
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
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
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.providers.EmbeddedAttachmentsProvider
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.ui.activity.CreateMessageActivity
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.ui.activity.fragment.dialog.ChangeGmailLabelsDialogFragmentArgs
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
        changeGmailLabels()
      }

      override fun onAttachmentDownloadClick(
        attachmentInfo: AttachmentInfo,
        message: MessagesInThreadListAdapter.Message
      ) {
        toast("Not yet implemented + ${attachmentInfo.name}")
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

        /*args.localFolder.searchQuery?.let {
          menuItemArchiveMsg?.isVisible = false
          menuItemDeleteMsg?.isVisible = false
          menuActionMoveToInbox?.isVisible = false
          menuActionMarkUnread?.isVisible = false
          menuActionMoveToSpam?.isVisible = false
          menuActionMarkAsNotSpam?.isVisible = false
          menuActionChangeLabels?.isVisible = false
        }*/
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
      if (message != null) {
        threadDetailsViewModel.onMessageChanged(
          message.copy(
            type = MessagesInThreadListAdapter.Type.MESSAGE_EXPANDED
          )
        )
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
            val messageId = it.getLong(REQUEST_KEY_MESSAGE_ID, Long.MIN_VALUE)
            if (messageId > 0) {
              (messagesInThreadListAdapter.currentList.firstOrNull { item ->
                item.id == messageId
              } as? MessagesInThreadListAdapter.Message)?.let {
                processMessageClick(it)
              }
            }
          }
        }

        REQUEST_CODE_FIX_MISSING_PASSPHRASE_BEFORE_DOWNLOAD_ATTACHMENT -> {
          bundle.getBundle(FixNeedPassphraseIssueDialogFragment.KEY_REQUEST_INCOMING_BUNDLE)?.let {
            val messageId = it.getLong(KEY_EXTRA_MESSAGE_ID, Long.MIN_VALUE)
            val attachmentId = it.getString(KEY_EXTRA_ATTACHMENT_ID)
            if (messageId > 0 && !attachmentId.isNullOrEmpty()) {
              (messagesInThreadListAdapter.currentList.firstOrNull { item ->
                item.id == messageId
              } as? MessagesInThreadListAdapter.Message)?.let { message ->
                val attachmentInfo = message.attachments.firstOrNull { attachmentInfo ->
                  attachmentInfo.uniqueStringId == attachmentId
                } ?: return@setFragmentResultListener
                handleAttachmentPreviewClick(attachmentInfo, message)
              }
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

      bundle.getBundle(DownloadAttachmentDialogFragment.KEY_INCOMING_BUNDLE)?.getLong(
        KEY_EXTRA_MESSAGE_ID
      ).let { messageId ->
        messagesInThreadListAdapter.currentList.firstOrNull {
          it.id == messageId
        }?.let { message ->
          (message as? MessagesInThreadListAdapter.Message)?.let {
            threadDetailsViewModel.onMessageChanged(
              message.copy(
                attachments = message.attachments.toMutableList().apply {
                  replaceAll {
                    if (attachmentInfo != null && it.id == attachmentInfo.id) {
                      attachmentInfo
                    } else {
                      it
                    }
                  }
                }
              )
            )
          }
        }
      }

      when (requestCode) {
        REQUEST_CODE_PREVIEW_ATTACHMENT -> {
          attachmentInfo?.let {
            previewAttachment(
              attachmentInfo = it,
              useContentApp = account?.isHandlingAttachmentRestricted() == true
            )
          }
        }

        REQUEST_CODE_SAVE_ATTACHMENT -> {
          //downloadAttachment()
        }
      }
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

  private fun processMessageClick(message: MessagesInThreadListAdapter.Message) {
    if (message.messageEntity.hasPgp == true) {
      val keysStorage = KeysStorageImpl.getInstance(requireContext())
      val fingerprints = keysStorage.getFingerprintsWithEmptyPassphrase()
      if (fingerprints.isNotEmpty()) {
        showNeedPassphraseDialog(
          requestKey = REQUEST_KEY_FIX_MISSING_PASSPHRASE + args.messageEntityId.toString(),
          requestCode = REQUEST_CODE_FIX_MISSING_PASSPHRASE_BEFORE_PROCESS_MESSAGE,
          fingerprints = fingerprints,
          bundle = Bundle().apply { putLong(REQUEST_KEY_MESSAGE_ID, message.id) }
        )
        return
      }
    }

    if (message.type == MessagesInThreadListAdapter.Type.MESSAGE_COLLAPSED) {
      if (message.incomingMessageInfo == null) {
        navController?.navigate(
          object : NavDirections {
            override val actionId = R.id.process_message_dialog_graph
            override val arguments = ProcessMessageDialogFragmentArgs(
              requestKey = REQUEST_KEY_PROCESS_MESSAGE + args.messageEntityId.toString(),
              message = message
            ).toBundle()
          }
        )
      } else {
        threadDetailsViewModel.onMessageClicked(message)
      }
    } else {
      threadDetailsViewModel.onMessageClicked(message)
    }
  }

  private fun changeGmailLabels() {
    if (AccountEntity.ACCOUNT_TYPE_GOOGLE == account?.accountType) {
      navController?.navigate(
        object : NavDirections {
          override val actionId = R.id.change_gmail_labels_for_single_message_dialog_graph
          override val arguments = ChangeGmailLabelsDialogFragmentArgs(
            requestKey = REQUEST_KEY_CHANGE_LABELS + args.messageEntityId.toString(),
            messageEntityIds = arrayOf(args.messageEntityId).toLongArray()
          ).toBundle()
        }
      )
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
      previewAttachment(
        attachmentInfo = attachmentInfo,
        useContentApp = account?.isHandlingAttachmentRestricted() == true
      )
    } else {
      if (attachmentInfo.isPossiblyEncrypted) {
        val keysStorage = KeysStorageImpl.getInstance(requireContext())
        val fingerprints = keysStorage.getFingerprintsWithEmptyPassphrase()
        if (fingerprints.isNotEmpty()) {
          showNeedPassphraseDialog(
            requestKey = REQUEST_KEY_FIX_MISSING_PASSPHRASE + args.messageEntityId.toString(),
            requestCode = REQUEST_CODE_FIX_MISSING_PASSPHRASE_BEFORE_DOWNLOAD_ATTACHMENT,
            fingerprints = fingerprints,
            bundle = Bundle().apply {
              putLong(KEY_EXTRA_MESSAGE_ID, message.id)
              putString(KEY_EXTRA_ATTACHMENT_ID, attachmentInfo.uniqueStringId)
            }
          )
          return
        }
      }

      navController?.navigate(
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
      )
    }
  }

  private fun previewAttachment(
    attachmentInfo: AttachmentInfo,
    useContentApp: Boolean = false
  ) {
    val intent = if (attachmentInfo.uri != null) {
      GeneralUtil.genViewAttachmentIntent(requireNotNull(attachmentInfo.uri), attachmentInfo)
    } else {
      toast(getString(R.string.preview_is_not_available))
      return
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

    private val REQUEST_KEY_MESSAGE_ID = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_MESSAGE",
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

    private val REQUEST_KEY_PREPARE_DOWNLOADED_ATTACHMENTS_FOR_FORWARDING =
      GeneralUtil.generateUniqueExtraKey(
        "REQUEST_KEY_PREPARE_DOWNLOADED_ATTACHMENTS_FOR_FORWARDING",
        ThreadDetailsFragment::class.java
      )

    private val REQUEST_KEY_TWO_WAY_DIALOG_BASE = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_TWO_WAY_DIALOG_BASE",
      ThreadDetailsFragment::class.java
    )

    private const val REQUEST_CODE_FIX_MISSING_PASSPHRASE_BEFORE_PROCESS_MESSAGE = 1
    private const val REQUEST_CODE_FIX_MISSING_PASSPHRASE_BEFORE_DOWNLOAD_ATTACHMENT = 2
    private const val REQUEST_CODE_DELETE_MESSAGE_DIALOG = 3
    private const val REQUEST_CODE_SAVE_ATTACHMENT = 1000
    private const val REQUEST_CODE_PREVIEW_ATTACHMENT = 1001
    private const val REQUEST_CODE_DECRYPT_ATTACHMENT = 1002
    private const val CONTENT_MAX_ALLOWED_LENGTH = 50000
  }
}