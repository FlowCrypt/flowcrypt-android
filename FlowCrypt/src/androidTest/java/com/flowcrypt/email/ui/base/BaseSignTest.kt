/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.base

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.util.google.GoogleApiClientHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import okhttp3.mockwebserver.RecordedRequest
import org.jose4j.jwa.AlgorithmConstraints
import org.jose4j.jws.AlgorithmIdentifiers
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.JwtClaims
import org.jose4j.jwt.consumer.JwtConsumerBuilder


/**
 * @author Denys Bondarenko
 */
abstract class BaseSignTest : BaseTest() {

  protected fun setupAndClickSignInButton(signInAccountJson: String) {
    val intent = Intent()
    intent.putExtra("googleSignInAccount", GoogleSignInAccount.zaa(signInAccountJson))

    val signInIntent = GoogleSignIn.getClient(
      getTargetContext(),
      GoogleApiClientHelper.generateGoogleSignInOptions()
    ).signInIntent

    intending(hasComponent(signInIntent.component))
      .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, intent))

    onView(withId(R.id.buttonSignInWithGmail))
      .check(matches(isDisplayed()))
      .perform(click())
  }

  protected fun genMockGoogleSignInAccountJson(
    email: String,
    tokenId: String = genMockGoogleSignInTokenId(email)
  ): String {
    return "{\n" +
        "   \"id\":\"111111111111111111111\",\n" +
        "   \"tokenId\":\"$tokenId\",\n" +
        "   \"email\":\"" + email + "\",\n" +
        "   \"displayName\":\"Test\",\n" +
        "   \"givenName\":\"Test\",\n" +
        "   \"familyName\":\"Test\",\n" +
        "   \"photoUrl\":\"https:\\/\\/example.com\",\n" +
        "   \"expirationTime\":1572537339,\n" +
        "   \"obfuscatedIdentifier\":\"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n" +
        "   \"grantedScopes\":[\n" +
        "      \"email\",\n" +
        "      \"https:\\/\\/mail.google.com\\/\",\n" +
        "      \"https:\\/\\/www.googleapis.com\\/auth\\/userinfo.email\",\n" +
        "      \"https:\\/\\/www.googleapis.com\\/auth\\/userinfo.profile\",\n" +
        "      \"openid\",\n" +
        "      \"profile\"\n" +
        "   ]\n" +
        "}"
  }

  /**
   * https://bitbucket.org/b_c/jose4j/wiki/JWT%20Examples
   * https://codecurated.com/blog/how-to-implement-json-web-token-jwt-in-java-spring-boot/#unprotected
   */
  private fun genMockGoogleSignInTokenId(email: String): String {
    val claims = JwtClaims()
    claims.issuer = "FlowCrypt" // who creates the token and signs it
    claims.setExpirationTimeMinutesInTheFuture(10f) // time when the token will expire (10 minutes from now)
    claims.setGeneratedJwtId() // a unique identifier for the token
    claims.setIssuedAtToNow() // when the token was issued/created (now)
    claims.subject = "debugging" // the subject/principal is whom the token is about
    claims.setStringClaim("email", email)

    // A JWT is a JWS and/or a JWE with JSON claims as the payload.
    // In this example it is a JWS so we create a JsonWebSignature object.
    val jws = JsonWebSignature()
    // The payload of the JWS is JSON content of the JWT Claims
    jws.payload = claims.toJson()

    jws.setAlgorithmConstraints(AlgorithmConstraints.NO_CONSTRAINTS)

    // Set the signature algorithm on the JWT/JWS that will integrity protect the claims
    jws.algorithmHeaderValue = AlgorithmIdentifiers.NONE

    // Sign the JWS and produce the compact serialization or the complete JWT/JWS
    // representation, which is a string consisting of three dot ('.') separated
    // base64url-encoded parts in the form Header.Payload.Signature
    // If you wanted to encrypt it, you can simply set this jwt as the payload
    // of a JsonWebEncryption object and set the cty (Content Type) header to "jwt".
    return jws.compactSerialization
  }

  companion object {
    fun extractEmailFromRecordedRequest(recordedRequest: RecordedRequest): String? {
      try {
        val auth = recordedRequest.getHeader("Authorization") ?: return null
        val idToken = auth.substringAfter("Bearer ")
        val jwtConsumerBuilder = JwtConsumerBuilder()
          .setDisableRequireSignature()
          .setJwsAlgorithmConstraints(AlgorithmConstraints.NO_CONSTRAINTS)
          .build()
        val claims = jwtConsumerBuilder.processToClaims(idToken)
        return claims.getClaimValueAsString("email")
      } catch (e: Exception) {
        return null
      }
    }
  }
}
