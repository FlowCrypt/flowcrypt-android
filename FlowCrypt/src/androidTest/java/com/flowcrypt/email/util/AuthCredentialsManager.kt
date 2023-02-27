/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import com.flowcrypt.email.api.email.model.AuthCredentials

/**
 * This class describes a logic of generation {@link AccountEntity} from the resources folder.
 *
 * @author Denys Bondarenko
 */
class AuthCredentialsManager {
  companion object {
    fun getAuthCredentials(resName: String = "base_account_settings.json"): AuthCredentials {
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
