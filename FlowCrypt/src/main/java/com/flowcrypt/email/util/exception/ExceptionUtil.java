/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception;

import com.sun.mail.util.MailConnectException;

import org.acra.ACRA;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.mail.MessagingException;
import javax.mail.StoreClosedException;
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
        if ((e instanceof MailConnectException)
                || (e instanceof UnknownHostException)
                || (e instanceof SocketTimeoutException)) {
            return false;
        }

        if (e instanceof IOException) {
            //Google network errors.
            if ("NetworkError".equalsIgnoreCase(e.getMessage())
                    || "Error on service connection.".equalsIgnoreCase(e.getMessage())) {
                return false;
            }

        }

        if ((e instanceof SSLHandshakeException || e instanceof MessagingException)) {
            if ("Connection closed by peer".equalsIgnoreCase(e.getMessage())
                    || e.getMessage().contains("I/O error during system call, Software caused connection abort")
                    || e.getMessage().contains("I/O error during system call, Connection reset by peer;")
                    || e.getMessage().contains("I/O error during system call, Socket operation on non-socket;")) {
                return false;
            }
        }

        if (e instanceof StoreClosedException) {
            //Connection limit exceeded
            if ("failed to create new store connection".equalsIgnoreCase(e.getMessage())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Handle an input {@link Exception} by {@link ACRA}.
     *
     * @param e An input {@link Exception}
     */
    public static void handleError(Exception e) {
        if (ExceptionUtil.isErrorHandleWithACRA(e)) {
            if (ACRA.isInitialised()) {
                ACRA.getErrorReporter().handleException(new ManualHandledException(e));
            }
        }
    }
}
