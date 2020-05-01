/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

/**
 * @author Denis Bondarenko
 * Date: 15.01.2018
 * Time: 16:34
 * E-mail: DenBond7@gmail.com
 */
class AccountDaoManager {
  companion object {
    @JvmStatic
    fun getDefaultAccountDao(): AccountEntity {
      return TestGeneralUtil.readObjectFromResources("default_account.json", AccountEntity::class.java)
    }

    @JvmStatic
    fun getAccountDao(accountPath: String): AccountEntity {
      return TestGeneralUtil.readObjectFromResources(accountPath, AccountEntity::class.java)
    }

    @JvmStatic
    fun getUserWitMoreThan21Letters(): AccountEntity {
      return TestGeneralUtil.readObjectFromResources("user_with_more_than_21_letters_account.json",
          AccountEntity::class.java)
    }
  }
}