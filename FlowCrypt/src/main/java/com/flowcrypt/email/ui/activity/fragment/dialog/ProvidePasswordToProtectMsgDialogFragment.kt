/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.FragmentProvidePasswordToProtectMsgBinding
import com.flowcrypt.email.extensions.hideKeyboard
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.PasswordStrengthViewModel
import com.flowcrypt.email.ui.activity.fragment.base.CheckPassphraseBehaviour

/**
 * @author Denis Bondarenko
 * Date: 01.08.2017
 * Time: 10:04
 * E-mail: DenBond7@gmail.com
 */
class ProvidePasswordToProtectMsgDialogFragment : BaseDialogFragment(), CheckPassphraseBehaviour {
  private var binding: FragmentProvidePasswordToProtectMsgBinding? = null
  private val args by navArgs<ProvidePasswordToProtectMsgDialogFragmentArgs>()
  override val currentContext: Context?
    get() = context
  override val passwordStrengthViewModel: PasswordStrengthViewModel by viewModels()
  override val buttonUsePassphrase: Button?
    get() = binding?.btSetPassphrase
  override val progressBarPassphraseQuality: ProgressBar?
    get() = binding?.pBarPassphraseQuality
  override val textViewPassphraseQuality: TextView?
    get() = binding?.tVPassphraseQuality

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initPasswordStrengthViewModel(this)
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val builder = AlertDialog.Builder(activity)
    binding = FragmentProvidePasswordToProtectMsgBinding.inflate(
      LayoutInflater.from(requireContext()),
      if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null,
      false
    )

    binding?.eTPassphrase?.addTextChangedListener { editable ->
      val passphrase = editable.toString()
      passwordStrengthViewModel.check(passphrase)
      if (TextUtils.isEmpty(editable)) {
        binding?.tVPassphraseQuality?.setText(R.string.passphrase_must_be_non_empty)
      }
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
    toast("Not yet implemented")
  }
}
