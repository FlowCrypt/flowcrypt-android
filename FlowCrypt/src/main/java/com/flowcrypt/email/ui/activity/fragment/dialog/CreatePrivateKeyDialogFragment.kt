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
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.databinding.FragmentCreatePrivateKeyDialogBinding
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.launchAndRepeatWithLifecycle
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.jetpack.viewmodel.CreatePrivateKeyViewModel
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.util.GeneralUtil

/**
 * @author Denys Bondarenko
 */
class CreatePrivateKeyDialogFragment : BaseDialogFragment() {
  private var binding: FragmentCreatePrivateKeyDialogBinding? = null
  private val args by navArgs<CreatePrivateKeyDialogFragmentArgs>()
  private val createPrivateKeyViewModel: CreatePrivateKeyViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setupCreatePrivateKeyViewModel()
    createPrivateKey()
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    binding = FragmentCreatePrivateKeyDialogBinding.inflate(
      LayoutInflater.from(requireContext()),
      if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null,
      false
    )

    binding?.btRetry?.setOnClickListener {
      createPrivateKey()
    }

    val builder = AlertDialog.Builder(requireContext()).apply {
      setView(binding?.root)
      setNegativeButton(R.string.cancel) { _, _ ->
        navController?.navigateUp()
      }
    }

    return builder.create()
  }

  private fun createPrivateKey() {
    createPrivateKeyViewModel.createPrivateKey(
      accountEntity = args.accountEntity,
      passphrase = args.passphrase,
      passphraseType = KeyEntity.PassphraseType.DATABASE
    )
  }

  private fun showStatusMsgWithRetryButton(msg: String?) {
    binding?.tVStatusMessage?.text = msg
    binding?.pBLoading?.gone()
    binding?.tVStatusMessage?.visible()
    binding?.btRetry?.visible()
  }

  private fun setupCreatePrivateKeyViewModel() {
    launchAndRepeatWithLifecycle {
      createPrivateKeyViewModel.createPrivateKeyStateFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource?.incrementSafely(this@CreatePrivateKeyDialogFragment)
            binding?.pBLoading?.visible()
            binding?.btRetry?.gone()
            binding?.tVStatusMessage?.text = getString(R.string.loading)
          }

          Result.Status.SUCCESS -> {
            val pgpKeyRingDetails: PgpKeyRingDetails? = it.data
            if (pgpKeyRingDetails == null) {
              handleException(NullPointerException("pgpKeyRingDetails == null"))
            } else {
              navController?.navigateUp()
              setFragmentResult(
                args.requestKey,
                bundleOf(KEY_CREATED_KEY to pgpKeyRingDetails)
              )
            }
            countingIdlingResource?.decrementSafely(this@CreatePrivateKeyDialogFragment)
          }

          Result.Status.EXCEPTION, Result.Status.ERROR -> {
            val exception = it.exception

            if (exception != null) {
              handleException(exception)
            } else {
              binding?.pBLoading?.gone()
              binding?.btRetry?.visible()
            }

            countingIdlingResource?.decrementSafely(this@CreatePrivateKeyDialogFragment)
          }

          else -> {
          }
        }
      }
    }
  }

  private fun handleException(exception: Throwable) {
    showStatusMsgWithRetryButton(
      if (exception.message.isNullOrEmpty()) {
        exception.javaClass.simpleName
      } else exception.message
    )
  }

  companion object {
    val KEY_CREATED_KEY = GeneralUtil.generateUniqueExtraKey(
      "KEY_PARSED_KEYS", CreatePrivateKeyDialogFragment::class.java
    )
  }
}
