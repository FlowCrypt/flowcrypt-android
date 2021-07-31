/*
 * © 2021-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.util

import com.flowcrypt.email.extensions.kotlin.isValidEmail

// https://en.wikipedia.org/wiki/Email_address#Internationalization_examples
class BetterInternetAddress(str: String, verifySpecialCharacters: Boolean = true) {

  companion object {
    const val alphanum = "\\p{L}\\u0900-\\u097F0-9"
    const val validEmail = "(?:[${alphanum}!#\$%&'*+/=?^_`{|}~-]+(?:\\.[${alphanum}!#\$%&'*+/=?^" +
        "_`{|}~-]+)*|\"(?:[\\x01-\\x08" +
        "\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f" +
        "])*\")@(?:(?:[${alphanum}](?:[${alphanum}-]*[${alphanum}])?\\.)+[${alphanum}](?:[" +
        "${alphanum}-]*[${alphanum}])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:" +
        "25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[${alphanum}-]*[${alphanum}]:(?:[\\x01-\\x08\\x0b" +
        "\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)])"
    private const val validPersonalNameWithEmail =
      "([$alphanum\\p{Punct}\\p{Space}]*)<($validEmail)>"

    private val validEmailRegex = validEmail.toRegex()
    private val validPersonalNameWithEmailRegex = validPersonalNameWithEmail.toRegex()
    // if these appear in the display-name they must be double quoted
    private val containsSpecialCharacterRegex = ".*[()<>\\[\\]:;@\\\\,.\"].*".toRegex()
    // double quotes at ends only
    private val doubleQuotedTextRegex = "\"[^\"]*\"".toRegex()

    fun isValidEmail(email: String): Boolean {
      return validEmailRegex.matchEntire(email) != null
    }

    fun areValidEmails(emails: Iterable<String>): Boolean {
      return emails.all { it.isValidEmail() }
    }
  }

  val personalName: String?
  val emailAddress: String

  init {
    val personalNameWithEmailMatch = validPersonalNameWithEmailRegex.find(str)
    val emailMatch = str.matches(validEmailRegex)
    when {
      personalNameWithEmailMatch != null -> {
        val group = personalNameWithEmailMatch.groupValues
        personalName = group[1].trim()
        emailAddress = group[2]
        if (
          verifySpecialCharacters &&
          personalName.matches(containsSpecialCharacterRegex) &&
          !personalName.matches(doubleQuotedTextRegex)
        ) {
          throw IllegalArgumentException(
            "Invalid email $str - display name containing special characters must be fully double quoted"
          )
        }
      }
      emailMatch -> {
        personalName = null
        emailAddress = str
      }
      else -> throw IllegalArgumentException("Invalid email $str")
    }
  }
}
