/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.base

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.util.google.GoogleApiClientHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

/**
 * @author Denis Bondarenko
 *         Date: 11/8/19
 *         Time: 3:39 PM
 *         E-mail: DenBond7@gmail.com
 */
abstract class BaseSignTest : BaseTest() {

  protected fun setupAndClickSignInButton(signInAccountJson: String) {
    val intent = Intent()
    intent.putExtra("googleSignInAccount", GoogleSignInAccount.zab(signInAccountJson))

    val signInIntent = GoogleSignIn.getClient(
      getTargetContext(),
      GoogleApiClientHelper.generateGoogleSignInOptions()
    ).signInIntent

    Intents.intending(IntentMatchers.hasComponent(signInIntent.component))
      .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, intent))

    Espresso.onView(ViewMatchers.withId(R.id.buttonSignInWithGmail))
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
      .perform(ViewActions.click())
  }

  protected fun genMockGoogleSignInAccountJson(email: String): String {
    return "{\n" +
        "   \"id\":\"111111111111111111111\",\n" +
        "   \"tokenId\":\"some_token\",\n" +
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
}
