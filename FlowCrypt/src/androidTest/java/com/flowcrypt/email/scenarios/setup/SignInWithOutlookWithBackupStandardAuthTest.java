/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.scenarios.setup;

import com.flowcrypt.email.api.email.model.AuthCredentials;
import com.flowcrypt.email.util.AuthCredentialsManager;

import org.junit.Ignore;

/**
 * This test is using the Outlook credentials.
 *
 * @author Denis Bondarenko
 * Date: 26.12.2017
 * Time: 13:35
 * E-mail: DenBond7@gmail.com
 */
@Ignore
public class SignInWithOutlookWithBackupStandardAuthTest extends SignInWithBackupStandardAuthTest {

  @Override
  AuthCredentials getAuthCreds() {
    return AuthCredentialsManager.getOutLookWithBackupAuthCreds();
  }
}
