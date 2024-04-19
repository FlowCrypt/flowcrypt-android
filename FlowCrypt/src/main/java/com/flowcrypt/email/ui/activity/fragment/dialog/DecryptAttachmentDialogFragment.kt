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
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentDecryptAttachmentBinding
import com.flowcrypt.email.extensions.exceptionMsg
import com.flowcrypt.email.extensions.invisible
import com.flowcrypt.email.extensions.launchAndRepeatWithLifecycle
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
import com.flowcrypt.email.jetpack.viewmodel.DecryptAttachmentViewModel
import org.apache.commons.io.FilenameUtils

/**
 * @author Denys Bondarenko
 */
class DecryptAttachmentDialogFragment : BaseDialogFragment() {
  private var binding: FragmentDecryptAttachmentBinding? = null
  private val args by navArgs<DecryptAttachmentDialogFragmentArgs>()
  private val decryptAttachmentViewModel: DecryptAttachmentViewModel by viewModels {
    object : CustomAndroidViewModelFactory(requireActivity().application) {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DecryptAttachmentViewModel(args.attachmentInfo, requireActivity().application) as T
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isCancelable = false
    collectDecryptAttachmentStateFlow()
    decryptAttachmentViewModel.decrypt()
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    binding = FragmentDecryptAttachmentBinding.inflate(
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

  private fun collectDecryptAttachmentStateFlow() {
    launchAndRepeatWithLifecycle {
      decryptAttachmentViewModel.decryptAttachmentStateFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
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
              val newFileName = FilenameUtils.getBaseName(args.attachmentInfo.name)
              setFragmentResult(
                args.requestKey,
                bundleOf(
                  KEY_ATTACHMENT to args.attachmentInfo.copy(
                    uri = null,
                    type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                      FilenameUtils.getExtension(newFileName).lowercase()
                    ) ?: args.attachmentInfo.type,
                    rawData = byteArray,
                    name = newFileName
                  ),
                  KEY_REQUEST_CODE to args.requestCode
                )
              )
            }
          }

          Result.Status.EXCEPTION -> {
            binding?.progressBar?.invisible()
            binding?.textViewStatus?.text =
              context?.getString(R.string.error_occurred_during_decrypting_att, it.exceptionMsg)
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
  }
}
