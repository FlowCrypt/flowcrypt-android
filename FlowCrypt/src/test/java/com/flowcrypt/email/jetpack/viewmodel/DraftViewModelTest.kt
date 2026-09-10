/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class DraftViewModelTest {
  @Test
  fun testRemoveSignatureWithRegexMetaCharacters() {
    val signature = """
      Kind regards

      (123 456
      [example].*+?\
    """.trimIndent()
    val message = "Message body\n\n$signature"

    assertEquals("Message body", DraftViewModel.removeSignature(message, signature))
  }
}
