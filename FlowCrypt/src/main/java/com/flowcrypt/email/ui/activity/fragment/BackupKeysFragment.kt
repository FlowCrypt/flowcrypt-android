/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.databinding.FragmentBackupKeysBinding
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.getNavigationResult
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.showInfoDialog
import com.flowcrypt.email.extensions.showNeedPassphraseDialog
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.BackupsViewModel
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.ui.activity.fragment.dialog.FixNeedPassphraseIssueDialogFragment
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.activity.result.contract.CreateCustomDocument
import com.flowcrypt.email.util.exception.DifferentPassPhrasesException
import com.flowcrypt.email.util.exception.EmptyPassphraseException
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.NoPrivateKeysAvailableException
import com.flowcrypt.email.util.exception.PrivateKeyStrengthException
import com.google.android.material.snackbar.Snackbar

/**
 * This activity helps to backup private keys
 *
 * @author Denys Bondarenko
 */
class BackupKeysFragment : BaseFragment<FragmentBackupKeysBinding>(), ProgressBehaviour {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentBackupKeysBinding.inflate(inflater, container, false)

  override val progressView: View?
    get() = binding?.iProgress?.root
  override val contentView: View?
    get() = binding?.gContent
  override val statusView: View?
    get() = binding?.iStatus?.root

  private val backupsViewModel: BackupsViewModel by viewModels()
  private val privateKeysViewModel: PrivateKeysViewModel by viewModels()

  private var isPrivateKeySendingNow: Boolean = false
  private var areBackupsSavingNow: Boolean = false
  private var destinationUri: Uri? = null

