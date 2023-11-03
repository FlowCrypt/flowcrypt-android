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
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.FragmentCreatePrivateKeySecondBinding
import com.flowcrypt.email.extensions.android.os.getParcelableViaExt
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.CreatePrivateKeyDialogFragment
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.google.android.material.snackbar.Snackbar

/**
 * @author Denys Bondarenko
 */
class CreatePrivateKeySecondFragment : BaseFragment<FragmentCreatePrivateKeySecondBinding>() {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentCreatePrivateKeySecondBinding.inflate(inflater, container, false)

  private val args by navArgs<CreatePrivateKeySecondFragmentArgs>()

  override val isDisplayHomeAsUpEnabled = false
  override val isToolbarVisible: Boolean = false

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    subscribeToCreatePrivateKey()
  }

  private fun initViews() {
    binding?.textViewSecondPasswordCheckTitle?.text =
      getString(R.string.set_up_flow_crypt, getString(R.string.app_name))
    binding?.editTextKeyPasswordSecond?.setOnEditorActionListener { v, actionId, _ ->
      return@setOnEditorActionListener when (actionId) {
        EditorInfo.IME_ACTION_DONE -> {
          checkAndMoveOn()
          UIUtil.hideSoftInput(requireContext(), v)
          true
        }
        else -> false
      }
    }
    binding?.buttonConfirmPassPhrases?.setOnClickListener {
      checkAndMoveOn()
    }
    binding?.buttonUseAnotherPassPhrase?.setOnClickListener {
      navController?.navigateUp()
    }
  }

  private fun checkAndMoveOn() {
    if (binding?.editTextKeyPasswordSecond?.text?.isEmpty() == true) {
      showInfoSnackbar(
        view = binding?.root,
        msgText = getString(R.string.passphrase_must_be_non_empty),
        duration = Snackbar.LENGTH_LONG
      )
    } else {
      snackBar?.dismiss()
      if ((binding?.editTextKeyPasswordSecond?.text?.chars()?.toArray()
          ?: intArrayOf()).contentEquals(
          args.passphrase
        )
      ) {
        navController?.navigate(
          CreatePrivateKeySecondFragmentDirections
            .actionCreatePrivateKeySecondFragmentToCreatePrivateKeyDialogFragment(
              requestKey = REQUEST_KEY_CREATE_PRIVATE_KEY,
              accountEntity = args.accountEntity,
              passphrase = requireNotNull(binding?.editTextKeyPasswordSecond?.text?.toString())
            )
        )
      } else {
        showInfoSnackbar(
          view = binding?.root,
          msgText = getString(R.string.pass_phrases_do_not_match),
          duration = Snackbar.LENGTH_LONG
        )
      }
    }
  }

  private fun subscribeToCreatePrivateKey() {
    setFragmentResultListener(REQUEST_KEY_CREATE_PRIVATE_KEY) { _, bundle ->
      val pgpKeyRingDetails =
        bundle.getParcelableViaExt<PgpKeyRingDetails>(CreatePrivateKeyDialogFragment.KEY_CREATED_KEY)
      navController?.navigateUp()
      setFragmentResult(
        REQUEST_KEY_CREATE_KEY,
        bundleOf(KEY_CREATED_KEY to pgpKeyRingDetails)
      )
    }
  }

  companion object {
    private val REQUEST_KEY_CREATE_PRIVATE_KEY = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_CREATE_PRIVATE_KEY",
      CreatePrivateKeySecondFragment::class.java
    )

    val REQUEST_KEY_CREATE_KEY = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_PARSED_KEYS", CreatePrivateKeySecondFragment::class.java
    )

    val KEY_CREATED_KEY = GeneralUtil.generateUniqueExtraKey(
      "KEY_PARSED_KEYS", CreatePrivateKeySecondFragment::class.java
    )
  }
}
