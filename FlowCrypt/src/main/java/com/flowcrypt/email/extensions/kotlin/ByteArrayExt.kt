/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.kotlin

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

// KMP search https://stackoverflow.com/a/67275192/1540501
fun ByteArray.indexOf(needle: ByteArray): Int {
  // needle is empty
  if (needle.isEmpty()) return 0

  // haystack's length is less than that of needle
  if (needle.size > size) return -1

  // pre construct failure array for needle pattern
  val failure = IntArray(needle.size)
  val n = needle.size
  failure[0] = -1
  for (j in 1 until n) {
    var i = failure[j - 1]
    while (needle[j] != needle[i + 1] && i >= 0) i = failure[i]
    if (needle[j] == needle[i + 1]) failure[j] = i + 1 else failure[j] = -1
  }

  // find match
  var i = 0
  var j = 0
  val haystackLen = size
  val needleLen = needle.size
  while (i < haystackLen && j < needleLen) {
    if (this[i] == needle[j]) {
      i++
      j++
    } else if (j == 0) {
      i++
    } else {
      j = failure[j - 1] + 1
    }
  }
  return if (j == needleLen) i - needleLen else -1
}
