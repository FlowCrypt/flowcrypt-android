/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fes.login

import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.response.api.FesServerResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.ui.base.BaseSignTest
import com.flowcrypt.email.util.TestGeneralUtil
import com.flowcrypt.email.util.exception.ApiException
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
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
class MainSignInFragmentEnterpriseTestUseFesUrlFlowTest : BaseSignTest() {
  override val useIntents: Boolean = true
  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      destinationId = R.id.mainSignInFragment
    )
  )

  private val mockWebServerRule =
    FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        val gson = ApiHelper.getInstance(getTargetContext()).gson

        when {
          request.path.equals("/api/") -> {
            return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
              .setBody(gson.toJson(FES_SUCCESS_RESPONSE))
          }

          request.path.equals("/api/v1/client-configuration?domain=flowcrypt.test") -> {
            return handleClientConfigurationAPI()
          }

          request.path.equals("/shared-tenant-fes/api/v1/client-configuration?domain=flowcrypt.test") -> {
            isSharedTenantFesUsed = true
            return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
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
    .around(mockWebServerRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  private var isCustomFesUrlUsed = false
  private var isSharedTenantFesUsed = false

  @Test
  fun testCallCustomFesUrlToGetClientConfiguration() {
    isCustomFesUrlUsed = false
    setupAndClickSignInButton(genMockGoogleSignInAccountJson(EMAIL_ENTERPRISE_USER))

    assertEquals(false, isSharedTenantFesUsed)
    assertEquals(true, isCustomFesUrlUsed)

    //the mock web server should return error for https://fes.$domain/api/
    isDialogWithTextDisplayed(
      decorView,
      "ApiException:" + ApiException(
        ApiError(
          code = HttpURLConnection.HTTP_UNAUTHORIZED,
          message = ""
        )
      ).message!!
    )
    //after this we will be sure that https://fes.$domain/api/ was called for an enterprise user
  }

  private fun handleClientConfigurationAPI(): MockResponse {
    isCustomFesUrlUsed = true
    return MockResponse().setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
  }

  companion object {
    private const val EMAIL_ENTERPRISE_USER = "enterprise_user@flowcrypt.test"

    private val FES_SUCCESS_RESPONSE = FesServerResponse(
      vendor = "FlowCrypt",
      service = "external-service",
      orgId = "localhost",
      version = "2023-01",
      endUserApiVersion = "v1",
      adminApiVersion = "v1"
    )
  }
}
