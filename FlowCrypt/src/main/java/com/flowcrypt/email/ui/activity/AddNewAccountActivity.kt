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
import com.flowcrypt.email.database.dao.source.ActionQueueDaoSource
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.service.actionqueue.actions.LoadGmailAliasesAction
import com.flowcrypt.email.ui.activity.base.BaseSignInActivity
import com.flowcrypt.email.ui.loader.LoadPrivateKeysFromMailAsyncTaskLoader
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
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

class AddNewAccountActivity : BaseSignInActivity(), View.OnClickListener,
    LoaderManager.LoaderCallbacks<LoaderResult> {

  override val isDisplayHomeAsUpEnabled: Boolean
    get() = true

  override val contentViewResourceId: Int
    get() = R.layout.activity_add_new_account

  override val rootView: View
    get() = findViewById(R.id.layoutContent)

  override val progressView: View
    get() = findViewById(R.id.progressView)

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
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

        AddNewAccountManuallyActivity.RESULT_CODE_CONTINUE_WITH_GMAIL ->
          super.onActivityResult(requestCode, resultCode, data)
      }

      REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL -> when (resultCode) {
        Activity.RESULT_OK, CheckKeysActivity.RESULT_SKIP_REMAINING_KEYS -> {
          val keys: List<NodeKeyDetails>? = data?.getParcelableArrayListExtra(
              CheckKeysActivity.KEY_EXTRA_UNLOCKED_PRIVATE_KEYS)

          if (keys.isNullOrEmpty()) {
            showInfoSnackbar(rootView, getString(R.string.unknown_error))
          } else {
            saveKeysAndReturnOkResult(keys)
          }
        }

        CheckKeysActivity.RESULT_USE_EXISTING_KEYS -> {
          returnResultOk()
        }

        CheckKeysActivity.RESULT_NO_NEW_KEYS -> {
          Toast.makeText(this, getString(R.string.key_already_imported_finishing_setup), Toast
              .LENGTH_SHORT).show()
          returnResultOk()
        }

        Activity.RESULT_CANCELED, CheckKeysActivity.RESULT_NEGATIVE -> {
          UIUtil.exchangeViewVisibility(false, progressView, rootView)
          LoaderManager.getInstance(this).destroyLoader(R.id.loader_id_load_private_key_backups_from_email)
        }
      }

      REQUEST_CODE_CREATE_OR_IMPORT_KEY_FOR_GMAIL -> when (resultCode) {
        Activity.RESULT_OK -> returnResultOk()

        Activity.RESULT_CANCELED,
        CreateOrImportKeyActivity.RESULT_CODE_USE_ANOTHER_ACCOUNT ->
          LoaderManager.getInstance(this).destroyLoader(R.id.loader_id_load_private_key_backups_from_email)
      }

      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<LoaderResult> {
    return when (id) {
      R.id.loader_id_load_private_key_backups_from_email -> {
        UIUtil.exchangeViewVisibility(true, progressView, rootView)
        val account = AccountDao(googleSignInAccount!!, uuid, domainRules)
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
      R.id.loader_id_load_private_key_backups_from_email -> this.googleSignInAccount = null
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun onSuccess(loaderId: Int, result: Any?) {
    when (loaderId) {
      R.id.loader_id_load_private_key_backups_from_email -> {
        val keyDetailsList = result as ArrayList<NodeKeyDetails>?
        if (CollectionUtils.isEmpty(keyDetailsList)) {
          val account = AccountDao(googleSignInAccount!!, uuid, domainRules)
          startActivityForResult(CreateOrImportKeyActivity.newIntent(this, account, true),
              REQUEST_CODE_CREATE_OR_IMPORT_KEY_FOR_GMAIL)
          UIUtil.exchangeViewVisibility(false, progressView, rootView)
        } else {
          val subTitle = resources.getQuantityString(R.plurals.found_backup_of_your_account_key,
              keyDetailsList!!.size, keyDetailsList.size)
          val intent = CheckKeysActivity.newIntent(this, privateKeys = keyDetailsList, type = KeyDetails.Type.EMAIL, subTitle = subTitle,
              positiveBtnTitle = getString(R.string.continue_), negativeBtnTitle = getString(R.string.use_another_account))
          startActivityForResult(intent, REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL)
        }
      }

      else -> super.onSuccess(loaderId, result)
    }
  }

  override fun onError(loaderId: Int, e: Exception?) {
    when (loaderId) {
      R.id.loader_id_load_private_key_backups_from_email -> {
        UIUtil.exchangeViewVisibility(false, progressView, rootView)
        showInfoSnackbar(rootView, if (e != null && !TextUtils.isEmpty(e.message))
          e.message
        else
          getString(R.string.unknown_error), Snackbar.LENGTH_LONG)
      }

      else -> super.onError(loaderId, e)
    }
  }

  override fun onSignSuccess(googleSignInAccount: GoogleSignInAccount?) {
    if (AccountDaoSource().getAccountInformation(this, this.googleSignInAccount!!.email!!) == null) {
      if (domainRules?.contains(AccountDao.DomainRule.NO_PRV_BACKUP.name) == true) {
        val account = AccountDao(googleSignInAccount!!, uuid, domainRules)
        startActivityForResult(CreateOrImportKeyActivity.newIntent(this, account, true),
            REQUEST_CODE_CREATE_OR_IMPORT_KEY_FOR_GMAIL)
        UIUtil.exchangeViewVisibility(false, progressView, rootView)
      } else {
        LoaderManager.getInstance(this)
            .restartLoader(R.id.loader_id_load_private_key_backups_from_email, null, this)
      }
    } else {
      UIUtil.exchangeViewVisibility(false, progressView, rootView)
      showInfoSnackbar(rootView, getString(R.string.template_email_alredy_added,
          this.googleSignInAccount!!.email), Snackbar.LENGTH_LONG)
    }
  }

  private fun returnResultOk() {
    val accountDaoSource = saveGmailAccount()
    val account = accountDaoSource.getActiveAccountInformation(this)
    ActionQueueDaoSource().addAction(this, LoadGmailAliasesAction(email = account?.email))

    val intent = Intent()
    intent.putExtra(KEY_EXTRA_NEW_ACCOUNT, account)

    setResult(Activity.RESULT_OK, intent)
    finish()
  }

  private fun saveGmailAccount(): AccountDaoSource {
    val accountDaoSource = AccountDaoSource()
    accountDaoSource.addRow(this, googleSignInAccount, uuid, domainRules)
    accountDaoSource.setActiveAccount(this, googleSignInAccount!!.email)
    return accountDaoSource
  }

  private fun saveKeysAndReturnOkResult(keys: List<NodeKeyDetails>) {
    try {
      SecurityUtils.encryptAndSaveKeysToDatabase(this, keys, KeyDetails.Type.EMAIL)
      returnResultOk()
    } catch (e: java.lang.Exception) {
      showSnackbar(rootView, e.message ?: getString(R.string.unknown_error),
          getString(R.string.retry), Snackbar.LENGTH_INDEFINITE, View.OnClickListener {
        saveKeysAndReturnOkResult(keys)
      })
    }
  }

  companion object {

    val KEY_EXTRA_NEW_ACCOUNT =
        GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_NEW_ACCOUNT", AddNewAccountActivity::class.java)

    private const val REQUEST_CODE_CREATE_OR_IMPORT_KEY_FOR_GMAIL = 100
    private const val REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL = 101
  }
}
