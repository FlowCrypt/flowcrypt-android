/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class PgpPwdTest {
  @Test
  fun testEstimateStrength() {
    val actualResult = PgpPwd.estimateStrength(
        BigInteger("88946283684264"), PgpPwd.PwdType.PASSPHRASE)
    val expectedResult = PgpPwd.PwdStrengthResult(
        word = PgpPwd.Word(
            match = "week",
            word = "poor",
            bar = 30,
            color = "darkred",
            pass = false
        ),
        seconds = BigInteger.valueOf(1111829),
        time = "2 weeks"
    )
    assertEquals(expectedResult, actualResult)
  }

  @Test
  fun testBytesToPassword() {
    val bytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 10, 11, 12, 13, 14, 15)
    assertEquals("1234-5678-90AB-CDEF", PgpPwd.bytesToPassword(bytes))
  }

  @Test
  fun testBytesToPasswordRejectsTooShortByteArray() {
    val bytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)

    // I'd better use assertThrows(), but the strange thing happens:
    // in the IntelliJ it resolves fine, but during compilation it says
    // something like "Unresolved symbol assertThrows"
    try {
      PgpPwd.bytesToPassword(bytes)
      throw Exception("IllegalArgumentException not thrown")
    } catch (ex: IllegalArgumentException) {
      // this is expected
    }
  }

  private val passwordRegex = Regex("[0-9A-Z]{4}-[0-9A-Z]{4}-[0-9A-Z]{4}-[0-9A-Z]{4}")

  @Test
  fun testRandom() {
    val password = PgpPwd.random()
    assertTrue("Password structure mismatch", passwordRegex.matches(password))
  }
}
