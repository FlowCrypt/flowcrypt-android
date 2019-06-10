/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import android.content.ContentValues
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import org.junit.Rule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * This [Rule] updates **an existed account** with given [ContentValues]
 *
 * @author Denis Bondarenko
 * Date: 15.05.2018
 * Time: 10:19
 * E-mail: DenBond7@gmail.com
 */
class UpdateAccountRule(private val account: AccountDao, private val contentValues: ContentValues) : BaseRule() {

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      @Throws(Throwable::class)
      override fun evaluate() {
        updateAccount()
        base.evaluate()
      }
    }
  }

  private fun updateAccount() {
    AccountDaoSource().updateAccountInformation(targetContext, account.account, contentValues)
  }
}
