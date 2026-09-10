/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import org.eclipse.angus.mail.iap.ConnectionException
import org.eclipse.angus.mail.smtp.SMTPSendFailedException
import org.eclipse.angus.mail.util.MailConnectException
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
    if (t is UserRecoverableAuthException) {
      if ("BadAuthentication" == t.message) {
        return true
      }
    }

    if (t is MailConnectException
      || t is SMTPSendFailedException
      || t is UnknownHostException
      || t is SocketTimeoutException
      || t is ConnectionException
      || t is java.net.ConnectException
      || t is UserRecoverableAuthException
      || t is AuthenticationFailedException
      || t is UserRecoverableAuthIOException
      || t is FlowCryptException
    ) {
      return false
    }

    if (t is IOException) {
      //Google network errors.
      if ("NetworkError".equals(t.message, ignoreCase = true)
        || "Error on service connection.".equals(t.message, ignoreCase = true)
      ) {
        return false
      }

      if ("Connection dropped by server?".equals(t.message, ignoreCase = true)) {
        return false
      }

      if (t is GoogleJsonResponseException) {
        return false
      }
    }

    if (t is SSLHandshakeException
      || t is SSLProtocolException
      || t is MessagingException
    ) {
      t.message?.let {
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

    if (t is StoreClosedException || t is FolderClosedException) {
      //Connection limit exceeded
      if ("failed to create new store connection".equals(t.message, ignoreCase = true)) {
        return false
      }

      if ("Lost folder connection to server".equals(t.message, ignoreCase = true)) {
        return false
      }

      if ("* BYE System Error".equals(t.message, ignoreCase = true)) {
        return false
      }

      if ("Error reading input stream;".equals(t.message, ignoreCase = true)) {
        return false
      }
    }

    if (t is GoogleAuthException) {
      if ("InternalError".equals(t.message, ignoreCase = true) ||
        "ServiceDisabled".equals(t.message, ignoreCase = true)
      ) {
        return false
      }
    }

    if (t is RuntimeException) {
      if ("error:04000044:RSA routines:OPENSSL_internal:internal error".equals(
          t.message,
          ignoreCase = true
        )
      ) {
        return false
      }
    }

    if (t is BadPaddingException) {
      val errorMsg =
        "error:0407109F:rsa routines:RSA_padding_check_PKCS1_type_2:pkcs decoding error"
      return !errorMsg.equals(t.message, ignoreCase = true)
    }

    if (t is IllegalStateException) {
      if ("This operation is not allowed on a closed folder".equals(t.message, ignoreCase = true)) {
        return false
      }

      if ("Not connected".equals(t.message, ignoreCase = true)) {
        return false
      }
    }

    if (t is CancellationException) {
      //we can drop such an error because we always use [CoroutineScope]
      return false
    }

    return true
  }
}
