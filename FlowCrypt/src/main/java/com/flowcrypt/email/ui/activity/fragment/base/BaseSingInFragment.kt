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
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.observeOnce
import com.flowcrypt.email.extensions.showInfoDialog
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.jetpack.workmanager.sync.BaseSyncWorker
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.service.IdleService
import com.flowcrypt.email.service.actionqueue.actions.LoadGmailAliasesAction
import com.flowcrypt.email.util.exception.SavePrivateKeyToDatabaseException
import com.google.android.material.snackbar.Snackbar

/**
 * @author Denis Bondarenko
 *         Date: 7/21/20
 *         Time: 6:29 PM
 *         E-mail: DenBond7@gmail.com
 */
abstract class BaseSingInFragment<T : ViewBinding> : BaseOAuthFragment<T>(), ProgressBehaviour {
  protected val privateKeysViewModel: PrivateKeysViewModel by viewModels()

  protected val existingAccounts = mutableListOf<AccountEntity>()
  protected val importCandidates = mutableListOf<PgpKeyDetails>()

  abstract fun getTempAccount(): AccountEntity?
  abstract fun onAccountAdded(accountEntity: AccountEntity)
  abstract fun onAdditionalActionsAfterPrivateKeyCreationCompleted(
    accountEntity: AccountEntity,
    pgpKeyDetails: PgpKeyDetails
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
    privateKeysViewModel.savePrivateKeysLiveData.observe(viewLifecycleOwner) {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource?.incrementSafely()
            showProgress(getString(R.string.processing))
          }

          Result.Status.SUCCESS -> {
            it.data?.let { pair -> onSetupCompleted(pair.first) }
            countingIdlingResource?.decrementSafely()
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
            countingIdlingResource?.decrementSafely()
          }
          else -> {}
        }
      }
    }
    privateKeysViewModel.additionalActionsAfterPrivateKeyCreationLiveData.observe(viewLifecycleOwner) {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource?.incrementSafely()
            showProgress(getString(R.string.processing))
          }

          Result.Status.SUCCESS -> {
            it.data?.let { pair ->
              onAdditionalActionsAfterPrivateKeyCreationCompleted(
                pair.first,
                pair.second
              )
            }
            countingIdlingResource?.decrementSafely()
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            showContent()
            val e = it.exception
            showInfoSnackbar(
              msgText = e?.message ?: e?.javaClass?.simpleName
              ?: getString(R.string.unknown_error)
            )
            countingIdlingResource?.decrementSafely()
          }
          else -> {}
        }
      }
    }
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
