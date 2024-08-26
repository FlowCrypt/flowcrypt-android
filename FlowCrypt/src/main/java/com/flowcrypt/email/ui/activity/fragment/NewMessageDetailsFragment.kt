/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.FragmentNewMessageDetailsBinding
import com.flowcrypt.email.extensions.androidx.fragment.app.launchAndRepeatWithViewLifecycle
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

/**
 * @author Denys Bondarenko
 */
class NewMessageDetailsFragment : BaseFragment<FragmentNewMessageDetailsBinding>(),
  ProgressBehaviour {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentNewMessageDetailsBinding.inflate(inflater, container, false)

  override val progressView: View?
    get() = binding?.progress?.root
  override val contentView: View?
    get() = binding?.content
  override val statusView: View?
    get() = binding?.status?.root

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
        updateReplyButtons(it.lastOrNull()?.hasPgp == true)
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

  private fun updateReplyButtons(usePgpMode: Boolean) {
    if (binding?.layoutReplyButtons != null) {
      val imageViewReply = binding?.layoutReplyButtons?.replyButton
      val imageViewReplyAll = binding?.layoutReplyButtons?.replyAllButton
      val imageViewFwd = binding?.layoutReplyButtons?.forwardButton

      val buttonsColorId: Int

      if (usePgpMode) {
        buttonsColorId = R.color.colorPrimary
        imageViewReply?.setText(R.string.reply_encrypted)
        imageViewReplyAll?.setText(R.string.reply_all_encrypted)
        imageViewFwd?.setText(R.string.forward_encrypted)
      } else {
        buttonsColorId = R.color.red
        imageViewReply?.setText(R.string.reply)
        imageViewReplyAll?.setText(R.string.reply_all)
        imageViewFwd?.setText(R.string.forward)
      }

      val colorStateList =
        ColorStateList.valueOf(ContextCompat.getColor(requireContext(), buttonsColorId))

      imageViewReply?.iconTint = colorStateList
      imageViewReplyAll?.iconTint = colorStateList
      imageViewFwd?.iconTint = colorStateList

      /*binding?.layoutReplyButtons?.layoutReplyButton?.setOnClickListener(this)
      binding?.layoutReplyButtons?.layoutFwdButton?.setOnClickListener(this)
      binding?.layoutReplyButtons?.layoutReplyAllButton?.setOnClickListener(this)*/
    }
  }
}