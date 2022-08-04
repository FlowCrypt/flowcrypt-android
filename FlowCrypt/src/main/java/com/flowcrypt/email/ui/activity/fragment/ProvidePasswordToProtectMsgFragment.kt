/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CheckedTextView
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.FragmentProvidePasswordToProtectMsgBinding
import com.flowcrypt.email.extensions.hideKeyboard
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.WebPortalPasswordStrengthViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.util.UIUtil

class ProvidePasswordToProtectMsgFragment :
  BaseFragment<FragmentProvidePasswordToProtectMsgBinding>() {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentProvidePasswordToProtectMsgBinding.inflate(inflater, container, false)

  private val args by navArgs<ProvidePasswordToProtectMsgFragmentArgs>()
  private val webPortalPasswordStrengthViewModel: WebPortalPasswordStrengthViewModel by viewModels()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    initWebPortalPasswordStrengthViewModel()
  }

  private fun initViews() {
    binding?.tVLostPassphraseWarning?.text =
      getString(R.string.warning_about_password_usage, getString(R.string.app_name))

    binding?.eTPassphrase?.addTextChangedListener { editable ->
      webPortalPasswordStrengthViewModel.check(editable.toString())
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

    binding?.btSetPassword?.setOnClickListener {
      checkAndMoveOn()
    }

    binding?.eTPassphrase?.setText(args.defaultPassword)

    binding?.pBarPassphraseQuality?.progressDrawable?.colorFilter =
      BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
        UIUtil.getColor(requireContext(), R.color.colorPrimary),
        BlendModeCompat.SRC_IN
      )
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

  private fun initWebPortalPasswordStrengthViewModel() {
    lifecycleScope.launchWhenStarted {
      webPortalPasswordStrengthViewModel.pwdStrengthResultStateFlow.collect {
        updateStrengthViews(it)
      }
    }
  }

  private fun updateStrengthViews(list: List<WebPortalPasswordStrengthViewModel.RequirementItem>) {
    list.forEach {
      when (it.requirement) {
        WebPortalPasswordStrengthViewModel.Requirement.MIN_LENGTH -> {
          updateRequirementItem(binding?.checkedTVMinLength, it)
        }
        WebPortalPasswordStrengthViewModel.Requirement.ONE_LOWERCASE -> {
          updateRequirementItem(binding?.checkedTVOneLowercase, it)
        }
        WebPortalPasswordStrengthViewModel.Requirement.ONE_UPPERCASE -> {
          updateRequirementItem(binding?.checkedTVOneUppercase, it)
        }
        WebPortalPasswordStrengthViewModel.Requirement.ONE_NUMBER -> {
          updateRequirementItem(binding?.checkedTVOneNumber, it)
        }
        WebPortalPasswordStrengthViewModel.Requirement.ONE_SPECIAL_CHARACTER -> {
          updateRequirementItem(binding?.checkedTVOneSpecialCharacter, it)
        }
      }
    }

    binding?.btSetPassword?.apply {
      isEnabled = list.all { it.isMatching }
      background?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
        UIUtil.getColor(
          requireContext(), if (isEnabled) R.color.colorPrimary else R.color.silver
        ), BlendModeCompat.MODULATE
      )
    }

    binding?.pBarPassphraseQuality?.apply {
      val progress = list.count { it.isMatching }
      this.progress = progress
    }
  }

  private fun updateRequirementItem(
    view: CheckedTextView?,
    it: WebPortalPasswordStrengthViewModel.RequirementItem
  ) {
    view?.apply {
      isChecked = it.isMatching
      setTextColor(
        UIUtil.getColor(
          requireContext(),
          if (it.isMatching) R.color.colorPrimary else R.color.orange
        )
      )
    }
  }

  companion object {
    const val REQUEST_KEY_PASSWORD = "REQUEST_KEY_PASSWORD"
    const val KEY_PASSWORD = "KEY_PASSWORD"
  }
}
