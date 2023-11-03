/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base

import androidx.fragment.app.viewModels
import androidx.viewbinding.ViewBinding
import androidx.work.WorkManager
import com.flowcrypt.email.NavGraphDirections
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.exceptionMsg
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.observeOnce
import com.flowcrypt.email.extensions.showInfoDialog
import com.flowcrypt.email.extensions.showInfoDialogWithExceptionDetails
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.jetpack.workmanager.sync.BaseSyncWorker
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.service.IdleService
import com.flowcrypt.email.service.actionqueue.actions.LoadGmailAliasesAction
import com.flowcrypt.email.util.exception.SavePrivateKeyToDatabaseException
import com.google.android.material.snackbar.Snackbar

/**
 * @author Denys Bondarenko
 */
abstract class BaseSingInFragment<T : ViewBinding> : BaseOAuthFragment<T>(), ProgressBehaviour {
  protected val privateKeysViewModel: PrivateKeysViewModel by viewModels()

  protected val existingAccounts = mutableListOf<AccountEntity>()
  protected val importCandidates = mutableListOf<PgpKeyRingDetails>()

  abstract fun getTempAccount(): AccountEntity?
  abstract fun onAccountAdded(accountEntity: AccountEntity)
  abstract fun onAdditionalActionsAfterPrivateKeyCreationCompleted(
    accountEntity: AccountEntity,
    pgpKeyRingDetails: PgpKeyRingDetails
  )

  abstract fun onAdditionalActionsAfterPrivateKeyImportingCompleted(
    accountEntity: AccountEntity,
    keys: List<PgpKeyRingDetails>
  )

  override val isToolbarVisible: Boolean = false

  protected fun onSetupCompleted(accountEntity: AccountEntity) {
    if (existingAccounts.isEmpty()) {
      navigateToPrimaryAccountMessagesList(accountEntity)
    } else {
      switchAccount(accountEntity)
    }
  }

  protected fun initPrivateKeysViewModel() {
    observeSavingPrivateKeys()
    observeCreatingPrivateKey()
    observeImportingPrivateKeys()
  }

  protected fun initAddNewAccountLiveData() {
    accountViewModel.addNewAccountLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          showProgress(getString(R.string.processing))
        }

        Result.Status.SUCCESS -> {
          if (it.data != null) {
            //clear LiveData value to prevent duplicate running
            accountViewModel.addNewAccountLiveData.value = Result.none()
            context?.let { context ->
              WorkManager.getInstance(context).cancelAllWorkByTag(BaseSyncWorker.TAG_SYNC)
            }

            onAccountAdded(it.data)
          }
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          val msg = StringBuilder()
            .append(getString(R.string.could_not_add_new_account))
            .append("/n/n")
            .append(it.exception?.message)
            .append(it.exception?.javaClass?.simpleName)
            .toString()

          showInfoDialog(dialogMsg = msg)
        }
        else -> {}
      }
    }
  }

  private fun observeCreatingPrivateKey() {
    privateKeysViewModel.additionalActionsAfterPrivateKeyCreationLiveData.observe(viewLifecycleOwner) {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource?.incrementSafely(this@BaseSingInFragment)
            showProgress(getString(R.string.processing))
          }

          Result.Status.SUCCESS -> {
            it.data?.let { pair ->
              onAdditionalActionsAfterPrivateKeyCreationCompleted(
                pair.first,
                pair.second.first()
              )
            }
            countingIdlingResource?.decrementSafely(this@BaseSingInFragment)
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            showContent()
            showInfoDialogWithExceptionDetails(it.exception, it.exceptionMsg)
            countingIdlingResource?.decrementSafely(this@BaseSingInFragment)
          }
          else -> {}
        }
      }
    }
  }

  private fun observeImportingPrivateKeys() {
    privateKeysViewModel.additionalActionsAfterPrivateKeysImportingLiveData.observe(
      viewLifecycleOwner
    ) {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource?.incrementSafely(this@BaseSingInFragment)
            showProgress(getString(R.string.processing))
          }

          Result.Status.SUCCESS -> {
            it.data?.let { pair ->
              onAdditionalActionsAfterPrivateKeyImportingCompleted(
                pair.first,
                pair.second
              )
            }
            countingIdlingResource?.decrementSafely(this@BaseSingInFragment)
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            showContent()
            showInfoDialogWithExceptionDetails(it.exception)
            countingIdlingResource?.decrementSafely(this@BaseSingInFragment)
          }
          else -> {}
        }
      }
    }
  }

  private fun observeSavingPrivateKeys() {
    privateKeysViewModel.savePrivateKeysLiveData.observe(viewLifecycleOwner) {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource?.incrementSafely(this@BaseSingInFragment)
            showProgress(getString(R.string.processing))
          }

          Result.Status.SUCCESS -> {
            it.data?.let { pair -> onSetupCompleted(pair.first) }
            countingIdlingResource?.decrementSafely(this@BaseSingInFragment)
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            showContent()
            val e = it.exception
            if (e is SavePrivateKeyToDatabaseException) {
              showSnackbar(
                msgText = e.message ?: e.javaClass.simpleName,
                btnName = getString(R.string.retry),
                duration = Snackbar.LENGTH_INDEFINITE,
                onClickListener = {
                  getTempAccount()?.let { accountEntity ->
                    privateKeysViewModel.encryptAndSaveKeysToDatabase(
                      accountEntity,
                      e.keys
                    )
                  }
                }
              )
            } else {
              showInfoSnackbar(
                msgText = e?.message ?: e?.javaClass?.simpleName
                ?: getString(R.string.unknown_error)
              )
            }
            countingIdlingResource?.decrementSafely(this@BaseSingInFragment)
          }
          else -> {}
        }
      }
    }
  }

  private fun initAllAccountsLiveData() {
    //here we receive only value and unsubscribe
    accountViewModel.pureAccountsLiveData.observeOnce(this) {
      existingAccounts.clear()
      existingAccounts.addAll(it)
    }
  }

  protected open fun navigateToPrimaryAccountMessagesList(accountEntity: AccountEntity) {
    IdleService.start(requireContext())
    if (accountEntity.accountType == AccountEntity.ACCOUNT_TYPE_GOOGLE) {
      roomBasicViewModel.addActionToQueue(LoadGmailAliasesAction(email = accountEntity.email))
    }
    navController?.navigate(NavGraphDirections.actionGlobalToMessagesListFragment())
  }

  protected open fun switchAccount(accountEntity: AccountEntity) {
    if (accountEntity.accountType == AccountEntity.ACCOUNT_TYPE_GOOGLE) {
      roomBasicViewModel.addActionToQueue(LoadGmailAliasesAction(email = accountEntity.email))
    }
    navController?.navigateUp()
  }
}
