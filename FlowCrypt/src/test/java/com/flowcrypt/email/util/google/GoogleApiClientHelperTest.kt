/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.util.google

import com.flowcrypt.email.Constants
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleApiClientHelperTest {
  @Test
  fun testGmailAccessIsGrantedWhenResultContainsGmailScope() {
    val grantedScopes = listOf("another-scope", Constants.SCOPE_MAIL_GOOGLE_COM)

    assertTrue(GoogleApiClientHelper.isGmailAccessGranted(grantedScopes))
  }

  @Test
  fun testGmailAccessIsNotGrantedWhenResultDoesNotContainGmailScope() {
    val grantedScopes = listOf("another-scope")

    assertFalse(GoogleApiClientHelper.isGmailAccessGranted(grantedScopes))
  }
}
