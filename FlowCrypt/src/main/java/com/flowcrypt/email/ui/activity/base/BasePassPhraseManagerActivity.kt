/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.jetpack.viewmodel.PasswordStrengthViewModel
import com.flowcrypt.email.security.pgp.PgpPwd
import com.flowcrypt.email.ui.activity.fragment.dialog.InfoDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.WebViewInfoDialogFragment
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.exception.IllegalTextForStrengthMeasuringException
import com.google.android.material.snackbar.Snackbar
import org.apache.commons.io.IOUtils
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * @author Denis Bondarenko
 * Date: 04.08.2018
 * Time: 14:53
 * E-mail: DenBond7@gmail.com
 */
abstract class BasePassPhraseManagerActivity : BaseBackStackActivity(), View.OnClickListener {

  protected lateinit var layoutProgress: View
  protected lateinit var layoutContentView: View
  protected lateinit var btnSetPassPhrase: View
  protected lateinit var layoutSecondPasswordCheck: View
  protected lateinit var layoutFirstPasswordCheck: View
  protected lateinit var layoutSuccess: View
  protected lateinit var editTextKeyPassword: EditText
  protected lateinit var editTextKeyPasswordSecond: EditText
  protected lateinit var progressBarPasswordQuality: ProgressBar
  protected lateinit var textViewPasswordQualityInfo: TextView
  protected lateinit var textViewSuccessTitle: TextView
  protected lateinit var textViewSuccessSubTitle: TextView
  protected lateinit var textViewFirstPasswordCheckTitle: TextView
  protected lateinit var textViewSecondPasswordCheckTitle: TextView
  protected lateinit var btnSuccess: Button

  protected var isBackEnabled = true

  private val passwordStrengthViewModel: PasswordStrengthViewModel by viewModels()
  private var pwdStrengthResult: PgpPwd.PwdStrengthResult? = null

  override val contentViewResourceId: Int = R.layout.activity_pass_phrase_manager

  override val rootView: View
    get() = findViewById(R.id.layoutContent)

  abstract fun onConfirmPassPhraseSuccess()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (intent == null) {
      finish()
    }

    initViews()
    initPasswordStrengthViewModel()
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.buttonSetPassPhrase -> if (TextUtils.isEmpty(editTextKeyPassword.text.toString())) {
        showInfoSnackbar(
          rootView,
          getString(R.string.passphrase_must_be_non_empty),
          Snackbar.LENGTH_LONG
        )
      } else {
        if (snackBar != null) {
          snackBar!!.dismiss()
        }

        pwdStrengthResult?.word?.let { word ->
          when (word.word) {
            Constants.PASSWORD_QUALITY_WEAK, Constants.PASSWORD_QUALITY_POOR -> {
              InfoDialogFragment.newInstance(
                dialogTitle = "",
                dialogMsg = getString(R.string.select_stronger_pass_phrase)
              ).show(supportFragmentManager, InfoDialogFragment::class.java.simpleName)
            }

            else -> UIUtil.exchangeViewVisibility(
              true,
              layoutSecondPasswordCheck,
              layoutFirstPasswordCheck
            )
          }
        }
      }

      R.id.imageButtonShowPasswordHint -> {
        snackBar?.dismiss()

        try {
          val webViewInfoDialogFragment = WebViewInfoDialogFragment.newInstance(
            "",
            IOUtils.toString(assets.open("html/pass_phrase_hint.htm"), StandardCharsets.UTF_8)
          )
          webViewInfoDialogFragment.show(
            supportFragmentManager,
            WebViewInfoDialogFragment::class.java.simpleName
          )
        } catch (e: IOException) {
          e.printStackTrace()
        }

      }

      R.id.buttonUseAnotherPassPhrase -> {
        if (snackBar != null) {
          snackBar!!.dismiss()
        }

        editTextKeyPasswordSecond.text = null
        editTextKeyPassword.text = null
        UIUtil.exchangeViewVisibility(false, layoutSecondPasswordCheck, layoutFirstPasswordCheck)
      }

      R.id.buttonConfirmPassPhrases -> if (TextUtils.isEmpty(editTextKeyPasswordSecond.text.toString())) {
        showInfoSnackbar(
          rootView,
          getString(R.string.passphrase_must_be_non_empty),
          Snackbar.LENGTH_LONG
        )
      } else {
        snackBar?.dismiss()

        if (editTextKeyPassword.text.toString() == editTextKeyPasswordSecond.text.toString()) {
          onConfirmPassPhraseSuccess()
        } else {
          editTextKeyPasswordSecond.text = null
          showInfoSnackbar(
            rootView,
            getString(R.string.pass_phrases_do_not_match),
            Snackbar.LENGTH_LONG
          )
        }
      }

