/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
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
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentLookUpPubKeysBinding
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.launchAndRepeatWithLifecycle
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.jetpack.viewmodel.RecipientsViewModel

/**
 * @author Denys Bondarenko
 */
class LookUpPubKeysDialogFragment : BaseDialogFragment() {
  private var binding: FragmentLookUpPubKeysBinding? = null
  private val args by navArgs<LookUpPubKeysDialogFragmentArgs>()
  private val recipientsViewModel: RecipientsViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    collectLookUpPubKeysStateFlow()
    recipientsViewModel.getRawPublicKeysFromRemoteServers(args.email)
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    binding = FragmentLookUpPubKeysBinding.inflate(
      LayoutInflater.from(requireContext()),
      if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null,
      false
    )

    binding?.btnRetry?.setOnClickListener {
      recipientsViewModel.getRawPublicKeysFromRemoteServers(args.email)
    }

    val builder = AlertDialog.Builder(requireContext()).apply {
      setView(binding?.root)
      setNegativeButton(R.string.cancel) { _, _ ->
        navController?.navigateUp()
      }
    }

    return builder.create()
  }

  private fun collectLookUpPubKeysStateFlow() {
    launchAndRepeatWithLifecycle {
      recipientsViewModel.lookUpPubKeysStateFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource.incrementSafely(this@LookUpPubKeysDialogFragment)
            binding?.pBLoading?.visible()
            binding?.btnRetry?.gone()
            binding?.tVStatusMessage?.text = getString(R.string.loading)
          }

          Result.Status.SUCCESS -> {
            navController?.navigateUp()
            it.data?.pubkey.let { pubKeys ->
              setFragmentResult(
                REQUEST_KEY_PUB_KEYS,
                bundleOf(KEY_PUB_KEYS to pubKeys)
              )
            }
            countingIdlingResource.decrementSafely(this@LookUpPubKeysDialogFragment)
          }

          Result.Status.EXCEPTION, Result.Status.ERROR -> {
            binding?.pBLoading?.gone()
            binding?.btnRetry?.visible()

            val exception = it.exception ?: return@collect
            binding?.tVStatusMessage?.text = if (exception.message.isNullOrEmpty()) {
              exception.javaClass.simpleName
            } else exception.message

            countingIdlingResource.decrementSafely(this@LookUpPubKeysDialogFragment)
          }
          else -> {
          }
        }
      }
    }
  }

  companion object {
    const val REQUEST_KEY_PUB_KEYS = "REQUEST_KEY_PUB_KEYS"
    const val KEY_PUB_KEYS = "KEY_PUB_KEYS"
  }
}
