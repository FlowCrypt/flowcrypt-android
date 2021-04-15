/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.kotlin

fun CharSequence.toCharArray(): CharArray {
  val result = CharArray(length)
  for (i in 0 until length) {
    result[i] = elementAt(i)
  }
  return result
}
