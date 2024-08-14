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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.FragmentNewMessageDetailsBinding
import com.flowcrypt.email.extensions.androidx.fragment.app.launchAndRepeatWithViewLifecycle
import com.flowcrypt.email.extensions.androidx.fragment.app.toast
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
import com.flowcrypt.email.jetpack.viewmodel.ThreadDetailsViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.adapter.GmailApiLabelsListAdapter
import com.flowcrypt.email.ui.adapter.MessagesInThreadListAdapter
import com.flowcrypt.email.ui.adapter.recyclerview.itemdecoration.MarginItemDecoration
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent

/**
 * @author Denys Bondarenko
 */
class NewMessageDetailsFragment : BaseFragment<FragmentNewMessageDetailsBinding>() {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentNewMessageDetailsBinding.inflate(inflater, container, false)

  private val args by navArgs<NewMessageDetailsFragmentArgs>()
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

  private val messagesInThreadListAdapter = MessagesInThreadListAdapter()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    setupLabelsViewModel()
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
        binding?.recyclerViewMessages?.visible()
      }
    }

    launchAndRepeatWithViewLifecycle {
      threadDetailsViewModel.messageGmailApiLabelsFlow.collect {
        gmailApiLabelsListAdapter.submitList(it)
      }
    }
  }

  private fun setupLabelsViewModel() {

  }

  private fun initViews() {
    binding?.recyclerViewMessages?.apply {
      val linearLayoutManager = LinearLayoutManager(context)
      layoutManager = linearLayoutManager
      addItemDecoration(
        DividerItemDecoration(context, linearLayoutManager.orientation)
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