/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.navigation.NavDirections
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.databinding.FragmentThreadDetailsBinding
import com.flowcrypt.email.extensions.android.os.getParcelableViaExt
import com.flowcrypt.email.extensions.androidx.fragment.app.launchAndRepeatWithViewLifecycle
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.extensions.androidx.fragment.app.setFragmentResultListener
import com.flowcrypt.email.extensions.androidx.fragment.app.showNeedPassphraseDialog
import com.flowcrypt.email.extensions.androidx.fragment.app.supportActionBar
import com.flowcrypt.email.extensions.androidx.fragment.app.toast
import com.flowcrypt.email.extensions.exceptionMsg
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
import com.flowcrypt.email.jetpack.viewmodel.ThreadDetailsViewModel
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.ui.activity.fragment.dialog.ChangeGmailLabelsDialogFragmentArgs
import com.flowcrypt.email.ui.activity.fragment.dialog.FixNeedPassphraseIssueDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.ProcessMessageDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.ProcessMessageDialogFragmentArgs
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
    get() = binding?.recyclerViewMessages
  override val statusView: View?
    get() = binding?.status?.root

  private var isActive: Boolean = false

  private val args by navArgs<ThreadDetailsFragmentArgs>()
  private val threadDetailsViewModel: ThreadDetailsViewModel by viewModels {
    object : CustomAndroidViewModelFactory(requireActivity().application) {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ThreadDetailsViewModel(
          args.messageEntityId, requireActivity().application
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
    })

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    updateActionBar()

    initViews()
    setupThreadDetailsViewModel()
    subscribeToProcessMessageDialogFragment()
    subscribeToFixNeedPassphraseIssueDialogFragment()
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

  private fun setupThreadDetailsViewModel() {
    launchAndRepeatWithViewLifecycle {
      threadDetailsViewModel.messagesInThreadFlow.collect {
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

  private fun subscribeToProcessMessageDialogFragment() {
    setFragmentResultListener(
      REQUEST_KEY_PROCESS_MESSAGE + args.messageEntityId.toString(),
      true
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
      REQUEST_KEY_FIX_MISSING_PASSPHRASE + args.messageEntityId.toString(),
      useSuperParentFragmentManagerIfPossible = true
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

        else -> toast(R.string.unknown_error)
      }
    }
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

  companion object {
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

    private const val REQUEST_CODE_FIX_MISSING_PASSPHRASE_BEFORE_PROCESS_MESSAGE = 1
  }
}