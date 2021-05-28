/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.flowcrypt.email.database.entity.AccountAliasesEntity
import com.flowcrypt.email.database.entity.AccountEntity

/**
 * This object describes a logic of work with [AccountAliases].
 *
 * @author Denis Bondarenko
 * Date: 26.10.2017
 * Time: 15:51
 * E-mail: DenBond7@gmail.com
 */
@Dao
interface AccountAliasesDao : BaseDao<AccountAliasesEntity> {
  @Query("SELECT * FROM accounts_aliases WHERE email = :account AND account_type = :accountType")
  fun getAliases(account: String, accountType: String): List<AccountAliasesEntity>

  @Query("SELECT * FROM accounts_aliases WHERE email = :account AND account_type = :accountType")
  fun getAliasesLD(account: String, accountType: String): LiveData<List<AccountAliasesEntity>>

  @Query("DELETE FROM accounts_aliases WHERE email = :email AND account_type = :accountType")
  suspend fun deleteByEmailSuspend(email: String, accountType: String): Int

  @Transaction
  suspend fun updateAliases(
    accountEntity: AccountEntity?,
    newAliases: Collection<AccountAliasesEntity>
  ) {
    accountEntity?.let {
      deleteByEmailSuspend(it.email, it.accountType ?: "")
      insertWithReplaceSuspend(newAliases)
    }
  }
}
