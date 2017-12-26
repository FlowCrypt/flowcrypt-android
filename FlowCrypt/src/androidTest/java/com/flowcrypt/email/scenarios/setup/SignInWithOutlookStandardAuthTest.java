/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.scenarios.setup;

import com.flowcrypt.email.api.email.model.AuthCredentials;
import com.flowcrypt.email.api.email.model.SecurityType;

/**
 * This test is using the Outlook credentials.
 *
 * @author Denis Bondarenko
 *         Date: 26.12.2017
 *         Time: 13:35
 *         E-mail: DenBond7@gmail.com
 */

public class SignInWithOutlookStandardAuthTest extends SignInWithStandardAuthTest {

    @Override
    AuthCredentials getAuthCredentials() {
        return new AuthCredentials.Builder().setEmail("espresso_tester@outlook.com")
                .setUsername("espresso_tester@outlook.com")
                .setPassword("O2LixFbT")
                .setImapServer("imap-mail.outlook.com")
                .setImapPort(993)
                .setImapSecurityTypeOption(SecurityType.Option.SSL_TLS)
                .setSmtpServer("smtp-mail.outlook.com")
                .setSmtpPort(587)
                .setSmtpSecurityTypeOption(SecurityType.Option.STARTLS)
                .setIsUseCustomSignInForSmtp(false)
                .build();
    }
}
