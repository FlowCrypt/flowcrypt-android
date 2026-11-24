/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.Constants
import org.bouncycastle.openpgp.PGPException
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class PgpArmorLimitTest {
  @Test
  fun testReadSignedClearTextMessageRejectsOversizedInput() {
    val exception: Throwable? = try {
      PgpArmor.readSignedClearTextMessage(
        ByteArray(Constants.MAX_PGP_MESSAGE_SIZE_IN_BYTES + 1).inputStream()
      )
      null
    } catch (e: Exception) {
      e
    }

    assertTrue("No error for oversized input", exception is PGPException)
    assertTrue(
      "Error is not a size limit failure: ${exception?.message}",
      generateSequence(exception) { it.cause }.any {
        it is IOException && it.message?.contains("exceeds the limit") == true
      }
    )
  }
}
