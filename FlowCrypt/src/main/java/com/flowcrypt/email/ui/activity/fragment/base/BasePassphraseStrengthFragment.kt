/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base

import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Typeface
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.viewModels
import androidx.viewbinding.ViewBinding
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.launchAndRepeatWithViewLifecycle
import com.flowcrypt.email.extensions.showInfoDialog
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.PasswordStrengthViewModel
import com.flowcrypt.email.security.pgp.PgpPwd
import com.flowcrypt.email.util.exception.IllegalTextForStrengthMeasuringException
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets

/**
 * @author Denys Bondarenko
 */
abstract class BasePassphraseStrengthFragment<T : ViewBinding> : BaseFragment<T>() {
  protected val passwordStrengthViewModel: PasswordStrengthViewModel by viewModels()
  protected var pwdStrengthResult: PgpPwd.PwdStrengthResult? = null

  abstract fun onButtonContinueColorChanged(@ColorRes colorRes: Int)
  abstract fun onPassphraseQualityChanged(progress: Int)
  abstract fun onPassphraseQualityProgressDrawableColorChanged(colorFilter: ColorFilter)
  abstract fun onPassphraseQualityTextChanged(charSequence: CharSequence)
  abstract fun onContinue()

  protected fun initPasswordStrengthViewModel() {
    launchAndRepeatWithViewLifecycle {
      passwordStrengthViewModel.pwdStrengthResultStateFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
            withContext(Dispatchers.Main) {
              countingIdlingResource?.incrementSafely(this@BasePassphraseStrengthFragment)
            }
          }

          Result.Status.SUCCESS -> {
            pwdStrengthResult = it.data
            updateStrengthViews()
            countingIdlingResource?.decrementSafely(this@BasePassphraseStrengthFragment)
          }

          Result.Status.EXCEPTION -> {
            if (it.exception !is IllegalTextForStrengthMeasuringException) {
              toast(
                it.exception?.message ?: it.exception?.javaClass?.simpleName
                ?: getString(R.string.unknown_error), Toast.LENGTH_LONG
              )
            }
            countingIdlingResource?.decrementSafely(this@BasePassphraseStrengthFragment)
          }

          else -> {
          }
        }
      }
    }
  }

  protected fun showPassphraseHint() {
    showInfoDialog(
      dialogTitle = "",
      dialogMsg = IOUtils.toString(
        requireContext().assets.open("html/pass_phrase_hint.htm"),
        StandardCharsets.UTF_8
      ),
      useWebViewToRender = true
    )
  }

  protected fun checkAndMoveOn(passphrase: Editable?, rootView: View?) {
    if (passphrase?.isEmpty() == true) {
      showInfoSnackbar(
        view = rootView,
        msgText = getString(R.string.passphrase_must_be_non_empty),
        duration = Snackbar.LENGTH_LONG
      )
    } else {
      snackBar?.dismiss()
      pwdStrengthResult?.word?.let { word ->
        when (word.word) {
          Constants.PASSWORD_QUALITY_WEAK, Constants.PASSWORD_QUALITY_POOR -> {
            showInfoDialog(
              dialogTitle = "",
              dialogMsg = getString(R.string.select_stronger_pass_phrase)
            )
          }

          else -> {
            onContinue()
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
        onButtonContinueColorChanged(R.color.silver)
      }

      else -> {
        onButtonContinueColorChanged(R.color.colorPrimary)
      }
    }

    val color = parseColor()
    onPassphraseQualityChanged(word?.bar?.toInt() ?: 0)
    val colorFilter =
      BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_IN)
    colorFilter?.let { onPassphraseQualityProgressDrawableColorChanged(it) }

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

    val spannableStringBuilder = SpannableStringBuilder(qualityValueSpannable)
    spannableStringBuilder.append()
    spannableStringBuilder.append(" ")
    spannableStringBuilder.append(getString(R.string.password_quality_subtext))

    val timeSpannable = SpannableString(pwdStrengthResult?.time ?: "")
    timeSpannable.setSpan(
      ForegroundColorSpan(color), 0, timeSpannable.length,
      Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    spannableStringBuilder.append(" ")
    spannableStringBuilder.append(timeSpannable)
    spannableStringBuilder.append(")")

    onPassphraseQualityTextChanged(spannableStringBuilder)
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
    }?.uppercase()
  }
}
