/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.security.KeyStoreCryptoManager

/**
 * @author Denis Bondarenko
 *         Date: 1/25/20
 *         Time: 10:21 AM
 *         E-mail: DenBond7@gmail.com
 */
open class AccountViewModel(application: Application) : BaseAndroidViewModel(application) {
  protected val roomDatabase = FlowCryptRoomDatabase.getDatabase(application)

  private val pureActiveAccountLiveData: LiveData<AccountEntity?> = roomDatabase.accountDao().getActiveAccountLD()
  val activeAccountLiveData: LiveData<AccountEntity?> = pureActiveAccountLiveData.switchMap { accountEntity ->
    liveData {
      emit(getAccountEntityWithDecryptedIfo(accountEntity))
    }
  }

  companion object {
    fun getAccountEntityWithDecryptedIfo(accountEntity: AccountEntity?): AccountEntity? {
      var originalPassword = accountEntity?.password

      //fixed a bug when try to decrypting the template password.
      // See https://github.com/FlowCrypt/flowcrypt-android/issues/168
      if ("password".equals(originalPassword, ignoreCase = true)) {
        originalPassword = ""
      }

      return accountEntity?.copy(
          password = KeyStoreCryptoManager.decrypt(originalPassword),
          smtpPassword = KeyStoreCryptoManager.decrypt(accountEntity.smtpPassword),
          uuid = KeyStoreCryptoManager.decrypt(accountEntity.uuid))
    }
  }
}