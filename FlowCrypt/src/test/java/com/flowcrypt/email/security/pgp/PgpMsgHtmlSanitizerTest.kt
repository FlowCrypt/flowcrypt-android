/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 */

package com.flowcrypt.email.security.pgp

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PgpMsgHtmlSanitizerTest {
  @Test
  fun testStripsArbitraryCssFromEmStyle() {
    val result = sanitize(
      "<em style=\"position:fixed;top:0;left:0;width:100%;height:100%;" +
          "background:red;z-index:99999;\">FAKE SECURITY ALERT</em>"
    )

    assertTrue(result.select("[style]").isEmpty())
    assertEquals("FAKE SECURITY ALERT", result.select("em").text())
  }

  @Test
  fun testStripsEmStyleWithObfuscatedCssAndAttributeCase() {
    val variants = listOf(
      "<EM STYLE=\"POSITION:FIXED;Z-INDEX:99999\">overlay</EM>",
      "<em style=\"  position : fixed ; opacity : 0.01 \">overlay</em>",
      "<em style=\"pos\\69tion:fixed;background:url(https://example.com/x)\">overlay</em>",
      "<em style=\"font-style:italic\">overlay</em>"
    )

    for (variant in variants) {
      val result = sanitize(variant)
      assertTrue("variant: $variant", result.select("[style]").isEmpty())
      assertEquals("variant: $variant", "overlay", result.select("em").text())
    }
  }

  @Test
  fun testStripsEmStyleWhenRemoteContentIsBlocked() {
    val result = sanitize(
      "<img src=\"https://example.com/pixel.png\">" +
          "<em style=\"position:fixed;top:0;width:100%;height:100%\">overlay</em>"
    )

    assertTrue(result.select("[style]").isEmpty())
    assertEquals("overlay", result.select("em").text())
  }

  @Test
  fun testKeepsPlainEmphasis() {
    val result = sanitize("<em>important</em>")

    assertTrue(result.select("[style]").isEmpty())
    assertEquals("important", result.select("em").text())
  }

  private fun sanitize(dirtyHtml: String) =
    Jsoup.parse(requireNotNull(PgpMsg.sanitizeHtmlKeepBasicTags(dirtyHtml)))
}
