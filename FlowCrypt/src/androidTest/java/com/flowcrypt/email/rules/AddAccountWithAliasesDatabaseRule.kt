/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountAliasesEntity
import com.flowcrypt.email.database.entity.AccountEntity

/**
 * @author Denys Bondarenko
 */
class AddAccountWithAliasesDatabaseRule constructor(
  val accountEntity: AccountEntity,
  val accountAliasesEntity: AccountAliasesEntity
) : BaseRule() {
  override fun execute() {
    storeDataToDatabase()
  }

  private fun storeDataToDatabase() {
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(targetContext)
    roomDatabase.accountDao().insert(accountEntity)
    roomDatabase.accountAliasesDao().insert(accountAliasesEntity)
  }
}
