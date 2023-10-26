/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.base

import androidx.test.ext.junit.rules.activityScenarioRule
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.util.TestGeneralUtil
import com.google.gson.Gson
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.rules.TestName

/**
 * @author Denys Bondarenko
 */
@FlowCryptTestSettings(useIntents = true)
abstract class BaseFesDuringSetupFlowTest : BaseSignTest() {
  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      destinationId = R.id.mainSignInFragment
    )
  )

  protected val testNameRule = TestName()
  protected val mockWebServerRule =
    FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        val gson = ApiHelper.getInstance(getTargetContext()).gson

        return when {
          request.path.equals("/api/") -> {
            return handleCheckIfFesIsAvailableAtCustomerFesUrl(gson)
          }

          request.path.equals("/api/v1/client-configuration?domain=flowcrypt.test") -> {
            return handleClientConfigurationAPI(gson)
          }

          request.requestUrl?.encodedPath == "/shared-tenant-fes/api/v1/client-configuration" &&
              request.requestUrl?.queryParameter("domain") in SHARED_TENANT_FES_DOMAINS -> {
            val account = extractEmailFromRecordedRequest(request)
            handleClientConfigurationAPIForSharedTenantFes(account, gson)
          }

          else -> handleAPI(request, gson)
        }
      }
    })


  abstract fun handleAPI(request: RecordedRequest, gson: Gson): MockResponse
  abstract fun handleCheckIfFesIsAvailableAtCustomerFesUrl(gson: Gson): MockResponse
  abstract fun handleClientConfigurationAPI(gson: Gson): MockResponse

  abstract fun handleClientConfigurationAPIForSharedTenantFes(
    account: String?,
    gson: Gson
  ): MockResponse

  companion object {
    val SHARED_TENANT_FES_DOMAINS = listOf(
      "flowcrypt.test",
      "flowcrypt.example",
    )
  }
}
