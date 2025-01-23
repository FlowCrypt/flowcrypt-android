/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
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
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentDownloadAttachmentBinding
import com.flowcrypt.email.extensions.androidx.fragment.app.countingIdlingResource
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.invisible
import com.flowcrypt.email.extensions.kotlin.getPossibleAndroidMimeType
import com.flowcrypt.email.extensions.launchAndRepeatWithLifecycle
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
import com.flowcrypt.email.jetpack.viewmodel.DownloadAttachmentViewModel
import org.apache.commons.io.FilenameUtils

/**
 * @author Denys Bondarenko
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

    binding?.textViewAttachmentName?.text = args.attachmentInfo.getSafeName()
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
    launchAndRepeatWithLifecycle {
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
            val finalName = if (args.attachmentInfo.isPossiblyEncrypted) {
              /*
               We need to drop the last extension section in the final result
               as we return a decrypted file here.
               */
              FilenameUtils.getBaseName(args.attachmentInfo.name)
            } else {
              args.attachmentInfo.name
            }

            it.data?.let { byteArray ->
              setFragmentResult(
                args.requestKey,
                bundleOf(
                  KEY_ATTACHMENT to args.attachmentInfo.copy(
                    rawData = byteArray,
                    name = finalName,
                    type = if (args.attachmentInfo.isPossiblyEncrypted) {
                      /*
                       We need to specify a new MIME type based on the new attachment name
                       after decryption. It will help the Android system use a right app
                       to open this attachment.
                      */
                      finalName.getPossibleAndroidMimeType() ?: args.attachmentInfo.type
                    } else {
                      args.attachmentInfo.type
                    },
                  ),
                  KEY_REQUEST_CODE to args.requestCode,
                  KEY_INCOMING_BUNDLE to args.bundle
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
    const val KEY_ATTACHMENT = "KEY_ATTACHMENT"
    const val KEY_REQUEST_CODE = "KEY_REQUEST_CODE"
    const val KEY_INCOMING_BUNDLE = "KEY_INCOMING_BUNDLE"
  }
}
