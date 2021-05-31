/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.util.AccountDaoManager
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * @author Denis Bondarenko
 * Date: 21.02.2018
 * Time: 17:54
 * E-mail: DenBond7@gmail.com
 */
class AddAccountToDatabaseRule constructor(val account: AccountEntity) : BaseRule() {

  constructor() : this(AccountDaoManager.getDefaultAccountDao())

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        saveAccountToDatabase()
        base.evaluate()
      }
    }
  }

  private fun saveAccountToDatabase() {
    FlowCryptRoomDatabase.getDatabase(targetContext).accountDao().addAccount(account)
  }
}
