/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.FragmentProvidePasswordToProtectMsgBinding
import com.flowcrypt.email.extensions.hideKeyboard
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.toast

class ProvidePasswordToProtectMsgDialogFragment : BaseDialogFragment() {
  private var binding: FragmentProvidePasswordToProtectMsgBinding? = null
  private val args by navArgs<ProvidePasswordToProtectMsgDialogFragmentArgs>()

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val builder = AlertDialog.Builder(activity)
    binding = FragmentProvidePasswordToProtectMsgBinding.inflate(
      LayoutInflater.from(requireContext()),
      if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null,
      false
    )

    binding?.eTPassphrase?.addTextChangedListener { editable ->
      //check typed password will be added a bit later(soon)
    }

    binding?.eTPassphrase?.setOnEditorActionListener { v, actionId, _ ->
      return@setOnEditorActionListener when (actionId) {
        EditorInfo.IME_ACTION_DONE -> {
          checkAndMoveOn()
          v.hideKeyboard()
          true
        }
        else -> false
      }
    }

    binding?.btSetPassphrase?.setOnClickListener {
      checkAndMoveOn()
    }

    binding?.eTPassphrase?.setText(args.defaultPassword)

    builder.setView(binding?.root)
    builder.setTitle(null)
    return builder.create()
  }

  private fun checkAndMoveOn() {
    if (binding?.eTPassphrase?.text?.isEmpty() == true) {
      toast(getString(R.string.password_cannot_be_empty))
    } else {
      navController?.navigateUp()
      setFragmentResult(
        REQUEST_KEY_PASSWORD,
        bundleOf(KEY_PASSWORD to (binding?.eTPassphrase?.text ?: ""))
      )
    }
  }

  companion object {
    const val REQUEST_KEY_PASSWORD = "REQUEST_KEY_PASSWORD"
    const val KEY_PASSWORD = "KEY_PASSWORD"
  }
}
