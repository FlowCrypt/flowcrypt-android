/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
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
import jakarta.mail.AuthenticationFailedException
import jakarta.mail.FolderClosedException
import jakarta.mail.MessagingException
import jakarta.mail.StoreClosedException
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.crypto.BadPaddingException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLProtocolException

/**
 * This class decides what an error can be handled by ACRA.
 *
 * @author Denys Bondarenko
 */
object ExceptionResolver {

  /**
   * Check if need to handle a happened error with  ACRA
   *
   * @param t A happened error
   * @return true if need to handle such exception with ACRA and send logs to the backend, false - otherwise.
   */
  fun isHandlingNeeded(t: Throwable): Boolean {
    val e =
      if (t is ManualHandledException) {
        t.cause ?: t
      } else {
        t
      }

    if (e is UserRecoverableAuthException) {
      if ("BadAuthentication" == e.message) {
        return true
      }
    }

    if (e is MailConnectException
      || e is SMTPSendFailedException
      || e is UnknownHostException
      || e is SocketTimeoutException
      || e is ConnectionException
      || e is java.net.ConnectException
      || e is UserRecoverableAuthException
      || e is AuthenticationFailedException
      || e is UserRecoverableAuthIOException
      || e is FlowCryptException
    ) {
      return false
    }

    if (e is IOException) {
      //Google network errors.
      if ("NetworkError".equals(e.message, ignoreCase = true)
        || "Error on service connection.".equals(e.message, ignoreCase = true)
      ) {
        return false
      }

      if ("Connection dropped by server?".equals(e.message, ignoreCase = true)) {
        return false
      }

      if (e is GoogleJsonResponseException) {
        return false
      }
    }

    if (e is SSLHandshakeException
      || e is SSLProtocolException
      || e is MessagingException
    ) {
      e.message?.let {
        if (it.contains("Connection closed by peer")
          || it.contains("I/O error during system call")
          || it.contains("Failure in SSL library, usually a protocol error")
          || it.contains("Handshake failed")
          || it.contains("Exception reading response")
          || it.contains("connection failure")
          || it.contains("Connection reset;")
        ) {
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

      if ("* BYE System Error".equals(e.message, ignoreCase = true)) {
        return false
      }

      if ("Error reading input stream;".equals(e.message, ignoreCase = true)) {
        return false
      }
    }

    if (e is GoogleAuthException) {
      if ("InternalError".equals(e.message, ignoreCase = true) ||
        "ServiceDisabled".equals(e.message, ignoreCase = true)
      ) {
        return false
      }
    }

    if (e is RuntimeException) {
      if ("error:04000044:RSA routines:OPENSSL_internal:internal error".equals(
          e.message,
          ignoreCase = true
        )
      ) {
        return false
      }
    }

    if (e is BadPaddingException) {
      val errorMsg =
        "error:0407109F:rsa routines:RSA_padding_check_PKCS1_type_2:pkcs decoding error"
      return !errorMsg.equals(e.message, ignoreCase = true)
    }

    if (e is IllegalStateException) {
      if ("This operation is not allowed on a closed folder".equals(e.message, ignoreCase = true)) {
        return false
      }

      if ("Not connected".equals(e.message, ignoreCase = true)) {
        return false
      }
    }

    if (e is CancellationException) {
      //we can drop such an error because we always use [CoroutineScope]
      return false
    }

    return true
  }
}
