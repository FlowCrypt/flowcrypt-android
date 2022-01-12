/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.Constants
import com.flowcrypt.email.NavGraphDirections
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentCheckPassphraseStrengthBinding
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.PasswordStrengthViewModel
import com.flowcrypt.email.security.pgp.PgpPwd
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.notifications.SystemNotificationManager
import com.flowcrypt.email.util.UIUtil
import com.google.android.material.snackbar.Snackbar
import org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * This fragment does a reliability check of a provided passphrase.
 *
 * @author Denis Bondarenko
 * Date: 05.08.2018
 * Time: 20:15
 * E-mail: DenBond7@gmail.com
 */
class CheckPassphraseStrengthFragment : BaseFragment() {
  private val args by navArgs<CheckPassphraseStrengthFragmentArgs>()
  private var binding: FragmentCheckPassphraseStrengthBinding? = null
  private val passwordStrengthViewModel: PasswordStrengthViewModel by viewModels()
  private var pwdStrengthResult: PgpPwd.PwdStrengthResult? = null

  override val contentResourceId: Int = R.layout.fragment_check_passphrase_strength

  override fun onAttach(context: Context) {
    super.onAttach(context)
    SystemNotificationManager(context)
      .cancel(SystemNotificationManager.NOTIFICATION_ID_PASSPHRASE_TOO_WEAK)
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
    initPasswordStrengthViewModel()
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
          UIUtil.hideSoftInput(requireContext(), v)
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

  private fun initPasswordStrengthViewModel() {
    lifecycleScope.launchWhenStarted {
      passwordStrengthViewModel.pwdStrengthResultStateFlow.collect {
        when (it.status) {
          Result.Status.SUCCESS -> {
            pwdStrengthResult = it.data
            updateStrengthViews()
          }

          Result.Status.EXCEPTION -> {
            toast(
              it.exception?.message ?: it.exception?.javaClass?.simpleName
              ?: getString(R.string.unknown_error), Toast.LENGTH_LONG
            )
          }
          else -> {
          }
        }
      }
    }
  }

  private fun updateStrengthViews() {
    if (pwdStrengthResult == null) {
      return
    }

    val word = pwdStrengthResult?.word

    when (word?.word) {
      Constants.PASSWORD_QUALITY_WEAK,
      Constants.PASSWORD_QUALITY_POOR -> {
        val colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
          ContextCompat.getColor(requireContext(), R.color.silver), BlendModeCompat.MODULATE
        )
        binding?.btSetPassphrase?.background?.colorFilter = colorFilter
      }

      else -> {
        val colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
          ContextCompat.getColor(requireContext(), R.color.colorPrimary), BlendModeCompat.MODULATE
        )
        binding?.btSetPassphrase?.background?.colorFilter = colorFilter
      }
    }

    val color = parseColor()

    binding?.pBarPassphraseQuality?.progress = word?.bar?.toInt() ?: 0
    val colorFilter =
      BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_IN)
    binding?.pBarPassphraseQuality?.progressDrawable?.colorFilter = colorFilter

    val qualityValue = getLocalizedPasswordQualityValue(word)

    val qualityValueSpannable = SpannableString(qualityValue)
    qualityValueSpannable.setSpan(
      ForegroundColorSpan(color), 0, qualityValueSpannable.length,
      Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    qualityValueSpannable.setSpan(
      StyleSpan(Typeface.BOLD), 0,
      qualityValueSpannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    binding?.tVPassphraseQuality?.text = qualityValueSpannable
    binding?.tVPassphraseQuality?.append(" ")
    binding?.tVPassphraseQuality?.append(getString(R.string.password_quality_subtext))

    val timeSpannable = SpannableString(pwdStrengthResult?.time ?: "")
    timeSpannable.setSpan(
      ForegroundColorSpan(color), 0, timeSpannable.length,
      Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    binding?.tVPassphraseQuality?.append(" ")
    binding?.tVPassphraseQuality?.append(timeSpannable)
    binding?.tVPassphraseQuality?.append(")")
  }

  private fun parseColor(): Int {
    return try {
      Color.parseColor(pwdStrengthResult?.word?.color)
    } catch (e: IllegalArgumentException) {
      e.printStackTrace()

      when (pwdStrengthResult?.word?.color) {
        "orange" -> Color.parseColor("#FFA500")

        "darkorange" -> Color.parseColor("#FF8C00")

        "darkred" -> Color.parseColor("#8B0000")

        else -> Color.DKGRAY
      }
    }
  }

  private fun getLocalizedPasswordQualityValue(word: PgpPwd.Word?): String? {
    return when (word?.word) {
      Constants.PASSWORD_QUALITY_PERFECT -> getString(R.string.password_quality_perfect)
      Constants.PASSWORD_QUALITY_GREAT -> getString(R.string.password_quality_great)
      Constants.PASSWORD_QUALITY_GOOD -> getString(R.string.password_quality_good)
      Constants.PASSWORD_QUALITY_REASONABLE -> getString(R.string.password_quality_reasonable)
      Constants.PASSWORD_QUALITY_POOR -> getString(R.string.password_quality_poor)
      Constants.PASSWORD_QUALITY_WEAK -> getString(R.string.password_quality_weak)
      else -> word?.word
    }?.uppercase(Locale.getDefault())
  }
}
