/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
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
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentSendFeedbackDialogBinding
import com.flowcrypt.email.extensions.androidx.fragment.app.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.launchAndRepeatWithLifecycle
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.jetpack.viewmodel.SendFeedbackViewModel
import com.flowcrypt.email.util.exception.ApiException

/**
 * @author Denys Bondarenko
 */
class SendFeedbackDialogFragment : BaseDialogFragment() {
  private var binding: FragmentSendFeedbackDialogBinding? = null
  private val args by navArgs<SendFeedbackDialogFragmentArgs>()
  private val sendFeedbackViewModel: SendFeedbackViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setupSendFeedbackViewModel()
    sendFeedback()
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    binding = FragmentSendFeedbackDialogBinding.inflate(
      LayoutInflater.from(requireContext()),
      if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null,
      false
    )

    binding?.btRetry?.setOnClickListener {
      sendFeedback()
    }

    val builder = AlertDialog.Builder(requireContext()).apply {
      setView(binding?.root)
      setNegativeButton(R.string.cancel) { _, _ ->
        navController?.navigateUp()
      }
    }

    return builder.create()
  }

  private fun sendFeedback() {
    sendFeedbackViewModel.postFeedback(args.accountEntity, args.feedbackMsg, args.screenshot)
  }

  private fun setupSendFeedbackViewModel() {
    launchAndRepeatWithLifecycle {
      sendFeedbackViewModel.postFeedbackStateFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource?.incrementSafely(this@SendFeedbackDialogFragment)
            binding?.pBLoading?.visible()
            binding?.btRetry?.gone()
            binding?.tVStatusMessage?.textAlignment = View.TEXT_ALIGNMENT_CENTER
            binding?.tVStatusMessage?.text = it.progressMsg
          }

          Result.Status.SUCCESS -> {
            navController?.navigateUp()
            setFragmentResult(
              args.requestKey,
              bundleOf(KEY_RESULT to (it.data?.isSent == true))
            )
            countingIdlingResource?.decrementSafely(this@SendFeedbackDialogFragment)
          }

          Result.Status.EXCEPTION, Result.Status.ERROR -> {
            binding?.pBLoading?.gone()
            binding?.btRetry?.visible()

            val exception = it.exception ?: ApiException(it.apiError)
            val errorMsg = if (exception.message.isNullOrEmpty()) {
              exception.javaClass.simpleName
            } else exception.message

            binding?.tVStatusMessage?.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            binding?.tVStatusMessage?.text = getString(
              R.string.send_feedback_failed_hint,
              getString(R.string.support_email),
              errorMsg
            )

            countingIdlingResource?.decrementSafely(this@SendFeedbackDialogFragment)
          }

          else -> {}
        }
      }
    }
  }

  companion object {
    const val KEY_RESULT = "KEY_RESULT"
  }
}
