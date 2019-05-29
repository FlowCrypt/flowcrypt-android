/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.sun.mail.iap.ConnectionException
import com.sun.mail.smtp.SMTPSendFailedException
import com.sun.mail.util.MailConnectException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.crypto.BadPaddingException
import javax.mail.AuthenticationFailedException
import javax.mail.FolderClosedException
import javax.mail.MessagingException
import javax.mail.StoreClosedException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLProtocolException

/**
 * This class decides what an error can be handled by ACRA.
 *
 * @author Denis Bondarenko
 * Date: 25.01.2018
 * Time: 10:22
 * E-mail: DenBond7@gmail.com
 */

object ExceptionResolver {

  /**
   * Check if need to handle a happened error with  ACRA
   *
   * @param e A happened error
   * @return true if need to handle such exception with ACRA and send logs to the backend, false - otherwise.
   */
  fun isHandlingNeeded(e: Throwable): Boolean {
    if (e is MailConnectException
        || e is SMTPSendFailedException
        || e is UnknownHostException
        || e is SocketTimeoutException
        || e is ConnectionException
        || e is java.net.ConnectException
        || e is UserRecoverableAuthException
        || e is AuthenticationFailedException
        || e is UserRecoverableAuthIOException
        || e is FlowCryptException) {
      return false
    }

    if (e is IOException) {
      //Google network errors.
      if ("NetworkError".equals(e.message, ignoreCase = true) || "Error on service connection.".equals(e.message, ignoreCase = true)) {
        return false
      }

      if (e is GoogleJsonResponseException) {
        return false
      }
    }

    if (e is SSLHandshakeException
        || e is SSLProtocolException
        || e is MessagingException) {
      e.message?.let {
        if (e.message!!.contains("Connection closed by peer")
            || e.message!!.contains("I/O error during system call")
            || e.message!!.contains("Failure in SSL library, usually a protocol error")
            || e.message!!.contains("Handshake failed")
            || e.message!!.contains("Exception reading response")
            || e.message!!.contains("connection failure")) {
          return false
        }
      }
    }

    if (e is StoreClosedException || e is FolderClosedException) {
      //Connection limit exceeded
      if ("failed to create new store connection".equals(e.message, ignoreCase = true)) {
        return false
      }

      if ("Lost folder connection to server".equals(e.message, ignoreCase = true)) {
        return false
      }
    }

    if (e is GoogleAuthException) {
      if ("InternalError".equals(e.message, ignoreCase = true)) {
        return false
      }
    }

    if (e is RuntimeException) {
      if ("error:04000044:RSA routines:OPENSSL_internal:internal error".equals(e.message, ignoreCase = true)) {
        return false
      }
    }

    if (e is BadPaddingException) {
      val errorMsg = "error:0407109F:rsa routines:RSA_padding_check_PKCS1_type_2:pkcs decoding error"
      return !errorMsg.equals(e.message, ignoreCase = true)
    }

    return true
  }
}
