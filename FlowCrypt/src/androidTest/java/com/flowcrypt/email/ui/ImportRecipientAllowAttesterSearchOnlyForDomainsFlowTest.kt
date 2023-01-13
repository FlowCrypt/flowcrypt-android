/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.TestGeneralUtil
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.net.HttpURLConnection

/**
 * @author Denis Bondarenko
 *         Date: 6/6/22
 *         Time: 4:30 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class ImportRecipientAllowAttesterSearchOnlyForDomainsFlowTest : BaseTest() {
  private val userWithClientConfiguration = AccountDaoManager.getUserWithClientConfiguration(
    ClientConfiguration(
      flags = listOf(
        ClientConfiguration.ConfigurationProperty.NO_PRV_CREATE,
        ClientConfiguration.ConfigurationProperty.NO_PRV_BACKUP
      ),
      customKeyserverUrl = null,
      keyManagerUrl = "https://keymanagerurl.test",
      disallowAttesterSearchForDomains = listOf("*"),
      allowAttesterSearchOnlyForDomains = listOf(ALLOWED_DOMAIN),
      enforceKeygenAlgo = null,
      enforceKeygenExpireMonths = null
    )
  )

  private val addAccountToDatabaseRule = AddAccountToDatabaseRule(userWithClientConfiguration)

  override val useIntents: Boolean = true
  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      destinationId = R.id.importRecipientsFromSourceFragment
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testDisallowLookupOnAttester() {
    onView(withId(R.id.eTKeyIdOrEmail))
      .perform(
        clearText(),
        typeText("user@$SOME_DOMAIN"),
        pressImeActionButton()
      )

    onView(withText(R.string.supported_public_key_not_found))
      .check(matches((isDisplayed())))
  }

  @Test
  fun testAllowAttesterSearchOnlyForDomains() {
    onView(withId(R.id.eTKeyIdOrEmail))
      .perform(
        clearText(),
        typeText(ALLOWED_USER),
        pressImeActionButton()
      )

    onView(withText(getResString(R.string.template_message_part_public_key_owner, ALLOWED_USER)))
      .check(matches(isDisplayed()))
  }

  companion object {
    private const val SOME_DOMAIN = "some.test"
    private const val ALLOWED_DOMAIN = "allowed.test"
    private const val ALLOWED_USER = "user@$ALLOWED_DOMAIN"
    private val ALLOWED_USER_PUB_KEY = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
        "Version: PGPainless\n" +
        "\n" +
        "mDMEYp4ILRYJKwYBBAHaRw8BAQdAbR/lV1cjANNrV3G6xKBSZvvUvSV/7tkAijfu\n" +
        "Ic1wkPG0EXVzZXJAYWxsb3dlZC50ZXN0iI8EExYKAEEFAmKeCC0JEBqKFwXMKQ1t\n" +
        "FiEEdezAhiRDeNnKk9VbGooXBcwpDW0CngECmwMFFgIDAQAECwkIBwUVCgkICwKZ\n" +
        "AQAAMwcA/i5VkgbmbH/KQg95FplXdbS0LlIThWU/9a5niUDjLBw/AP4zbzcHmN/9\n" +
        "pyLsGVej1vogLKHWvOoMhw/cj2z9OQfiCrg4BGKeCC0SCisGAQQBl1UBBQEBB0Bj\n" +
        "ooQjx+z6b71CWOZewG0LSFGY13IuLmViuzOwAgXpUgMBCAeIdQQYFgoAHQUCYp4I\n" +
        "LQKeAQKbDAUWAgMBAAQLCQgHBRUKCQgLAAoJEBqKFwXMKQ1tWrsA/RaP0ZVXxVoQ\n" +
        "lXF9Qd24bBc63Y1F0sSEQxFKJgOCDJ3yAP4uDyqiUbVy8munXvxaBpMhxpqZx1B3\n" +
        "bo7D52MQznBWBQ==\n" +
        "=MoFp\n" +
        "-----END PGP PUBLIC KEY BLOCK-----"

    @get:ClassRule
    @JvmStatic
    val mockWebServerRule = FlowCryptMockWebServerRule(
      TestConstants.MOCK_WEB_SERVER_PORT,
      object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
          if (request.path?.startsWith("/pub", ignoreCase = true) == true) {
            val lastSegment = request.requestUrl?.pathSegments?.lastOrNull()

            when {
              ALLOWED_USER.equals(lastSegment, true) -> {
                return MockResponse()
                  .setResponseCode(HttpURLConnection.HTTP_OK)
                  .setBody(ALLOWED_USER_PUB_KEY)
              }
            }
          }

          return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
        }
      })
  }
}
