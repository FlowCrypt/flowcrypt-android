/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.scenarios.setup;

import com.flowcrypt.email.api.email.model.AuthCredentials;
import com.flowcrypt.email.util.AuthCredentialsManager;

/**
 * This test is using credentials of the user which has only one backup.
 *
 * @author Denis Bondarenko
 *         Date: 26.12.2017
 *         Time: 13:35
 *         E-mail: DenBond7@gmail.com
 */

public class SignInWithLocalWithOneBackupStandardAuthTest extends SignInWithBackupStandardAuthTest {

    @Override
    AuthCredentials getAuthCredentials() {
        return AuthCredentialsManager.getLocalWithOneBackupAuthCredentials();
    }
}
