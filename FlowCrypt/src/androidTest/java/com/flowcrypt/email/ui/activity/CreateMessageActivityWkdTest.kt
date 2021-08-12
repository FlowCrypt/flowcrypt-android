/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withChipsBackgroundColor
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.LazyActivityScenarioRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.base.BaseCreateMessageActivityTest
import com.flowcrypt.email.ui.widget.CustomChipSpanChipCreator
import com.flowcrypt.email.util.UIUtil
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestName
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.net.HttpURLConnection

/*adb root
adb shell "echo 1 > /proc/sys/net/ipv4/ip_forward"
adb shell "iptables -t nat -A PREROUTING -s 127.0.0.1 -p tcp --dport 443 -j REDIRECT --to 1212"
adb shell "iptables -t nat -A OUTPUT -s 127.0.0.1 -p tcp --dport 443 -j REDIRECT --to 1212"*/

/**
 * @author Denis Bondarenko
 *         Date: 8/12/21
 *         Time: 10:58 AM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class CreateMessageActivityWkdTest : BaseCreateMessageActivityTest() {
  override val activeActivityRule: LazyActivityScenarioRule<CreateMessageActivity>? = null
  override val activityScenarioRule = activityScenarioRule<CreateMessageActivity>(intent = intent)
  override val activityScenario: ActivityScenario<*>?
    get() = activityScenarioRule.scenario

  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule()

  @get:Rule
  val testNameRule = TestName()

  val mockWebServerRule =
    FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        if ("/.well-known/openpgpkey/.*/policy".toRegex().matches(request.path ?: "")) {
          return handleAdvancedPolicyRequest()
        }

        if ("/.well-known/openpgpkey/.*/hu/.*\\?l=.*".toRegex().matches(request.path ?: "")) {
          return handleAdvancedWkdRequest()
        }

        if (request.path == "/.well-known/openpgpkey/policy") {
          return handleDirectPolicyRequest()
        }

        if ("/.well-known/openpgpkey/.*/hu/.*\\?l=.*".toRegex().matches(request.path ?: "")) {
          return handleDirectWkdRequest()
        }

        return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
      }
    })

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(ClearAppSettingsRule())
    .around(mockWebServerRule)
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(RetryRule.DEFAULT)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testWkdNoResult() {
    val recipient = "wkd_no_result@localhost"
    fillInAllFields(recipient)
    onView(withId(R.id.editTextRecipientTo))
      .check(
        matches(
          withChipsBackgroundColor(
            chipText = recipient,
            backgroundColor = UIUtil.getColor(
              context = getTargetContext(),
              colorResourcesId = CustomChipSpanChipCreator.CHIP_COLOR_RES_ID_PGP_NOT_EXISTS
            )
          )
        )
      )
  }

  private fun handleAdvancedPolicyRequest(): MockResponse {
    return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
  }

  private fun handleAdvancedWkdRequest(): MockResponse {
    return when (testNameRule.methodName) {
      "testWkdNoResult" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
      }

      else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
    }
  }

  private fun handleDirectPolicyRequest(): MockResponse {
    return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
  }

  private fun handleDirectWkdRequest(): MockResponse {
    return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_ACCEPTABLE)
  }
}
