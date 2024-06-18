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
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.FragmentDialogUpdateSignatureBinding
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.util.GeneralUtil

/**
 * @author Denys Bondarenko
 */
class UpdateSignatureDialogFragment : BaseDialogFragment() {
  private var binding: FragmentDialogUpdateSignatureBinding? = null
  private val args by navArgs<UpdateSignatureDialogFragmentArgs>()

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    binding = FragmentDialogUpdateSignatureBinding.inflate(
      LayoutInflater.from(requireContext()),
      if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null,
      false
    )

    initViews()

    val builder = AlertDialog.Builder(requireContext()).apply {
      setView(binding?.root)
      setPositiveButton(R.string.update) { _, _ ->
        sendResult()
      }
      setNegativeButton(R.string.cancel) { _, _ ->
        navController?.navigateUp()
      }
    }

    return builder.create()
  }

  private fun initViews() {
    binding?.editTextSignature?.setText(args.signature)
  }

  private fun sendResult() {
    navController?.navigateUp()
    setFragmentResult(
      args.requestKey,
      bundleOf(KEY_SIGNATURE to binding?.editTextSignature?.text?.toString())
    )
  }

  companion object {
    val KEY_SIGNATURE = GeneralUtil.generateUniqueExtraKey(
      "KEY_SIGNATURE",
      UpdateSignatureDialogFragment::class.java
    )
  }
}