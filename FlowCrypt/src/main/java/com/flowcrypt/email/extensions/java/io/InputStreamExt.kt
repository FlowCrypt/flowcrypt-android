/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.java.io

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

fun InputStream.readText(charset: Charset = StandardCharsets.UTF_8): String =
  String(readBytes(), charset)

/**
 * Read all bytes, but fail with [IOException] as soon as more than [maxBytes] bytes were read.
 * Use it for untrusted sources where the size of the content is not known in advance.
 */
fun InputStream.readBytesWithLimit(maxBytes: Int): ByteArray {
  require(maxBytes >= 0) { "maxBytes must not be negative" }
  val output = ByteArrayOutputStream()
  val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
  var total = 0L
  while (true) {
    val read = read(buffer)
    if (read == -1) break
    total += read
    if (total > maxBytes) {
      throw IOException("The stream exceeds the limit of $maxBytes bytes")
    }
    output.write(buffer, 0, read)
  }
  return output.toByteArray()
}
