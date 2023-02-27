/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.content.Context
import android.graphics.ColorFilter
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.FragmentCheckPassphraseStrengthBinding
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.ui.activity.fragment.base.BasePassphraseStrengthFragment
import com.flowcrypt.email.ui.notifications.SystemNotificationManager
import com.flowcrypt.email.util.UIUtil

/**
 * This fragment does a reliability check of a provided passphrase.
 *
 * @author Denys Bondarenko
 */
class CheckPassphraseStrengthFragment :
  BasePassphraseStrengthFragment<FragmentCheckPassphraseStrengthBinding>() {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentCheckPassphraseStrengthBinding.inflate(inflater, container, false)

  private val args by navArgs<CheckPassphraseStrengthFragmentArgs>()

  override val isToolbarVisible: Boolean = false

  override fun onAttach(context: Context) {
    super.onAttach(context)
    SystemNotificationManager(context)
      .cancel(SystemNotificationManager.NOTIFICATION_ID_PASSPHRASE_TOO_WEAK)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    initPasswordStrengthViewModel()
  }

  override fun onButtonContinueColorChanged(colorRes: Int) {
    context?.let {
      binding?.btSetPassphrase?.backgroundTintList = ContextCompat.getColorStateList(it, colorRes)
    }
  }

  override fun onPassphraseQualityChanged(progress: Int) {
    binding?.pBarPassphraseQuality?.progress = progress
  }

  override fun onPassphraseQualityProgressDrawableColorChanged(colorFilter: ColorFilter) {
    binding?.pBarPassphraseQuality?.progressDrawable?.colorFilter = colorFilter
  }

  override fun onPassphraseQualityTextChanged(charSequence: CharSequence) {
    binding?.tVPassphraseQuality?.text = charSequence
  }

  override fun onContinue() {
    navController?.navigate(
      CheckPassphraseStrengthFragmentDirections
        .actionCheckPassphraseStrengthFragmentToRecheckProvidedPassphraseFragment(
          popBackStackIdIfSuccess = args.popBackStackIdIfSuccess,
          title = args.title,
          passphrase = binding?.eTPassphrase?.text.toString()
        )
    )
  }

  private fun initViews() {
    binding?.tVTitle?.text = args.title
    binding?.tVLostPassphraseWarning?.text = args.lostPassphraseTitle
    binding?.iBShowPasswordHint?.setOnClickListener {
      showPassphraseHint()
    }

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
          checkAndMoveOn(binding?.eTPassphrase?.text, binding?.root)
          UIUtil.hideSoftInput(requireContext(), v)
          true
        }
        else -> false
      }
    }

    binding?.btSetPassphrase?.setOnClickListener {
      checkAndMoveOn(binding?.eTPassphrase?.text, binding?.root)
    }
  }
}
