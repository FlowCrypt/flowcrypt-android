/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.ui.activity.base.BasePassPhraseManagerActivity
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.exception.ApiException
import com.google.android.material.snackbar.Snackbar

/**
 * @author Denis Bondarenko
 * Date: 08.01.2018
 * Time: 15:58
 * E-mail: DenBond7@gmail.com
 */
class CreatePrivateKeyActivity : BasePassPhraseManagerActivity() {
  private val privateKeysViewModel: PrivateKeysViewModel by viewModels()
  private var createdPrivateKeyFingerprint: String? = null
  private var tempAccount: AccountEntity? = null

  override val contentViewResourceId: Int
    get() = R.layout.activity_pass_phrase_manager

  override val rootView: View
    get() = findViewById(R.id.layoutContent)

  override fun onConfirmPassPhraseSuccess() {
    tempAccount?.let {
      privateKeysViewModel.createPrivateKey(
        accountEntity = it,
        passphrase = editTextKeyPassword.text.toString(),
        passphraseType = KeyEntity.PassphraseType.DATABASE
      )
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.tempAccount = intent.getParcelableExtra(KEY_EXTRA_ACCOUNT)

    if (tempAccount == null) {
      finish()
    }

    if (savedInstanceState != null) {
      this.createdPrivateKeyFingerprint =
        savedInstanceState.getString(KEY_CREATED_PRIVATE_KEY_FINGERPRINT)
    }

    setupPrivateKeyViewModel()
  }

  override fun onBackPressed() {
    if (isBackEnabled) {
      if (TextUtils.isEmpty(createdPrivateKeyFingerprint)) {
        super.onBackPressed()
      } else {
        setResult(Activity.RESULT_OK)
        finish()
      }
    } else {
      Toast.makeText(this, R.string.please_wait_while_key_will_be_created, Toast.LENGTH_SHORT)
        .show()
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(KEY_CREATED_PRIVATE_KEY_FINGERPRINT, createdPrivateKeyFingerprint)
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.buttonSuccess -> {
        setResult(Activity.RESULT_OK)
        finish()
      }

      else -> super.onClick(v)
    }
  }

  override fun initViews() {
    super.initViews()

    textViewFirstPasswordCheckTitle.setText(R.string.set_up_flow_crypt)
    textViewSecondPasswordCheckTitle.setText(R.string.set_up_flow_crypt)

    textViewSuccessTitle.setText(R.string.you_are_all_set)
    textViewSuccessSubTitle.setText(R.string.you_can_send_and_receive_encrypted_emails)
    btnSuccess.setText(R.string.continue_)

    if (!TextUtils.isEmpty(this.createdPrivateKeyFingerprint)) {
      layoutProgress.visibility = View.GONE
      layoutFirstPasswordCheck.visibility = View.GONE
      layoutSecondPasswordCheck.visibility = View.GONE
      layoutSuccess.visibility = View.VISIBLE
      layoutContentView.visibility = View.VISIBLE
    }
  }

  private fun setupPrivateKeyViewModel() {
    privateKeysViewModel.createPrivateKeyLiveData.observe(this, {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource.incrementSafely()
            isBackEnabled = false
            UIUtil.exchangeViewVisibility(true, layoutProgress, layoutContentView)
          }

          Result.Status.SUCCESS -> {
            isBackEnabled = true
            val pgpKeyDetails: PgpKeyDetails? = it.data
            createdPrivateKeyFingerprint = pgpKeyDetails?.fingerprint
            layoutSecondPasswordCheck.visibility = View.GONE
            layoutSuccess.visibility = View.VISIBLE
            UIUtil.exchangeViewVisibility(false, layoutProgress, layoutContentView)
            countingIdlingResource.decrementSafely()
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            isBackEnabled = true
            UIUtil.exchangeViewVisibility(false, layoutProgress, layoutContentView)

            it.exception?.let { exception ->
              if (exception is ApiException) {
                showSnackbar(
                  rootView, exception.apiError?.msg ?: it.javaClass.simpleName,
                  getString(R.string.retry), Snackbar.LENGTH_LONG
                ) {
                  onConfirmPassPhraseSuccess()
                }
              } else {
                editTextKeyPasswordSecond.text = null
                showInfoSnackbar(rootView, exception.message)
              }
            }
            countingIdlingResource.decrementSafely()
          }
        }
      }
    })
  }

  companion object {
    val KEY_EXTRA_ACCOUNT = GeneralUtil.generateUniqueExtraKey(
      "KEY_EXTRA_ACCOUNT",
      BasePassPhraseManagerActivity::class.java
    )
    val KEY_CREATED_PRIVATE_KEY_FINGERPRINT =
      GeneralUtil.generateUniqueExtraKey(
        "KEY_CREATED_PRIVATE_KEY_FINGERPRINT",
        CreatePrivateKeyActivity::class.java
      )

    fun newIntent(context: Context, account: AccountEntity?): Intent {
      val intent = Intent(context, CreatePrivateKeyActivity::class.java)
      intent.putExtra(KEY_EXTRA_ACCOUNT, account)
      return intent
    }
  }
}
