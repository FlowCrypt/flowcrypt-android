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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.databinding.FragmentThreadDetailsBinding
import com.flowcrypt.email.extensions.androidx.fragment.app.launchAndRepeatWithViewLifecycle
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.extensions.androidx.fragment.app.supportActionBar
import com.flowcrypt.email.extensions.androidx.fragment.app.toast
import com.flowcrypt.email.extensions.exceptionMsg
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
import com.flowcrypt.email.jetpack.viewmodel.ThreadDetailsViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.ui.adapter.MessagesInThreadListAdapter
import com.flowcrypt.email.ui.adapter.recyclerview.itemdecoration.SkipFirstAndLastDividerItemDecoration

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
    object : MessagesInThreadListAdapter.OnMessageClickListener {
      override fun onMessageClick(messageEntity: MessageEntity) {
        toast(messageEntity.uidAsHEX)
      }
    })

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    updateActionBar()

    initViews()
    setupThreadDetailsViewModel()
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
}