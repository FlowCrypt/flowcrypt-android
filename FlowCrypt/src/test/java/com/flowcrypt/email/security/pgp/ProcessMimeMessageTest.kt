/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.api.retrofit.response.model.GenericMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.MsgBlock
import com.flowcrypt.email.util.TestUtil
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.pgpainless.key.protection.PasswordBasedSecretKeyRingProtector
import org.pgpainless.util.Passphrase
import java.util.Properties

/**
 * @author Denis Bondarenko
 *         Date: 4/19/22
 *         Time: 6:02 PM
 *         E-mail: DenBond7@gmail.com
 */
class ProcessMimeMessageTest {
  @Test
  fun testProcessProtonmailPgpMime() {
    val processedMimeMessageResult = PgpMsg.processMimeMessage(
      MimeMessage(
        Session.getInstance(Properties()),
        TestUtil.readResourceAsByteArray("mime/protonmail_pgp_mime.eml").inputStream()
      ),
      VERIFICATION_PUBLIC_KEYS,
      SECRET_KEYS,
      protector
    )

    assertEquals(
      "It's an encrypted message\n\n    \n                    " +
          "\n                \n        Sent with ProtonMail secure email.",
      processedMimeMessageResult.text
    )

    val verificationResult = processedMimeMessageResult.verificationResult
    assertTrue(verificationResult.hasEncryptedParts)
    assertTrue(verificationResult.hasSignedParts)
    assertFalse(verificationResult.hasMixedSignatures)
    assertFalse(verificationResult.isPartialSigned)
    assertFalse(verificationResult.hasBadSignatures)
    assertFalse(verificationResult.hasUnverifiedSignatures)

    val extractedBlocks = processedMimeMessageResult.blocks
    assertEquals(1, extractedBlocks.size)
    val msgBlock = extractedBlocks.first()
    assertThat(msgBlock, instanceOf(GenericMsgBlock::class.java))
    assertEquals(msgBlock.type, MsgBlock.Type.PLAIN_HTML)
    assertFalse(msgBlock.isOpenPGPMimeSigned)
  }

  companion object {
    private val VERIFICATION_PUBLIC_KEYS = PgpKey.parseKeys(
      TestUtil.readResourceAsByteArray("pgp/keys/default@flowcrypt.test_fisrtKey_pub.asc")
    ).pgpKeyRingCollection.pgpPublicKeyRingCollection

    private val SECRET_KEYS = PgpKey.parseKeys(
      TestUtil.readResourceAsByteArray("pgp/keys/default@flowcrypt.test_fisrtKey_prv_default.asc")
    ).pgpKeyRingCollection.pgpSecretKeyRingCollection

    private val protector = PasswordBasedSecretKeyRingProtector.forKey(
      SECRET_KEYS.first(),
      Passphrase.fromPassword("android")
    )
  }
}
