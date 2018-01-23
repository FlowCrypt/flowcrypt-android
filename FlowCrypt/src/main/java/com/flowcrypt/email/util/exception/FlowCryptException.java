/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception;

import android.os.Build;
import android.support.annotation.RequiresApi;

/**
 * The base exception class.
 *
 * @author Denis Bondarenko
 *         Date: 23.01.2018
 *         Time: 12:16
 *         E-mail: DenBond7@gmail.com
 */

public abstract class FlowCryptException extends Exception {
    public FlowCryptException() {
    }

    public FlowCryptException(String message) {
        super(message);
    }

    public FlowCryptException(String message, Throwable cause) {
        super(message, cause);
    }

    public FlowCryptException(Throwable cause) {
        super(cause);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public FlowCryptException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
