/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception;

import org.acra.ACRA;

/**
 * This class is using with {@link ACRA} when we handle some exception <b>manually</b> and send logs to the server.
 *
 * @author Denis Bondarenko
 *         Date: 23.01.2018
 *         Time: 12:17
 *         E-mail: DenBond7@gmail.com
 */

public class ManualHandledException extends FlowCryptException {
    public ManualHandledException(Throwable cause) {
        super("Handled manually:", cause);
    }
}
