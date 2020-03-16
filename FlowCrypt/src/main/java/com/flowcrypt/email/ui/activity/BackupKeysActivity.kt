/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Observer
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.test.espresso.idling.CountingIdlingResource
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.ui.activity.base.BaseSettingsBackStackSyncActivity
import com.flowcrypt.email.ui.loader.SavePrivateKeyAsFileAsyncTaskLoader
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.exception.DifferentPassPhrasesException
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.NoPrivateKeysAvailableException
import com.flowcrypt.email.util.exception.PrivateKeyStrengthException
import com.google.android.gms.common.util.CollectionUtils
import com.google.android.material.snackbar.Snackbar

/**
 * This activity helps to backup private keys
 *
 * @author Denis Bondarenko
 * Date: 07.08.2018
 * Time: 15:06
 * E-mail: DenBond7@gmail.com
 */
class BackupKeysActivity : BaseSettingsBackStackSyncActivity(), View.OnClickListener,
    RadioGroup.OnCheckedChangeListener, LoaderManager.LoaderCallbacks<LoaderResult> {

  private val privateKeysViewModel: PrivateKeysViewModel by viewModels()
  private var longIdsOfCurrentAccount: MutableList<String> = mutableListOf()

  @get:VisibleForTesting
  var countingIdlingResource: CountingIdlingResource? = null
    private set
  private var progressBar: View? = null
  override lateinit var rootView: View
  private var layoutSyncStatus: View? = null
  private var textViewOptionsHint: TextView? = null
  private var radioGroupBackupsVariants: RadioGroup? = null
  private var btnBackupAction: Button? = null

  private var destinationUri: Uri? = null
  private var account: AccountDao? = null

  private var isPrivateKeySendingNow: Boolean = false
  private var isPrivateKeySavingNow: Boolean = false

  override val contentViewResourceId: Int
    get() = R.layout.activity_backup_keys

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initViews()
    account = AccountDaoSource().getActiveAccountInformation(this)
    countingIdlingResource = CountingIdlingResource(GeneralUtil.genIdlingResourcesName(BackupKeysActivity::class.java),
        GeneralUtil.isDebugBuild())

    privateKeysViewModel.longIdsOfCurrentAccountLiveData.observe(this, Observer {
      longIdsOfCurrentAccount.clear()
      longIdsOfCurrentAccount.addAll(it)
    })
  }

  override fun onReplyReceived(requestCode: Int, resultCode: Int, obj: Any?) {
    when (requestCode) {
      R.id.syns_send_backup_with_private_key_to_key_owner -> {
        isPrivateKeySendingNow = false
        if (countingIdlingResource?.isIdleNow == false) {
          countingIdlingResource?.decrement()
        }
        setResult(Activity.RESULT_OK)
        finish()
      }
    }
  }

  override fun onErrorHappened(requestCode: Int, errorType: Int, e: Exception) {
    when (requestCode) {
      R.id.syns_send_backup_with_private_key_to_key_owner -> {
        isPrivateKeySendingNow = false
        if (countingIdlingResource?.isIdleNow == false) {
          countingIdlingResource?.decrement()
        }

        when (e) {
          is PrivateKeyStrengthException -> {
            UIUtil.exchangeViewVisibility(false, progressBar!!, rootView)
            showPassWeakHint()
          }

          is DifferentPassPhrasesException -> {
            UIUtil.exchangeViewVisibility(false, progressBar!!, rootView)
            showDifferentPassHint()
          }

          is NoPrivateKeysAvailableException -> {
            UIUtil.exchangeViewVisibility(false, progressBar!!, rootView)
            showInfoSnackbar(rootView, e.message, Snackbar.LENGTH_LONG)
          }

          else -> {
            UIUtil.exchangeViewVisibility(false, progressBar!!, rootView)
            showBackupingErrorHint()
          }
        }
      }
    }
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.buttonBackupAction -> {
        if (CollectionUtils.isEmpty(longIdsOfCurrentAccount)) {
          showInfoSnackbar(rootView, getString(R.string.there_are_no_private_keys,
              account!!.email), Snackbar.LENGTH_LONG)
        } else {
          when (radioGroupBackupsVariants!!.checkedRadioButtonId) {
            R.id.radioButtonEmail -> {
              dismissSnackBar()
              if (GeneralUtil.isConnected(this)) {
                countingIdlingResource?.increment()
                isPrivateKeySendingNow = true
                UIUtil.exchangeViewVisibility(true, progressBar!!, rootView)
                sendMsgWithPrivateKeyBackup(R.id.syns_send_backup_with_private_key_to_key_owner)
              } else {
                UIUtil.showInfoSnackbar(rootView, getString(R.string.internet_connection_is_not_available))
              }
            }

            R.id.radioButtonDownload -> {
              dismissSnackBar()
              destinationUri = null
              chooseDestForExportedKey()
            }
          }
        }
      }
    }
  }

  override fun onError(loaderId: Int, e: Exception?) {
    when (loaderId) {
      R.id.loader_id_save_private_key_as_file -> {
        isPrivateKeySavingNow = false
        when (e) {
          is PrivateKeyStrengthException -> {
            UIUtil.exchangeViewVisibility(false, progressBar!!, rootView)
            showPassWeakHint()
          }

          is DifferentPassPhrasesException -> {
            UIUtil.exchangeViewVisibility(false, progressBar!!, rootView)
            showDifferentPassHint()
          }

          else -> {
            UIUtil.exchangeViewVisibility(false, progressBar!!, rootView)
            showInfoSnackbar(rootView, e!!.message)
          }
        }
      }

      else -> super.onError(loaderId, e)
    }
  }

  override fun onBackPressed() {
    when {
      isPrivateKeySavingNow -> {
        LoaderManager.getInstance(this).destroyLoader(R.id.loader_id_validate_key_from_file)
        isPrivateKeySavingNow = false
        UIUtil.exchangeViewVisibility(false, progressBar!!, rootView)
      }

      isPrivateKeySendingNow -> Toast.makeText(this, R.string.please_wait_while_message_will_be_sent,
          Toast.LENGTH_SHORT).show()

      else -> super.onBackPressed()
    }
  }

  override fun onCheckedChanged(group: RadioGroup, @IdRes checkedId: Int) {
    when (group.id) {
      R.id.radioGroupBackupsVariants -> when (checkedId) {
        R.id.radioButtonEmail -> if (textViewOptionsHint != null) {
          textViewOptionsHint!!.setText(R.string.backup_as_email_hint)
          btnBackupAction!!.setText(R.string.backup_as_email)
        }

        R.id.radioButtonDownload -> if (textViewOptionsHint != null) {
          textViewOptionsHint!!.setText(R.string.backup_as_download_hint)
          btnBackupAction!!.setText(R.string.backup_as_a_file)
        }
      }
    }
  }

  public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    when (requestCode) {
      REQUEST_CODE_GET_URI_FOR_SAVING_PRIVATE_KEY -> when (resultCode) {
        Activity.RESULT_OK -> if (data != null && data.data != null) {
          try {
            destinationUri = data.data
            LoaderManager.getInstance(this).restartLoader(R.id.loader_id_save_private_key_as_file, null, this)
          } catch (e: Exception) {
            e.printStackTrace()
            ExceptionUtil.handleError(e)
            UIUtil.showInfoSnackbar(rootView, e.message ?: "")
          }

        }
      }

      REQUEST_CODE_RUN_CHANGE_PASS_PHRASE_ACTIVITY -> {
        layoutSyncStatus!!.visibility = View.GONE
        UIUtil.exchangeViewVisibility(false, progressBar!!, rootView)
      }
    }
  }

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<LoaderResult> {
    return when (id) {
      R.id.loader_id_save_private_key_as_file -> {
        isPrivateKeySavingNow = true
        UIUtil.exchangeViewVisibility(true, progressBar!!, rootView)
        SavePrivateKeyAsFileAsyncTaskLoader(applicationContext, account!!, destinationUri!!)
      }

      else -> Loader(this)
    }
  }

  override fun onLoadFinished(loader: Loader<LoaderResult>, loaderResult: LoaderResult) {
    handleLoaderResult(loader, loaderResult)
  }

  override fun onLoaderReset(loader: Loader<LoaderResult>) {
    when (loader.id) {
      R.id.loader_id_save_private_key_as_file -> {
        isPrivateKeySavingNow = false
        UIUtil.exchangeViewVisibility(false, progressBar!!, rootView)
      }
    }
  }

  override fun onSuccess(loaderId: Int, result: Any?) {
    when (loaderId) {
      R.id.loader_id_save_private_key_as_file -> {
        isPrivateKeySavingNow = false
        if (result as Boolean) {
          setResult(Activity.RESULT_OK)
          finish()
        } else {
          UIUtil.exchangeViewVisibility(false, progressBar!!, rootView)
          showInfoSnackbar(rootView, getString(R.string.error_occurred_please_try_again))
        }
      }

      else -> super.onSuccess(loaderId, result)
    }
  }

  private fun showBackupingErrorHint() {
    showSnackbar(rootView, getString(R.string.backup_was_not_sent), getString(R.string.retry),
        Snackbar.LENGTH_LONG, View.OnClickListener {
      layoutSyncStatus!!.visibility = View.GONE
      UIUtil.exchangeViewVisibility(true, progressBar!!, rootView)
      sendMsgWithPrivateKeyBackup(R.id.syns_send_backup_with_private_key_to_key_owner)
    })
  }

  private fun showDifferentPassHint() {
    showSnackbar(rootView, getString(R.string.different_pass_phrases), getString(R.string.fix),
        Snackbar.LENGTH_LONG, View.OnClickListener {
      startActivityForResult(ChangePassPhraseActivity.newIntent(this@BackupKeysActivity,
          account), REQUEST_CODE_RUN_CHANGE_PASS_PHRASE_ACTIVITY)
    })
  }

  private fun showPassWeakHint() {
    showSnackbar(rootView, getString(R.string.pass_phrase_is_too_weak),
        getString(R.string.change_pass_phrase), Snackbar.LENGTH_LONG, View.OnClickListener {
      startActivityForResult(ChangePassPhraseActivity.newIntent(this@BackupKeysActivity,
          account), REQUEST_CODE_RUN_CHANGE_PASS_PHRASE_ACTIVITY)
    })
  }

  private fun initViews() {
    this.progressBar = findViewById(R.id.progressBar)
    this.rootView = findViewById(R.id.layoutContent)
    this.layoutSyncStatus = findViewById(R.id.layoutSyncStatus)
    this.textViewOptionsHint = findViewById(R.id.textViewOptionsHint)
    this.radioGroupBackupsVariants = findViewById(R.id.radioGroupBackupsVariants)

    if (radioGroupBackupsVariants != null) {
      radioGroupBackupsVariants!!.setOnCheckedChangeListener(this)
    }

    btnBackupAction = findViewById(R.id.buttonBackupAction)
    if (btnBackupAction != null) {
      btnBackupAction!!.setOnClickListener(this)
    }
  }

  /**
   * Start a new Activity with return results to choose a destination for an exported key.
   */
  private fun chooseDestForExportedKey() {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    intent.type = Constants.MIME_TYPE_PGP_KEY
    intent.putExtra(Intent.EXTRA_TITLE, SecurityUtils.genPrivateKeyName(account!!.email))
    startActivityForResult(intent, REQUEST_CODE_GET_URI_FOR_SAVING_PRIVATE_KEY)
  }

  companion object {
    private const val REQUEST_CODE_GET_URI_FOR_SAVING_PRIVATE_KEY = 10
    private const val REQUEST_CODE_RUN_CHANGE_PASS_PHRASE_ACTIVITY = 11
  }
}
