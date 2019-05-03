/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import com.flowcrypt.email.database.dao.source.AccountDao

/**
 * @author Denis Bondarenko
 * Date: 15.01.2018
 * Time: 16:34
 * E-mail: DenBond7@gmail.com
 */
class AccountDaoManager {
  companion object {
    @JvmStatic
    fun getDefaultAccountDao(): AccountDao? {
      return TestGeneralUtil.readObjectFromResources("default_account.json", AccountDao::class.java)
    }

    @JvmStatic
    fun getAccountDao(accountPath: String): AccountDao? {
      return TestGeneralUtil.readObjectFromResources(accountPath, AccountDao::class.java)
    }

    @JvmStatic
    fun getUserWitMoreThan21Letters(): AccountDao? {
      return TestGeneralUtil.readObjectFromResources("user_with_more_than_21_letters_account.json", AccountDao::class.java)
    }
  }
}