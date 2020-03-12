/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.Toast
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.database.dao.source.ActionQueueDaoSource
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.service.actionqueue.actions.LoadGmailAliasesAction
import com.flowcrypt.email.ui.activity.base.BaseSignInActivity
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
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

class AddNewAccountActivity : BaseSignInActivity(), View.OnClickListener {

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
          accountDaoSource.setActiveAccount(this, authCreds?.email)

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
            privateKeysViewModel.encryptAndSaveKeysToDatabase(keys, KeyDetails.Type.EMAIL)
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
        }
      }

      REQUEST_CODE_CREATE_OR_IMPORT_KEY_FOR_GMAIL -> when (resultCode) {
        Activity.RESULT_OK -> returnResultOk()
      }

      else -> super.onActivityResult(requestCode, resultCode, data)
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
        googleSignInAccount?.let { loadPrivateKeysViewModel.fetchAvailableKeys(AccountDao(it, uuid, domainRules)) }
      }
    } else {
      UIUtil.exchangeViewVisibility(false, progressView, rootView)
      showInfoSnackbar(rootView, getString(R.string.template_email_alredy_added,
          this.googleSignInAccount!!.email), Snackbar.LENGTH_LONG)
    }
  }

  override fun onFetchKeysCompleted(keyDetailsList: ArrayList<NodeKeyDetails>?) {
    if (keyDetailsList.isNullOrEmpty()) {
      googleSignInAccount?.let {
        val account = AccountDao(it, uuid, domainRules)
        startActivityForResult(CreateOrImportKeyActivity.newIntent(this, account, true),
            REQUEST_CODE_CREATE_OR_IMPORT_KEY_FOR_GMAIL)
        UIUtil.exchangeViewVisibility(false, progressView, rootView)
      }
    } else {
      val subTitle = resources.getQuantityString(R.plurals.found_backup_of_your_account_key, keyDetailsList.size, keyDetailsList.size)
      val intent = CheckKeysActivity.newIntent(this, privateKeys = keyDetailsList, type = KeyDetails.Type.EMAIL, subTitle = subTitle,
          positiveBtnTitle = getString(R.string.continue_), negativeBtnTitle = getString(R.string.use_another_account))
      startActivityForResult(intent, REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL)
    }
  }

  override fun onPrivateKeysSaved() {
    returnResultOk()
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

  companion object {

    val KEY_EXTRA_NEW_ACCOUNT =
        GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_NEW_ACCOUNT", AddNewAccountActivity::class.java)

    private const val REQUEST_CODE_CREATE_OR_IMPORT_KEY_FOR_GMAIL = 100
    private const val REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL = 101
  }
}
