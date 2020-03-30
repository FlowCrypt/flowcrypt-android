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
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Observer
import androidx.test.espresso.idling.CountingIdlingResource
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.extensions.showDialogFragment
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.jetpack.viewmodel.SubmitPubKeyViewModel
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.model.KeyImportModel
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.ui.activity.base.BaseImportKeyActivity
import com.flowcrypt.email.ui.activity.fragment.dialog.TwoWayDialogFragment
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.exception.SavePrivateKeyToDatabaseException
import com.google.android.gms.common.util.CollectionUtils
import com.google.android.material.snackbar.Snackbar

/**
 * This activity describes a logic of import private keys.
 *
 * @author Denis Bondarenko
 * Date: 20.07.2017
 * Time: 16:59
 * E-mail: DenBond7@gmail.com
 */

class ImportPrivateKeyActivity : BaseImportKeyActivity(), TwoWayDialogFragment.OnTwoWayDialogListener {
  @get:VisibleForTesting
  var countingIdlingResource: CountingIdlingResource? = null
    private set
  private var privateKeysFromEmailBackups: ArrayList<NodeKeyDetails>? = null
  private val unlockedKeys: MutableList<NodeKeyDetails> = ArrayList()
  private var keyDetailsType: KeyDetails.Type = KeyDetails.Type.EMAIL

  private var layoutSyncStatus: View? = null
  private var buttonImportBackup: Button? = null

  private var isLoadPrivateKeysRequestSent: Boolean = false

  private val submitPubKeyViewModel: SubmitPubKeyViewModel by viewModels()
  private val privateKeysViewModel: PrivateKeysViewModel by viewModels()

  override val contentViewResourceId: Int
    get() = R.layout.activity_import_private_key

  override val isPrivateKeyMode: Boolean
    get() = true

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (isSyncEnabled && GeneralUtil.isConnected(this)) {
      textViewProgressText.setText(R.string.loading_backups)
      UIUtil.exchangeViewVisibility(true, layoutProgress, layoutContentView)
      countingIdlingResource = CountingIdlingResource(
          GeneralUtil.genIdlingResourcesName(ImportPrivateKeyActivity::class.java), GeneralUtil.isDebugBuild())
    } else {
      hideImportButton()
      UIUtil.exchangeViewVisibility(false, layoutProgress, layoutContentView)
    }

