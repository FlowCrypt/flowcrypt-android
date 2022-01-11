/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.Constants
import com.flowcrypt.email.NavGraphDirections
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.FragmentCheckPassphraseStrengthBinding
import com.flowcrypt.email.extensions.hideKeyboard
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.jetpack.viewmodel.PasswordStrengthViewModel
import com.flowcrypt.email.security.pgp.PgpPwd
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.CheckPassphraseBehaviour
import com.flowcrypt.email.ui.notifications.SystemNotificationManager
import com.google.android.material.snackbar.Snackbar
import org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets

/**
 * This fragment does a reliability check of a provided passphrase.
 *
 * @author Denis Bondarenko
 * Date: 05.08.2018
 * Time: 20:15
 * E-mail: DenBond7@gmail.com
 */
class CheckPassphraseStrengthFragment : BaseFragment(), CheckPassphraseBehaviour {
  private val args by navArgs<CheckPassphraseStrengthFragmentArgs>()
  private var binding: FragmentCheckPassphraseStrengthBinding? = null
  override val currentContext: Context?
    get() = context
  override val passwordStrengthViewModel: PasswordStrengthViewModel by viewModels()
  override val buttonUsePassphrase: Button?
    get() = binding?.btSetPassphrase
  override val progressBarPassphraseQuality: ProgressBar?
    get() = binding?.pBarPassphraseQuality
  override val textViewPassphraseQuality: TextView?
    get() = binding?.tVPassphraseQuality
  private val pwdStrengthResult: PgpPwd.PwdStrengthResult?
    get() = passwordStrengthViewModel.pwdStrengthResultStateFlow.value.data

  override val contentResourceId: Int = R.layout.fragment_check_passphrase_strength

  override fun onAttach(context: Context) {
    super.onAttach(context)
    SystemNotificationManager(context)
      .cancel(SystemNotificationManager.NOTIFICATION_ID_PASSPHRASE_TOO_WEAK)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initPasswordStrengthViewModel(this)
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View? {
    binding = FragmentCheckPassphraseStrengthBinding.inflate(inflater, container, false)
    return binding?.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    supportActionBar?.title = getString(R.string.security)
    initViews()
  }

  private fun initViews() {
    binding?.tVTitle?.text = args.title
    binding?.tVLostPassphraseWarning?.text = args.lostPassphraseTitle
    binding?.iBShowPasswordHint?.setOnClickListener {
      navController?.navigate(
        NavGraphDirections.actionGlobalInfoDialogFragment(
          requestCode = 0,
          dialogTitle = "",
          dialogMsg = IOUtils.toString(
            requireContext().assets.open("html/pass_phrase_hint.htm"),
            StandardCharsets.UTF_8
          ),
          useWebViewToRender = true
        )
      )
    }

    binding?.eTPassphrase?.addTextChangedListener { editable ->
       passwordStrengthViewModel.check(editable.toString())
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
  }

  private fun checkAndMoveOn() {
    if (binding?.eTPassphrase?.text?.isEmpty() == true) {
      showInfoSnackbar(
        view = binding?.root,
        msgText = getString(R.string.passphrase_must_be_non_empty),
        duration = Snackbar.LENGTH_LONG
      )
    } else {
      snackBar?.dismiss()
      pwdStrengthResult?.word?.let { word ->
        when (word.word) {
          Constants.PASSWORD_QUALITY_WEAK, Constants.PASSWORD_QUALITY_POOR -> {
            navController?.navigate(
              NavGraphDirections.actionGlobalInfoDialogFragment(
                dialogTitle = "",
                dialogMsg = getString(R.string.select_stronger_pass_phrase)
              )
            )
          }

          else -> {
            navController?.navigate(
              CheckPassphraseStrengthFragmentDirections
                .actionCheckPassphraseStrengthFragmentToRecheckProvidedPassphraseFragment(
                  popBackStackIdIfSuccess = args.popBackStackIdIfSuccess,
                  title = args.title,
                  passphrase = binding?.eTPassphrase?.text.toString()
                )
            )
          }
        }
      }
    }
  }
}
