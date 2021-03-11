/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.BuildConfig
import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKey
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.util.io.Streams
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * @author Denis Bondarenko
 *         Date: 3/11/21
 *         Time: 1:49 PM
 *         E-mail: DenBond7@gmail.com
 */
object PgpArmorUtils {
  @Throws(IOException::class)
  fun toAsciiArmoredString(secretKeys: PGPSecretKeyRing): String {
    return toAsciiArmoredString(secretKeys.encoded)
  }

  @Throws(IOException::class)
  fun toAsciiArmoredString(pgpSecretKey: PGPSecretKey): String {
    return toAsciiArmoredString(pgpSecretKey.encoded)
  }

  @Throws(IOException::class)
  fun toAsciiArmoredString(publicKeys: PGPPublicKeyRing): String {
    return toAsciiArmoredString(publicKeys.encoded)
  }

  @Throws(IOException::class)
  fun toAsciiArmoredString(pgpPublicKey: PGPPublicKey): String {
    return toAsciiArmoredString(pgpPublicKey.encoded)
  }

  @Throws(IOException::class)
  private fun toAsciiArmoredString(byteArray: ByteArray): String {
    ByteArrayOutputStream().use { out ->
      ArmoredOutputStream(out).use {
        it.setHeader(ArmoredOutputStream.VERSION_HDR, "FlowCrypt ${BuildConfig.VERSION_NAME} Gmail Encryption")
        it.setHeader("Comment", "Seamlessly send and receive encrypted email")
        Streams.pipeAll(ByteArrayInputStream(byteArray), it)
      }
      return out.toString()
    }
  }
}