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
interface AccountDaoSource : BaseDao<AccountEntity> {

  @Query("SELECT * FROM accounts WHERE is_active = 1")
  suspend fun getActiveAccountSuspend(): AccountEntity?

  @Query("SELECT * FROM accounts WHERE is_active = 1")
  fun getActiveAccount(): AccountEntity?

  @Query("SELECT * FROM accounts WHERE is_active = 1")
  fun getActiveAccountLD(): LiveData<AccountEntity?>

  @Query("SELECT * FROM accounts WHERE email = :email")
  suspend fun getAccountSuspend(email: String): AccountEntity?

  @Query("SELECT * FROM accounts WHERE email = :email")
  fun getAccount(email: String): AccountEntity?

  @Query("SELECT * FROM accounts WHERE is_active = 0")
  suspend fun getAllNonactiveAccounts(): List<AccountEntity>

  @Query("SELECT * FROM accounts")
  suspend fun getAccounts(): List<AccountEntity>

  @Transaction
  suspend fun addAccount(accountEntity: AccountEntity) {
    val availableAccounts = getAccounts()
    //mark all accounts as non-active
    updateSuspend(availableAccounts.map { it.copy(isActive = false) })

    //encrypt sensitive info
    val encryptedPassword = KeyStoreCryptoManager.encryptSuspend(accountEntity.password)
    val encryptedSmtpPassword = KeyStoreCryptoManager.encryptSuspend(accountEntity.smtpPassword)
    val encryptedUuid = KeyStoreCryptoManager.encryptSuspend(accountEntity.uuid)

    insertSuspend(accountEntity.copy(
        password = encryptedPassword,
        smtpPassword = encryptedSmtpPassword,
        uuid = encryptedUuid,
        isActive = true))
  }
}