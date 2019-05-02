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

fun getDefaultAccountDao(): AccountDao? {
  return readObjectFromResources("default_account.json", AccountDao::class.java)
}

fun getAccountDao(accountPath: String): AccountDao? {
  return readObjectFromResources(accountPath, AccountDao::class.java)
}

fun getUserWitMoreThan21Letters(): AccountDao? {
  return readObjectFromResources("user_with_more_than_21_letters_account.json", AccountDao::class.java)
}
