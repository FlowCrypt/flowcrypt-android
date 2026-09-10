/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailSignatureUtilTest {
  @Test
  fun testMatchingAndReplacementWithRegexMetaCharacters() {
    val oldSignature = """
      Best regards,
      (111 222 333).*+?\
      $100 \path
    """.trimIndent()
    val newSignature = """
      Sincerely,
      $500 & \path\to\file
    """.trimIndent()
    val message = "Hello World\n\n$oldSignature"

    assertTrue(EmailSignatureUtil.containsSignature(message, oldSignature))
    assertEquals(
      "Hello World\n\n$newSignature",
      EmailSignatureUtil.replaceSignature(message, oldSignature, newSignature)
    )
  }
}
