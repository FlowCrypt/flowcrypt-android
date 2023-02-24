/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.util.AccountDaoManager

/**
 * @author Denys Bondarenko
 */
class AddAccountToDatabaseRule constructor(val account: AccountEntity) : BaseRule() {
  constructor() : this(AccountDaoManager.getDefaultAccountDao())

  override fun execute() {
    saveAccountToDatabase()
  }

  private fun saveAccountToDatabase() {
    FlowCryptRoomDatabase.getDatabase(targetContext).accountDao().addAccount(account)
  }
}
