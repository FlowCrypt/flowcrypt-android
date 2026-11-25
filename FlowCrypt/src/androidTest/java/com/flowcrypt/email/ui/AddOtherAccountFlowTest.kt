/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.ui.base.AddOtherAccountBaseTest
import com.flowcrypt.email.util.AuthCredentialsManager
import com.flowcrypt.email.util.TestGeneralUtil
import org.hamcrest.Matchers.anyOf
import org.hamcrest.Matchers.startsWith
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@FlowCryptTestSettings(useCommonIdling = false)
class AddOtherAccountFlowTest : AddOtherAccountBaseTest() {

  override val activeActivityRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      destinationId = R.id.addOtherAccountFragment
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  @Test
  fun testShowWarningIfAuthFail() {
    enableAdvancedMode()
    val credentials = AuthCredentialsManager.getAuthCredentials("user_with_not_existed_server.json")
    fillAllFields(credentials)

    // Nothing listens on the IMAP and SMTP ports of *.flowcrypt.test (127.0.0.1), so every
    // attempt fails fast with a connection error, whichever field has an incorrect value.
    // The email goes last: changing it also overwrites the servers and the username.
    val checks = listOf(
      FieldCheck(R.id.editTextUserName, credentials.username, "wrong_user"),
      FieldCheck(R.id.editTextImapServer, credentials.imapServer, "wrong.imap.flowcrypt.test"),
      FieldCheck(R.id.editTextImapPort, credentials.imapPort.toString(), CLOSED_PORT),
      FieldCheck(R.id.editTextSmtpServer, credentials.smtpServer, "wrong.smtp.flowcrypt.test"),
      FieldCheck(R.id.editTextSmtpPort, credentials.smtpPort.toString(), CLOSED_PORT),
      FieldCheck(R.id.editTextEmail, credentials.email, "wrong_user@flowcrypt.test")
    )

    for (check in checks) {
      replaceFieldText(check.viewId, check.incorrectValue)
      onView(withId(R.id.buttonTryToConnect))
        .perform(scrollTo(), click())

      // The settings are checked in the background, so wait for the result dialog
      waitUntilViewMatches(
        viewId = android.R.id.message,
        matcher = withText(anyOf(startsWith(TestConstants.IMAP), startsWith(TestConstants.SMTP))),
        root = isDialog(),
        timeoutMs = TimeUnit.SECONDS.toMillis(30)
      )
      onView(withText(getResString(R.string.network_error)))
        .inRoot(isDialog())
        .check(matches(isDisplayed()))

      onView(withText(getResString(R.string.cancel)))
        .inRoot(isDialog())
        .check(matches(isDisplayed()))
        .perform(click())
      waitUntilViewMatches(viewId = R.id.buttonTryToConnect, matcher = isDisplayed())

      replaceFieldText(check.viewId, check.correctValue)
    }
  }

  private fun replaceFieldText(viewId: Int, text: String) {
    onView(withId(viewId))
      .perform(scrollTo(), replaceText(text), closeSoftKeyboard())
  }

  private data class FieldCheck(
    val viewId: Int,
    val correctValue: String,
    val incorrectValue: String
  )

  companion object {
    private const val CLOSED_PORT = "1"
  }
}
