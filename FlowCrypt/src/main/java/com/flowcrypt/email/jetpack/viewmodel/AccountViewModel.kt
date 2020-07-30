/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.security.KeyStoreCryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @author Denis Bondarenko
 *         Date: 1/25/20
 *         Time: 10:21 AM
 *         E-mail: DenBond7@gmail.com
 */
open class AccountViewModel(application: Application) : RoomBasicViewModel(application) {
  private val pureActiveAccountLiveData: LiveData<AccountEntity?> = roomDatabase.accountDao().getActiveAccountLD()
  val activeAccountLiveData: LiveData<AccountEntity?> = pureActiveAccountLiveData.switchMap { accountEntity ->
    liveData {
      emit(getAccountEntityWithDecryptedInfoSuspend(accountEntity))
    }
  }

  private val pureNonActiveAccountsLiveData: LiveData<List<AccountEntity>> = roomDatabase.accountDao().getAllNonactiveAccountsLD()
  val nonActiveAccountsLiveData: LiveData<List<AccountEntity>> = pureNonActiveAccountsLiveData.switchMap { accountEntities ->
    liveData {
      emit(accountEntities.mapNotNull { accountEntity -> getAccountEntityWithDecryptedInfoSuspend(accountEntity) })
    }
  }

  val pureAccountsLiveData: LiveData<List<AccountEntity>> = roomDatabase.accountDao().getAccountsLD()

  suspend fun getActiveAccountSuspend(): AccountEntity? {
    return activeAccountLiveData.value ?: return roomDatabase.accountDao().getActiveAccountSuspend()
  }

  companion object {
    fun getAccountEntityWithDecryptedInfo(accountEntity: AccountEntity?): AccountEntity? {
      var originalPassword = accountEntity?.password

      //fixed a bug when try to decrypting the template password.
      // See https://github.com/FlowCrypt/flowcrypt-android/issues/168
      //todo-denbond7 remove this in 2022
      if ("password".equals(originalPassword, ignoreCase = true)) {
        originalPassword = ""
      }

      return accountEntity?.copy(
          password = KeyStoreCryptoManager.decrypt(originalPassword),
          smtpPassword = KeyStoreCryptoManager.decrypt(accountEntity.smtpPassword),
          uuid = KeyStoreCryptoManager.decrypt(accountEntity.uuid))
    }

    suspend fun getAccountEntityWithDecryptedInfoSuspend(accountEntity: AccountEntity?): AccountEntity? =
        withContext(Dispatchers.IO) {
          var originalPassword = accountEntity?.password

          //fixed a bug when try to decrypting the template password.
          // See https://github.com/FlowCrypt/flowcrypt-android/issues/168
          //todo-denbond7 remove this in 2022
          if ("password".equals(originalPassword, ignoreCase = true)) {
            originalPassword = ""
          }

          return@withContext accountEntity?.copy(
              password = KeyStoreCryptoManager.decryptSuspend(originalPassword),
              smtpPassword = KeyStoreCryptoManager.decryptSuspend(accountEntity.smtpPassword),
              uuid = KeyStoreCryptoManager.decryptSuspend(accountEntity.uuid))
        }
  }
}