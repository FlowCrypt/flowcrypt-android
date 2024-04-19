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
import com.flowcrypt.email.databinding.FragmentCommonProcessingBinding
import com.flowcrypt.email.extensions.exceptionMsg
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.launchAndRepeatWithLifecycle
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
import com.flowcrypt.email.jetpack.viewmodel.EditPrivateKeyViewModel
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.util.GeneralUtil
import org.pgpainless.key.util.UserId

/**
 * @author Denys Bondarenko
 */
class AddNewUserIdToPrivateKeyDialogFragment : BaseDialogFragment(), ProgressBehaviour {
  private var binding: FragmentCommonProcessingBinding? = null
  private val args by navArgs<AddNewUserIdToPrivateKeyDialogFragmentArgs>()
  private val editPrivateKeyViewModel: EditPrivateKeyViewModel by viewModels {
    object : CustomAndroidViewModelFactory(requireActivity().application) {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EditPrivateKeyViewModel(args.fingerprint, requireActivity().application) as T
      }
    }
  }

  override val progressView: View?
    get() = binding?.layoutProgress?.root
  override val contentView: View?
    get() = null
  override val statusView: View?
    get() = binding?.layoutStatus?.root

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isCancelable = false
    collectEditPrivateKeyStateFlow()
    modifyKey()
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    binding = FragmentCommonProcessingBinding.inflate(
      LayoutInflater.from(requireContext()),
      if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null,
      false
    )

    val builder = AlertDialog.Builder(requireContext()).apply {
      setView(binding?.root)
      //just need to describe a button
      setPositiveButton(R.string.retry) { _, _ -> }

      setNegativeButton(R.string.cancel) { _, _ ->
        navController?.navigateUp()
      }
    }

    return builder.create()
  }

  override fun onStart() {
    super.onStart()
    getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
      setOnClickListener {
        modifyKey()
        gone()
      }
      //we hide the button at the start up.
      gone()
    }
  }

  private fun modifyKey() {
    editPrivateKeyViewModel.addUserId(UserId.onlyEmail(args.userId))
  }

  private fun collectEditPrivateKeyStateFlow() {
    launchAndRepeatWithLifecycle {
      editPrivateKeyViewModel.editPrivateKeyStateFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
            showProgress(getString(R.string.processing_please_wait))
          }

          Result.Status.SUCCESS -> {
            navController?.navigateUp()
            if (it.data == true) {
              setFragmentResult(
                args.requestKey,
                bundleOf(KEY_RESULT to args.fingerprint)
              )
            }
          }

          Result.Status.EXCEPTION, Result.Status.ERROR -> {
            showStatus(msg = it.exceptionMsg)
            getButton(AlertDialog.BUTTON_POSITIVE)?.apply { visible() }
          }

          else -> {}
        }
      }
    }
  }

  companion object {
    val KEY_RESULT = GeneralUtil.generateUniqueExtraKey(
      "KEY_RESULT", AddNewUserIdToPrivateKeyDialogFragment::class.java
    )
  }
}