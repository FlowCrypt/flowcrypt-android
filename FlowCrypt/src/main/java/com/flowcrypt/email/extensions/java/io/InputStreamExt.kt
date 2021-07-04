/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.java.io

import java.io.BufferedInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Base64

fun InputStream.readText(charset: Charset = StandardCharsets.UTF_8): String
    = String(readBytes(), charset)

// See https://stackoverflow.com/a/39099064/1540501
fun InputStream.toBase64EncodedString(): String {
  val bufferSize = 3 * 1024
  BufferedInputStream(this, bufferSize).use { stream ->
    val encoder = Base64.getEncoder()
    val result = StringBuilder()
    val chunk = ByteArray(bufferSize)
    var len = 0
    while (stream.read(chunk).also { len = it } == bufferSize) {
      result.append(encoder.encodeToString(chunk))
    }
    if (len > 0) result.append(encoder.encodeToString(chunk.copyOf(len)))
    return result.toString()
  }
}
