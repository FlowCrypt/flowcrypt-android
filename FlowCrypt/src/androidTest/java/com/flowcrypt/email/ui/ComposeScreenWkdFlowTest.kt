/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.contrib.RecyclerViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.junit.annotations.NotReadyForCI
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withChipsBackgroundColor
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.LazyActivityScenarioRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.CreateMessageActivity
import com.flowcrypt.email.ui.adapter.RecipientChipRecyclerViewAdapter
import com.flowcrypt.email.ui.base.BaseComposeScreenTest
import com.flowcrypt.email.util.TestGeneralUtil
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestName
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.net.HttpURLConnection

/**
 * To be able to test WKD need to execute the following:
 * adb root
 * adb shell "echo 1 > /proc/sys/net/ipv4/ip_forward"
 * adb shell "iptables -t nat -A PREROUTING -s 127.0.0.1 -p tcp --dport 443 -j REDIRECT --to 1212"
 * adb shell "iptables -t nat -A OUTPUT -s 127.0.0.1 -p tcp --dport 443 -j REDIRECT --to 1212"
 *
 * @author Denis Bondarenko
 *         Date: 8/12/21
 *         Time: 10:58 AM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@NotReadyForCI
class ComposeScreenWkdFlowTest : BaseComposeScreenTest() {
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

        if ("/.well-known/openpgpkey/hu/.*\\?l=.*".toRegex().matches(request.path ?: "")) {
          return handleDirectWkdRequest()
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
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testWkdAdvancedNoResult() {
    check(
      recipient = "wkd_advanced_no_result@localhost",
      colorResourcesId = RecipientChipRecyclerViewAdapter.CHIP_COLOR_RES_ID_NO_PUB_KEY
    )
  }

  @Test
  fun testWkdAdvancedPub() {
    check(
      recipient = "wkd_advanced_pub@localhost",
      colorResourcesId = RecipientChipRecyclerViewAdapter.CHIP_COLOR_RES_ID_HAS_USABLE_PUB_KEY
    )
  }

  @Test
  fun testWkdAdvancedSkippedWkdDirectNoPolicyPub() {
    check(
      recipient = "wkd_direct_no_policy@localhost",
      colorResourcesId = RecipientChipRecyclerViewAdapter.CHIP_COLOR_RES_ID_NO_PUB_KEY
    )
  }

  @Test
  fun testWkdAdvancedSkippedWkdDirectNoResult() {
    check(
      recipient = "wkd_direct_no_result@localhost",
      colorResourcesId = RecipientChipRecyclerViewAdapter.CHIP_COLOR_RES_ID_NO_PUB_KEY
    )
  }

  @Test
  fun testWkdAdvancedSkippedWkdDirectPub() {
    check(
      recipient = "wkd_direct_pub@localhost",
      colorResourcesId = RecipientChipRecyclerViewAdapter.CHIP_COLOR_RES_ID_HAS_USABLE_PUB_KEY
    )
  }

  @Test
  fun testWkdAdvancedTimeOutWkdDirectAvailable() {
    check(
      recipient = "wkd_direct_pub@localhost",
      colorResourcesId = RecipientChipRecyclerViewAdapter.CHIP_COLOR_RES_ID_HAS_USABLE_PUB_KEY
    )
  }

  @Test
  fun testWkdAdvancedTimeOutWkdDirectTimeOut() {
    check(
      recipient = "wkd_advanced_direct_timeout@localhost",
      colorResourcesId = RecipientChipRecyclerViewAdapter.CHIP_COLOR_RES_ID_NO_PUB_KEY
    )
  }

  @Test
  fun testWkdPrv() {
    check(
      recipient = "wkd_prv@localhost",
      colorResourcesId = RecipientChipRecyclerViewAdapter.CHIP_COLOR_RES_ID_NO_PUB_KEY
    )
  }

  private fun handleAdvancedPolicyRequest(): MockResponse {
    return when (testNameRule.methodName) {
      "testWkdAdvancedNoResult",
      "testWkdAdvancedPub",
      "testWkdPrv" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
      }

      "testWkdAdvancedTimeOutWkdDirectAvailable",
      "testWkdAdvancedTimeOutWkdDirectTimeOut" -> {
        Thread.sleep(5000)
        MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
      }

      else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
    }
  }

  private fun handleAdvancedWkdRequest(): MockResponse {
    return when (testNameRule.methodName) {
      "testWkdAdvancedNoResult" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
      }

      "testWkdAdvancedPub" -> {
        genSuccessMockResponseWithKey("pgp/keys/wkd_advanced_pub@localhost_pub.asc")
      }

      "testWkdPrv" -> {
        genSuccessMockResponseWithKey("pgp/keys/wkd_prv@localhost_sec.asc")
      }

      else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
    }
  }

  private fun handleDirectPolicyRequest(): MockResponse {
    return when (testNameRule.methodName) {
      "testWkdAdvancedSkippedWkdDirectNoResult",
      "testWkdAdvancedSkippedWkdDirectPub",
      "testWkdAdvancedTimeOutWkdDirectAvailable" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
      }

      "testWkdAdvancedTimeOutWkdDirectTimeOut" -> {
        Thread.sleep(5000)
        MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
      }

      else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
    }
  }

  private fun handleDirectWkdRequest(): MockResponse {
    return when (testNameRule.methodName) {
      "testWkdAdvancedSkippedWkdDirectNoResult" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
      }

      "testWkdAdvancedSkippedWkdDirectPub",
      "testWkdAdvancedTimeOutWkdDirectAvailable" -> {
        genSuccessMockResponseWithKey("pgp/keys/wkd_direct_pub@localhost_pub.asc")
      }

      else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
    }
  }

  private fun genSuccessMockResponseWithKey(keyPath: String) = MockResponse()
    .setResponseCode(HttpURLConnection.HTTP_OK)
    .setBody(Buffer().write(TestGeneralUtil.readFileFromAssetsAsByteArray(keyPath)))

  private fun check(recipient: String, colorResourcesId: Int) {
    fillInAllFields(recipient)
    onView(withId(R.id.recyclerViewChipsTo))
      .perform(
        scrollTo<RecyclerView.ViewHolder>(
          allOf(withText(recipient), withChipsBackgroundColor(getTargetContext(), colorResourcesId))
        )
      )
  }
}
