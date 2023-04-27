/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LongDef
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentParsePgpKeysFromSourceBinding
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.kotlin.toInputStream
import com.flowcrypt.email.extensions.launchAndRepeatWithLifecycle
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.jetpack.viewmodel.ParseKeysViewModel
import com.flowcrypt.email.ui.activity.fragment.dialog.ParsePgpKeysFromSourceDialogFragment.FilterType.Companion.ALL
import com.flowcrypt.email.ui.activity.fragment.dialog.ParsePgpKeysFromSourceDialogFragment.FilterType.Companion.PRIVATE_ONLY
import com.flowcrypt.email.ui.activity.fragment.dialog.ParsePgpKeysFromSourceDialogFragment.FilterType.Companion.PUBLIC_ONLY
import com.flowcrypt.email.util.GeneralUtil

/**
 * @author Denys Bondarenko
 */
class ParsePgpKeysFromSourceDialogFragment : BaseDialogFragment() {
  private var binding: FragmentParsePgpKeysFromSourceBinding? = null
  private val args by navArgs<ParsePgpKeysFromSourceDialogFragmentArgs>()
  private val parseKeysViewModel: ParseKeysViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setupParseKeysViewModel()
    parseKeysFromSource()
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    binding = FragmentParsePgpKeysFromSourceBinding.inflate(
      LayoutInflater.from(requireContext()),
      if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null,
      false
    )

    binding?.btRetry?.setOnClickListener {
      parseKeysFromSource()
    }

    val builder = AlertDialog.Builder(requireContext()).apply {
      setView(binding?.root)
      setNegativeButton(R.string.cancel) { _, _ ->
        navController?.navigateUp()
      }
    }

    return builder.create()
  }

  private fun parseKeysFromSource() {
    try {
      val inputStream = when {
        args.source?.isNotEmpty() == true -> args.source?.toInputStream()
        args.uri != null -> args.uri?.let { context?.contentResolver?.openInputStream(it) }
          ?: throw IllegalStateException(context?.getString(R.string.source_is_empty_or_not_available))
        else -> {
          throw IllegalStateException(context?.getString(R.string.source_is_empty_or_not_available))
        }
      }
        ?: throw IllegalStateException(context?.getString(R.string.source_is_empty_or_not_available))
      parseKeysViewModel.parseKeys(inputStream)
    } catch (e: Exception) {
      handleException(e)
    }
  }

  private fun showStatusMsgWithRetryButton(msg: String) {
    binding?.tVStatusMessage?.text = msg
    binding?.pBLoading?.gone()
    binding?.tVStatusMessage?.visible()
    binding?.btRetry?.visible()
  }

  private fun setupParseKeysViewModel() {
    launchAndRepeatWithLifecycle {
      parseKeysViewModel.pgpKeyDetailsListStateFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource.incrementSafely(this@ParsePgpKeysFromSourceDialogFragment)
            binding?.pBLoading?.visible()
            binding?.btRetry?.gone()
            binding?.tVStatusMessage?.text = getString(R.string.loading)
          }

          Result.Status.SUCCESS -> {
            if (it.data?.isEmpty() == true) {
              handleWrongSourceIssue()
            } else {
              val keys = when (args.filterType) {
                PRIVATE_ONLY -> it.data?.filter { pgpKeyDetails -> pgpKeyDetails.isPrivate }
                PUBLIC_ONLY -> it.data?.filter { pgpKeyDetails -> !pgpKeyDetails.isPrivate }
                else -> it.data
              }

              if (keys?.isEmpty() == true) {
                handleWrongSourceIssue()
              } else {
                navController?.navigateUp()
                setFragmentResult(
                  REQUEST_KEY_PARSED_KEYS,
                  bundleOf(KEY_PARSED_KEYS to keys)
                )
              }
            }
            countingIdlingResource.decrementSafely(this@ParsePgpKeysFromSourceDialogFragment)
          }

          Result.Status.EXCEPTION, Result.Status.ERROR -> {
            val exception = it.exception

            if (exception != null) {
              handleException(exception)
            } else {
              binding?.pBLoading?.gone()
              binding?.btRetry?.visible()
            }

            countingIdlingResource.decrementSafely(this@ParsePgpKeysFromSourceDialogFragment)
          }
          else -> {
          }
        }
      }
    }
  }

  private fun handleWrongSourceIssue() {
    val msg = getString(
      R.string.file_has_wrong_pgp_structure,
      when (args.filterType) {
        PRIVATE_ONLY -> getString(R.string.private_)
        PUBLIC_ONLY -> getString(R.string.public_)
        else -> ""
      }
    )
    showStatusMsgWithRetryButton(msg)
  }

  private fun handleException(exception: Throwable) {
    binding?.pBLoading?.gone()
    binding?.btRetry?.visible()

    binding?.tVStatusMessage?.text = if (exception.message.isNullOrEmpty()) {
      exception.javaClass.simpleName
    } else exception.message
  }

  @Retention(AnnotationRetention.SOURCE)
  @LongDef(ALL, PRIVATE_ONLY, PUBLIC_ONLY)
  annotation class FilterType {
    companion object {
      const val ALL = 0L
      const val PRIVATE_ONLY = 1L
      const val PUBLIC_ONLY = 2L
    }
  }

  companion object {
    val REQUEST_KEY_PARSED_KEYS = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_PARSED_KEYS", ParsePgpKeysFromSourceDialogFragment::class.java
    )

    val KEY_PARSED_KEYS = GeneralUtil.generateUniqueExtraKey(
      "KEY_PARSED_KEYS", ParsePgpKeysFromSourceDialogFragment::class.java
    )
  }
}
