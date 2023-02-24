/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import android.content.ContentValues
import com.flowcrypt.email.database.entity.AccountEntity
import org.junit.Rule

/**
 * This [Rule] updates **an existed account** with given [ContentValues]
 *
 * @author Denys Bondarenko
 */
class UpdateAccountRule(private val account: AccountEntity) : BaseRule() {
  override fun execute() {
    updateAccount()
  }

  private fun updateAccount() {
    //todo-denbond7 fix me
    //AccountDaoSource().updateAccountInformation(targetContext, account, contentValues)
  }
}
