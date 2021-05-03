/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.OrgRules
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.jetpack.viewmodel.LoadPrivateKeysViewModel
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.ui.activity.base.BasePassPhraseManagerActivity
import com.flowcrypt.email.ui.notifications.SystemNotificationManager
import com.flowcrypt.email.util.UIUtil

/**
 * This activity describes a logic of changing the pass phrase of all imported private keys of an active account.
 *
 * @author Denis Bondarenko
 * Date: 05.08.2018
 * Time: 20:15
 * E-mail: DenBond7@gmail.com
 */
class ChangePassPhraseActivity : BasePassPhraseManagerActivity() {
  private val loadPrivateKeysViewModel: LoadPrivateKeysViewModel by viewModels()
  private val privateKeysViewModel: PrivateKeysViewModel by viewModels()

  override fun onConfirmPassPhraseSuccess() {
    privateKeysViewModel.changePassphrase(editTextKeyPassword.text.toString())
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    SystemNotificationManager(this).cancel(SystemNotificationManager.NOTIFICATION_ID_PASSPHRASE_TOO_WEAK)
    setupLoadPrivateKeysViewModel()
    setupPrivateKeysViewModel()
  }

  override fun onBackPressed() {
    if (isBackEnabled) {
      super.onBackPressed()
    } else {
      Toast.makeText(this, R.string.please_wait_while_pass_phrase_will_be_changed, Toast.LENGTH_SHORT).show()
    }
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

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_BACKUP_WITH_OPTION -> {
        when (resultCode) {
          Activity.RESULT_OK -> Toast.makeText(this, R.string.backed_up_successfully, Toast.LENGTH_SHORT).show()
        }
        setResult(Activity.RESULT_OK)
        finish()
      }
      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun initViews() {
    super.initViews()

    textViewFirstPasswordCheckTitle.setText(R.string.change_pass_phrase)
    textViewSecondPasswordCheckTitle.setText(R.string.change_pass_phrase)

    textViewSuccessTitle.setText(R.string.done)
    textViewSuccessSubTitle.setText(R.string.pass_phrase_changed)
    btnSuccess.setText(R.string.back)
  }

  private fun runBackupKeysActivity() {
    isBackEnabled = true
    Toast.makeText(this, R.string.back_up_updated_key, Toast.LENGTH_LONG).show()
    startActivityForResult(Intent(this, BackupKeysActivity::class.java), REQUEST_CODE_BACKUP_WITH_OPTION)
  }

  private fun setupLoadPrivateKeysViewModel() {
    loadPrivateKeysViewModel.privateKeysLiveData.observe(this, {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource.incrementSafely()
          }

          Result.Status.SUCCESS -> {
            val keyDetailsList = it.data
            if (keyDetailsList?.isEmpty() == true) {
              runBackupKeysActivity()
            } else {
              privateKeysViewModel.saveBackupsToInbox()
            }
            countingIdlingResource.decrementSafely()
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            runBackupKeysActivity()
            countingIdlingResource.decrementSafely()
          }

          else -> countingIdlingResource.decrementSafely()
        }
      }
    })
  }

  private fun setupPrivateKeysViewModel() {
    privateKeysViewModel.changePassphraseLiveData.observe(this, {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource.incrementSafely()
            isBackEnabled = false
            UIUtil.exchangeViewVisibility(true, layoutProgress, layoutContentView)
          }

          Result.Status.SUCCESS -> {
            if (it.data == true) {
              if (activeAccount?.isRuleExist(OrgRules.DomainRule.NO_PRV_BACKUP) == true) {
                isBackEnabled = true
                Toast.makeText(this, R.string.pass_phrase_changed, Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
              } else {
                activeAccount?.let { accountEntity -> loadPrivateKeysViewModel.fetchAvailableKeys(accountEntity) }
              }
            }

            countingIdlingResource.decrementSafely()
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            isBackEnabled = true
            editTextKeyPasswordSecond.text = null
            UIUtil.exchangeViewVisibility(false, layoutProgress, layoutContentView)
            showInfoSnackbar(rootView, it.exception?.message ?: getString(R.string.unknown_error))

            countingIdlingResource.decrementSafely()
          }

          else -> countingIdlingResource.decrementSafely()
        }
      }
    })

    privateKeysViewModel.saveBackupToInboxLiveData.observe(this, {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource.incrementSafely()
          }

          Result.Status.SUCCESS -> {
            isBackEnabled = true

            Toast.makeText(this, R.string.pass_phrase_changed, Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_OK)
            countingIdlingResource.decrementSafely()
            finish()
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            isBackEnabled = true
            runBackupKeysActivity()
            countingIdlingResource.decrementSafely()
          }

          else -> countingIdlingResource.decrementSafely()
        }
      }
    })
  }

  companion object {

    const val REQUEST_CODE_BACKUP_WITH_OPTION = 100

    fun newIntent(context: Context?): Intent {
      return Intent(context, ChangePassPhraseActivity::class.java)
    }
  }
}
