/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.kotlin

private const val upperCaseHex = "0123456789ABCDEF"
private const val lowerCaseHex = "0123456789abcdef"

fun ByteArray.toHexString(upperCase: Boolean = true): String {
  val chars = CharArray(size * 2)
  val alphabet = if (upperCase) upperCaseHex else lowerCaseHex
  var i = 0
  for (b in this) {
    val v = b.toInt()
    chars[i++] = alphabet[((v shr 4) and 15)]
    chars[i++] = alphabet[v and 15]
  }
  return String(chars)
}
