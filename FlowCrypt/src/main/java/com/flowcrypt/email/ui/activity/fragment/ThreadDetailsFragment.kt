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
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentThreadDetailsBinding
import com.flowcrypt.email.extensions.android.os.getSerializableViaExt
import com.flowcrypt.email.extensions.androidx.fragment.app.launchAndRepeatWithViewLifecycle
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.extensions.androidx.fragment.app.setFragmentResultListener
import com.flowcrypt.email.extensions.androidx.fragment.app.showInfoDialog
import com.flowcrypt.email.extensions.androidx.fragment.app.supportActionBar
import com.flowcrypt.email.extensions.androidx.fragment.app.toast
import com.flowcrypt.email.extensions.exceptionMsg
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
import com.flowcrypt.email.jetpack.viewmodel.ThreadDetailsViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.ui.activity.fragment.dialog.ProcessMessageDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.ProcessMessageDialogFragmentArgs
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
        threadDetailsViewModel.onMessageClicked(message)
        if (message.type == MessagesInThreadListAdapter.Type.MESSAGE_COLLAPSED
          && message.incomingMessageInfo == null
        ) {
          navController?.navigate(
            object : NavDirections {
              override val actionId = R.id.process_message_dialog_graph
              override val arguments = ProcessMessageDialogFragmentArgs(
                requestKey = REQUEST_KEY_PROCESS_MESSAGE + args.messageEntityId.toString(),
                messageIdInLocalDatabase = message.messageEntity.id ?: -1L
              ).toBundle()
            }
          )
        }
      }

      override fun onHeadersDetailsClick(
        position: Int,
        message: MessagesInThreadListAdapter.Message
      ) {
        threadDetailsViewModel.onHeadersDetailsClick(message)
      }
    })

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    updateActionBar()

    initViews()
    setupThreadDetailsViewModel()
    subscribeToProcessMessageDialogFragment()
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

  private fun subscribeToProcessMessageDialogFragment() {
    setFragmentResultListener(
      REQUEST_KEY_PROCESS_MESSAGE + args.messageEntityId.toString(),
      true
    ) { _, bundle ->
      val result: Result<IncomingMessageInfo>? = bundle.getSerializableViaExt(
        ProcessMessageDialogFragment.KEY_RESULT
      ) as? Result<IncomingMessageInfo>

      result?.let {
        when (result.status) {
          Result.Status.SUCCESS -> {
            it.data?.let { it1 -> threadDetailsViewModel.onMessageProcessed(it1) }
            toast("loaded!")
          }

          Result.Status.EXCEPTION -> {
            showInfoDialog(
              dialogTitle = "",
              dialogMsg = it.exceptionMsg,
              isCancelable = true
            )
          }

          else -> {
            toast(getString(R.string.unknown_error))
          }
        }
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

  companion object {
    private val REQUEST_KEY_PROCESS_MESSAGE = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_PROCESS_MESSAGE",
      ThreadDetailsFragment::class.java
    )
  }
}