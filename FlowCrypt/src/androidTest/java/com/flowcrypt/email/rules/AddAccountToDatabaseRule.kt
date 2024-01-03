/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.util.AccountDaoManager
import kotlinx.coroutines.runBlocking

/**
 * @author Denys Bondarenko
 */
class AddAccountToDatabaseRule(val account: AccountEntity) : BaseRule() {
  constructor() : this(AccountDaoManager.getDefaultAccountDao())

  private var accountEntityWithDecryptedInfoCached: AccountEntity? = null
  val accountEntityWithDecryptedInfo: AccountEntity
    get() {
      return accountEntityWithDecryptedInfoCached
        ?: runBlocking {
          accountEntityWithDecryptedInfoCached = getAccountEntityWithDecryptedInfo()
          requireNotNull(accountEntityWithDecryptedInfoCached)
        }
    }

  override fun execute() {
    saveAccountToDatabase()
  }

  private fun saveAccountToDatabase() {
    accountEntityWithDecryptedInfoCached = runBlocking {
      try {
        FlowCryptRoomDatabase.getDatabase(targetContext).accountDao().addAccountSuspend(account)
      } catch (e: Exception) {
        e.printStackTrace()
      }
      getAccountEntityWithDecryptedInfo()
    }
  }

  private suspend fun getAccountEntityWithDecryptedInfo(): AccountEntity? {
    val accountEntity = FlowCryptRoomDatabase.getDatabase(targetContext).accountDao()
      .getAccountSuspend(account.email)
    return accountEntity?.copy(
      password = KeyStoreCryptoManager.decryptSuspend(accountEntity.password),
      smtpPassword = KeyStoreCryptoManager.decryptSuspend(accountEntity.smtpPassword),
      servicePgpPassphrase = KeyStoreCryptoManager.decryptSuspend(accountEntity.servicePgpPassphrase),
      servicePgpPrivateKey = KeyStoreCryptoManager.decryptSuspend(accountEntity.servicePgpPrivateKey)
    )

  }
}
