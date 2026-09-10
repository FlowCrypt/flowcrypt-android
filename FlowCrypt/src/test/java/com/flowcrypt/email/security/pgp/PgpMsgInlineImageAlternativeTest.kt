/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.extensions.kotlin.toInputStream
import com.flowcrypt.email.util.TestUtil
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.pgpainless.key.protection.SecretKeyRingProtector
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Properties

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [BuildConfig.MIN_SDK_VERSION])
class PgpMsgInlineImageAlternativeTest {
  @Test
  fun testRejectsInlineImageFromUnsignedHtmlAlternative() {
    val source = TestUtil.readResourceAsString(
      "mime/signed-plaintext-unsigned-html-alternative.eml"
    )
    val sourceWithRelatedHtml = addInlineImageToHtmlAlternative(source)

    val processedMimeMessageResult = PgpMsg.processMimeMessage(
      MimeMessage(
        Session.getInstance(Properties()),
        sourceWithRelatedHtml.toInputStream()
      ),
      verificationPublicKeys = DENBOND_VERIFICATION_PUBLIC_KEYS,
      secretKeys = PGPSecretKeyRingCollection(emptyList()),
      protector = SecretKeyRingProtector.unprotectedKeys()
    )

    val verificationResult = processedMimeMessageResult.verificationResult
    assertTrue(verificationResult.hasSignedParts)
    assertFalse(verificationResult.isPartialSigned)
    val displayedContent = requireNotNull(processedMimeMessageResult.blocks.first().content)
    assertTrue(displayedContent.contains("It's a cleartext signed message"))
    assertFalse(displayedContent.contains("ATTACKER-ACCOUNT"))
    assertFalse(displayedContent.contains("attacker.png"))
  }

  @Test
  fun testKeepsInlineImageFromDisplayedUnsignedHtmlAlternative() {
    val source = TestUtil.readResourceAsString(
      "mime/signed-plaintext-unsigned-html-alternative.eml"
    )
    val boundary = "fc-alt-signed-plain-unsigned-html"
    val clearSignedContent = source
      .substringAfter("Content-Transfer-Encoding: 7bit\n\n")
      .substringBefore("\n\n--$boundary")
    val sourceWithUnsignedPlainText = source.replace(
      clearSignedContent,
      "Unsigned plaintext fallback"
    )
    val sourceWithRelatedHtml = addInlineImageToHtmlAlternative(sourceWithUnsignedPlainText)

    val processedMimeMessageResult = PgpMsg.processMimeMessage(
      MimeMessage(
        Session.getInstance(Properties()),
        sourceWithRelatedHtml.toInputStream()
      ),
      verificationPublicKeys = PGPPublicKeyRingCollection(emptyList()),
      secretKeys = PGPSecretKeyRingCollection(emptyList()),
      protector = SecretKeyRingProtector.unprotectedKeys()
    )

    assertFalse(processedMimeMessageResult.verificationResult.hasSignedParts)
    val displayedContent = requireNotNull(processedMimeMessageResult.blocks.first().content)
    assertTrue(displayedContent.contains("ATTACKER-ACCOUNT"))
    assertTrue(displayedContent.contains("src=\"data:image/png"))
    assertTrue(displayedContent.contains("alt=\"attacker.png\""))
  }

  private fun addInlineImageToHtmlAlternative(source: String): String {
    val boundary = "fc-alt-signed-plain-unsigned-html"
    val relatedBoundary = "fc-related-html-image"
    val alternativeBoundaryMarker = "\n--$boundary\n"
    val firstAlternativeBoundaryIndex = source.indexOf(alternativeBoundaryMarker)
    val secondAlternativeBoundaryIndex = source.indexOf(
      alternativeBoundaryMarker,
      firstAlternativeBoundaryIndex + alternativeBoundaryMarker.length
    )
    val closingBoundaryIndex = source.lastIndexOf("\n--$boundary--")
    val htmlAlternative = source.substring(
      secondAlternativeBoundaryIndex + alternativeBoundaryMarker.length,
      closingBoundaryIndex
    ).trimEnd().replace(
      "</body>",
      "<img src=\"cid:attacker-image\" alt=\"attacker.png\"></body>"
    )
    val relatedHtmlAlternative = listOf(
      "Content-Type: multipart/related; boundary=\"$relatedBoundary\"",
      "",
      "--$relatedBoundary",
      htmlAlternative,
      "--$relatedBoundary",
      "Content-Type: image/png; name=\"attacker.png\"",
      "Content-Disposition: inline; filename=\"attacker.png\"",
      "Content-Transfer-Encoding: base64",
      "Content-ID: <attacker-image>",
      "",
      "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl2n0Y" +
          "AAAAASUVORK5CYII=",
      "--$relatedBoundary--"
    ).joinToString("\n")

    return source.replaceRange(
      secondAlternativeBoundaryIndex + alternativeBoundaryMarker.length,
      closingBoundaryIndex,
      relatedHtmlAlternative
    )
  }

  companion object {
    private val DENBOND_VERIFICATION_PUBLIC_KEYS = PgpKey.parseKeys(
      source = TestUtil.readResourceAsByteArray(
        "pgp/keys/denbond7@flowcrypt.test_pub_primary.asc"
      )
    ).pgpKeyRingCollection.pgpPublicKeyRingCollection
  }
}
