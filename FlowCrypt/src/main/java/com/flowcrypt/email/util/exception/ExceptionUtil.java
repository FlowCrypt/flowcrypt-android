/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception;

import android.text.TextUtils;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.sun.mail.iap.ConnectionException;
import com.sun.mail.util.MailConnectException;

import org.acra.ACRA;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.mail.AuthenticationFailedException;
import javax.mail.FolderClosedException;
import javax.mail.MessagingException;
import javax.mail.StoreClosedException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLProtocolException;

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
                || (e instanceof SocketTimeoutException)
                || (e instanceof ConnectionException)
                || (e instanceof UserRecoverableAuthException)
                || (e instanceof AuthenticationFailedException)
                || (e instanceof UserRecoverableAuthIOException)
                || (e instanceof FlowCryptException)) {
            return false;
        }

        if (e instanceof IOException) {
            //Google network errors.
            if ("NetworkError".equalsIgnoreCase(e.getMessage())
                    || "Error on service connection.".equalsIgnoreCase(e.getMessage())) {
                return false;
            }

            if (e instanceof GoogleJsonResponseException) {
                return false;
            }
        }

        if ((e instanceof SSLHandshakeException
                || e instanceof SSLProtocolException
                || e instanceof MessagingException)) {
            if (!TextUtils.isEmpty(e.getMessage())) {
                if (e.getMessage().contains("Connection closed by peer")
                        || e.getMessage().contains("I/O error during system call")
                        || e.getMessage().contains("Failure in SSL library, usually a protocol error")
                        || e.getMessage().contains("Handshake failed")
                        || e.getMessage().contains("Exception reading response")
                        || e.getMessage().contains("connection failure")) {
                    return false;
                }
            }
        }

        if (e instanceof StoreClosedException || e instanceof FolderClosedException) {
            //Connection limit exceeded
            if ("failed to create new store connection".equalsIgnoreCase(e.getMessage())) {
                return false;
            }

            if ("Lost folder connection to server".equalsIgnoreCase(e.getMessage())) {
                return false;
            }
        }

        if (e instanceof GoogleAuthException) {
            if ("InternalError".equalsIgnoreCase(e.getMessage())) {
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
