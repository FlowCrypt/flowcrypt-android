/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
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
import com.flowcrypt.email.util.TestGeneralUtil
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.googleapis.json.GoogleJsonErrorContainer
import com.google.api.client.json.Json
import com.google.api.client.json.gson.GsonFactory
import com.google.gson.Gson
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestName
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.net.HttpURLConnection

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@Ignore("not completed")
class FesDuringSetupEnterpriseFlowTest : BaseSignTest() {
  override val useIntents: Boolean = true
  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      destinationId = R.id.mainSignInFragment
    )
  )

  private val testNameRule = TestName()
  private val mockWebServerRule =
    FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        val gson = ApiHelper.getInstance(getTargetContext()).gson

        when {
          request.path.equals("/api/v1/client-configuration?domain=flowcrypt.test") -> {
            return handleClientConfigurationAPI(gson)
          }

          request.requestUrl?.encodedPath == "/gmail/v1/users/me/messages"
              && request.requestUrl?.queryParameterNames?.contains("q") == true -> {
            val q = requireNotNull(request.requestUrl?.queryParameter("q"))
            return when {
              q.startsWith("from:${EMAIL_GMAIL}") ->
                prepareMockResponseForPublicDomains(EMAIL_GMAIL)

              q.startsWith("from:${EMAIL_GOOGLEMAIL}") ->
                prepareMockResponseForPublicDomains(EMAIL_GOOGLEMAIL)

              else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
            }
          }
        }

        return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
      }
    })

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(testNameRule)
    .around(mockWebServerRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Before
  fun waitWhileToastWillBeDismissed() {
    Thread.sleep(1000)
  }

  /**
   * Users of gmail.com just
   */
  @Test
  fun testFlowForPublicEmailDomainsGmail() {
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_GMAIL))
    checkIsSnackBarDisplayed(EMAIL_GMAIL)
  }

  @Test
  fun testFlowForPublicEmailDomainsGoogleMail() {
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_GOOGLEMAIL))
    checkIsSnackBarDisplayed(EMAIL_GOOGLEMAIL)
  }

  private fun handleClientConfigurationAPI(gson: Gson): MockResponse {
    val responseCode = when (testNameRule.methodName) {
      "testFesServerAvailableGetClientConfigurationFailed" -> HttpURLConnection.HTTP_FORBIDDEN
      "testFesServerExternalServiceAlias" -> HttpURLConnection.HTTP_NOT_ACCEPTABLE
      "testFesServerEnterpriseServerAlias" -> HttpURLConnection.HTTP_CONFLICT
      "testCallFesUrlToGetClientConfigurationForEnterpriseUser" -> HttpURLConnection.HTTP_UNAUTHORIZED
      else -> HttpURLConnection.HTTP_OK
    }

    val body = when (testNameRule.methodName) {
      "testFesServerAvailableGetClientConfigurationFailed",
      "testFesServerExternalServiceAlias",
      "testFesServerEnterpriseServerAlias",
      "testCallFesUrlToGetClientConfigurationForEnterpriseUser" -> null
      else -> gson.toJson(
        ClientConfigurationResponse(
          clientConfiguration = ClientConfiguration(
            flags = emptyList()
          )
        )
      )
    }

    return MockResponse().setResponseCode(responseCode).apply {
      body?.let { setBody(it) }
    }
  }

  private fun prepareMockResponseForPublicDomains(string: String) =
    MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
      .setHeader("Content-Type", Json.MEDIA_TYPE)
      .setBody(GoogleJsonErrorContainer().apply {
        factory = GsonFactory.getDefaultInstance()
        error = GoogleJsonError().apply {
          code = HttpURLConnection.HTTP_NOT_FOUND
          message = string
          errors = listOf(GoogleJsonError.ErrorInfo().apply {
            message = string
            domain = "local"
            reason = "notFound"
          })
        }
      }.toString())

  companion object {
    private const val EMAIL_GMAIL = "gmail@gmail.com"
    private const val EMAIL_GOOGLEMAIL = "googlemail@googlemail.com"
  }
}
