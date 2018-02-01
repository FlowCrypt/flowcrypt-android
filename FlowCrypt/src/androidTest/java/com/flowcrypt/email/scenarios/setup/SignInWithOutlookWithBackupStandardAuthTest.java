/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
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
 *         Date: 26.12.2017
 *         Time: 13:35
 *         E-mail: DenBond7@gmail.com
 */
@Ignore
public class SignInWithOutlookWithBackupStandardAuthTest extends SignInWithBackupStandardAuthTest {

    @Override
    AuthCredentials getAuthCredentials() {
        return AuthCredentialsManager.getOutLookWithBackupAuthCredentials();
    }
}
