/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.response.api.ClientConfigurationResponse
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.ui.base.BaseSignTest
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
import java.net.HttpURLConnection

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class AddNewAccountEnterpriseFlowTest : BaseSignTest() {
  override val useIntents: Boolean = true
  override val activityScenarioRule = activityScenarioRule<MainActivity>()

  @get:Rule
  val ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testNoPrvCreateRule() {
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_WITH_NO_PRV_CREATE_RULE))

    onView(withId(R.id.buttonCreateNewKey))
      .check(matches(not(isDisplayed())))
  }

  companion object {
    const val EMAIL_WITH_NO_PRV_CREATE_RULE = "no_prv_create@flowcrypt.test"

    @get:ClassRule
    @JvmStatic
    val mockWebServerRule =
      FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
          val gson =
            ApiHelper.getInstance(InstrumentationRegistry.getInstrumentation().targetContext).gson

          if (request.path == "/shared-tenant-fes/api/v1/client-configuration?domain=flowcrypt.test") {
            when (extractEmailFromRecordedRequest(request)) {
              EMAIL_WITH_NO_PRV_CREATE_RULE -> return MockResponse().setResponseCode(
                HttpURLConnection.HTTP_OK
              )
                .setBody(
                  gson.toJson(
                    ClientConfigurationResponse(
                      clientConfiguration = ClientConfiguration(
                        flags = listOf(
                          ClientConfiguration.ConfigurationProperty.NO_PRV_CREATE,
                          ClientConfiguration.ConfigurationProperty.NO_PRV_BACKUP
                        ),
                        customKeyserverUrl = null,
                        keyManagerUrl = null,
                        disallowAttesterSearchForDomains = null,
                        enforceKeygenAlgo = null,
                        enforceKeygenExpireMonths = null
                      )
                    )
                  )
                )
            }
          }

          return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
        }
      })
  }
}
