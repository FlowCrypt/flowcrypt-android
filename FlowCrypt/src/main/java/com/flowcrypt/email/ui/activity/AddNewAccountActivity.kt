/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.ui.activity.base.BaseSignInActivity
import com.flowcrypt.email.ui.loader.LoadPrivateKeysFromMailAsyncTaskLoader
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.util.CollectionUtils
import com.google.android.material.snackbar.Snackbar
import java.util.*

/**
 * This activity describes a logic of add a new email account.
 *
 * @author Denis Bondarenko
 * Date: 05.10.2017
 * Time: 10:34
 * E-mail: DenBond7@gmail.com
 */

class AddNewAccountActivity : BaseSignInActivity(), View.OnClickListener, GoogleApiClient.OnConnectionFailedListener,
    GoogleApiClient.ConnectionCallbacks, LoaderManager.LoaderCallbacks<LoaderResult> {

  private var progressView: View? = null
  private var content: View? = null

  override val isDisplayHomeAsUpEnabled: Boolean
    get() = true

  override val contentViewResourceId: Int
    get() = R.layout.activity_add_new_account

  override val rootView: View
    get() = findViewById(R.id.screenContent)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    progressView = findViewById(R.id.progressView)
    content = findViewById(R.id.layoutContent)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_SIGN_IN -> {
        val signInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
        if (signInResult.isSuccess) {
          this.sign = signInResult.signInAccount
          if (this.sign != null) {
            if (AccountDaoSource().getAccountInformation(this, this.sign!!.email!!) == null) {
              LoaderManager.getInstance(this).restartLoader(R.id.loader_id_load_private_key_backups_from_email, null,
                  this)
            } else {
              showInfoSnackbar(rootView, getString(R.string.template_email_alredy_added,
                  this.sign!!.email), Snackbar.LENGTH_LONG)
            }
          } else
            throw NullPointerException("GoogleSignInAccount is null!")
        }
      }

      REQUEST_CODE_ADD_OTHER_ACCOUNT -> when (resultCode) {
        Activity.RESULT_OK -> try {
          val authCreds = data!!.getParcelableExtra<AuthCredentials>(AddNewAccountManuallyActivity
              .KEY_EXTRA_AUTH_CREDENTIALS)
          val accountDaoSource = AccountDaoSource()
          accountDaoSource.addRow(this, authCreds)
          accountDaoSource.setActiveAccount(this, authCreds.email)

          val intent = Intent()
          intent.putExtra(KEY_EXTRA_NEW_ACCOUNT, accountDaoSource.getActiveAccountInformation(this))

          setResult(Activity.RESULT_OK, intent)
          finish()
        } catch (e: Exception) {
          e.printStackTrace()
          ExceptionUtil.handleError(e)
          Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_SHORT).show()
        }

        AddNewAccountManuallyActivity.RESULT_CODE_CONTINUE_WITH_GMAIL -> super.onActivityResult(requestCode, resultCode, data)
      }

      REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL -> when (resultCode) {
        Activity.RESULT_OK, CheckKeysActivity.RESULT_NEUTRAL -> returnResultOk()

        Activity.RESULT_CANCELED, CheckKeysActivity.RESULT_NEGATIVE -> {
          UIUtil.exchangeViewVisibility(this, false, progressView!!, content!!)
          LoaderManager.getInstance(this).destroyLoader(R.id.loader_id_load_private_key_backups_from_email)
        }
      }

      REQUEST_CODE_CREATE_OR_IMPORT_KEY_FOR_GMAIL -> when (resultCode) {
        Activity.RESULT_OK -> returnResultOk()

        Activity.RESULT_CANCELED, CreateOrImportKeyActivity.RESULT_CODE_USE_ANOTHER_ACCOUNT -> LoaderManager.getInstance(this).destroyLoader(R.id.loader_id_load_private_key_backups_from_email)
      }

      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<LoaderResult> {
    return when (id) {
      R.id.loader_id_load_private_key_backups_from_email -> {
        UIUtil.exchangeViewVisibility(this, true, progressView!!, content!!)
        val account = AccountDao(sign!!.email!!, AccountDao.ACCOUNT_TYPE_GOOGLE)
        LoadPrivateKeysFromMailAsyncTaskLoader(this, account)
      }

      else -> Loader(this)
    }
  }

  override fun onLoadFinished(loader: Loader<LoaderResult>, loaderResult: LoaderResult) {
    handleLoaderResult(loader, loaderResult)
  }

  override fun onLoaderReset(loader: Loader<LoaderResult>) {
    when (loader.id) {
      R.id.loader_id_load_private_key_backups_from_email -> this.sign = null
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun onSuccess(loaderId: Int, result: Any?) {
    when (loaderId) {
      R.id.loader_id_load_private_key_backups_from_email -> {
        val keyDetailsList = result as ArrayList<NodeKeyDetails>?
        if (CollectionUtils.isEmpty(keyDetailsList)) {
          val account = AccountDao(sign!!.email!!, AccountDao.ACCOUNT_TYPE_GOOGLE)
          startActivityForResult(CreateOrImportKeyActivity.newIntent(this, account, true),
              REQUEST_CODE_CREATE_OR_IMPORT_KEY_FOR_GMAIL)
          UIUtil.exchangeViewVisibility(this, false, progressView!!, content!!)
        } else {
          val bottomTitle = resources.getQuantityString(R.plurals.found_backup_of_your_account_key,
              keyDetailsList!!.size, keyDetailsList.size)
          val neutralBtnTitle = if (SecurityUtils.hasBackup(this)) getString(R.string.use_existing_keys) else null
          val intent = CheckKeysActivity.newIntent(this, keyDetailsList, KeyDetails.Type.EMAIL, bottomTitle,
              getString(R.string.continue_), neutralBtnTitle, getString(R.string.use_another_account))
          startActivityForResult(intent, REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL)
        }
      }

      else -> super.onSuccess(loaderId, result)
    }
  }

  override fun onError(loaderId: Int, e: Exception?) {
    when (loaderId) {
      R.id.loader_id_load_private_key_backups_from_email -> {
        UIUtil.exchangeViewVisibility(this, false, progressView!!, content!!)
        showInfoSnackbar(rootView, if (e != null && !TextUtils.isEmpty(e.message))
          e.message
        else
          getString(R.string.unknown_error), Snackbar.LENGTH_LONG)
      }

      else -> super.onError(loaderId, e)
    }
  }

  private fun returnResultOk() {
    val accountDaoSource = saveGmailAccount()

    val intent = Intent()
    intent.putExtra(KEY_EXTRA_NEW_ACCOUNT, accountDaoSource.getActiveAccountInformation(this))

    setResult(Activity.RESULT_OK, intent)
    finish()
  }

  private fun saveGmailAccount(): AccountDaoSource {
    val accountDaoSource = AccountDaoSource()
    accountDaoSource.addRow(this, sign)
    accountDaoSource.setActiveAccount(this, sign!!.email)
    return accountDaoSource
  }

  companion object {

    val KEY_EXTRA_NEW_ACCOUNT = GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_NEW_ACCOUNT", AddNewAccountActivity::class.java)

    private const val REQUEST_CODE_CREATE_OR_IMPORT_KEY_FOR_GMAIL = 100
    private const val REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL = 101
  }
}
