/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.database.provider.FlowcryptContract
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.service.CheckClipboardToFindKeyService
import com.flowcrypt.email.service.EmailSyncService
import com.flowcrypt.email.ui.activity.base.BaseSignInActivity
import com.flowcrypt.email.ui.loader.LoadPrivateKeysFromMailAsyncTaskLoader
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.util.CollectionUtils
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import java.util.*

/**
 * This [Activity] shows a screen where a user can to sign in to his account.
 *
 * @author DenBond7
 * Date: 26.14.2017
 * Time: 14:50
 * E-mail: DenBond7@gmail.com
 */
class SignInActivity : BaseSignInActivity(), LoaderManager.LoaderCallbacks<LoaderResult> {

  override lateinit var rootView: View
  private var progressView: View? = null

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
          Activity.RESULT_OK, CheckKeysActivity.RESULT_NEUTRAL -> runEmailManagerActivity()

          Activity.RESULT_CANCELED, CheckKeysActivity.RESULT_NEGATIVE -> {
            this.googleSignInAccount = null
            UIUtil.exchangeViewVisibility(this, false, progressView!!, rootView)
          }
        }
      }

      REQUEST_CODE_CREATE_OR_IMPORT_KEY -> when (resultCode) {
        Activity.RESULT_OK -> runEmailManagerActivity()

        Activity.RESULT_CANCELED, CreateOrImportKeyActivity.RESULT_CODE_USE_ANOTHER_ACCOUNT -> {
          this.googleSignInAccount = null
          UIUtil.exchangeViewVisibility(this, false, progressView!!, rootView)
        }
      }

      REQUEST_CODE_ADD_OTHER_ACCOUNT -> when (resultCode) {
        Activity.RESULT_OK -> try {
          val authCreds = data!!.getParcelableExtra<AuthCredentials>(AddNewAccountManuallyActivity
              .KEY_EXTRA_AUTH_CREDENTIALS)
          if (authCreds != null) {
            addNewAccount(authCreds)
          } else {
            ExceptionUtil.handleError(NullPointerException("AuthCredentials is null!"))
            Toast.makeText(this, R.string.error_occurred_try_again_later, Toast.LENGTH_SHORT).show()
          }
        } catch (e: Exception) {
          e.printStackTrace()
          ExceptionUtil.handleError(e)
          Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_SHORT).show()
        }

        CreateOrImportKeyActivity.RESULT_CODE_USE_ANOTHER_ACCOUNT -> if (data != null) {
          val accountDao = data.getParcelableExtra<AccountDao>(CreateOrImportKeyActivity.EXTRA_KEY_ACCOUNT_DAO)
          clearInfoAboutOldAccount(accountDao)
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
      else -> super.onClick(v)
    }

  }

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<LoaderResult> {
    return when (id) {
      R.id.loader_id_load_private_key_backups_from_email -> {
        isStartCheckKeysActivityEnabled = true

        var account: AccountDao? = null
        UIUtil.exchangeViewVisibility(this, true, progressView!!, rootView)
        if (googleSignInAccount != null) {
          account = AccountDao(googleSignInAccount!!.email!!, AccountDao.ACCOUNT_TYPE_GOOGLE)
        }
        LoadPrivateKeysFromMailAsyncTaskLoader(this, account!!)
      }

      else -> Loader(this)
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun onLoadFinished(loader: Loader<LoaderResult>, loaderResult: LoaderResult) {
    when (loader.id) {
      R.id.loader_id_load_private_key_backups_from_email -> if (loaderResult.result != null) {
        val keyDetailsList = loaderResult.result as ArrayList<NodeKeyDetails>?
        if (CollectionUtils.isEmpty(keyDetailsList)) {
          if (googleSignInAccount != null) {
            val intent = CreateOrImportKeyActivity.newIntent(this, AccountDao(googleSignInAccount!!), true)
            startActivityForResult(intent, REQUEST_CODE_CREATE_OR_IMPORT_KEY)
          }
        } else if (isStartCheckKeysActivityEnabled) {
          val bottomTitle = resources.getQuantityString(R.plurals.found_backup_of_your_account_key,
              keyDetailsList!!.size, keyDetailsList.size)
          val positiveBtnTitle = getString(R.string.continue_)
          val neutralBtnTitle = if (SecurityUtils.hasBackup(this))
            getString(R.string.use_existing_keys)
          else
            null
          val negativeBtnTitle = getString(R.string.use_another_account)
          val intent = CheckKeysActivity.newIntent(this, keyDetailsList, KeyDetails.Type.EMAIL, bottomTitle,
              positiveBtnTitle, neutralBtnTitle, negativeBtnTitle)
          startActivityForResult(intent, REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL)
        }
      } else if (loaderResult.exception != null) {
        UIUtil.exchangeViewVisibility(this, false, progressView!!, rootView)

        if (loaderResult.exception is UserRecoverableAuthIOException) {
          startActivityForResult((loaderResult.exception as UserRecoverableAuthIOException).intent,
              REQUEST_CODE_RESOLVE_SIGN_IN_ERROR)
        } else {
          UIUtil.showInfoSnackbar(rootView, loaderResult.exception?.message ?: "")
        }
      }
    }
  }

  override fun onLoaderReset(loader: Loader<LoaderResult>) {

  }

  override fun onSignSuccess(googleSignInAccount: GoogleSignInAccount?) {
    startService(Intent(this, CheckClipboardToFindKeyService::class.java))
    LoaderManager.getInstance(this).restartLoader(R.id.loader_id_load_private_key_backups_from_email, null, this)
  }

  private fun addNewAccount(authCreds: AuthCredentials) {
    val accountDaoSource = AccountDaoSource()
    accountDaoSource.addRow(this, authCreds)
    EmailSyncService.startEmailSyncService(this)

    val account = accountDaoSource.getAccountInformation(this, authCreds.email)

    if (account != null) {
      EmailManagerActivity.runEmailManagerActivity(this)
      finish()
    } else {
      Toast.makeText(this, R.string.error_occurred_try_again_later, Toast.LENGTH_SHORT).show()
    }
  }

  private fun runEmailManagerActivity() {
    EmailSyncService.startEmailSyncService(this)

    val account = addGmailAccount(googleSignInAccount)
    if (account != null) {
      EmailManagerActivity.runEmailManagerActivity(this)
      finish()
    } else {
      Toast.makeText(this, R.string.error_occurred_try_again_later, Toast.LENGTH_SHORT).show()
    }
  }

  /**
   * Clear information about created but a not used account.
   *
   * @param account The account which will be deleted from the local database.
   */
  private fun clearInfoAboutOldAccount(account: AccountDao?) {
    if (account != null) {
      val uri = Uri.parse(FlowcryptContract.AUTHORITY_URI.toString() + "/" + FlowcryptContract.CLEAN_DATABASE)
      contentResolver.delete(uri, null, arrayOf(account.email))
    }
  }

  /**
   * Created a GMAIL [AccountDao] and add it to the database.
   *
   * @param googleSignInAccount The [GoogleSignInAccount] object which contains information about a Google
   * account.
   * @return Generated [AccountDao].
   */
  private fun addGmailAccount(googleSignInAccount: GoogleSignInAccount?): AccountDao? {
    if (googleSignInAccount == null) {
      ExceptionUtil.handleError(NullPointerException("GoogleSignInAccount is null!"))
      return null
    }

    val accountDaoSource = AccountDaoSource()

    val isAccountUpdated = accountDaoSource.updateAccountInformation(this, googleSignInAccount) > 0
    if (!isAccountUpdated) {
      accountDaoSource.addRow(this, googleSignInAccount)
    }

    return AccountDaoSource().getAccountInformation(this, googleSignInAccount.email!!)
  }

  /**
   * In this method we init all used views.
   */
  private fun initViews() {
    rootView = findViewById(R.id.signInView)
    progressView = findViewById(R.id.progressView)

    if (findViewById<View>(R.id.buttonSignInWithGmail) != null) {
      findViewById<View>(R.id.buttonSignInWithGmail).setOnClickListener(this)
    }

    if (findViewById<View>(R.id.buttonOtherEmailProvider) != null) {
      findViewById<View>(R.id.buttonOtherEmailProvider).setOnClickListener(this)
    }

    if (findViewById<View>(R.id.buttonPrivacy) != null) {
      findViewById<View>(R.id.buttonPrivacy).setOnClickListener(this)
    }

    if (findViewById<View>(R.id.buttonTerms) != null) {
      findViewById<View>(R.id.buttonTerms).setOnClickListener(this)
    }

    if (findViewById<View>(R.id.buttonSecurity) != null) {
      findViewById<View>(R.id.buttonSecurity).setOnClickListener(this)
    }
  }

  companion object {
    private const val REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL = 101
    private const val REQUEST_CODE_CREATE_OR_IMPORT_KEY = 102
  }
}
