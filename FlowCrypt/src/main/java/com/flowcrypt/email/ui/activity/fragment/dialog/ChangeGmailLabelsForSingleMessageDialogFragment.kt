/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentChangeGmailLabelsForSingleMessageBinding
import com.flowcrypt.email.extensions.exceptionMsg
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.launchAndRepeatWithLifecycle
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
import com.flowcrypt.email.jetpack.viewmodel.GmailLabelsViewModel
import com.flowcrypt.email.ui.activity.fragment.base.ListProgressBehaviour
import com.flowcrypt.email.ui.adapter.GmailApiLabelsWithChoiceListAdapter

/**
 * @author Denys Bondarenko
 */
class ChangeGmailLabelsForSingleMessageDialogFragment : BaseDialogFragment(),
  ListProgressBehaviour {
  private var binding: FragmentChangeGmailLabelsForSingleMessageBinding? = null
  private val args by navArgs<ChangeGmailLabelsForSingleMessageDialogFragmentArgs>()
  private val gmailLabelsViewModel: GmailLabelsViewModel by viewModels {
    object : CustomAndroidViewModelFactory(requireActivity().application) {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GmailLabelsViewModel(requireActivity().application, args.messageEntity) as T
      }
    }
  }
  private val gmailApiLabelsWithChoiceListAdapter = GmailApiLabelsWithChoiceListAdapter()

  override val emptyView: View?
    get() = null
  override val progressView: View?
    get() = binding?.layoutProgress?.root
  override val contentView: View?
    get() = binding?.groupContent
  override val statusView: View?
    get() = binding?.status?.root

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setupGmailLabelsViewModel()
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    binding = FragmentChangeGmailLabelsForSingleMessageBinding.inflate(
      LayoutInflater.from(requireContext()),
      if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null,
      false
    )

    binding?.recyclerViewLabels?.apply {
      layoutManager = LinearLayoutManager(context)
      adapter = gmailApiLabelsWithChoiceListAdapter
    }

    val builder = AlertDialog.Builder(requireContext()).apply {
      setView(binding?.root)

      setNegativeButton(R.string.cancel) { _, _ ->
        navController?.navigateUp()
      }

      setPositiveButton(R.string.change_labels) { _, _ -> }
    }

    return builder.create()
  }

  override fun onStart() {
    super.onStart()
    overridePositiveButtonDefaultBehavior()
  }

  private fun overridePositiveButtonDefaultBehavior() {
    (dialog as? AlertDialog)?.getButton(
      AlertDialog.BUTTON_POSITIVE
    )?.apply {
      setOnClickListener {
        val newLabels = gmailApiLabelsWithChoiceListAdapter.currentList
          .filter { it.isChecked }
          .map { it.id }
          .toSet()
        gmailLabelsViewModel.changeLabels(newLabels)
        this.gone()
      }
    }
  }

  private fun setupGmailLabelsViewModel() {
    launchAndRepeatWithLifecycle {
      gmailLabelsViewModel.labelsInfoFlow.collect {
        gmailApiLabelsWithChoiceListAdapter.submitList(it)
        if (it.isEmpty()) {
          showEmptyView()
        } else {
          showContent()
        }
      }
    }

    launchAndRepeatWithLifecycle {
      gmailLabelsViewModel.changeLabelsStateFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
            showProgress(progressMsg = getString(R.string.processing_please_wait))
          }

          Result.Status.SUCCESS -> {
            navController?.navigateUp()
          }

          Result.Status.EXCEPTION -> {
            showStatus(msg = it.exceptionMsg)
            (dialog as? AlertDialog)?.getButton(
              AlertDialog.BUTTON_POSITIVE
            )?.apply {
              setText(R.string.retry)
            }?.visible()
          }

          else -> {}
        }
      }
    }
  }
}