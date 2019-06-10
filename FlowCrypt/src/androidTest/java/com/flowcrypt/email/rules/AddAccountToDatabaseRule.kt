/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.util.AccountDaoManager
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * @author Denis Bondarenko
 * Date: 21.02.2018
 * Time: 17:54
 * E-mail: DenBond7@gmail.com
 */
class AddAccountToDatabaseRule(val account: AccountDao) : BaseRule() {

  constructor() : this(AccountDaoManager.getDefaultAccountDao())

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      @Throws(Throwable::class)
      override fun evaluate() {
        saveAccountToDatabase()
        base.evaluate()
      }
    }
  }

  @Throws(Exception::class)
  private fun saveAccountToDatabase() {
    val accountDaoSource = AccountDaoSource()
    accountDaoSource.addRow(targetContext, account.authCreds)
    accountDaoSource.setActiveAccount(targetContext, account.email)
  }
}