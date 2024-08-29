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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.databinding.FragmentNewMessageDetailsBinding
import com.flowcrypt.email.extensions.androidx.fragment.app.launchAndRepeatWithViewLifecycle
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.extensions.androidx.fragment.app.toast
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
import com.flowcrypt.email.jetpack.viewmodel.ThreadDetailsViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.ui.adapter.GmailApiLabelsListAdapter
import com.flowcrypt.email.ui.adapter.MessagesInThreadListAdapter
import com.flowcrypt.email.ui.adapter.recyclerview.itemdecoration.MarginItemDecoration
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.divider.MaterialDividerItemDecoration
import kotlinx.coroutines.launch

/**
 * @author Denys Bondarenko
 */
class GmailThreadFragment : BaseFragment<FragmentNewMessageDetailsBinding>(),
  ProgressBehaviour {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentNewMessageDetailsBinding.inflate(inflater, container, false)

  override val progressView: View?
    get() = binding?.progress?.root
  override val contentView: View?
    get() = binding?.content
  override val statusView: View?
    get() = binding?.status?.root

  private val args by navArgs<GmailThreadFragmentArgs>()
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

  private val gmailApiLabelsListAdapter = GmailApiLabelsListAdapter(
    object : GmailApiLabelsListAdapter.OnLabelClickListener {
      override fun onLabelClick(label: GmailApiLabelsListAdapter.Label) {
        toast("fix me")
      }
    })

  private val messagesInThreadListAdapter =
    MessagesInThreadListAdapter(object : MessagesInThreadListAdapter.OnMessageClickListener {
      override fun onMessageClick(messageEntity: MessageEntity) {
        lifecycleScope.launch {
          FlowCryptRoomDatabase.getDatabase(requireContext()).msgDao()
            .getMsgSuspend(messageEntity.account, messageEntity.folder, messageEntity.uid)?.let {
              navController?.navigate(
                GmailThreadFragmentDirections.actionGmailThreadFragmentToMessageDetailsFragment(
                  messageEntity = it,
                  localFolder = LocalFolder(
                    messageEntity.account,
                    GmailApiHelper.LABEL_INBOX
                  )//fix me
                )
              )
            }
        }
      }
    })

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    setupThreadDetailsViewModel()
  }

  private fun setupThreadDetailsViewModel() {
    launchAndRepeatWithViewLifecycle {
      threadDetailsViewModel.messageFlow.collect {
        binding?.textViewSubject?.text = it?.subject
      }
    }

    launchAndRepeatWithViewLifecycle {
      threadDetailsViewModel.messagesInThreadFlow.collect {
        messagesInThreadListAdapter.submitList(it)
        showContent()
      }
    }

    launchAndRepeatWithViewLifecycle {
      threadDetailsViewModel.messageGmailApiLabelsFlow.collect {
        gmailApiLabelsListAdapter.submitList(it)
      }
    }
  }

  private fun initViews() {
    binding?.recyclerViewMessages?.apply {
      val linearLayoutManager = LinearLayoutManager(context)
      layoutManager = linearLayoutManager
      addItemDecoration(
        MaterialDividerItemDecoration(
          context, linearLayoutManager.orientation
        ).apply {
          isLastItemDecorated = false
        }
      )
      adapter = messagesInThreadListAdapter
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
  }
}