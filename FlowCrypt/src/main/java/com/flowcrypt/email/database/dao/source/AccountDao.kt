/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.flowcrypt.email.database.dao.BaseDao
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.security.KeyStoreCryptoManager

/**
 * This class describe creating of table which has name
 * ["accounts"], add, delete and update rows.
 *
 * @author Denis Bondarenko
 * Date: 14.07.2017
 * Time: 17:43
 * E-mail: DenBond7@gmail.com
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

  @Transaction
  open suspend fun addAccountSuspend(accountEntity: AccountEntity) {
    val availableAccounts = getAccountsSuspend()
    //mark all accounts as non-active
    updateAccountsSuspend(availableAccounts.map { it.copy(isActive = false) })

    //encrypt sensitive info
    val encryptedPassword = KeyStoreCryptoManager.encryptSuspend(accountEntity.password)
    val encryptedSmtpPassword = KeyStoreCryptoManager.encryptSuspend(accountEntity.smtpPassword)
    val encryptedUuid = KeyStoreCryptoManager.encryptSuspend(accountEntity.uuid)

    insertSuspend(
        accountEntity.copy(
            password = encryptedPassword,
            smtpPassword = encryptedSmtpPassword,
            uuid = encryptedUuid,
            isActive = true))
  }

  @Transaction
  open fun addAccount(accountEntity: AccountEntity) {
    val availableAccounts = getAccounts()
    //mark all accounts as non-active
    updateAccounts(availableAccounts.map { it.copy(isActive = false) })

    //encrypt sensitive info
    val encryptedPassword = KeyStoreCryptoManager.encrypt(accountEntity.password)
    val encryptedSmtpPassword = KeyStoreCryptoManager.encrypt(accountEntity.smtpPassword)
    val encryptedUuid = KeyStoreCryptoManager.encrypt(accountEntity.uuid)

    insert(
        accountEntity.copy(
            password = encryptedPassword,
            smtpPassword = encryptedSmtpPassword,
            uuid = encryptedUuid,
            isActive = true))
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
  open fun updateAccount(entity: AccountEntity): Int {
    val existedAccount = getAccount(entity.email) ?: return 0
    return update(entity.copy(
        id = existedAccount.id,
        password = existedAccount.password,
        smtpPassword = existedAccount.smtpPassword,
        uuid = existedAccount.uuid))
  }

  @Transaction
  open suspend fun updateAccountSuspend(entity: AccountEntity): Int {
    val existedAccount = getAccountSuspend(entity.email) ?: return 0
    return updateSuspend(entity.copy(
        id = existedAccount.id,
        password = existedAccount.password,
        smtpPassword = existedAccount.smtpPassword,
        uuid = existedAccount.uuid))
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
}