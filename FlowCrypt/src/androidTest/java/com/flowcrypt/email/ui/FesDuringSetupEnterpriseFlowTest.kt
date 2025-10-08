/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.api.ClientConfigurationResponse
import com.flowcrypt.email.api.retrofit.response.api.EkmPrivateKeysResponse
import com.flowcrypt.email.api.retrofit.response.api.FesServerResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.api.retrofit.response.model.Key
import com.flowcrypt.email.extensions.exceptionMsg
import com.flowcrypt.email.junit.annotations.EnterpriseTest
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.base.BaseFesDuringSetupFlowTest
import com.flowcrypt.email.util.TestGeneralUtil
import com.flowcrypt.email.util.exception.ApiException
import com.flowcrypt.email.util.exception.CommonConnectionException
import com.google.gson.Gson
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@EnterpriseTest
class FesDuringSetupEnterpriseFlowTest : BaseFesDuringSetupFlowTest() {
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
    return when {
      request.path?.startsWith("/ekm") == true -> handleEkmAPI(request, gson)
      else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
    }
  }

  override fun handleCheckIfFesIsAvailableAtCustomerFesUrl(gson: Gson): MockResponse {
    return if ("testFesAvailableRequestTimeOutHasConnection" == testNameRule.methodName) {
      MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
        .setHeadersDelay(6, TimeUnit.SECONDS)
    } else when (testNameRule.methodName) {

      "testFesAvailableHasConnectionHttpCode404" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
      }

      "testFesAvailableHasConnectionHttpCodeNot200" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
      }

      "testFesAvailableWrongServiceName" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setBody(gson.toJson(FES_SUCCESS_RESPONSE.copy(service = "hello")))
      }

      else -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setBody(gson.toJson(FES_SUCCESS_RESPONSE))
      }
    }
  }

  override fun handleClientConfigurationAPI(gson: Gson): MockResponse {
    return MockResponse()
      .setResponseCode(HttpURLConnection.HTTP_OK)
      .setBody(
        gson.toJson(
          ClientConfigurationResponse(
            clientConfiguration = ClientConfiguration(
              flags = ACCEPTED_FLAGS,
              keyManagerUrl = EMAIL_EKM_URL_SUCCESS,
            )
          )
        )
      )
  }

  override fun handleClientConfigurationAPIForSharedTenantFes(
    account: String?,
    gson: Gson
  ): MockResponse {
    return when (account) {
      //shared-tenant-fes allowed only for http code = 404
      EMAIL_FES_HTTP_404 -> MockResponse()
        .setResponseCode(HttpURLConnection.HTTP_FORBIDDEN)

      else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
    }
  }

  @Test
  @Ignore("need to fix")
  fun testFesAvailableWrongServiceName() {
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(email = EMAIL_FES_NOT_ALLOWED_SERVER))
    isDialogWithTextDisplayed(decorView, getResString(R.string.fes_server_has_wrong_settings))
  }

  @Test
  @Ignore("need to fix")
  fun testFesAvailableRequestTimeOutHasConnection() {
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_FES_REQUEST_TIME_OUT))
    isDialogWithTextDisplayed(
      decorView,
      getResString(
        R.string.check_fes_error_with_retry,
        com.flowcrypt.email.api.retrofit.response.base.Result.exception<Any>(
          CommonConnectionException(SocketTimeoutException("timeout"))
        ).exceptionMsg
      )
    )
  }

  @Test
  fun testFesAvailableHasConnectionHttpCode404() {
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_FES_HTTP_404))
    isDialogWithTextDisplayed(
      decorView,
      "ApiException:" + ApiException(
        ApiError(
          code = HttpURLConnection.HTTP_FORBIDDEN,
          message = ""
        )
      ).message
    )
  }

  @Test
  @Ignore("need to fix")
  fun testFesAvailableHasConnectionHttpCodeNot200() {
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_FES_HTTP_NOT_404_NOT_SUCCESS))
    isDialogWithTextDisplayed(
      decorView,
      getResString(
        R.string.fes_server_error,
        com.flowcrypt.email.api.retrofit.response.base.Result.exception<Any>(
          ApiException(
            ApiError(
              code = HttpURLConnection.HTTP_INTERNAL_ERROR,
              message = ""
            )
          )
        ).exceptionMsg
      )
    )
  }

  @Test
  fun testFesAvailableSuccess() {
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_FES_SUCCESS))
    Thread.sleep(2000)
    onView(withText(R.string.set_pass_phrase))
      .check(matches(isDisplayed()))
  }

  @Test
  @Ignore("need to fix")
  fun testFesAvailableSSLError() {
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_FES_SSL_ERROR))
    //as our mock server support only flowcrypt.test and flowcrypt.example we will receive
    onView(withText(containsString("Hostname fes.wrongssl.test not verified")))
      .inRoot(withDecorView(not(`is`(decorView))))
      .check(matches(isDisplayed()))
  }

  @Test
  @Ignore("need to fix")
  fun testFlowForPublicEmailDomainsGmail() {
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_GMAIL))
    isDialogWithTextDisplayed(
      decorView,
      getResString(
        R.string.enterprise_does_not_support_pub_domains,
        getResString(R.string.app_name),
        "gmail.com"
      )
    )
  }

  @Test
  @Ignore("need to fix")
  fun testFlowForPublicEmailDomainsGoogleMail() {
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_GOOGLEMAIL))
    isDialogWithTextDisplayed(
      decorView,
      getResString(
        R.string.enterprise_does_not_support_pub_domains,
        getResString(R.string.app_name),
        "googlemail.com"
      )
    )
  }

  private fun handleEkmAPI(request: RecordedRequest, gson: Gson): MockResponse {
    return when {
      request.path.equals("/ekm/v1/keys/private") ->
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
          .setBody(gson.toJson(EKM_FES_RESPONSE))

      else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
        .setBody(gson.toJson(EkmPrivateKeysResponse(privateKeys = emptyList())))
    }
  }

  companion object {
    private const val EMAIL_EKM_URL_SUCCESS = "https://flowcrypt.test/ekm/"
    private const val EMAIL_FES_REQUEST_TIME_OUT = "fes_request_timeout@flowcrypt.test"
    private const val EMAIL_FES_HTTP_404 = "fes_404@flowcrypt.test"
    private const val EMAIL_FES_HTTP_NOT_404_NOT_SUCCESS = "fes_not404_not_success@flowcrypt.test"
    private const val EMAIL_FES_NOT_ALLOWED_SERVER = "fes_not_allowed_server@flowcrypt.test"
    private const val EMAIL_FES_SUCCESS = "fes_success@flowcrypt.test"
    private const val EMAIL_FES_SSL_ERROR = "fes_ssl_error@wrongssl.test"

    private const val EMAIL_GMAIL = "gmail@gmail.com"
    private const val EMAIL_GOOGLEMAIL = "googlemail@googlemail.com"

    private val ACCEPTED_FLAGS = listOf(
      ClientConfiguration.ConfigurationProperty.PRV_AUTOIMPORT_OR_AUTOGEN,
      ClientConfiguration.ConfigurationProperty.FORBID_STORING_PASS_PHRASE,
      ClientConfiguration.ConfigurationProperty.NO_PRV_CREATE
    )

    private val EKM_FES_RESPONSE = EkmPrivateKeysResponse(
      privateKeys = listOf(
        Key(
          TestGeneralUtil.readFileFromAssetsAsString("pgp/fes@flowcrypt.test_prv_decrypted.asc")
        )
      )
    )

    private val FES_SUCCESS_RESPONSE = FesServerResponse(
      vendor = "FlowCrypt",
      service = "enterprise-server",
      orgId = "localhost",
      version = "2021",
      endUserApiVersion = "v1",
      adminApiVersion = "v1"
    )
  }
}