  private val createDocumentActivityResultLauncher =
    registerForActivityResult(CreateCustomDocument(Constants.MIME_TYPE_PGP_KEY)) { uri: Uri? ->
      try {
        destinationUri = uri
        destinationUri?.let { privateKeysViewModel.saveBackupsAsFile(it) }
      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
        showInfoSnackbar(binding?.root, e.message ?: "")
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requireActivity().onBackPressedDispatcher.addCallback(this) {
      when {
        areBackupsSavingNow -> toast(R.string.please_wait_while_backup_will_be_saved)
        isPrivateKeySendingNow -> toast(R.string.please_wait_while_message_will_be_sent)
        else -> navController?.navigateUp()
      }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    setupPrivateKeysViewModel()
    setupBackupsViewModel()
    observeOnResultLiveData()
  }

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    super.onAccountInfoRefreshed(accountEntity)
    updateBackupButtonVisibility(accountEntity)
  }

  private fun initViews() {
    binding?.rGBackupOptions?.setOnCheckedChangeListener { group, checkedId ->
      when (group.id) {
        R.id.rGBackupOptions -> when (checkedId) {
          R.id.rBEmailOption -> {
            binding?.tVHint?.text = getString(R.string.backup_as_email_hint)
            binding?.btBackup?.text = getString(R.string.backup_as_email)
            updateBackupButtonVisibility(account)
          }

          R.id.rBDownloadOption -> {
            binding?.tVHint?.text = getString(R.string.backup_as_download_hint)
            binding?.btBackup?.text = getString(R.string.backup_as_a_file)
            updateBackupButtonVisibility(account)
          }
        }
      }
    }

    binding?.btBackup?.setOnClickListener {
      if (account?.hasClientConfigurationProperty(ClientConfiguration.ConfigurationProperty.NO_PRV_BACKUP)
          ?.not() == true
      ) {
        if (KeysStorageImpl.getInstance(requireContext()).getRawKeys().isEmpty()) {
          showInfoSnackbar(
            view = binding?.root,
            msgText = getString(
              R.string.there_are_no_private_keys,
              account?.email
            ), duration = Snackbar.LENGTH_LONG
          )
        } else {
          when (binding?.rGBackupOptions?.checkedRadioButtonId) {
            R.id.rBEmailOption -> {
              dismissCurrentSnackBar()
              checkForEmptyPassphraseOrRunAction {
                if (GeneralUtil.isConnected(requireContext())) {
                  backupsViewModel.postBackup()
                } else {
                  showInfoSnackbar(
                    view = binding?.root,
                    msgText = getString(R.string.internet_connection_is_not_available)
                  )
                }
              }
            }

            R.id.rBDownloadOption -> {
              dismissCurrentSnackBar()
              destinationUri = null
              checkForEmptyPassphraseOrRunAction {
                chooseDestForExportedKey()
              }
            }
          }
        }
      }
    }
  }

  private fun setupPrivateKeysViewModel() {
    privateKeysViewModel.saveBackupAsFileLiveData.observe(viewLifecycleOwner) {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource?.incrementSafely(this@BackupKeysFragment)
            showProgress(getString(R.string.processing))
            areBackupsSavingNow = true
          }

          Result.Status.SUCCESS -> {
            areBackupsSavingNow = false
            val result = it.data
            if (result == true) {
              toast(R.string.backed_up_successfully)
              navController?.popBackStack(R.id.mainSettingsFragment, false)
            } else {
              showContent()
              showInfoSnackbar(binding?.root, getString(R.string.error_occurred_please_try_again))
            }
            countingIdlingResource?.decrementSafely(this@BackupKeysFragment)
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            showContent()
            areBackupsSavingNow = false

            if (!handleKnownException(it.exception)) {
              showInfoDialog(
                dialogTitle = "",
                dialogMsg = it.exception?.message
                  ?: getString(R.string.error_could_not_save_private_keys)
              )
            }
            privateKeysViewModel.saveBackupAsFileLiveData.value = Result.none()
            countingIdlingResource?.decrementSafely(this@BackupKeysFragment)
          }

          else -> {}
        }
      }
    }
  }

  private fun setupBackupsViewModel() {
    backupsViewModel.postBackupLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource?.incrementSafely(this@BackupKeysFragment)
          isPrivateKeySendingNow = true
          showProgress(getString(R.string.processing))
        }

        Result.Status.SUCCESS -> {
          isPrivateKeySendingNow = false
          toast(R.string.backed_up_successfully)
          navController?.popBackStack(R.id.mainSettingsFragment, false)
          countingIdlingResource?.decrementSafely(this@BackupKeysFragment)
        }

        Result.Status.EXCEPTION -> {
          showContent()
          isPrivateKeySendingNow = false

          if (!handleKnownException(it.exception)) {
            showBackuppingErrorHint()
          }

          backupsViewModel.postBackupLiveData.value = Result.none()
          countingIdlingResource?.decrementSafely(this@BackupKeysFragment)
        }

        else -> {
        }
      }
    }
  }

  private fun handleKnownException(e: Throwable?): Boolean {
    when (e) {
      is EmptyPassphraseException -> {
        showNeedPassphraseDialog(
          requestKey = REQUEST_KEY_FIX_MISSING_PASSPHRASE,
          fingerprints = e.fingerprints,
          logicType = FixNeedPassphraseIssueDialogFragment.LogicType.ALL
        )
      }

      is PrivateKeyStrengthException -> {
        showFixPassphraseIssueHint(
          msgText = getString(R.string.pass_phrase_is_too_weak),
          btnName = getString(R.string.change_pass_phrase)
        )
      }

      is DifferentPassPhrasesException -> {
        showFixPassphraseIssueHint(
          msgText = getString(R.string.different_pass_phrases),
          btnName = getString(R.string.fix)
        )
      }

      is NoPrivateKeysAvailableException -> {
        showInfoSnackbar(binding?.root, e.message, Snackbar.LENGTH_LONG)
      }

      else -> {
        return false
      }
    }

    return true
  }

  private fun showBackuppingErrorHint() {
    showSnackbar(
      view = binding?.root,
      msgText = getString(R.string.backup_was_not_sent),
      btnName = getString(R.string.retry),
      duration = Snackbar.LENGTH_LONG
    ) {
      backupsViewModel.postBackup()
    }
  }

  private fun showFixPassphraseIssueHint(msgText: String, btnName: String) {
    showSnackbar(
      view = binding?.root,
      msgText = msgText,
      btnName = btnName,
      duration = Snackbar.LENGTH_LONG
    ) {
      navController?.navigate(
        object : NavDirections {
          override val actionId = R.id.pass_phrase_strength_graph
          override val arguments = CheckPassphraseStrengthFragmentArgs(
            popBackStackIdIfSuccess = R.id.backupKeysFragment,
            title = getString(R.string.change_pass_phrase),
            lostPassphraseTitle = getString(R.string.loss_of_this_pass_phrase_cannot_be_recovered)
          ).toBundle()
        }
      )
    }
  }

  /**
   * Start a new Activity with return results to choose a destination for an exported key.
   */
  private fun chooseDestForExportedKey() {
    account?.let {
      createDocumentActivityResultLauncher.launch(SecurityUtils.genPrivateKeyName(it.email))
    } ?: ExceptionUtil.handleError(NullPointerException("account is null"))
  }

  private fun checkForEmptyPassphraseOrRunAction(action: () -> Unit) {
    val keysStorage = KeysStorageImpl.getInstance(requireContext())
    val fingerprints = keysStorage.getFingerprintsWithEmptyPassphrase()
    if (fingerprints.isNotEmpty()) {
      showNeedPassphraseDialog(
        requestKey = REQUEST_KEY_FIX_MISSING_PASSPHRASE,
        fingerprints = fingerprints,
        logicType = FixNeedPassphraseIssueDialogFragment.LogicType.ALL
      )
    } else {
      action.invoke()
    }
  }

  private fun observeOnResultLiveData() {
    getNavigationResult<kotlin.Result<*>>(RecheckProvidedPassphraseFragment.KEY_ACCEPTED_PASSPHRASE_RESULT) {
      if (it.isSuccess) {
        val passphrase = it.getOrNull() as? CharArray ?: return@getNavigationResult
        account?.let { accountEntity ->
          navController?.navigate(
            BackupKeysFragmentDirections
              .actionBackupKeysFragmentToChangePassphraseOfImportedKeysFragment(
                popBackStackIdIfSuccess = R.id.backupKeysFragment,
                title = getString(R.string.pass_phrase_changed),
                subTitle = getString(R.string.passphrase_was_changed),
                passphrase = String(passphrase),
                accountEntity = accountEntity
              )
          )
        }
      }
    }
  }

  private fun updateBackupButtonVisibility(accountEntity: AccountEntity?) {
    if (accountEntity?.hasClientConfigurationProperty(ClientConfiguration.ConfigurationProperty.NO_PRV_BACKUP) == true) {
      binding?.btBackup?.gone()
    }
  }

  companion object {
    private val REQUEST_KEY_FIX_MISSING_PASSPHRASE = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_FIX_MISSING_PASSPHRASE",
      BackupKeysFragment::class.java
    )
  }
}
