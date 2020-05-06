/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.service.CheckClipboardToFindKeyService
import com.flowcrypt.email.service.EmailSyncService
import com.flowcrypt.email.service.actionqueue.actions.LoadGmailAliasesAction
import com.flowcrypt.email.ui.activity.base.BaseSignInActivity
import com.flowcrypt.email.ui.activity.settings.FeedbackActivity
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.launch
import java.util.*

/**
 * This [Activity] shows a screen where a user can to sign in to his account.
 *
 * @author DenBond7
 * Date: 26.14.2017
 * Time: 14:50
 * E-mail: DenBond7@gmail.com
 */
class SignInActivity : BaseSignInActivity() {

  override val rootView: View
    get() = findViewById(R.id.signInView)

  override val progressView: View
    get() = findViewById(R.id.progressView)

  private var isStartCheckKeysActivityEnabled: Boolean = false

  override val isDisplayHomeAsUpEnabled: Boolean
    get() = false

  override val contentViewResourceId: Int
    get() = R.layout.activity_splash

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initViews()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL -> {
        isStartCheckKeysActivityEnabled = false

        when (resultCode) {
          Activity.RESULT_OK, CheckKeysActivity.RESULT_SKIP_REMAINING_KEYS -> {
            val keys: List<NodeKeyDetails>? = data?.getParcelableArrayListExtra(
                CheckKeysActivity.KEY_EXTRA_UNLOCKED_PRIVATE_KEYS)

            if (keys.isNullOrEmpty()) {
              showInfoSnackbar(rootView, getString(R.string.error_no_keys))
            } else {
              privateKeysViewModel.encryptAndSaveKeysToDatabase(keys, KeyDetails.Type.EMAIL)
            }
          }

          CheckKeysActivity.RESULT_USE_EXISTING_KEYS -> {
            runEmailManagerActivity()
          }

          CheckKeysActivity.RESULT_NO_NEW_KEYS -> {
            Toast.makeText(this, getString(R.string.key_already_imported_finishing_setup), Toast
                .LENGTH_SHORT).show()
            runEmailManagerActivity()
          }

          Activity.RESULT_CANCELED, CheckKeysActivity.RESULT_NEGATIVE -> {
            this.googleSignInAccount = null
            UIUtil.exchangeViewVisibility(false, progressView, rootView)
          }
        }
      }

      REQUEST_CODE_CREATE_OR_IMPORT_KEY -> when (resultCode) {
        Activity.RESULT_OK -> runEmailManagerActivity()

        Activity.RESULT_CANCELED, CreateOrImportKeyActivity.RESULT_CODE_USE_ANOTHER_ACCOUNT -> {
          this.googleSignInAccount = null
          UIUtil.exchangeViewVisibility(false, progressView, rootView)
        }
      }

      REQUEST_CODE_ADD_OTHER_ACCOUNT -> when (resultCode) {
        Activity.RESULT_OK -> try {
          val authCreds = data?.getParcelableExtra<AccountEntity>(
              AddNewAccountManuallyActivity.KEY_EXTRA_AUTH_CREDENTIALS)
          if (authCreds != null) {
            addNewAccount(authCreds)
          } else {
            ExceptionUtil.handleError(NullPointerException("AccountEntity is null!"))
            Toast.makeText(this, R.string.error_occurred_try_again_later, Toast.LENGTH_SHORT).show()
          }
        } catch (e: Exception) {
          e.printStackTrace()
          ExceptionUtil.handleError(e)
          Toast.makeText(this, e.message ?: e.javaClass.simpleName, Toast.LENGTH_SHORT).show()
        }

        AddNewAccountManuallyActivity.RESULT_CODE_CONTINUE_WITH_GMAIL ->
          super.onActivityResult(requestCode, resultCode, data)
      }

      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.buttonPrivacy -> GeneralUtil.openCustomTab(this, Constants.FLOWCRYPT_PRIVACY_URL)

      R.id.buttonTerms -> GeneralUtil.openCustomTab(this, Constants.FLOWCRYPT_TERMS_URL)

      R.id.buttonSecurity ->
        startActivity(HtmlViewFromAssetsRawActivity.newIntent(this, getString(R.string.security), "html/security.htm"))

      R.id.buttonHelp -> FeedbackActivity.show(this)

