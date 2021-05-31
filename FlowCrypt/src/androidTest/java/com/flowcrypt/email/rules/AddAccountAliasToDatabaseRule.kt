/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountAliasesEntity
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * @author Denis Bondarenko
 *         Date: 2/13/20
 *         Time: 12:49 PM
 *         E-mail: DenBond7@gmail.com
 */
class AddAccountAliasToDatabaseRule constructor(val alias: AccountAliasesEntity) : BaseRule() {

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        saveAliasToDatabase()
        base.evaluate()
      }
    }
  }

  private fun saveAliasToDatabase() {
    FlowCryptRoomDatabase.getDatabase(targetContext).accountAliasesDao().insert(alias)
  }
}
