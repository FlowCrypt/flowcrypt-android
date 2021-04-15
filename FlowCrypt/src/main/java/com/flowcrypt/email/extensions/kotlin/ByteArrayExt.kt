/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.kotlin

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

fun ByteArray.toLowerHexString(): String {
  val result = StringBuilder()
  for (b in this) {
    result.append("%02x".format(b))
  }
  return result.toString()
}

fun ByteArray.toUpperHexString(): String {
  val result = StringBuilder()
  for (b in this) {
    result.append("%02X".format(b))
  }
  return result.toString()
}

fun ByteArray.decodeToCharArray(charset: Charset = StandardCharsets.UTF_8): CharArray {
  val buffer = charset.newDecoder()
      .onMalformedInput(CodingErrorAction.REPORT)
      .onUnmappableCharacter(CodingErrorAction.REPORT)
      .decode(ByteBuffer.wrap(this))
  val result = CharArray(buffer.remaining())
  buffer.get(result)
  return result
}
