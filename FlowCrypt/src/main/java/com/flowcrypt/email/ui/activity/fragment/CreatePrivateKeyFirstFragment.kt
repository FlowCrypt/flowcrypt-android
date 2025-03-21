/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.graphics.ColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.FragmentCreatePrivateKeyFirstBinding
import com.flowcrypt.email.extensions.android.os.getParcelableViaExt
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.ui.activity.fragment.base.BasePassphraseStrengthFragment
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil

/**
 * @author Denys Bondarenko
 */
class CreatePrivateKeyFirstFragment :
  BasePassphraseStrengthFragment<FragmentCreatePrivateKeyFirstBinding>() {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentCreatePrivateKeyFirstBinding.inflate(inflater, container, false)

  private val args by navArgs<CreatePrivateKeyFirstFragmentArgs>()

  override val isDisplayHomeAsUpEnabled = false
  override val isToolbarVisible: Boolean = false

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    initPasswordStrengthViewModel()
    subscribeToCreatePrivateKey()
  }

  override fun onButtonContinueColorChanged(colorRes: Int) {
    context?.let {
      binding?.buttonSetPassPhrase?.backgroundTintList =
        ContextCompat.getColorStateList(it, colorRes)
    }
  }

  override fun onPassphraseQualityChanged(progress: Int) {
    binding?.progressBarPasswordQuality?.progress = progress
  }

  override fun onPassphraseQualityProgressDrawableColorChanged(colorFilter: ColorFilter) {
    binding?.progressBarPasswordQuality?.progressDrawable?.colorFilter = colorFilter
  }

  override fun onPassphraseQualityTextChanged(charSequence: CharSequence) {
    binding?.textViewPasswordQualityInfo?.text = charSequence
  }

  override fun onContinue() {
    navController?.navigate(
      CreatePrivateKeyFirstFragmentDirections
        .actionCreatePrivateKeyFirstFragmentToCreatePrivateKeySecondFragment(
          args.accountEntity, binding?.editTextKeyPassword?.text?.chars()?.toArray() ?: intArrayOf()
        )
    )
  }

  private fun initViews() {
    binding?.textViewFirstPasswordCheckTitle?.text =
      getString(R.string.set_up_flow_crypt, getString(R.string.app_name))
    binding?.imageButtonShowPasswordHint?.setOnClickListener { showPassphraseHint() }

    binding?.editTextKeyPassword?.addTextChangedListener { editable ->
      val passphrase = editable.toString()
      passwordStrengthViewModel.check(passphrase)
      if (editable?.isEmpty() == true) {
        binding?.textViewPasswordQualityInfo?.setText(R.string.passphrase_must_be_non_empty)
      }
    }

    binding?.editTextKeyPassword?.setOnEditorActionListener { v, actionId, _ ->
      return@setOnEditorActionListener when (actionId) {
        EditorInfo.IME_ACTION_DONE -> {
          checkAndMoveOn(binding?.editTextKeyPassword?.text, binding?.root)
          UIUtil.hideSoftInput(requireContext(), v)
          true
        }

        else -> false
      }
    }

    binding?.buttonSetPassPhrase?.setOnClickListener {
      checkAndMoveOn(binding?.editTextKeyPassword?.text, binding?.root)
    }
  }

  private fun subscribeToCreatePrivateKey() {
    setFragmentResultListener(CreatePrivateKeySecondFragment.REQUEST_KEY_CREATE_KEY) { _, bundle ->
      val pgpKeyRingDetails =
        bundle.getParcelableViaExt<PgpKeyRingDetails>(CreatePrivateKeySecondFragment.KEY_CREATED_KEY)
      navController?.navigateUp()
      setFragmentResult(
        args.requestKey,
        bundleOf(KEY_CREATED_KEY to pgpKeyRingDetails, KEY_ACCOUNT to args.accountEntity)
      )
    }
  }

  companion object {
    val KEY_CREATED_KEY = GeneralUtil.generateUniqueExtraKey(
      "KEY_PARSED_KEYS", CreatePrivateKeyFirstFragment::class.java
    )

    val KEY_ACCOUNT = GeneralUtil.generateUniqueExtraKey(
      "KEY_ACCOUNT", CreatePrivateKeyFirstFragment::class.java
    )
  }
}
