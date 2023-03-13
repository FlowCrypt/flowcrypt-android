/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.api.FesServerResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.base.BaseFesDuringSetupFlowTest
import com.flowcrypt.email.util.exception.ApiException
import com.google.gson.Gson
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
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
class FesDuringSetupCommonFlowTest : BaseFesDuringSetupFlowTest() {

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(testNameRule)
    .around(mockWebServerRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  override fun handleAPI(request: RecordedRequest, gson: Gson): MockResponse {
    return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
  }

  override fun handleCheckIfFesIsAvailableAtCustomerFesUrl(gson: Gson): MockResponse {
    return when (testNameRule.methodName) {
      "testFesFailedApiError" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setBody(
            gson.toJson(
              FesServerResponse(
                ApiError(
                  code = HttpURLConnection.HTTP_FORBIDDEN,
                  msg = ERROR_TEXT
                )
              )
            )
          )
      }

      "testFesAvailableExternalServiceAlias" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setBody(gson.toJson(FES_SUCCESS_RESPONSE.copy(service = "external-service")))
      }

      "testFesAvailableEnterpriseServerAlias" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setBody(gson.toJson(FES_SUCCESS_RESPONSE.copy(service = "enterprise-server")))
      }

      else -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setBody(gson.toJson(FES_SUCCESS_RESPONSE))
      }
    }
  }

  override fun handleClientConfigurationAPI(gson: Gson): MockResponse {
    val responseCode = when (testNameRule.methodName) {
      "testFesAvailableExternalServiceAlias" -> HttpURLConnection.HTTP_NOT_ACCEPTABLE
      "testFesAvailableEnterpriseServerAlias" -> HttpURLConnection.HTTP_CONFLICT
      else -> HttpURLConnection.HTTP_OK
    }

    return MockResponse().setResponseCode(responseCode)
  }

  override fun handleClientConfigurationAPIForSharedTenantFes(
    account: String?,
    gson: Gson
  ): MockResponse {
    return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
  }

  @Test
  fun testFesAvailableExternalServiceAlias() {
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_FES_SERVER_EXTERNAL_SERVICE))
    //we simulate error for https://fes.$domain/api/v1/client-configuration?domain=$domain
    //to check that external-service was accepted and we called getClientConfigurationFromFes()

    isDialogWithTextDisplayed(
      decorView,
      "ApiException:" + ApiException(
        ApiError(
          code = HttpURLConnection.HTTP_NOT_ACCEPTABLE,
          msg = ""
        )
      ).message!!
    )
  }

  @Test
  fun testFesAvailableEnterpriseServerAlias() {
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_FES_SERVER_ENTERPRISE_SERVER))
    //we simulate error for https://fes.$domain/api/v1/client-configuration?domain=$domain
    //to check that enterprise-service was accepted and we called getClientConfigurationFromFes()
    isDialogWithTextDisplayed(
      decorView,
      "ApiException:" + ApiException(
        ApiError(
          code = HttpURLConnection.HTTP_CONFLICT,
          msg = ""
        )
      ).message!!
    )
  }

  @Test
  fun testFesFailedApiError() {
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_FES_API_ERROR))
    isDialogWithTextDisplayed(decorView, ERROR_TEXT)
    onView(withText(R.string.retry))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testFesFailedNoConnection() {
    try {
      changeConnectionState(false)
      setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_FES_NO_CONNECTION))
      isDialogWithTextDisplayed(
        decorView = decorView,
        message = getResString(R.string.no_connection_or_server_is_not_reachable)
      )
    } finally {
      changeConnectionState(true)
    }
  }

  companion object {
    private const val EMAIL_FES_NO_CONNECTION = "fes_no_connection@flowcrypt.test"
    private const val EMAIL_FES_SERVER_EXTERNAL_SERVICE =
      "fes_server_external_service@flowcrypt.test"
    private const val EMAIL_FES_SERVER_ENTERPRISE_SERVER =
      "fes_server_enterprise_server@flowcrypt.test"
    private const val EMAIL_FES_API_ERROR = "fes_api_error@flowcrypt.test"
    private const val ERROR_TEXT = "ERROR_TEXT"

    private val FES_SUCCESS_RESPONSE = FesServerResponse(
      apiError = null,
      vendor = "FlowCrypt",
      service = "enterprise-server",
      orgId = "localhost",
      version = "2021",
      endUserApiVersion = "v1",
      adminApiVersion = "v1"
    )
  }
}
