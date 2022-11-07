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
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.jetpack.workmanager.sync.LoadRecipientsWorker
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.service.IdleService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author Denis Bondarenko
 *         Date: 1/25/20
 *         Time: 10:21 AM
 *         E-mail: DenBond7@gmail.com
 */
open class AccountViewModel(application: Application) : RoomBasicViewModel(application) {
  val addNewAccountLiveData = MutableLiveData<Result<AccountEntity?>>()
  val updateAccountLiveData = MutableLiveData<Result<Boolean?>>()

  private val pureActiveAccountLiveData: LiveData<AccountEntity?> =
    roomDatabase.accountDao().getActiveAccountLD()
  val activeAccountLiveData: LiveData<AccountEntity?> =
    pureActiveAccountLiveData.switchMap { accountEntity ->
      liveData {
        emit(getAccountEntityWithDecryptedInfoSuspend(accountEntity))
      }
    }

  private val pureNonActiveAccountsLiveData: LiveData<List<AccountEntity>> =
    roomDatabase.accountDao().getAllNonactiveAccountsLD()
  val nonActiveAccountsLiveData: LiveData<List<AccountEntity>> =
    pureNonActiveAccountsLiveData.switchMap { accountEntities ->
      liveData {
        emit(accountEntities.mapNotNull { accountEntity ->
          getAccountEntityWithDecryptedInfoSuspend(
            accountEntity
          )
        })
      }
    }

  val pureAccountsLiveData: LiveData<List<AccountEntity>> =
    roomDatabase.accountDao().getAccountsLD()

  suspend fun getActiveAccountSuspend(): AccountEntity? {
    return activeAccountLiveData.value
      ?: return getAccountEntityWithDecryptedInfoSuspend(
        roomDatabase.accountDao().getActiveAccountSuspend()
      )
  }

  fun addNewAccount(accountEntity: AccountEntity) {
    viewModelScope.launch {
      addNewAccountLiveData.value = Result.loading()
      try {
        val existingAccount = roomDatabase.accountDao().getAccountSuspend(accountEntity.email)

        if (existingAccount == null) {
          roomDatabase.accountDao().addAccountSuspend(accountEntity)
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

  fun deleteAccount(accountEntity: AccountEntity?) {
    viewModelScope.launch {
      accountEntity?.let { roomDatabase.accountDao().deleteSuspend(it) }
    }
  }

  companion object {
    fun getAccountEntityWithDecryptedInfo(accountEntity: AccountEntity?): AccountEntity? {
      return accountEntity?.copy(
        password = KeyStoreCryptoManager.decrypt(accountEntity.password),
        smtpPassword = KeyStoreCryptoManager.decrypt(accountEntity.smtpPassword),
      )
    }

    suspend fun getAccountEntityWithDecryptedInfoSuspend(accountEntity: AccountEntity?): AccountEntity? =
      withContext(Dispatchers.IO) {
        return@withContext accountEntity?.copy(
          password = KeyStoreCryptoManager.decryptSuspend(accountEntity.password),
          smtpPassword = KeyStoreCryptoManager.decryptSuspend(accountEntity.smtpPassword),
        )
      }
  }
}
