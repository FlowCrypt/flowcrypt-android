/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security.pgp


import org.pgpainless.decryption_verification.cleartext_signatures.ClearsignedMessageUtil
import org.pgpainless.decryption_verification.cleartext_signatures.MultiPassStrategy
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * @author Denis Bondarenko
 *         Date: 8/31/21
 *         Time: 2:45 PM
 *         E-mail: DenBond7@gmail.com
 */
object PgpSignature {
  fun extractClearText(source: String?): String? {
    return extractClearText(ByteArrayInputStream(source?.toByteArray()))
  }

  @Suppress("MemberVisibilityCanBePrivate")
  fun extractClearText(srcInputStream: InputStream): String? {
    srcInputStream.use { srcStream ->
      return try {
        val multiPassStrategy = MultiPassStrategy.keepMessageInMemory()
        ClearsignedMessageUtil.detachSignaturesFromInbandClearsignedMessage(
          srcStream,
          multiPassStrategy.messageOutputStream
        )
        String(multiPassStrategy.bytes)
      } catch (e: Exception) {
        e.printStackTrace()
        null
      }
    }
  }
}
