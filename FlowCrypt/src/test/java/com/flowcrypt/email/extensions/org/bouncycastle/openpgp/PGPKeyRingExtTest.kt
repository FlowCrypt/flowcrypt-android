/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 */

package com.flowcrypt.email.extensions.org.bouncycastle.openpgp

import org.bouncycastle.openpgp.PGPException
import org.junit.Assert.assertThrows
import org.junit.Test

class PGPKeyRingExtTest {
  @Test
  fun testRejectsMoreThanMaximumUserIds() {
    val userIds = (0..MAX_USER_IDS_PER_KEY).map { "user-$it" }.iterator()

    assertThrows(PGPException::class.java) {
      requireAcceptableUserIdCount(userIds)
    }
  }

  @Test
  fun testAcceptsMaximumUserIds() {
    val userIds = (0 until MAX_USER_IDS_PER_KEY).map { "user-$it" }.iterator()

    requireAcceptableUserIdCount(userIds)
  }
}
