/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.kotlin

import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

// Based on this: https://stackoverflow.com/a/18515106/1540501
fun CharArray.toByteArray(charset: Charset = StandardCharsets.UTF_8): ByteArray {
  val buffer = charset.encode(CharBuffer.wrap(this))
  val result = ByteArray(buffer.remaining())
  buffer.get(result)
  return result
}
