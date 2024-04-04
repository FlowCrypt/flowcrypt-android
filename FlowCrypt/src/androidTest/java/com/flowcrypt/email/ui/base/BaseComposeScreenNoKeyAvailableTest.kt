/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.base

import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.util.TestGeneralUtil
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.ClassRule
import java.net.HttpURLConnection

/**
 * @author Denys Bondarenko
 */
abstract class BaseComposeScreenNoKeyAvailableTest : BaseComposeScreenTest() {
  companion object {
    @get:ClassRule
    @JvmStatic
    val mockWebServerRule = FlowCryptMockWebServerRule(
      TestConstants.MOCK_WEB_SERVER_PORT,
      object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
          if (request.path?.startsWith("/attester/pub", ignoreCase = true) == true) {
            val lastSegment = request.requestUrl?.pathSegments?.lastOrNull()

            when {
              TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER.equals(
                lastSegment, true
              ) -> {
                return MockResponse()
                  .setResponseCode(HttpURLConnection.HTTP_OK)
                  .setBody(TestGeneralUtil.readResourceAsString("3.txt"))
              }
            }
          }

          return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
        }
      })
  }
}