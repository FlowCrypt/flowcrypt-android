/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.jetpack.workmanager.sync.LoadRecipientsWorker
import com.flowcrypt.email.service.IdleService
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author Denys Bondarenko
 */
open class AccountViewModel(application: Application) : RoomBasicViewModel(application) {
  val addNewAccountLiveData = MutableLiveData<Result<AccountEntity?>>()
  val updateAccountLiveData = MutableLiveData<Result<Boolean?>>()

  private val pureActiveAccountLiveData: LiveData<AccountEntity?> =
    roomDatabase.accountDao().getActiveAccountLD()
  val activeAccountLiveData: LiveData<AccountEntity?> =
    pureActiveAccountLiveData.distinctUntilChanged().switchMap { accountEntity ->
      liveData {
        emit(accountEntity?.withDecryptedInfo())
      }
    }

  private val pureNonActiveAccountsLiveData: LiveData<List<AccountEntity>> =
    roomDatabase.accountDao().getAllNonactiveAccountsLD()
  val nonActiveAccountsLiveData: LiveData<List<AccountEntity>> =
    pureNonActiveAccountsLiveData.switchMap { accountEntities ->
      liveData {
        emit(accountEntities.map {
          it.withDecryptedInfo()
        })
      }
    }

  private val controlledRunnerForUpdatingSignature = ControlledRunner<Unit>()

  val pureAccountsLiveData: LiveData<List<AccountEntity>> =
    roomDatabase.accountDao().getAccountsLD()

  suspend fun getActiveAccountSuspend(): AccountEntity? {
    return activeAccountLiveData.value
      ?: return roomDatabase.accountDao().getActiveAccountSuspend()?.withDecryptedInfo()
  }

  fun addNewAccount(accountEntity: AccountEntity) {
    viewModelScope.launch {
      addNewAccountLiveData.value = Result.loading()
      try {
        val existingAccount = roomDatabase.accountDao().getAccountSuspend(accountEntity.email)

        if (existingAccount == null) {
          roomDatabase.accountDao().addAccountSuspend(accountEntity)
          roomDatabase.accountSettingsDao().insertSuspend(
            accountEntity.toAccountSettingsEntity()
          )
        } else {
          roomDatabase.accountDao().updateAccountSuspend(
            accountEntity.copy(
              id = existingAccount.id,
              clientConfiguration = existingAccount.clientConfiguration,
            )
          )
        }

        LoadRecipientsWorker.enqueue(getApplication())

        addNewAccountLiveData.value =
          Result.success(roomDatabase.accountDao().getAccountSuspend(accountEntity.email))
      } catch (e: Exception) {
        e.printStackTrace()
        addNewAccountLiveData.value = Result.exception(e)
      }
    }
  }

  fun updateAccount(accountEntity: AccountEntity) {
    viewModelScope.launch {
      val context: Context = getApplication()
      updateAccountLiveData.value =
        Result.loading(progressMsg = context.getString(R.string.updating_server_settings))
      try {
        val accountDao = roomDatabase.accountDao()
        val isUpdated = accountDao.updateExistingAccountSettings(accountEntity) > 0
        val intent = Intent(context, IdleService::class.java)
        context.stopService(intent)
        context.startService(intent)
        updateAccountLiveData.value = Result.success(isUpdated)
      } catch (e: Exception) {
        e.printStackTrace()
        updateAccountLiveData.value = Result.exception(e)
      }
    }
  }

  fun updateAccountSignature(signature: String?) {
    viewModelScope.launch {
      val activeAccount = roomDatabase.accountDao().getActiveAccountSuspend() ?: return@launch
      controlledRunnerForUpdatingSignature.cancelPreviousThenRun {
        delay(500)
        roomDatabase.accountDao().updateAccountSuspend(activeAccount.copy(signature = signature))
      }
    }
  }

  fun switchLoadingOnlyPgpMessagesMode() {
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        val activeAccount =
          roomDatabase.accountDao().getActiveAccountSuspend() ?: return@withContext
        roomDatabase.accountDao()
          .updateAccountSuspend(
            activeAccount.copy(showOnlyEncrypted = activeAccount.showOnlyEncrypted != true)
          )
        roomDatabase.msgDao().deleteAllExceptOutgoing(activeAccount.email)
      }
    }
  }

  fun deleteAccount(accountEntity: AccountEntity?) {
    viewModelScope.launch {
      accountEntity?.let { roomDatabase.accountDao().deleteSuspend(it) }
    }
  }
}
