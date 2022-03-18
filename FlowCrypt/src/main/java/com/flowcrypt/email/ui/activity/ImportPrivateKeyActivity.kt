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
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.showDialogFragment
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.BackupsViewModel
import com.flowcrypt.email.jetpack.viewmodel.SubmitPubKeyViewModel
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.model.KeyImportModel
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.model.PgpKeyDetails
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
class ImportPrivateKeyActivity : BaseImportKeyActivity(),
  TwoWayDialogFragment.OnTwoWayDialogListener {
  private val backupsViewModel: BackupsViewModel by viewModels()
  private var privateKeysFromEmailBackups = mutableListOf<PgpKeyDetails>()
  private val unlockedKeys: MutableList<PgpKeyDetails> = ArrayList()
  private var sourceType: KeyImportDetails.SourceType = KeyImportDetails.SourceType.EMAIL

  private var layoutSyncStatus: View? = null
  private var buttonImportBackup: Button? = null

  private val submitPubKeyViewModel: SubmitPubKeyViewModel by viewModels()

  override val contentViewResourceId: Int
    get() = R.layout.activity_import_private_key

  override val isPrivateKeyMode: Boolean = true

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (intent.getBooleanExtra(KEY_EXTRA_IS_SYNC_ENABLE, true) && GeneralUtil.isConnected(this)) {
      textViewProgressText.setText(R.string.loading_backups)
      initSearchBackupsInEmailViewModel()
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
    this.buttonImportBackup?.setOnClickListener(this)
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.buttonImportBackup -> {
        unlockedKeys.clear()
        if (!CollectionUtils.isEmpty(privateKeysFromEmailBackups)) {
          sourceType = KeyImportDetails.SourceType.EMAIL
          /*startActivityForResult(
            CheckKeysActivity.newIntent(
              context = this,
              privateKeys = ArrayList(privateKeysFromEmailBackups),
              sourceType = KeyImportDetails.SourceType.EMAIL,
              positiveBtnTitle = getString(R.string.continue_),
              negativeBtnTitle = getString(R.string.choose_another_key),
              skipImportedKeys = intent.getBooleanExtra(KEY_EXTRA_SKIP_IMPORTED_KEYS, false)
            ), REQUEST_CODE_CHECK_PRIVATE_KEYS
          )*/
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
            /*val keys: List<PgpKeyDetails>? = data?.getParcelableArrayListExtra(
              CheckKeysActivity.KEY_EXTRA_UNLOCKED_PRIVATE_KEYS
            )

            keys?.let {
              unlockedKeys.clear()
              unlockedKeys.addAll(it)
              if (intent?.getBooleanExtra(KEY_EXTRA_IS_SUBMITTING_PUB_KEYS_ENABLED, true) == true) {
                tempAccount?.let { accountEntity ->
                  submitPubKeyViewModel.submitPubKey(
                    accountEntity,
                    unlockedKeys
                  )
                }
              } else {
                handleSuccessSubmit()
              }
            }*/
          }

          /*CheckKeysActivity.RESULT_SKIP_REMAINING_KEYS -> {
            setResult(CheckKeysActivity.RESULT_SKIP_REMAINING_KEYS, data)
            finish()
          }*/
        }
      }
      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onKeyFound(
    sourceType: KeyImportDetails.SourceType,
    keyDetailsList: List<PgpKeyDetails>
  ) {
    var areFreshKeysExisted = false
    var arePrivateKeysExisted = false

    for (key in keyDetailsList) {
      if (key.isPrivate) {
        arePrivateKeysExisted = true
      }

      val fingerprint = key.fingerprint
      if (KeysStorageImpl.getInstance(application)
          .getPGPSecretKeyRingByFingerprint(fingerprint) == null
      ) {
        areFreshKeysExisted = true
      }
    }

    if (!arePrivateKeysExisted) {
      showInfoSnackbar(
        rootView, getString(
          R.string.file_has_wrong_pgp_structure, getString(
            R
              .string.private_
          )
        ), Snackbar.LENGTH_LONG
      )
      return
    }

    if (!areFreshKeysExisted) {
      if (intent?.getBooleanExtra(KEY_EXTRA_IS_SUBMITTING_PUB_KEYS_ENABLED, true) == true) {
        unlockedKeys.addAll(keyDetailsList)
        tempAccount?.let { accountEntity ->
          submitPubKeyViewModel.submitPubKey(
            accountEntity,
            unlockedKeys
          )
        }
        Toast.makeText(
          this, getString(R.string.key_already_imported_finishing_setup), Toast
            .LENGTH_SHORT
        ).show()
      } else {
        showInfoSnackbar(rootView, getString(R.string.the_key_already_added), Snackbar.LENGTH_LONG)
      }
      return
    }

    when (sourceType) {
      KeyImportDetails.SourceType.FILE -> {
        this.sourceType = KeyImportDetails.SourceType.FILE
        val fileName = GeneralUtil.getFileNameFromUri(this, keyImportModel!!.fileUri)
        val bottomTitle = resources.getQuantityString(
          R.plurals.file_contains_some_amount_of_keys,
          keyDetailsList.size, fileName, keyDetailsList.size
        )
        val posBtnTitle = getString(R.string.continue_)
        /*val intent = CheckKeysActivity.newIntent(
          context = this,
          privateKeys = ArrayList(keyDetailsList),
          sourceType = sourceType,
          subTitle = bottomTitle,
          positiveBtnTitle = posBtnTitle,
          negativeBtnTitle = getString
            (R.string.choose_another_key),
          isExtraImportOpt = true,
          skipImportedKeys = intent.getBooleanExtra(KEY_EXTRA_SKIP_IMPORTED_KEYS, false)
        )
        startActivityForResult(intent, REQUEST_CODE_CHECK_PRIVATE_KEYS)*/
      }

      KeyImportDetails.SourceType.CLIPBOARD -> {
        this.sourceType = KeyImportDetails.SourceType.CLIPBOARD
        val title = resources.getQuantityString(
          R.plurals.loaded_private_keys_from_clipboard,
          keyDetailsList.size, keyDetailsList.size
        )
        /*val clipboardIntent = CheckKeysActivity.newIntent(
          context = this,
          privateKeys = ArrayList(keyDetailsList),
          sourceType = sourceType,
          subTitle = title,
          positiveBtnTitle = getString(R.string.continue_),
          negativeBtnTitle = getString(
            R
              .string.choose_another_key
          ),
          isExtraImportOpt = true,
          skipImportedKeys = intent.getBooleanExtra(KEY_EXTRA_SKIP_IMPORTED_KEYS, false)
        )
        startActivityForResult(
          clipboardIntent,
          REQUEST_CODE_CHECK_PRIVATE_KEYS
        )*/
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
            tempAccount?.let { accountEntity ->
              submitPubKeyViewModel.submitPubKey(
                accountEntity,
                unlockedKeys
              )
            }
          }

          TwoWayDialogFragment.RESULT_CANCELED -> {
            unlockedKeys.clear()
          }
        }
      }
    }
  }

  private fun hideImportButton() {
    buttonImportBackup?.visibility = View.GONE
    val marginLayoutParams = buttonLoadFromFile.layoutParams as ViewGroup.MarginLayoutParams
    marginLayoutParams.topMargin = resources.getDimensionPixelSize(
      R.dimen
        .margin_top_first_button
    )
    buttonLoadFromFile.requestLayout()
  }

  private fun filterKeys(): Set<String> {
    val connector = KeysStorageImpl.getInstance(this)

    val iterator = privateKeysFromEmailBackups.iterator()
    val uniqueKeysFingerprints = HashSet<String>()

    while (iterator.hasNext()) {
      val pgpKeyDetails = iterator.next()
      uniqueKeysFingerprints.add(pgpKeyDetails.fingerprint)
      if (connector.getPGPSecretKeyRingByFingerprint(pgpKeyDetails.fingerprint) != null) {
        iterator.remove()
        uniqueKeysFingerprints.remove(pgpKeyDetails.fingerprint)
      }
    }
    return uniqueKeysFingerprints
  }

  private fun setupSubmitPubKeyViewModel() {
    submitPubKeyViewModel.submitPubKeyLiveData.observe(this, {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource.incrementSafely()
            textViewProgressText.setText(R.string.submitting_pub_key)
            UIUtil.exchangeViewVisibility(true, layoutProgress, layoutContentView)
          }

          Result.Status.SUCCESS -> {
            handleSuccessSubmit()
            countingIdlingResource.decrementSafely()
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            UIUtil.exchangeViewVisibility(false, layoutProgress, layoutContentView)
            val msg = when (it.status) {
              Result.Status.ERROR -> {
                it.data?.apiError?.msg ?: getString(R.string.unknown_error)
              }

              Result.Status.EXCEPTION -> {
                it.exception?.message ?: it.exception?.javaClass?.simpleName
                ?: getString(R.string.unknown_error)
              }

              else -> it.exception?.javaClass?.simpleName ?: getString(R.string.unknown_error)
            }

            showDialogFragment(
              TwoWayDialogFragment.newInstance(
                requestCode = REQUEST_CODE_SHOW_SUBMIT_ERROR_DIALOG,
                dialogTitle = "",
                dialogMsg = msg,
                positiveButtonTitle = getString(R.string.retry),
                negativeButtonTitle = getString(R.string.cancel),
                isCancelable = false
              )
            )
            countingIdlingResource.decrementSafely()
          }
        }
      }
    })
  }

  private fun setupPrivateKeysViewModel() {
    privateKeysViewModel.savePrivateKeysLiveData.observe(this, {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource.incrementSafely()
            UIUtil.exchangeViewVisibility(true, layoutProgress, layoutContentView)
          }

          Result.Status.SUCCESS -> {
            setResult(Activity.RESULT_OK)
            countingIdlingResource.decrementSafely()
            finish()
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            UIUtil.exchangeViewVisibility(false, layoutProgress, layoutContentView)
            val e = it.exception
            if (e is SavePrivateKeyToDatabaseException) {
              showSnackbar(
                rootView, e.message ?: e.javaClass.simpleName,
                getString(R.string.retry), Snackbar.LENGTH_INDEFINITE
              ) {
                privateKeysViewModel.encryptAndSaveKeysToDatabase(
                  tempAccount,
                  e.keys
                  //, KeyImportDetails.SourceType.EMAIL
                )
              }
            } else {
              showInfoSnackbar(
                rootView, e?.message ?: e?.javaClass?.simpleName
                ?: getString(R.string.unknown_error)
              )
            }
            countingIdlingResource.decrementSafely()
          }
        }
      }
    })
  }

  private fun initSearchBackupsInEmailViewModel() {
    backupsViewModel.onlineBackupsLiveData.observe(this, {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource.incrementSafely()
          UIUtil.exchangeViewVisibility(true, layoutProgress, layoutContentView)
        }

        Result.Status.SUCCESS -> {
          val keys = it.data
          privateKeysFromEmailBackups.clear()
          if (keys != null) {
            if (keys.isNotEmpty()) {
              privateKeysFromEmailBackups.addAll(keys)

              val uniqueKeysFingerprints = filterKeys()

              if (privateKeysFromEmailBackups.isEmpty()) {
                hideImportButton()
              } else {
                buttonImportBackup?.text =
                  resources.getQuantityString(R.plurals.import_keys, uniqueKeysFingerprints.size)
                textViewTitle.text = resources.getQuantityString(
                  R.plurals.you_have_backups_that_was_not_imported,
                  uniqueKeysFingerprints.size
                )
              }
            } else {
              hideImportButton()
            }
          } else {
            hideImportButton()
          }
          UIUtil.exchangeViewVisibility(false, layoutProgress, layoutContentView)
          countingIdlingResource.decrementSafely()
        }

        Result.Status.EXCEPTION -> {
          hideImportButton()
          toast(R.string.error_occurred_while_receiving_private_keys)
          UIUtil.exchangeViewVisibility(false, layoutProgress, layoutContentView)
          countingIdlingResource.decrementSafely()
        }

        else -> {
          countingIdlingResource.decrementSafely()
        }
      }
    })
  }

  private fun handleSuccessSubmit() {
    textViewProgressText.setText(R.string.saving_prv_keys)
    privateKeysViewModel.encryptAndSaveKeysToDatabase(
      tempAccount,
      unlockedKeys,
      //sourceType,
      intent.getBooleanExtra(KEY_EXTRA_ADD_ACCOUNT_IF_NOT_EXIST, false)
    )
  }

  companion object {
    private const val REQUEST_CODE_CHECK_PRIVATE_KEYS = 100
    private const val REQUEST_CODE_SHOW_SUBMIT_ERROR_DIALOG = 101

    private val KEY_EXTRA_IS_SUBMITTING_PUB_KEYS_ENABLED = GeneralUtil.generateUniqueExtraKey(
      "KEY_EXTRA_IS_SUBMITTING_PUB_KEYS_ENABLED", ImportPrivateKeyActivity::class.java
    )
    private val KEY_EXTRA_SKIP_IMPORTED_KEYS = GeneralUtil.generateUniqueExtraKey(
      "KEY_EXTRA_SKIP_IMPORTED_KEYS", ImportPrivateKeyActivity::class.java
    )
    private val KEY_EXTRA_ADD_ACCOUNT_IF_NOT_EXIST = GeneralUtil.generateUniqueExtraKey(
      "KEY_EXTRA_ADD_ACCOUNT_IF_NOT_EXIST", ImportPrivateKeyActivity::class.java
    )

    fun getIntent(
      context: Context?, accountEntity: AccountEntity?, isSyncEnabled: Boolean = false,
      title: String, model: KeyImportModel? = null,
      throwErrorIfDuplicateFoundEnabled: Boolean = false, cls: Class<*>,
      isSubmittingPubKeysEnabled: Boolean = true,
      skipImportedKeys: Boolean = false,
      addAccountIfNotExist: Boolean = false
    ): Intent {
      val intent = newIntent(
        context = context,
        accountEntity = accountEntity,
        isSyncEnabled = isSyncEnabled,
        title = title,
        model = model,
        throwErrorIfDuplicateFoundEnabled = throwErrorIfDuplicateFoundEnabled,
        cls = cls
      )
      intent.putExtra(KEY_EXTRA_IS_SUBMITTING_PUB_KEYS_ENABLED, isSubmittingPubKeysEnabled)
      intent.putExtra(KEY_EXTRA_SKIP_IMPORTED_KEYS, skipImportedKeys)
      intent.putExtra(KEY_EXTRA_ADD_ACCOUNT_IF_NOT_EXIST, addAccountIfNotExist)
      return intent
    }
  }
}
