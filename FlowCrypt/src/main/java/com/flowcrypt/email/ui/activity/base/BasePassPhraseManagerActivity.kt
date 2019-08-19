/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import android.app.Activity
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.Status
import com.flowcrypt.email.api.retrofit.node.NodeRepository
import com.flowcrypt.email.api.retrofit.response.model.node.Word
import com.flowcrypt.email.api.retrofit.response.node.NodeResponseWrapper
import com.flowcrypt.email.api.retrofit.response.node.ZxcvbnStrengthBarResult
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.jetpack.viewmodel.PasswordStrengthViewModel
import com.flowcrypt.email.ui.activity.fragment.dialog.InfoDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.WebViewInfoDialogFragment
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.idling.SingleIdlingResources
import com.google.android.material.snackbar.Snackbar
import org.apache.commons.io.IOUtils
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * @author Denis Bondarenko
 * Date: 04.08.2018
 * Time: 14:53
 * E-mail: DenBond7@gmail.com
 */
abstract class BasePassPhraseManagerActivity : BaseBackStackActivity(), View.OnClickListener, TextWatcher,
    Observer<NodeResponseWrapper<*>> {

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

  @JvmField
  protected var account: AccountDao? = null
  @JvmField
  protected var isBackEnabled = true

  @get:VisibleForTesting
  var idlingForPassphraseChecking: SingleIdlingResources? = null
    private set
  private lateinit var viewModel: PasswordStrengthViewModel
  private var strengthBarResult: ZxcvbnStrengthBarResult? = null
  private var timer: Timer? = null

  override val contentViewResourceId: Int = R.layout.activity_pass_phrase_manager

  override val rootView: View
    get() = findViewById(R.id.layoutContent)

  abstract fun onConfirmPassPhraseSuccess()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (intent == null) {
      finish()
    }

    this.account = intent.getParcelableExtra(KEY_EXTRA_ACCOUNT_DAO)

    if (account == null) {
      finish()
    }

    initViews()

    idlingForPassphraseChecking = SingleIdlingResources()
    timer = Timer()

    viewModel = ViewModelProviders.of(this).get(PasswordStrengthViewModel::class.java)
    viewModel.init(NodeRepository())
    viewModel.responsesLiveData.observe(this, this)
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.buttonSetPassPhrase -> if (TextUtils.isEmpty(editTextKeyPassword.text.toString())) {
        showInfoSnackbar(rootView, getString(R.string.passphrase_must_be_non_empty), Snackbar.LENGTH_LONG)
      } else {
        if (snackBar != null) {
          snackBar!!.dismiss()
        }

        if (strengthBarResult != null && strengthBarResult!!.word != null) {
          when (strengthBarResult!!.word!!.word) {
            Constants.PASSWORD_QUALITY_WEAK, Constants.PASSWORD_QUALITY_POOR -> {
              val infoDialogFragment = InfoDialogFragment.newInstance("",
                  getString(R.string.select_stronger_pass_phrase))
              infoDialogFragment.show(supportFragmentManager, InfoDialogFragment::class.java.simpleName)
            }

            else -> UIUtil.exchangeViewVisibility(this, true, layoutSecondPasswordCheck, layoutFirstPasswordCheck)
          }
        }
      }

      R.id.imageButtonShowPasswordHint -> {
        if (snackBar != null) {
          snackBar!!.dismiss()
        }

        try {
          val webViewInfoDialogFragment = WebViewInfoDialogFragment.newInstance("",
              IOUtils.toString(assets.open("html/pass_phrase_hint.htm"), StandardCharsets.UTF_8))
          webViewInfoDialogFragment.show(supportFragmentManager, WebViewInfoDialogFragment::class.java.simpleName)
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
        UIUtil.exchangeViewVisibility(this, false, layoutSecondPasswordCheck, layoutFirstPasswordCheck)
      }

      R.id.buttonConfirmPassPhrases -> if (TextUtils.isEmpty(editTextKeyPasswordSecond.text.toString())) {
        showInfoSnackbar(rootView, getString(R.string.passphrase_must_be_non_empty), Snackbar.LENGTH_LONG)
      } else {
        if (snackBar != null) {
          snackBar!!.dismiss()
        }

        if (editTextKeyPassword.text.toString() == editTextKeyPasswordSecond.text.toString()) {
          onConfirmPassPhraseSuccess()
        } else {
          editTextKeyPasswordSecond.text = null
          showInfoSnackbar(rootView, getString(R.string.pass_phrases_do_not_match), Snackbar.LENGTH_LONG)
        }
      }

      R.id.buttonSuccess -> {
        setResult(Activity.RESULT_OK)
        finish()
      }
    }
  }

  override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

  }

  override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

  }

  override fun afterTextChanged(editable: Editable) {
    idlingForPassphraseChecking!!.setIdleState(false)
    val passphrase = editable.toString()
    timer!!.cancel()
    timer = Timer()
    timer!!.schedule(
        object : TimerTask() {
          override fun run() {
            runOnUiThread(object : TimerTask() {
              override fun run() {
                viewModel.check(passphrase)
              }
            })
          }
        },
        DELAY
    )

    if (TextUtils.isEmpty(editable)) {
      textViewPasswordQualityInfo.setText(R.string.passphrase_must_be_non_empty)
    }
  }

  override fun onChanged(nodeResponseWrapper: NodeResponseWrapper<*>) {
    when (nodeResponseWrapper.requestCode) {
      R.id.live_data_id_check_passphrase_strength -> {
        idlingForPassphraseChecking!!.setIdleState(true)

        when (nodeResponseWrapper.status) {
          Status.SUCCESS -> {
            strengthBarResult = nodeResponseWrapper.result as ZxcvbnStrengthBarResult?
            updateStrengthViews()
          }

          Status.ERROR -> if (nodeResponseWrapper.result != null) {
            val error = nodeResponseWrapper.result?.error
            Toast.makeText(this, error?.toString() ?: "", Toast.LENGTH_SHORT).show()
          }

          Status.EXCEPTION -> if (nodeResponseWrapper.result != null) {
            val throwable = nodeResponseWrapper.exception
            if (throwable != null) {
              Toast.makeText(this, throwable.message, Toast.LENGTH_SHORT).show()
            }
          }

          else -> {
          }
        }
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
    editTextKeyPassword.addTextChangedListener(this)
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
    if (strengthBarResult == null || strengthBarResult!!.word == null) {
      return
    }

    val word = strengthBarResult!!.word

    when (word!!.word) {
      Constants.PASSWORD_QUALITY_WEAK,
      Constants.PASSWORD_QUALITY_POOR -> btnSetPassPhrase.background.setColorFilter(
          ContextCompat.getColor(this, R.color.silver), PorterDuff.Mode.MULTIPLY)

      else -> btnSetPassPhrase.background.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary),
          PorterDuff.Mode.MULTIPLY)
    }

    val color = parseColor()

    progressBarPasswordQuality.progress = word.bar
    progressBarPasswordQuality.progressDrawable.setColorFilter(color, PorterDuff.Mode.SRC_IN)

    val qualityValue = getLocalizedPasswordQualityValue(word)

    val qualityValueSpannable = SpannableString(qualityValue)
    qualityValueSpannable.setSpan(ForegroundColorSpan(color), 0, qualityValueSpannable.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    qualityValueSpannable.setSpan(StyleSpan(android.graphics.Typeface.BOLD), 0,
        qualityValueSpannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    textViewPasswordQualityInfo.text = qualityValueSpannable
    textViewPasswordQualityInfo.append(" ")
    textViewPasswordQualityInfo.append(getString(R.string.password_quality_subtext))

    val timeSpannable = SpannableString(strengthBarResult!!.time)
    timeSpannable.setSpan(ForegroundColorSpan(color), 0, timeSpannable.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    textViewPasswordQualityInfo.append(" ")
    textViewPasswordQualityInfo.append(timeSpannable)
    textViewPasswordQualityInfo.append(")")
  }

  private fun parseColor(): Int {
    return try {
      Color.parseColor(strengthBarResult!!.word!!.color)
    } catch (e: IllegalArgumentException) {
      e.printStackTrace()

      when (strengthBarResult!!.word!!.color) {
        "orange" -> Color.parseColor("#FFA500")

        "darkorange" -> Color.parseColor("#FF8C00")

        "darkred" -> Color.parseColor("#8B0000")

        else -> Color.DKGRAY
      }
    }
  }

  private fun getLocalizedPasswordQualityValue(passwordStrength: Word): String? {
    var qualityValue = passwordStrength.word

    if (qualityValue != null) {
      when (qualityValue) {
        Constants.PASSWORD_QUALITY_PERFECT -> qualityValue = getString(R.string.password_quality_perfect)

        Constants.PASSWORD_QUALITY_GREAT -> qualityValue = getString(R.string.password_quality_great)

        Constants.PASSWORD_QUALITY_GOOD -> qualityValue = getString(R.string.password_quality_good)

        Constants.PASSWORD_QUALITY_REASONABLE -> qualityValue = getString(R.string.password_quality_reasonable)

        Constants.PASSWORD_QUALITY_POOR -> qualityValue = getString(R.string.password_quality_poor)

        Constants.PASSWORD_QUALITY_WEAK -> qualityValue = getString(R.string.password_quality_weak)
      }

      qualityValue = qualityValue!!.toUpperCase()
    }

    return qualityValue
  }

  companion object {
    @JvmField
    val KEY_EXTRA_ACCOUNT_DAO = GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_ACCOUNT_DAO", BasePassPhraseManagerActivity::class.java)
    private const val DELAY: Long = 350
  }
}
