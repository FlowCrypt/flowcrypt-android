/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.Constants
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class PgpSignatureLimitTest {
  @Test
  fun testExtractClearTextRejectsOversizedContent() {
    val source = "-----BEGIN PGP SIGNED MESSAGE-----\n" +
      "Hash: SHA256\n\n" +
      "a".repeat(Constants.MAX_PGP_MESSAGE_SIZE_IN_BYTES + 1) + "\n" +
      "-----BEGIN PGP SIGNATURE-----\n\n-----END PGP SIGNATURE-----"

    val exception: Throwable? = try {
      PgpSignature.extractClearText(source, isSilent = false)
      null
    } catch (e: Exception) {
      e
    }

    assertTrue("No error for oversized cleartext", exception != null)
    assertTrue(
      "Error is not a size limit failure: ${exception?.message}",
      generateSequence(exception) { it.cause }.any {
        it is IOException && it.message?.contains("exceeds the limit") == true
      }
    )
  }
}
