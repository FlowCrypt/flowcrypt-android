/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountAliasesEntity
import kotlinx.coroutines.runBlocking

/**
 * @author Denis Bondarenko
 *         Date: 11/18/22
 *         Time: 12:18 PM
 *         E-mail: DenBond7@gmail.com
 */
class AddGmailAliasToDatabaseRule constructor(
  private val accountAliasesEntity: AccountAliasesEntity
) : BaseRule() {
  override fun execute() {
    saveAliasToDatabase()
  }

  private fun saveAliasToDatabase() {
    runBlocking {
      FlowCryptRoomDatabase.getDatabase(targetContext).accountAliasesDao()
        .insertSuspend(accountAliasesEntity)
    }
  }
}
