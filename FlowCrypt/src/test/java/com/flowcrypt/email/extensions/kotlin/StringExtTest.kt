/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.kotlin

import com.flowcrypt.email.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class StringExtTest {
  @Test
  fun testToEscapedHtmlEscapesSpecialCharacters() {
    assertEquals(
      "&amp;&quot;&#39;&lt;&gt;&#x2F;<br>",
      "&\"'<>/\n".toEscapedHtml()
    )
  }

  @Test
  fun testToEscapedHtmlThrowsWhenOutputExceedsLimit() {
    val source = "/".repeat(Constants.MAX_ESCAPED_HTML_OUTPUT_LENGTH_IN_CHARS)
    assertThrows(IOException::class.java) { source.toEscapedHtml() }
  }
}
