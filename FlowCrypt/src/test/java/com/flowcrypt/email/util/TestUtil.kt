/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.util

import org.apache.commons.io.IOUtils
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object TestUtil {
  @Suppress("unused")
  fun readResourceAsByteArray(path: String): ByteArray {
    return IOUtils.toByteArray(TestUtil::class.java.classLoader!!.getResourceAsStream(path))
  }

  fun readResourceAsString(path: String, charset: Charset = StandardCharsets.UTF_8): String {
    return IOUtils.toString(TestUtil::class.java.classLoader!!.getResourceAsStream(path), charset)
  }

  @Suppress("SameParameterValue")
  fun decodeString(s: String, charsetName: String): String {
    val bytes = s.substring(1).split('=').map { Integer.parseInt(it, 16).toByte() }.toByteArray()
    return String(bytes, Charset.forName(charsetName))
  }
}
