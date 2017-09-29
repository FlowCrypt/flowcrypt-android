/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email;

import java.util.UUID;

/**
 * @author Denis Bondarenko
 *         Date: 29.09.2017
 *         Time: 15:31
 *         E-mail: DenBond7@gmail.com
 */

public class EmailUtil {
    public static String generateContentId() {
        return "<" + UUID.randomUUID().toString() + "@flowcrypt" + ">";
    }
}
