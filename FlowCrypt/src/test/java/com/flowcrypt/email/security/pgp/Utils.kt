/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.pgpainless.PGPainless
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object Utils {

  @Suppress("SameParameterValue")
  fun decodeString(s: String, charsetName: String): String {
    val bytes = s.substring(1).split('=').map { Integer.parseInt(it, 16).toByte() }.toByteArray()
    return String(bytes, Charset.forName(charsetName))
  }

  @Suppress("Private")
  fun loadResource(path: String): ByteArray {
    return PgpMsgTest::class.java.classLoader!!
      .getResourceAsStream("${PgpMsgTest::class.simpleName}/$path")
      .readBytes()
  }

  fun loadResourceAsString(path: String, charset: Charset = StandardCharsets.UTF_8): String {
    return String(loadResource(path), charset)
  }

  @Suppress("Private")
  fun loadResourceAsAsciiString(path: String) = loadResourceAsString(path, StandardCharsets.UTF_8)

  fun loadSecretKey(file: String): PGPSecretKeyRing {
    return PGPainless.readKeyRing().secretKeyRing(loadResourceAsAsciiString("keys/$file"))
  }

  fun loadPublicKey(file: String): PGPPublicKeyRing {
    return PGPainless.readKeyRing().publicKeyRing(loadResourceAsAsciiString("keys/$file"))
  }
}
