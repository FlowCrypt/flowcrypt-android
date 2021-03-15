/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.BuildConfig
import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.openpgp.PGPKeyRing
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * @author Denis Bondarenko
 *         Date: 3/11/21
 *         Time: 1:49 PM
 *         E-mail: DenBond7@gmail.com
 */
object PgpArmorUtils {
  private const val HEADER_NAME_COMMENT = "Comment"

  @Throws(IOException::class)
  fun toAsciiArmoredString(pgpKeyRing: PGPKeyRing): String {
    ByteArrayOutputStream().use { out ->
      ArmoredOutputStream(out).use { armoredOut ->
        addHeaders(armoredOut)
        pgpKeyRing.encode(armoredOut)
      }
      return out.toString()
    }
  }

  private fun addHeaders(armoredOutputStream: ArmoredOutputStream) {
    armoredOutputStream.setHeader(ArmoredOutputStream.VERSION_HDR, "FlowCrypt ${BuildConfig.VERSION_NAME} Gmail Encryption")
    armoredOutputStream.setHeader(HEADER_NAME_COMMENT, "Seamlessly send and receive encrypted email")
  }
}