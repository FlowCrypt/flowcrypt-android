/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.kotlin

object StringExtensionsHelper {
  @JvmStatic
  val char160 = 160.toChar()

  @JvmStatic
  val dashesRegex = Regex("^—–|—–$")
}

fun String.normalizeDashes(): String {
  return this.replace(StringExtensionsHelper.dashesRegex, "-----")
}

fun String.normalizeSpaces(): String {
  return this.replace(StringExtensionsHelper.char160, ' ')
}

fun String.normalize(): String {
  return this.normalizeSpaces().normalizeDashes()
}

fun String.countOfMatches(needle: String): Int {
  var result = 0
  var pos = this.indexOf(needle)
  while (pos > -1) {
    ++result
    pos = this.indexOf(needle, pos + needle.length)
  }
  return result
}

fun String.countOfMatchesZeroOneOrMore(needle: String): Int {
  var result = 0
  var pos = this.indexOf(needle)
  while (pos > -1) {
    if (++result == 2) break
    pos = this.indexOf(needle, pos + needle.length)
  }
  return result
}
