/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

/*
 * © 2021-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.util

import com.flowcrypt.email.extensions.kotlin.isValidEmail

// https://en.wikipedia.org/wiki/Email_address#Internationalization_examples
class BetterInternetAddress(str: String, verifySpecialCharacters: Boolean = true) {
  val personalName: String?
  val emailAddress: String

  init {
    val personalNameWithEmailMatch = VALID_PERSONAL_NAME_WITH_EMAIL_REGEX.find(str)
    val emailMatch = str.matches(VALID_EMAIL_REGEX)
    when {
      personalNameWithEmailMatch != null -> {
        val group = personalNameWithEmailMatch.groupValues
        personalName = group[1].trim()
        emailAddress = group[2]
        if (
          verifySpecialCharacters &&
          personalName.replace("(\")([^\"]*)(\")".toRegex(), "$2")
            .matches(SPECIAL_CHARACTERS_REGEX) &&
          !personalName.matches(DOUBLE_QUOTED_TEXT_REGEX)
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
    if (!isValidEmail(emailAddress)) throw IllegalArgumentException("Invalid email address")
  }

  companion object {
    private const val ALPHANUM = "\\p{L}\\u0900-\\u097F0-9"
    private const val VALID_EMAIL =
      "(?:[${ALPHANUM}!#\$%&'*+/=?^_`{|}~-]+(?:\\.[${ALPHANUM}!#\$%&'*+/=?^" +
          "_`{|}~-]+)*|\"(?:[\\x01-\\x08" +
          "\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f" +
          "])*\")@((?:(?:[${ALPHANUM}](?:[${ALPHANUM}-]*[${ALPHANUM}])?\\.)+[${ALPHANUM}](?:[" +
          "${ALPHANUM}-]*[${ALPHANUM}])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:" +
          "25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[${ALPHANUM}-]*[${ALPHANUM}]:(?:[\\x01-\\x08\\x0b" +
          "\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)])" +
          "|localhost)"
    private const val VALID_PERSONAL_NAME_WITH_EMAIL =
      "([$ALPHANUM\\p{Punct}\\p{Space}]*)<($VALID_EMAIL)>"

    val VALID_EMAIL_REGEX = VALID_EMAIL.toRegex(RegexOption.IGNORE_CASE)
    private val VALID_PERSONAL_NAME_WITH_EMAIL_REGEX = VALID_PERSONAL_NAME_WITH_EMAIL.toRegex()

    // if these appear in the display-name they must be double quoted
    private val SPECIAL_CHARACTERS_REGEX = ".*[()<>\\[\\]:;@\\\\,.\"].*".toRegex()

    // double quotes at ends only
    private val DOUBLE_QUOTED_TEXT_REGEX = "(^\")([\\S\\s]*)(\"\$)".toRegex()
    private const val MAX_LOCAL_PART_LENGTH = 64

    fun isValidEmail(email: String): Boolean {
      if (VALID_EMAIL_REGEX.matchEntire(email) == null) return false
      return email.indexOf('@') < MAX_LOCAL_PART_LENGTH
    }

    @Suppress("unused")
    fun areValidEmails(emails: Iterable<String>): Boolean {
      return emails.all { it.isValidEmail() }
    }
  }
}
