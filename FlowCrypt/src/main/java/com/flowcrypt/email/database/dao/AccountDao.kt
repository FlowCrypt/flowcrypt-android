/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.security.pgp.PgpKey
import java.util.UUID

/**
 * This class describe creating of table which has name
 * ["accounts"], add, delete and update rows.
 *
 * @author Denys Bondarenko
 */
@Dao
abstract class AccountDao : BaseDao<AccountEntity> {
  @Query("SELECT * FROM accounts WHERE is_active = 1")
  abstract suspend fun getActiveAccountSuspend(): AccountEntity?

  @Query("SELECT * FROM accounts WHERE is_active = 1")
  abstract fun getActiveAccount(): AccountEntity?

  @Query("SELECT * FROM accounts WHERE is_active = 1")
  abstract fun getActiveAccountLD(): LiveData<AccountEntity?>

  @Query("SELECT * FROM accounts WHERE email = :email")
  abstract suspend fun getAccountSuspend(email: String): AccountEntity?

  @Query("SELECT * FROM accounts WHERE email = :email")
  abstract fun getAccount(email: String): AccountEntity?

  @Query("SELECT * FROM accounts WHERE _id = :id")
  abstract fun getAccountById(id: Long?): AccountEntity?

  @Query("SELECT * FROM accounts WHERE is_active = 0")
  abstract fun getAllNonactiveAccounts(): List<AccountEntity>

  @Query("SELECT * FROM accounts WHERE is_active = 0")
  abstract suspend fun getAllNonactiveAccountsSuspend(): List<AccountEntity>

  @Query("SELECT * FROM accounts WHERE is_active = 0")
  abstract fun getAllNonactiveAccountsLD(): LiveData<List<AccountEntity>>

  @Query("SELECT * FROM accounts")
  abstract suspend fun getAccountsSuspend(): List<AccountEntity>

  @Query("SELECT * FROM accounts")
  abstract fun getAccounts(): List<AccountEntity>

  @Query("SELECT * FROM accounts")
  abstract fun getAccountsLD(): LiveData<List<AccountEntity>>

  @Transaction
  open suspend fun addAccountSuspend(accountEntity: AccountEntity) {
    val availableAccounts = getAccountsSuspend()
    //mark all accounts as non-active
    updateAccountsSuspend(availableAccounts.map { it.copy(isActive = false) })
    val pgpPassphrase = UUID.randomUUID().toString()
    val pgpPrivateKey = PgpKey.create(
      email = accountEntity.email,
      passphrase = pgpPassphrase
    ).encoded

    //encrypt sensitive info
    val encryptedPassword = KeyStoreCryptoManager.encryptSuspend(accountEntity.password)
    val encryptedSmtpPassword = KeyStoreCryptoManager.encryptSuspend(accountEntity.smtpPassword)
    val encryptedPgpPassphrase = KeyStoreCryptoManager.encryptSuspend(pgpPassphrase)
    val encryptedPgpPrivateKey = KeyStoreCryptoManager.encryptSuspend(pgpPrivateKey)

    insertSuspend(
      accountEntity.copy(
        email = accountEntity.email.lowercase(),
        password = encryptedPassword,
        smtpPassword = encryptedSmtpPassword,
        isActive = true,
        servicePgpPassphrase = encryptedPgpPassphrase,
        servicePgpPrivateKey = encryptedPgpPrivateKey
      )
    )
  }

  @Transaction
  open suspend fun switchAccountSuspend(accountEntity: AccountEntity) {
    val existedAccount = getAccountSuspend(accountEntity.email) ?: return
    val availableAccounts = getAccountsSuspend()
    //mark all accounts as non-active
    updateAccountsSuspend(availableAccounts.map { it.copy(isActive = false) })
    //mark the given account as active
    updateAccountSuspend(existedAccount.copy(isActive = true))
  }

  @Transaction
  open suspend fun logout(accountEntity: AccountEntity) {
    deleteSuspend(accountEntity)
    getAllNonactiveAccountsSuspend().firstOrNull()?.let {
      switchAccountSuspend(it)
    }
  }

  @Transaction
  open fun updateAccount(entity: AccountEntity): Int {
    val existedAccount = getAccount(entity.email) ?: return 0
    return update(
      entity.copy(
        id = existedAccount.id,
        password = existedAccount.password,
        smtpPassword = existedAccount.smtpPassword,
      )
    )
  }

  @Transaction
  open suspend fun updateAccountSuspend(entity: AccountEntity): Int {
    val existedAccount = getAccountSuspend(entity.email) ?: return 0
    return updateSuspend(
      entity.copy(
        id = existedAccount.id,
        password = existedAccount.password,
        smtpPassword = existedAccount.smtpPassword,
      )
    )
  }

  @Transaction
  open fun updateAccounts(entities: Iterable<AccountEntity>): Int {
    var result = 0

    for (accountEntity in entities) {
      result += updateAccount(accountEntity)
    }

    return result
  }

  @Transaction
  open suspend fun updateAccountsSuspend(entities: Iterable<AccountEntity>): Int {
    var result = 0

    for (accountEntity in entities) {
      result += updateAccountSuspend(accountEntity)
    }

    return result
  }

  @Transaction
  open suspend fun updateExistingAccountSettings(accountEntity: AccountEntity): Int {
    //encrypt sensitive info
    val encryptedPassword = KeyStoreCryptoManager.encrypt(accountEntity.password)
    val encryptedSmtpPassword = KeyStoreCryptoManager.encrypt(accountEntity.smtpPassword)

    return updateSuspend(
      accountEntity.copy(
        password = encryptedPassword,
        smtpPassword = encryptedSmtpPassword,
      )
    )
  }
}
