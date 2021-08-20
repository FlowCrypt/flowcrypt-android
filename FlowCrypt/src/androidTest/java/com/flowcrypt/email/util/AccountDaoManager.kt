/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import com.flowcrypt.email.api.retrofit.response.model.OrgRules
import com.flowcrypt.email.database.entity.AccountEntity

/**
 * @author Denis Bondarenko
 * Date: 15.01.2018
 * Time: 16:34
 * E-mail: DenBond7@gmail.com
 */
class AccountDaoManager {
  companion object {

    fun getDefaultAccountDao(): AccountEntity {
      return getAccountDao("base_account_settings.json")
    }

    fun getAccountDao(accountPath: String): AccountEntity {
      return TestGeneralUtil.readObjectFromResources(accountPath, AccountEntity::class.java)
    }

    fun getUserWithMoreThan21Letters(): AccountEntity {
      return getUserFromBaseSettings("user_with_more_than_21_letters@flowcrypt.test")
    }

    fun getUserWithoutLetters(): AccountEntity {
      return getUserFromBaseSettings("user_without_letters@flowcrypt.test")
    }

    fun getUserWithSingleBackup(): AccountEntity {
      return getUserFromBaseSettings("single_backup@flowcrypt.test")
    }

    fun getUserWithoutBackup(): AccountEntity {
      return getUserFromBaseSettings("no_backups@flowcrypt.test")
    }

    fun getUserWithOrgRules(orgRules: OrgRules): AccountEntity {
      return getUserFromBaseSettings("user_with_org_rules@flowcrypt.test")
        .copy(clientConfiguration = orgRules)
    }

    fun getUserFromBaseSettings(user: String): AccountEntity {
      return getDefaultAccountDao().copy(
        email = user,
        smtpUsername = user,
        username = user,
      )
    }
  }
}
