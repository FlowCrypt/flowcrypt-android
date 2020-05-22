/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.enterprise

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
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
import com.flowcrypt.email.api.retrofit.response.model.DomainRules
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.StubAllExternalIntentsRule
import com.flowcrypt.email.ui.activity.AddNewAccountActivity
import com.flowcrypt.email.ui.activity.CreateOrImportKeyActivity
import com.flowcrypt.email.ui.activity.base.BaseSignActivityTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.Matchers.not
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.InputStreamReader

/**
 * @author Denis Bondarenko
 *         Date: 11/8/19
 *         Time: 3:30 PM
 *         E-mail: DenBond7@gmail.com
 */
@DoesNotNeedMailserverEnterprise
@LargeTest
@RunWith(AndroidJUnit4::class)
class AddNewAccountActivityEnterpriseTest : BaseSignActivityTest() {
  override val activityTestRule: ActivityTestRule<*>? = IntentsTestRule(AddNewAccountActivity::class.java)

  @get:Rule
  val ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(AddAccountToDatabaseRule())
      .around(activityTestRule)
      .around(StubAllExternalIntentsRule())

  @Test
  fun testNoPrvCreateRule() {
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_WITH_NO_PRV_CREATE_RULE))
    intended(hasComponent(CreateOrImportKeyActivity::class.java.name))

    onView(withId(R.id.buttonCreateNewKey))
        .check(matches(not(isDisplayed())))
    onView(withId(R.id.buttonSkipSetup))
        .check(matches(not(isDisplayed())))
  }

  companion object {
    const val EMAIL_WITH_NO_PRV_CREATE_RULE = "no_prv_create@example.com"

    @get:ClassRule
    @JvmStatic
    val mockWebServerRule = FlowCryptMockWebServerRule(1212, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        val gson = ApiHelper.getInstance(InstrumentationRegistry.getInstrumentation().targetContext).gson
        val model = gson.fromJson<LoginModel>(InputStreamReader(request.body.inputStream()),
            LoginModel::class.java)

        if (request.path.equals("/account/login")) {
          when (model.account) {
            EMAIL_WITH_NO_PRV_CREATE_RULE -> return MockResponse().setResponseCode(200)
                .setBody(gson.toJson(LoginResponse(null, isRegistered = true, isVerified = true)))
          }
        }

        if (request.path.equals("/account/get")) {
          when (model.account) {
            EMAIL_WITH_NO_PRV_CREATE_RULE -> return MockResponse().setResponseCode(200)
                .setBody(gson.toJson(DomainRulesResponse(null, DomainRules(listOf
                ("NO_PRV_CREATE", "NO_PRV_BACKUP")))))
          }
        }

        return MockResponse().setResponseCode(404)
      }
    })
  }
}