    setupSubmitPubKeyViewModel()
    setupPrivateKeysViewModel()
  }

  override fun initViews() {
    super.initViews()
    this.layoutSyncStatus = findViewById(R.id.layoutSyncStatus)
    this.buttonImportBackup = findViewById(R.id.buttonImportBackup)
    this.buttonImportBackup!!.setOnClickListener(this)
  }

  override fun onSyncServiceConnected() {
    if (!isLoadPrivateKeysRequestSent) {
      isLoadPrivateKeysRequestSent = true
      loadPrivateKeys(R.id.syns_load_private_keys)

      countingIdlingResource?.increment()
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun onReplyReceived(requestCode: Int, resultCode: Int, obj: Any?) {
    when (requestCode) {
      R.id.syns_load_private_keys -> {
        if (privateKeysFromEmailBackups == null) {
          val keys = obj as ArrayList<NodeKeyDetails>?
          if (keys != null) {
            if (keys.isNotEmpty()) {
              this.privateKeysFromEmailBackups = keys

              val uniqueKeysLongIds = filterKeys()

              if (this.privateKeysFromEmailBackups!!.isEmpty()) {
                hideImportButton()
              } else {
                buttonImportBackup!!.text = resources.getQuantityString(
                    R.plurals.import_keys, uniqueKeysLongIds.size)
                textViewTitle.text = resources.getQuantityString(
                    R.plurals.you_have_backups_that_was_not_imported, uniqueKeysLongIds.size)
              }
            } else {
              hideImportButton()
            }
          } else {
            hideImportButton()
          }
          UIUtil.exchangeViewVisibility(false, layoutProgress, layoutContentView)
        }
        if (countingIdlingResource?.isIdleNow == false) {
          countingIdlingResource?.decrement()
        }
      }
    }
  }

  override fun onErrorHappened(requestCode: Int, errorType: Int, e: Exception) {
    when (requestCode) {
      R.id.syns_load_private_keys -> {
        hideImportButton()
        UIUtil.exchangeViewVisibility(false, layoutProgress, layoutSyncStatus)
        UIUtil.showSnackbar(rootView, getString(R.string.error_occurred_while_receiving_private_keys),
            getString(android.R.string.ok), View.OnClickListener {
          layoutSyncStatus?.visibility = View.GONE
          UIUtil.exchangeViewVisibility(
              false, layoutProgress, layoutContentView)
        })

        if (countingIdlingResource?.isIdleNow == false) {
          countingIdlingResource?.decrement()
        }
      }
    }
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.buttonImportBackup -> {
        unlockedKeys.clear()
        if (!CollectionUtils.isEmpty(privateKeysFromEmailBackups)) {
          keyDetailsType = KeyDetails.Type.EMAIL
          startActivityForResult(CheckKeysActivity.newIntent(this, privateKeys = privateKeysFromEmailBackups!!, type = KeyDetails.Type.EMAIL,
              positiveBtnTitle = getString(R.string.continue_), negativeBtnTitle = getString(R
              .string.choose_another_key), isUseExistingKeysEnabled = intent?.getBooleanExtra
          (KEY_EXTRA_IS_USE_EXISTING_KEYS_ENABLED, true) == true), REQUEST_CODE_CHECK_PRIVATE_KEYS)
        }
      }

      else -> {
        when (v.id) {
          R.id.buttonLoadFromFile, R.id.buttonLoadFromClipboard -> unlockedKeys.clear()
        }
        super.onClick(v)
      }
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_CHECK_PRIVATE_KEYS -> {
        isCheckingClipboardEnabled = false

        when (resultCode) {
          Activity.RESULT_OK -> {
            val keys: List<NodeKeyDetails>? = data?.getParcelableArrayListExtra(
                CheckKeysActivity.KEY_EXTRA_UNLOCKED_PRIVATE_KEYS)

            keys?.let {
              unlockedKeys.clear()
              unlockedKeys.addAll(it)
              if (intent?.getBooleanExtra(KEY_EXTRA_IS_SUBMITTING_PUB_KEYS_ENABLED, true) == true) {
                account?.let { accountDao -> submitPubKeyViewModel.submitPubKey(accountDao, unlockedKeys) }
              } else {
                privateKeysViewModel.encryptAndSaveKeysToDatabase(keys, KeyDetails.Type.EMAIL)
                handleSuccessSubmit()
              }
            }
          }
        }
      }
      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onKeyFound(type: KeyDetails.Type, keyDetailsList: ArrayList<NodeKeyDetails>) {
    var areFreshKeysExisted = false
    var arePrivateKeysExisted = false

    for (key in keyDetailsList) {
      if (key.isPrivate) {
        arePrivateKeysExisted = true
      }

      val longId = key.longId ?: continue
      if (KeysStorageImpl.getInstance(application).getPgpPrivateKey(longId) == null) {
        areFreshKeysExisted = true
      }
    }

    if (!arePrivateKeysExisted) {
      showInfoSnackbar(rootView, getString(R.string.file_has_wrong_pgp_structure, getString(R
          .string.private_)), Snackbar.LENGTH_LONG)
      return
    }

    if (!areFreshKeysExisted) {
      if (intent?.getBooleanExtra(KEY_EXTRA_IS_SUBMITTING_PUB_KEYS_ENABLED, true) == true) {
        unlockedKeys.addAll(keyDetailsList)
        account?.let { accountDao -> submitPubKeyViewModel.submitPubKey(accountDao, unlockedKeys) }
        Toast.makeText(this, getString(R.string.key_already_imported_finishing_setup), Toast
            .LENGTH_SHORT).show()
      } else {
        showInfoSnackbar(rootView, getString(R.string.the_key_already_added), Snackbar.LENGTH_LONG)
      }
      return
    }

    when (type) {
      KeyDetails.Type.FILE -> {
        keyDetailsType = KeyDetails.Type.FILE
        val fileName = GeneralUtil.getFileNameFromUri(this, keyImportModel!!.fileUri)
        val bottomTitle = resources.getQuantityString(R.plurals.file_contains_some_amount_of_keys,
            keyDetailsList.size, fileName, keyDetailsList.size)
        val posBtnTitle = getString(R.string.continue_)
        val intent = CheckKeysActivity.newIntent(this, privateKeys = keyDetailsList, type = keyDetailsType,
            subTitle = bottomTitle, positiveBtnTitle = posBtnTitle, negativeBtnTitle = getString
        (R.string.choose_another_key), isExtraImportOpt = true, isUseExistingKeysEnabled = intent?.getBooleanExtra
        (KEY_EXTRA_IS_USE_EXISTING_KEYS_ENABLED, true) == true)
        startActivityForResult(intent, REQUEST_CODE_CHECK_PRIVATE_KEYS)
      }

      KeyDetails.Type.CLIPBOARD -> {
        keyDetailsType = KeyDetails.Type.CLIPBOARD
        val title = resources.getQuantityString(R.plurals.loaded_private_keys_from_clipboard,
            keyDetailsList.size, keyDetailsList.size)
        val clipboardIntent = CheckKeysActivity.newIntent(this, keyDetailsList, type = keyDetailsType, subTitle = title,
            positiveBtnTitle = getString(R.string.continue_), negativeBtnTitle = getString(R
            .string.choose_another_key), isExtraImportOpt = true, isUseExistingKeysEnabled = intent?.getBooleanExtra
        (KEY_EXTRA_IS_USE_EXISTING_KEYS_ENABLED, true) == true)
        startActivityForResult(clipboardIntent,
            REQUEST_CODE_CHECK_PRIVATE_KEYS)
      }

      else -> {
      }
    }
  }

  override fun onDialogButtonClick(requestCode: Int, result: Int) {
    when (requestCode) {
      REQUEST_CODE_SHOW_SUBMIT_ERROR_DIALOG -> {
        when (result) {
          TwoWayDialogFragment.RESULT_OK -> {
            account?.let { accountDao -> submitPubKeyViewModel.submitPubKey(accountDao, unlockedKeys) }
          }

          TwoWayDialogFragment.RESULT_CANCELED -> {
            unlockedKeys.clear()
          }
        }
      }
    }
  }

  private fun hideImportButton() {
    buttonImportBackup!!.visibility = View.GONE
    val marginLayoutParams = buttonLoadFromFile
        .layoutParams as ViewGroup.MarginLayoutParams
    marginLayoutParams.topMargin = resources.getDimensionPixelSize(R.dimen
        .margin_top_first_button)
    buttonLoadFromFile.requestLayout()
  }

  private fun filterKeys(): Set<String> {
    val connector = KeysStorageImpl.getInstance(this)

    val iterator = privateKeysFromEmailBackups!!.iterator()
    val uniqueKeysLongIds = HashSet<String>()

    while (iterator.hasNext()) {
      val privateKey = iterator.next()
      uniqueKeysLongIds.add(privateKey.longId!!)
      if (connector.getPgpPrivateKey(privateKey.longId!!) != null) {
        iterator.remove()
        uniqueKeysLongIds.remove(privateKey.longId!!)
      }
    }
    return uniqueKeysLongIds
  }

  private fun setupSubmitPubKeyViewModel() {
    submitPubKeyViewModel.submitPubKeyLiveData.observe(this, Observer<Result<ApiResponse>?> {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            textViewProgressText.setText(R.string.submitting_pub_key)
            UIUtil.exchangeViewVisibility(true, layoutProgress, layoutContentView)
          }

          Result.Status.SUCCESS -> {
            handleSuccessSubmit()
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            UIUtil.exchangeViewVisibility(false, layoutProgress, layoutContentView)
            val msg = when (it.status) {
              Result.Status.ERROR -> {
                it.data?.apiError?.msg ?: getString(R.string.unknown_error)
              }

              Result.Status.SUCCESS -> {
                it.exception?.message ?: it.exception?.javaClass?.simpleName
                ?: getString(R.string.unknown_error)
              }

              else -> it.exception?.javaClass?.simpleName ?: getString(R.string.unknown_error)
            }

            showDialogFragment(TwoWayDialogFragment.newInstance(requestCode = REQUEST_CODE_SHOW_SUBMIT_ERROR_DIALOG,
                dialogTitle = "",
                dialogMsg = msg,
                positiveButtonTitle = getString(R.string.retry),
                negativeButtonTitle = getString(R.string.cancel),
                isCancelable = false))
          }
        }
      }
    })
  }

  private fun setupPrivateKeysViewModel() {
    privateKeysViewModel.savePrivateKeysLiveData.observe(this, Observer {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            UIUtil.exchangeViewVisibility(true, layoutProgress, layoutContentView)
          }

          Result.Status.SUCCESS -> {
            setResult(Activity.RESULT_OK)
            finish()
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            UIUtil.exchangeViewVisibility(false, layoutProgress, layoutContentView)
            val e = it.exception
            if (e is SavePrivateKeyToDatabaseException) {
              showSnackbar(rootView, e.message ?: e.javaClass.simpleName,
                  getString(R.string.retry), Snackbar.LENGTH_INDEFINITE, View.OnClickListener {
                privateKeysViewModel.encryptAndSaveKeysToDatabase(e.keys, KeyDetails.Type.EMAIL)
              })
            } else {
              showInfoSnackbar(rootView, e?.message ?: e?.javaClass?.simpleName
              ?: getString(R.string.unknown_error))
            }
          }
        }
      }
    })
  }

  private fun handleSuccessSubmit() {
    textViewProgressText.setText(R.string.saving_prv_keys)
    privateKeysViewModel.encryptAndSaveKeysToDatabase(unlockedKeys, keyDetailsType)
  }

  companion object {
    private const val REQUEST_CODE_CHECK_PRIVATE_KEYS = 100
    private const val REQUEST_CODE_SHOW_SUBMIT_ERROR_DIALOG = 101

    val KEY_EXTRA_IS_USE_EXISTING_KEYS_ENABLED = GeneralUtil.generateUniqueExtraKey(
        "KEY_EXTRA_IS_USE_EXISTING_KEYS_ENABLED", ImportPrivateKeyActivity::class.java)
    val KEY_EXTRA_IS_SUBMITTING_PUB_KEYS_ENABLED = GeneralUtil.generateUniqueExtraKey(
        "KEY_EXTRA_IS_SUBMITTING_PUB_KEYS_ENABLED", ImportPrivateKeyActivity::class.java)

    fun getIntent(context: Context?, accountDao: AccountDao, isSyncEnabled: Boolean = false,
                  title: String, model: KeyImportModel? = null,
                  throwErrorIfDuplicateFoundEnabled: Boolean = false, cls: Class<*>,
                  isUseExistingKeysEnabled: Boolean = true,
                  isSubmittingPubKeysEnabled: Boolean = true): Intent {
      val intent = newIntent(context, accountDao, isSyncEnabled, title, model, throwErrorIfDuplicateFoundEnabled, cls)
      intent.putExtra(KEY_EXTRA_IS_USE_EXISTING_KEYS_ENABLED, isUseExistingKeysEnabled)
      intent.putExtra(KEY_EXTRA_IS_SUBMITTING_PUB_KEYS_ENABLED, isSubmittingPubKeysEnabled)
      return intent
    }
  }
}
