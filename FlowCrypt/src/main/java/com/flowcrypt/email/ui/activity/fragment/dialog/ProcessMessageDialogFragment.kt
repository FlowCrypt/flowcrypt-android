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
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentProcessMessageBinding
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.extensions.exceptionMsg
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.launchAndRepeatWithLifecycle
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
import com.flowcrypt.email.jetpack.viewmodel.ProcessMessageViewModel
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour

/**
 * @author Denys Bondarenko
 */
class ProcessMessageDialogFragment : BaseDialogFragment(), ProgressBehaviour {
  private var binding: FragmentProcessMessageBinding? = null
  private val args by navArgs<ProcessMessageDialogFragmentArgs>()
  private val processMessageViewModel: ProcessMessageViewModel by viewModels {
    object : CustomAndroidViewModelFactory(requireActivity().application) {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProcessMessageViewModel(
          args.message,
          requireActivity().application
        ) as T
      }
    }
  }

  override val progressView: View?
    get() = binding?.layoutProgress?.root
  override val contentView: View?
    get() = null
  override val statusView: View?
    get() = binding?.status?.root

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isCancelable = false
    collectProcessMessageStateFlow()
  }

  override fun onStart() {
    super.onStart()
    getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
      setOnClickListener {
        processMessageViewModel.retry()
        gone()
      }
      //we hide the button at the start up.
      gone()
    }
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    binding = FragmentProcessMessageBinding.inflate(
      LayoutInflater.from(requireContext()),
      if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null,
      false
    )

    val builder = AlertDialog.Builder(requireContext()).apply {
      setView(binding?.root)

      setNegativeButton(R.string.cancel) { _, _ ->
        navController?.navigateUp()
      }

      setPositiveButton(R.string.retry) { _, _ -> }
    }

    return builder.create()
  }

  private fun collectProcessMessageStateFlow() {
    launchAndRepeatWithLifecycle {
      processMessageViewModel.incomingMessageInfoFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
            showProgress()
          }

          Result.Status.SUCCESS -> {
            navController?.navigateUp()
            setFragmentResult(
              args.requestKey,
              bundleOf(
                KEY_REQUEST_KEY to args.requestKey,
                KEY_RESULT to it.data,
              )
            )
          }

          Result.Status.EXCEPTION -> {
            showStatus(msg = it.exceptionMsg)
            (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_POSITIVE)?.visible()
          }

          else -> {}
        }
      }
    }
  }

  companion object {
    const val KEY_RESULT = "KEY_RESULT"
    const val KEY_REQUEST_KEY = "KEY_REQUEST_CODE"
  }
}