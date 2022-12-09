/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentDownloadAttachmentBinding
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.invisible
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
import com.flowcrypt.email.jetpack.viewmodel.DownloadAttachmentViewModel

/**
 * @author Denis Bondarenko
 *         Date: 2/21/22
 *         Time: 10:52 AM
 *         E-mail: DenBond7@gmail.com
 */
class DownloadAttachmentDialogFragment : BaseDialogFragment() {
  private var binding: FragmentDownloadAttachmentBinding? = null
  private val args by navArgs<DownloadAttachmentDialogFragmentArgs>()
  private val downloadAttachmentViewModel: DownloadAttachmentViewModel by viewModels {
    object : CustomAndroidViewModelFactory(requireActivity().application) {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DownloadAttachmentViewModel(args.attachmentInfo, requireActivity().application) as T
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isCancelable = false
    collectDownloadAttachmentStateFlow()
    downloadAttachmentViewModel.download()
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    binding = FragmentDownloadAttachmentBinding.inflate(
      LayoutInflater.from(requireContext()),
      if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null,
      false
    )

    binding?.textViewAttachmentName?.text = args.attachmentInfo.name
    binding?.progressBar?.isIndeterminate = true

    val builder = AlertDialog.Builder(requireContext()).apply {
      setView(binding?.root)
      setNegativeButton(R.string.cancel) { _, _ ->
        navController?.navigateUp()
      }
    }

    return builder.create()
  }

  private fun collectDownloadAttachmentStateFlow() {
    lifecycleScope.launchWhenStarted {
      downloadAttachmentViewModel.downloadAttachmentStateFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource?.incrementSafely(this@DownloadAttachmentDialogFragment)
            binding?.progressBar?.visible()
            if (it.progress != null) {
              binding?.progressBar?.isIndeterminate = false
              binding?.progressBar?.progress = it.progress.toInt()
              binding?.textViewLeftTime?.text = DateUtils.formatElapsedTime(
                (it.progressMsg?.toLongOrNull() ?: 0) / DateUtils.SECOND_IN_MILLIS
              )
            } else {
              binding?.progressBar?.isIndeterminate = true
              binding?.textViewStatus?.text = it.progressMsg
            }
          }

          Result.Status.SUCCESS -> {
            navController?.navigateUp()
            it.data?.let { byteArray ->
              setFragmentResult(
                REQUEST_KEY_ATTACHMENT_DATA,
                bundleOf(
                  KEY_ATTACHMENT to args.attachmentInfo,
                  KEY_ATTACHMENT_DATA to byteArray,
                  KEY_REQUEST_CODE to args.requestCode
                )
              )
            }
            countingIdlingResource?.decrementSafely(this@DownloadAttachmentDialogFragment)
          }

          Result.Status.EXCEPTION -> {
            binding?.progressBar?.invisible()
            val exception = it.exception ?: return@collect
            val errorMsg = if (exception.message.isNullOrEmpty()) {
              exception.javaClass.simpleName
            } else exception.message
            binding?.textViewStatus?.text =
              context?.getString(R.string.error_occurred_during_downloading_att, errorMsg)
            countingIdlingResource?.decrementSafely(this@DownloadAttachmentDialogFragment)
          }

          else -> {
          }
        }
      }
    }
  }

  companion object {
    const val REQUEST_KEY_ATTACHMENT_DATA = "REQUEST_KEY_PUB_KEYS"
    const val KEY_ATTACHMENT = "KEY_ATTACHMENT"
    const val KEY_ATTACHMENT_DATA = "KEY_ATTACHMENT_DATA"
    const val KEY_REQUEST_CODE = "KEY_REQUEST_CODE"
  }
}
