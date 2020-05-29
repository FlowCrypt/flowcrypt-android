/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import android.content.ContentValues
import com.flowcrypt.email.database.entity.AccountEntity
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
class UpdateAccountRule(private val account: AccountEntity) : BaseRule() {

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        updateAccount()
        base.evaluate()
      }
    }
  }

  private fun updateAccount() {
    //todo-denbond7 fix me
    //AccountDaoSource().updateAccountInformation(targetContext, account, contentValues)
  }
}
