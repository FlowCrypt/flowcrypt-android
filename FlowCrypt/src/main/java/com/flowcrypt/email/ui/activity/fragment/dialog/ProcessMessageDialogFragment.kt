/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
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
import com.flowcrypt.email.extensions.launchAndRepeatWithLifecycle
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
import com.flowcrypt.email.jetpack.viewmodel.ProcessMessageViewModel

/**
 * @author Denys Bondarenko
 */
class ProcessMessageDialogFragment : BaseDialogFragment() {
  private var binding: FragmentProcessMessageBinding? = null
  private val args by navArgs<ProcessMessageDialogFragmentArgs>()
  private val processMessageViewModel: ProcessMessageViewModel by viewModels {
    object : CustomAndroidViewModelFactory(requireActivity().application) {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProcessMessageViewModel(
          args.messageIdInLocalDatabase,
          requireActivity().application
        ) as T
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isCancelable = false
    collectProcessMessageStateFlow()
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
    }

    return builder.create()
  }

  private fun collectProcessMessageStateFlow() {
    launchAndRepeatWithLifecycle {
      processMessageViewModel.incomingMessageInfoFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
            binding?.progressBar?.visible()
          }

          Result.Status.SUCCESS, Result.Status.EXCEPTION -> {
            navController?.navigateUp()
            setFragmentResult(
              args.requestKey,
              bundleOf(
                KEY_REQUEST_KEY to args.requestKey,
                KEY_RESULT to it,
              )
            )
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