      R.id.buttonSuccess -> {
        setResult(Activity.RESULT_OK)
        finish()
      }
    }
  }

  protected open fun initViews() {
    layoutProgress = findViewById(R.id.layoutProgress)
    layoutContentView = findViewById(R.id.layoutContentView)
    layoutFirstPasswordCheck = findViewById(R.id.layoutFirstPasswordCheck)
    layoutSecondPasswordCheck = findViewById(R.id.layoutSecondPasswordCheck)
    layoutSuccess = findViewById(R.id.layoutSuccess)
    textViewSuccessTitle = findViewById(R.id.textViewSuccessTitle)
    textViewSuccessSubTitle = findViewById(R.id.textViewSuccessSubTitle)
    textViewFirstPasswordCheckTitle = findViewById(R.id.textViewFirstPasswordCheckTitle)
    textViewSecondPasswordCheckTitle = findViewById(R.id.textViewSecondPasswordCheckTitle)
    btnSuccess = findViewById(R.id.buttonSuccess)

    editTextKeyPassword = findViewById(R.id.editTextKeyPassword)
    editTextKeyPassword.addTextChangedListener { editable ->
      val passphrase = editable.toString()
      passwordStrengthViewModel.check(passphrase)
      if (TextUtils.isEmpty(editable)) {
        textViewPasswordQualityInfo.setText(R.string.passphrase_must_be_non_empty)
      }
    }
    editTextKeyPasswordSecond = findViewById(R.id.editTextKeyPasswordSecond)
    progressBarPasswordQuality = findViewById(R.id.progressBarPasswordQuality)
    textViewPasswordQualityInfo = findViewById(R.id.textViewPasswordQualityInfo)
    btnSetPassPhrase = findViewById(R.id.buttonSetPassPhrase)
    btnSetPassPhrase.setOnClickListener(this)
    findViewById<View>(R.id.imageButtonShowPasswordHint).setOnClickListener(this)
    findViewById<View>(R.id.buttonConfirmPassPhrases).setOnClickListener(this)
    findViewById<View>(R.id.buttonUseAnotherPassPhrase).setOnClickListener(this)
    btnSuccess.setOnClickListener(this)
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
          ContextCompat.getColor(this, R.color.silver), BlendModeCompat.MODULATE
        )
        btnSetPassPhrase.background.colorFilter = colorFilter
      }

      else -> {
        val colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
          ContextCompat.getColor(this, R.color.colorPrimary), BlendModeCompat.MODULATE
        )
        btnSetPassPhrase.background.colorFilter = colorFilter
      }
    }

    val color = parseColor()

    progressBarPasswordQuality.progress = word?.bar?.toInt() ?: 0
    val colorFilter =
      BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_IN)
    progressBarPasswordQuality.progressDrawable.colorFilter = colorFilter

    val qualityValue = getLocalizedPasswordQualityValue(word)

    val qualityValueSpannable = SpannableString(qualityValue)
    qualityValueSpannable.setSpan(
      ForegroundColorSpan(color), 0, qualityValueSpannable.length,
      Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    qualityValueSpannable.setSpan(
      StyleSpan(android.graphics.Typeface.BOLD), 0,
      qualityValueSpannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    textViewPasswordQualityInfo.text = qualityValueSpannable
    textViewPasswordQualityInfo.append(" ")
    textViewPasswordQualityInfo.append(getString(R.string.password_quality_subtext))

    val timeSpannable = SpannableString(pwdStrengthResult?.time ?: "")
    timeSpannable.setSpan(
      ForegroundColorSpan(color), 0, timeSpannable.length,
      Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    textViewPasswordQualityInfo.append(" ")
    textViewPasswordQualityInfo.append(timeSpannable)
    textViewPasswordQualityInfo.append(")")
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

  private fun initPasswordStrengthViewModel() {
    lifecycleScope.launchWhenStarted {
      passwordStrengthViewModel.pwdStrengthResultStateFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource.incrementSafely()
          }

          Result.Status.SUCCESS -> {
            pwdStrengthResult = it.data
            updateStrengthViews()
            countingIdlingResource.decrementSafely()
          }

          Result.Status.EXCEPTION -> {
            if (it.exception !is IllegalTextForStrengthMeasuringException) {
              val msg = it.exception?.message ?: it.exception?.javaClass?.simpleName
              ?: getString(R.string.unknown_error)
              Toast.makeText(this@BasePassPhraseManagerActivity, msg, Toast.LENGTH_LONG).show()
            }
            countingIdlingResource.decrementSafely()
          }
        }
      }
    }
  }
}
