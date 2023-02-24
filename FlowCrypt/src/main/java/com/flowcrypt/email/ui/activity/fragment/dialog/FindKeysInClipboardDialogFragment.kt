/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.app.Dialog
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentFindKeysInClipboardBinding
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.jetpack.viewmodel.ParseKeysViewModel

/**
 * @author Denys Bondarenko
 */
class FindKeysInClipboardDialogFragment : BaseDialogFragment() {
  private var binding: FragmentFindKeysInClipboardBinding? = null
  private val args by navArgs<FindKeysInClipboardDialogFragmentArgs>()
  private val parseKeysViewModel: ParseKeysViewModel by viewModels()
  private var clipboardManager: ClipboardManager? = null
  private var clipboardText: String? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setupParseKeysViewModel()
    clipboardManager = context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    checkClipboard()
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    binding = FragmentFindKeysInClipboardBinding.inflate(
      LayoutInflater.from(requireContext()),
      if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null,
      false
    )

    binding?.btRetry?.setOnClickListener {
      checkClipboard()
    }

    val builder = AlertDialog.Builder(requireContext()).apply {
      setView(binding?.root)
      setNegativeButton(R.string.cancel) { _, _ ->
        navController?.navigateUp()
      }
    }

    if (clipboardManager?.hasPrimaryClip() == false
      || clipboardManager?.primaryClip?.getItemAt(0)?.text.isNullOrEmpty()
    ) {
      showEmptyClipboardHint()
    }

    return builder.create()
  }

  private fun showEmptyClipboardHint() {
    showStatusMsgWithRetryButton(getEmptyClipboardMsg())
  }

  private fun showStatusMsgWithRetryButton(msg: String) {
    binding?.tVStatusMessage?.text = msg
    binding?.pBLoading?.gone()
    binding?.tVStatusMessage?.visible()
    binding?.btRetry?.visible()
  }

  private fun getEmptyClipboardMsg() = getString(
    R.string.hint_clipboard_is_empty, if (args.isPrivateKeyMode)
      getString(R.string.private_)
    else
      getString(R.string.public_), getString(R.string.app_name)
  )

  private fun checkClipboard() {
    if (clipboardManager?.hasPrimaryClip() == true) {
      val item = clipboardManager?.primaryClip?.getItemAt(0) ?: return
      clipboardText = item.text.toString()
      val privateKeyFromClipboard = clipboardText?.toByteArray()
      if (privateKeyFromClipboard?.isEmpty() == true) {
        showEmptyClipboardHint()
      } else {
        parseKeysViewModel.parseKeys(privateKeyFromClipboard)
      }
    } else {
      showEmptyClipboardHint()
    }
  }

  private fun setupParseKeysViewModel() {
    lifecycleScope.launchWhenStarted {
      parseKeysViewModel.pgpKeyDetailsListStateFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource?.incrementSafely(this@FindKeysInClipboardDialogFragment)
            binding?.pBLoading?.visible()
            binding?.btRetry?.gone()
            binding?.tVStatusMessage?.text = getString(R.string.loading)
          }

          Result.Status.SUCCESS -> {
            if (it.data?.isEmpty() == true) {
              val msg = getString(
                R.string.file_has_wrong_pgp_structure,
                if (args.isPrivateKeyMode) getString(R.string.private_) else getString(R.string.public_)
              )
              showStatusMsgWithRetryButton(msg)
            } else {
              navController?.navigateUp()
              setFragmentResult(
                REQUEST_KEY_CLIPBOARD_RESULT,
                bundleOf(KEY_CLIPBOARD_TEXT to clipboardText)
              )
            }
            countingIdlingResource?.decrementSafely(this@FindKeysInClipboardDialogFragment)
          }

          Result.Status.EXCEPTION, Result.Status.ERROR -> {
            binding?.pBLoading?.gone()
            binding?.btRetry?.visible()

            val exception = it.exception ?: return@collect
            binding?.tVStatusMessage?.text = if (exception.message.isNullOrEmpty()) {
              exception.javaClass.simpleName
            } else exception.message

            countingIdlingResource?.decrementSafely(this@FindKeysInClipboardDialogFragment)
          }
          else -> {
          }
        }
      }
    }
  }

  companion object {
    const val REQUEST_KEY_CLIPBOARD_RESULT = "REQUEST_KEY_CLIPBOARD_RESULT"
    const val KEY_CLIPBOARD_TEXT = "KEY_CLIPBOARD_TEXT"
  }
}
