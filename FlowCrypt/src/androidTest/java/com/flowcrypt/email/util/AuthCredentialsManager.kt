/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import com.flowcrypt.email.api.email.model.AuthCredentials

/**
 * This class describes a logic of generation {@link AccountEntity} from the resources folder.
 *
 * @author Denis Bondarenko
 * Date: 27.12.2017
 * Time: 14:49
 * E-mail: DenBond7@gmail.com
 */
class AuthCredentialsManager {
  companion object {
    fun getAuthCredentials(resName: String = "default_account.json"): AuthCredentials {
      return TestGeneralUtil.readObjectFromResources(resName, AuthCredentials::class.java)
    }

    fun getLocalWithOneBackupAuthCreds(): AuthCredentials {
      return TestGeneralUtil.readObjectFromResources(
        "user_with_one_backup.json",
        AuthCredentials::class.java
      )
    }
  }
}
