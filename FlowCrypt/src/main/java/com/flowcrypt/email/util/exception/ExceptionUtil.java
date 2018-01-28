/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception;

import com.sun.mail.util.MailConnectException;

import org.acra.ACRA;

import java.io.IOException;
import java.net.UnknownHostException;

import javax.mail.MessagingException;
import javax.net.ssl.SSLHandshakeException;

/**
 * This class describes methods for a work with {@link Exception}
 *
 * @author Denis Bondarenko
 *         Date: 25.01.2018
 *         Time: 10:22
 *         E-mail: DenBond7@gmail.com
 */

public class ExceptionUtil {

    /**
     * Check if need to handle a happened error with {@link ACRA}
     *
     * @param e A happened error
     * @return true if need to handle such exception with {@link ACRA} and send logs to the backend, false - otherwise.
     */
    public static boolean isErrorHandleWithACRA(Exception e) {
        if ((e instanceof MailConnectException) || (e instanceof UnknownHostException)) {
            return false;
        }

        if (e instanceof IOException && "NetworkError".equalsIgnoreCase(e.getMessage())) {
            return false;
        }

        if ((e instanceof SSLHandshakeException || e instanceof MessagingException)) {

            if ("Connection closed by peer".equalsIgnoreCase(e.getMessage())
                    || e.getMessage().contains("I/O error during system call, Software caused connection abort")) {
                return false;
            }
        }

        return true;
    }
}
