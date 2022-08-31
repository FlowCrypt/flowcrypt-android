/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BetterInternetAddressTest {

  @Test
  fun genericEmailTest() {
    val validEmails = arrayOf(
      "simple@example.com",
      "very.common@example.com",
      "disposable.style.email.with+symbol@example.com",
      "other.email-with-hyphen@example.com",
      "fully-qualified-domain@example.com",

      // may go to user.name@example.com inbox depending on mail server
      "user.name+tag+sorting@example.com",

      "x@example.com", // one-letter local-part
      "example-indeed@strange-example.com",
      "test/test@test.com", // slashes are a printable character, and allowed

      // local domain name with no TLD, although ICANN highly discourages dotless email addresses,
      // doesn't work with this regex
      // "admin@mailserver1",

      "example@s.example", // see the List of Internet top-level domains

      // space between the quotes
      // doesn't work with this regex, however it is really odd one
      // "\" \"@example.org",

      "\"john..doe\"@example.org", // quoted double dot
      "mailhost!username@example.org", // bangified host route used for UUCP mailers
      "user%example.com@example.org",  // % escaped mail route to user@example.com via example.org

      // local part ending with non-alphanumeric character from the list of
      // allowed printable characters
      "user-@example.org",
    )
    for (email in validEmails) {
      assertTrue(
        "valid email '$email' detected as invalid",
        BetterInternetAddress.isValidEmail(email)
      )
      val parsedEmail = BetterInternetAddress(email)
      assertEquals(email, parsedEmail.emailAddress)
    }

    val invalidEmails = arrayOf(
      // no @ character
      "Abc.example.com",

      // only one @ is allowed outside quotation marks
      "A@b@c@example.com",

      // none of the special characters in this local-part are allowed outside quotation marks
      "a\"b(c)d,e:f;gi[j\\k]l@example.com",

      // quoted strings must be dot separated or the only element making up the local-part
      "just\"not\"right@example.com",

      // spaces, quotes, and backslashes may only exist when within quoted strings and preceded
      // by a backslash
      "this is\"not\\allowed@example.com",

      // even if escaped (preceded by a backslash), spaces, quotes, and backslashes must
      // still be contained by quotes
      "this\\ still\"not\\allowed@example.com",

      // local-part is longer than 64 characters
      "1234567890123456789012345678901234567890123456789012345678901234+x@example.com",

      // Underscore is not allowed in domain part)
      "i_like_underscore@but_its_not_allowed_in_this_part.example.com",

      // icon characters
      "QA[icon]CHOCOLATE[icon]@test.com"
    )
    for (email in invalidEmails) {
      assertFalse(
        "invalid email '$email' detected as valid",
        BetterInternetAddress.isValidEmail(email)
      )
      assertThrows(IllegalArgumentException::class.java) { BetterInternetAddress(email) }
    }
  }

  @Test
  fun localhostEmailTest() {
    val validEmails = arrayOf(
      "root@localhost",
      "user123@localhost"
    )

    for (email in validEmails) {
      assertTrue(
        "valid localhost email '$email' detected as invalid",
        BetterInternetAddress.isValidEmail(email)
      )
      // doesn't work
      // val parsedEmail = BetterInternetAddress(email)
    }

    val invalidEmails = arrayOf(
      "invalid email@localhost",
      "invalid.email@otherhost"
    )

    for (email in invalidEmails) {
      assertFalse(
        "invalid localhost email '$email' detected as valid",
        BetterInternetAddress.isValidEmail(email)
      )
    }
  }

  @Test
  fun testFitRFC822() {
    val invalidEmails = arrayOf(
      // "("  ")"  "<"  ">"  "@"  ","  ";"  ":"  "\"  <">  "."  "["  "]"
      // Must be in quoted-string, to use within a word.
      //https://datatracker.ietf.org/doc/html/rfc822#section-3.3
      "a(b)c<d>e@f,g;h:i\\j\"k.l[m]n <test@example.com>",
    )

    for (email in invalidEmails) {
      assertThrows(
        "invalid email '$email' detected as valid",
        IllegalArgumentException::class.java
      ) { BetterInternetAddress(email) }
    }
  }
}
