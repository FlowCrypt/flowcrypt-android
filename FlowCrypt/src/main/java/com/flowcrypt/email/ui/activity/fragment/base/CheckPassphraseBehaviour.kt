/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.activity.fragment.base

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.PasswordStrengthViewModel
import com.flowcrypt.email.security.pgp.PgpPwd

interface CheckPassphraseBehaviour {
  val currentContext: Context?
  val passwordStrengthViewModel: PasswordStrengthViewModel
  val buttonUsePassphrase: Button?
  val progressBarPassphraseQuality: ProgressBar?
  val textViewPassphraseQuality: TextView?

  fun initPasswordStrengthViewModel(lifecycleOwner: LifecycleOwner) {
    lifecycleOwner.lifecycleScope.launchWhenStarted {
      passwordStrengthViewModel.pwdStrengthResultStateFlow.collect {
        when (it.status) {
          Result.Status.SUCCESS -> {
            it.data?.let { pwdStrengthResult ->
              currentContext?.let { context -> updateStrengthViews(context, pwdStrengthResult) }
            }
          }

          Result.Status.EXCEPTION -> {
            currentContext?.let { context ->
              context.toast(
                it.exception?.message ?: it.exception?.javaClass?.simpleName
                ?: context.getString(R.string.unknown_error), Toast.LENGTH_LONG
              )
            }
          }
          else -> {
          }
        }
      }
    }
  }

  private fun updateStrengthViews(context: Context, pwdStrengthResult: PgpPwd.PwdStrengthResult) {
    val word = pwdStrengthResult.word
    when (word.word) {
      Constants.PASSWORD_QUALITY_WEAK,
      Constants.PASSWORD_QUALITY_POOR -> {
        val colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
          ContextCompat.getColor(context, R.color.silver), BlendModeCompat.MODULATE
        )
        buttonUsePassphrase?.background?.colorFilter = colorFilter
      }

      else -> {
        val colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
          ContextCompat.getColor(context, R.color.colorPrimary), BlendModeCompat.MODULATE
        )
        buttonUsePassphrase?.background?.colorFilter = colorFilter
      }
    }

    val color = parseColor(pwdStrengthResult)

    progressBarPassphraseQuality?.progress = word.bar.toInt()
    val colorFilter =
      BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_IN)
    progressBarPassphraseQuality?.progressDrawable?.colorFilter = colorFilter

    val qualityValue = getLocalizedPasswordQualityValue(context, word)

    val qualityValueSpannable = SpannableString(qualityValue)
    qualityValueSpannable.setSpan(
      ForegroundColorSpan(color), 0, qualityValueSpannable.length,
      Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    qualityValueSpannable.setSpan(
      StyleSpan(Typeface.BOLD), 0,
      qualityValueSpannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    textViewPassphraseQuality?.text = qualityValueSpannable
    textViewPassphraseQuality?.append(" ")
    textViewPassphraseQuality?.append(context.getString(R.string.password_quality_subtext))

    val timeSpannable = SpannableString(pwdStrengthResult.time)
    timeSpannable.setSpan(
      ForegroundColorSpan(color), 0, timeSpannable.length,
      Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    textViewPassphraseQuality?.append(" ")
    textViewPassphraseQuality?.append(timeSpannable)
    textViewPassphraseQuality?.append(")")
  }

  private fun parseColor(pwdStrengthResult: PgpPwd.PwdStrengthResult): Int {
    return try {
      Color.parseColor(pwdStrengthResult.word.color)
    } catch (e: IllegalArgumentException) {
      e.printStackTrace()

      when (pwdStrengthResult.word.color) {
        "orange" -> Color.parseColor("#FFA500")

        "darkorange" -> Color.parseColor("#FF8C00")

        "darkred" -> Color.parseColor("#8B0000")

        else -> Color.DKGRAY
      }
    }
  }

  private fun getLocalizedPasswordQualityValue(context: Context, word: PgpPwd.Word?): String? {
    return when (word?.word) {
      Constants.PASSWORD_QUALITY_PERFECT -> context.getString(R.string.password_quality_perfect)
      Constants.PASSWORD_QUALITY_GREAT -> context.getString(R.string.password_quality_great)
      Constants.PASSWORD_QUALITY_GOOD -> context.getString(R.string.password_quality_good)
      Constants.PASSWORD_QUALITY_REASONABLE -> context.getString(R.string.password_quality_reasonable)
      Constants.PASSWORD_QUALITY_POOR -> context.getString(R.string.password_quality_poor)
      Constants.PASSWORD_QUALITY_WEAK -> context.getString(R.string.password_quality_weak)
      else -> word?.word
    }?.uppercase()
  }
}
