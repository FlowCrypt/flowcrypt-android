/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.flowcrypt.email.R
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.ui.activity.base.BasePassPhraseManagerActivity
import com.flowcrypt.email.ui.loader.ChangePassPhraseAsyncTaskLoader
import com.flowcrypt.email.ui.loader.LoadPrivateKeysFromMailAsyncTaskLoader
import com.flowcrypt.email.ui.loader.SaveBackupToInboxAsyncTaskLoader
import com.flowcrypt.email.util.UIUtil
import java.util.*

/**
 * This activity describes a logic of changing the pass phrase of all imported private keys of an active account.
 *
 * @author Denis Bondarenko
 * Date: 05.08.2018
 * Time: 20:15
 * E-mail: DenBond7@gmail.com
 */
class ChangePassPhraseActivity : BasePassPhraseManagerActivity(), LoaderManager.LoaderCallbacks<LoaderResult> {

  override fun onConfirmPassPhraseSuccess() {
    LoaderManager.getInstance(this).restartLoader(R.id.loader_id_change_pass_phrase, null, this)
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

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<LoaderResult> {
    return when (id) {
      R.id.loader_id_change_pass_phrase -> {
        isBackEnabled = false
        UIUtil.exchangeViewVisibility(this, true, layoutProgress, layoutContentView)
        ChangePassPhraseAsyncTaskLoader(this, account!!, editTextKeyPassword.text.toString())
      }

      R.id.loader_id_load_private_key_backups_from_email -> LoadPrivateKeysFromMailAsyncTaskLoader(this, account!!)

      R.id.loader_id_save_backup_to_inbox -> SaveBackupToInboxAsyncTaskLoader(this, account!!)

      else -> Loader(this)
    }
  }

  override fun onLoadFinished(loader: Loader<LoaderResult>, loaderResult: LoaderResult) {
    handleLoaderResult(loader, loaderResult)
  }

  override fun onLoaderReset(loader: Loader<LoaderResult>) {
    when (loader.id) {
      R.id.loader_id_change_pass_phrase,
      R.id.loader_id_load_private_key_backups_from_email,
      R.id.loader_id_save_backup_to_inbox -> isBackEnabled = true
    }
  }

  override fun onSuccess(loaderId: Int, result: Any?) {
    when (loaderId) {
      R.id.loader_id_change_pass_phrase -> {
        KeysStorageImpl.getInstance(this).refresh(this)
        LoaderManager.getInstance(this).initLoader(R.id.loader_id_load_private_key_backups_from_email, null, this)
      }

      R.id.loader_id_load_private_key_backups_from_email -> {
        val keyDetailsList = result as ArrayList<KeyDetails>?
        if (keyDetailsList!!.isEmpty()) {
          runBackupKeysActivity()
        } else {
          LoaderManager.getInstance(this).initLoader(R.id.loader_id_save_backup_to_inbox, null, this)
        }
      }

      R.id.loader_id_save_backup_to_inbox -> {
        isBackEnabled = true
        Toast.makeText(this, R.string.pass_phrase_changed, Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_OK)
        finish()
      }

      else -> super.onSuccess(loaderId, result)
    }
  }

  override fun onError(loaderId: Int, e: Exception?) {
    when (loaderId) {
      R.id.loader_id_change_pass_phrase -> {
        isBackEnabled = true
        editTextKeyPasswordSecond.text = null
        UIUtil.exchangeViewVisibility(this, false, layoutProgress, layoutContentView)
        showInfoSnackbar(rootView, e!!.message)
      }

      R.id.loader_id_load_private_key_backups_from_email, R.id.loader_id_save_backup_to_inbox -> runBackupKeysActivity()

      else -> super.onError(loaderId, e)
    }
  }

  private fun runBackupKeysActivity() {
    isBackEnabled = true
    Toast.makeText(this, R.string.back_up_updated_key, Toast.LENGTH_LONG).show()
    startActivityForResult(Intent(this, BackupKeysActivity::class.java), REQUEST_CODE_BACKUP_WITH_OPTION)
  }

  companion object {

    const val REQUEST_CODE_BACKUP_WITH_OPTION = 100

    fun newIntent(context: Context?, account: AccountDao?): Intent {
      val intent = Intent(context, ChangePassPhraseActivity::class.java)
      intent.putExtra(KEY_EXTRA_ACCOUNT_DAO, account)
      return intent
    }
  }
}