      else -> super.onClick(v)
    }
  }

  override fun onSignSuccess(googleSignInAccount: GoogleSignInAccount?) {
    if (domainRules?.contains(AccountEntity.DomainRule.NO_PRV_BACKUP.name) == true) {
      if (googleSignInAccount != null) {
        startService(Intent(this, CheckClipboardToFindKeyService::class.java))
        val intent = CreateOrImportKeyActivity.newIntent(this,
            AccountEntity(googleSignInAccount, uuid, domainRules), true)
        startActivityForResult(intent, REQUEST_CODE_CREATE_OR_IMPORT_KEY)
      }
    } else {
      googleSignInAccount?.let {
        isStartCheckKeysActivityEnabled = true
        loadPrivateKeysViewModel.fetchAvailableKeys(AccountEntity(it, uuid, domainRules))
      }
    }
  }

  override fun onFetchKeysCompleted(keyDetailsList: ArrayList<NodeKeyDetails>?) {
    if (keyDetailsList.isNullOrEmpty()) {
      googleSignInAccount?.let {
        startService(Intent(this, CheckClipboardToFindKeyService::class.java))
        val intent = CreateOrImportKeyActivity.newIntent(this,
            AccountEntity(it, uuid, domainRules), true)
        startActivityForResult(intent, REQUEST_CODE_CREATE_OR_IMPORT_KEY)
      }
    } else if (isStartCheckKeysActivityEnabled) {
      val subTitle = resources.getQuantityString(R.plurals.found_backup_of_your_account_key, keyDetailsList.size, keyDetailsList.size)
      val positiveBtnTitle = getString(R.string.continue_)
      val negativeBtnTitle = getString(R.string.use_another_account)
      val intent = CheckKeysActivity.newIntent(context = this, privateKeys = keyDetailsList,
          type = KeyDetails.Type.EMAIL, subTitle = subTitle, positiveBtnTitle = positiveBtnTitle,
          negativeBtnTitle = negativeBtnTitle)
      startActivityForResult(intent, REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL)
    }
  }

  override fun onPrivateKeysSaved() {
    runEmailManagerActivity()
  }

  private fun addNewAccount(accountEntity: AccountEntity) {
    lifecycleScope.launch {
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(this@SignInActivity)
      roomDatabase.accountDao().insertSuspend(accountEntity.copy(isActive = true))
      EmailSyncService.startEmailSyncService(this@SignInActivity)

      val addedAccount = roomDatabase.accountDao().getAccount(accountEntity.email)
      if (addedAccount != null) {
        EmailManagerActivity.runEmailManagerActivity(this@SignInActivity)
        finish()
      } else {
        Toast.makeText(this@SignInActivity, R.string.error_occurred_try_again_later, Toast.LENGTH_SHORT).show()
      }
    }
  }

  private fun runEmailManagerActivity() {
    EmailSyncService.startEmailSyncService(this)

    if (googleSignInAccount == null) {
      ExceptionUtil.handleError(NullPointerException("GoogleSignInAccount is null!"))
      Toast.makeText(this, R.string.error_occurred_try_again_later, Toast.LENGTH_SHORT).show()
      return
    }

    googleSignInAccount?.email?.let { email ->
      lifecycleScope.launch {
        val roomDatabase = FlowCryptRoomDatabase.getDatabase(this@SignInActivity)
        val existedAccount = roomDatabase.accountDao().getAccountSuspend(email)

        val insertOrUpdateCandidate = googleSignInAccount?.let {
          AccountEntity(it, uuid, domainRules)
        }

        insertOrUpdateCandidate?.let {
          if (existedAccount == null) {
            roomDatabase.accountDao().insertSuspend(insertOrUpdateCandidate)
          } else {
            roomDatabase.accountDao().updateAccountSuspend(insertOrUpdateCandidate.copy(
                id = existedAccount.id, uuid = existedAccount.uuid, domainRules = existedAccount.domainRules))
          }
        }

        roomBasicViewModel.addActionToQueue(LoadGmailAliasesAction(email = googleSignInAccount?.email))
        EmailManagerActivity.runEmailManagerActivity(this@SignInActivity)
        finish()
      }
    }
  }

  /**
   * In this method we init all used views.
   */
  private fun initViews() {
    findViewById<View>(R.id.buttonSignInWithGmail)?.setOnClickListener(this)
    findViewById<View>(R.id.buttonOtherEmailProvider)?.setOnClickListener(this)
    findViewById<View>(R.id.buttonPrivacy)?.setOnClickListener(this)
    findViewById<View>(R.id.buttonTerms)?.setOnClickListener(this)
    findViewById<View>(R.id.buttonSecurity)?.setOnClickListener(this)
    findViewById<View>(R.id.buttonHelp)?.setOnClickListener(this)
  }

  companion object {
    private const val REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL = 101
    private const val REQUEST_CODE_CREATE_OR_IMPORT_KEY = 102
  }
}
