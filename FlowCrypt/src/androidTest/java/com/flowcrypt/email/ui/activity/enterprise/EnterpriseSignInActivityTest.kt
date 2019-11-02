/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.enterprise

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.flowcrypt.email.DoesNotNeedMailserverEnterprise
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.request.model.LoginModel
import com.flowcrypt.email.api.retrofit.response.api.DomainRulesResponse
import com.flowcrypt.email.api.retrofit.response.api.LoginResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.ui.activity.SignInActivity
import com.flowcrypt.email.util.google.GoogleApiClientHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.InputStreamReader

/**
 * @author Denis Bondarenko
 *         Date: 10/31/19
 *         Time: 3:08 PM
 *         E-mail: DenBond7@gmail.com
 */
@DoesNotNeedMailserverEnterprise
@LargeTest
@RunWith(AndroidJUnit4::class)
class EnterpriseSignInActivityTest : BaseTest() {
  override val activityTestRule: ActivityTestRule<*>? = IntentsTestRule(SignInActivity::class.java)

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(activityTestRule)

  @Test
  fun testErrorLogin() {
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_LOGIN_ERROR))
    isToastDisplayed(activityTestRule?.activity, LOGIN_API_ERROR_RESPONSE.apiError?.msg!!)
  }

  @Test
  fun testSuccessLoginNotVerified() {
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_LOGIN_NOT_VERIFIED))
    isToastDisplayed(activityTestRule?.activity, getResString(R.string.user_not_verified))
  }

  @Test
  fun testSuccessLoginNotRegisteredNotVerified() {
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_LOGIN_NOT_REGISTERED_NOT_VERIFIED))
    isToastDisplayed(activityTestRule?.activity, getResString(R.string.user_not_registered_not_verified))
  }

  @Test
  fun testErrorGetDomainRules() {
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_DOMAIN_RULES_ERROR))
    isToastDisplayed(activityTestRule?.activity, DOMAIN_RULES_ERROR_RESPONSE.apiError?.msg!!)
  }

  private fun genMockGoogleSignInAccountJson(email: String): String {
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

  private fun setupAndClickSignInButton(signInAccountJson: String) {
    val intent = Intent()
    intent.putExtra("googleSignInAccount", GoogleSignInAccount.zaa(signInAccountJson))

    val signInIntent = GoogleSignIn.getClient(activityTestRule?.activity!!,
        GoogleApiClientHelper.generateGoogleSignInOptions()).signInIntent

    intending(hasComponent(signInIntent.component))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, intent))

    onView(withId(R.id.buttonSignInWithGmail))
        .check(matches(ViewMatchers.isDisplayed()))
        .perform(click())
  }

  companion object {
    const val EMAIL_LOGIN_ERROR = "login_error@example.com"
    const val EMAIL_LOGIN_NOT_VERIFIED = "login_not_verified@example.com"
    const val EMAIL_LOGIN_NOT_REGISTERED_NOT_VERIFIED = "login_not_registered_not_verified@example.com"
    const val EMAIL_DOMAIN_RULES_ERROR = "domain_rules_error@example.com"

    val LOGIN_API_ERROR_RESPONSE = LoginResponse(ApiError(400, "Something wrong happened.",
        "api input: missing key: token"), null, null)

    val DOMAIN_RULES_ERROR_RESPONSE = DomainRulesResponse(ApiError(401,
        "Not logged in or unknown account", "auth"), null)

    @get:ClassRule
    @JvmStatic
    val mockWebServerRule = FlowCryptMockWebServerRule(1212, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest?): MockResponse {
        val gson = ApiHelper.getInstance(InstrumentationRegistry.getInstrumentation().targetContext).gson
        val model = gson.fromJson<LoginModel>(InputStreamReader(request?.body?.inputStream()),
            LoginModel::class.java)

        if (request?.path.equals("/account/login")) {
          when (model.account) {
            EMAIL_LOGIN_ERROR -> return MockResponse().setResponseCode(200)
                .setBody(gson.toJson(LOGIN_API_ERROR_RESPONSE))

            EMAIL_LOGIN_NOT_REGISTERED_NOT_VERIFIED -> return MockResponse().setResponseCode(200)
                .setBody(gson.toJson(LoginResponse(null, isRegistered = false, isVerified = false)))

            EMAIL_LOGIN_NOT_VERIFIED -> return MockResponse().setResponseCode(200)
                .setBody(gson.toJson(LoginResponse(null, isRegistered = true, isVerified = false)))

            EMAIL_DOMAIN_RULES_ERROR -> return MockResponse().setResponseCode(200)
                .setBody(gson.toJson(LoginResponse(null, isRegistered = true, isVerified = true)))
          }
        }

        if (request?.path.equals("/account/get")) {
          when (model.account) {
            EMAIL_DOMAIN_RULES_ERROR -> return MockResponse().setResponseCode(200)
                .setBody(gson.toJson(DOMAIN_RULES_ERROR_RESPONSE))
          }
        }

        return MockResponse().setResponseCode(404)
      }
    })
  }
}