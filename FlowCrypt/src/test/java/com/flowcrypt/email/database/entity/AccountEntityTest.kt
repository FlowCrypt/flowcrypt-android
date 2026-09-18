/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.database.entity

import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import org.jose4j.jwa.AlgorithmConstraints
import org.jose4j.jws.AlgorithmIdentifiers
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.JwtClaims
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountEntityTest {
  @Test
  fun testGoogleAccountUsesEmailFromIdToken() {
    val credential = genGoogleIdTokenCredential(
      id = "credential-id@example.com",
      tokenEmail = "token-email@example.com"
    )

    val account = AccountEntity(
      googleIdTokenCredential = credential,
      useCustomerFesUrl = false
    )

    assertEquals("token-email@example.com", account.email)
    assertEquals("token-email@example.com", account.username)
  }

  @Test
  fun testGoogleAccountRejectsIdTokenWithoutEmail() {
    val credential = genGoogleIdTokenCredential(
      id = "credential-id@example.com",
      tokenEmail = null
    )

    assertThrows(IllegalArgumentException::class.java) {
      AccountEntity(
        googleIdTokenCredential = credential,
        useCustomerFesUrl = false
      )
    }
  }

  private fun genGoogleIdTokenCredential(
    id: String,
    tokenEmail: String?
  ): GoogleIdTokenCredential {
    val claims = JwtClaims().apply {
      subject = "test-google-account-id"
      tokenEmail?.let { setStringClaim("email", it) }
    }
    val jws = JsonWebSignature().apply {
      payload = claims.toJson()
      setAlgorithmConstraints(AlgorithmConstraints.NO_CONSTRAINTS)
      algorithmHeaderValue = AlgorithmIdentifiers.NONE
    }

    return GoogleIdTokenCredential(
      id = id,
      idToken = jws.compactSerialization,
      displayName = null,
      familyName = null,
      givenName = null,
      profilePictureUri = null,
      phoneNumber = null
    )
  }
}
