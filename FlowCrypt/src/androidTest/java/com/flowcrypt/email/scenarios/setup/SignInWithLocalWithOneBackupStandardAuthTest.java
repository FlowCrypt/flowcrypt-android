/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.scenarios.setup;

import com.flowcrypt.email.api.email.model.AuthCredentials;
import com.flowcrypt.email.util.AuthCredentialsManager;

/**
 * This test is using credentials of the user which has only one backup.
 *
 * @author Denis Bondarenko
 * Date: 26.12.2017
 * Time: 13:35
 * E-mail: DenBond7@gmail.com
 */

public class SignInWithLocalWithOneBackupStandardAuthTest extends SignInWithBackupStandardAuthTest {

  @Override
  AuthCredentials getAuthCreds() {
    return AuthCredentialsManager.getLocalWithOneBackupAuthCreds();
  }
}
