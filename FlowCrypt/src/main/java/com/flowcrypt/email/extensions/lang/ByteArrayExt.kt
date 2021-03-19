/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.lang

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
