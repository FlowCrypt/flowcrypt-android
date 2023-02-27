/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountAliasesEntity

/**
 * @author Denys Bondarenko
 */
class AddAccountAliasToDatabaseRule constructor(val alias: AccountAliasesEntity) : BaseRule() {
  override fun execute() {
    saveAliasToDatabase()
  }

  private fun saveAliasToDatabase() {
    FlowCryptRoomDatabase.getDatabase(targetContext).accountAliasesDao().insert(alias)
  }
}
