/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.ui.base.AddOtherAccountBaseTest
import com.flowcrypt.email.util.AuthCredentialsManager
import com.flowcrypt.email.util.TestGeneralUtil
import org.hamcrest.Matchers
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 *         Date: 12/14/22
 *         Time: 1:18 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class AddOtherAccountFlowBaseTest : AddOtherAccountBaseTest() {

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
    val creds = AuthCredentialsManager.getAuthCredentials("user_with_not_existed_server.json")
    fillAllFields(creds)
    val someFailTextToChangeRightValue = "123"

    val fieldIdentifiersWithIncorrectData = intArrayOf(
      R.id.editTextEmail,
      R.id.editTextUserName,
      R.id.editTextImapServer,
      R.id.editTextImapPort,
      R.id.editTextSmtpServer,
      R.id.editTextSmtpPort,
      R.id.editTextSmtpUsername,
      R.id.editTextSmtpPassword
    )

    val correctData = arrayOf(
      creds.email,
      creds.username,
      creds.password,
      creds.imapServer,
      creds.imapPort.toString(),
      creds.smtpServer,
      creds.smtpPort.toString(),
      creds.smtpSigInUsername,
      creds.smtpSignInPassword
    )

    val numberOfChecks = if (creds.hasCustomSignInForSmtp) {
      fieldIdentifiersWithIncorrectData.size
    } else {
      fieldIdentifiersWithIncorrectData.size - 2
    }

    for (i in 0 until numberOfChecks) {
      Espresso.onView(ViewMatchers.withId(fieldIdentifiersWithIncorrectData[i]))
        .perform(
          ViewActions.scrollTo(),
          ViewActions.typeText(someFailTextToChangeRightValue),
          ViewActions.closeSoftKeyboard()
        )
      Espresso.onView(ViewMatchers.withId(R.id.buttonTryToConnect))
        .perform(ViewActions.scrollTo(), ViewActions.click())

      Espresso.onView(
        Matchers.anyOf(
          ViewMatchers.withText(Matchers.startsWith(TestConstants.IMAP)),
          ViewMatchers.withText(Matchers.startsWith(TestConstants.SMTP))
        )
      )
        .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

      if (i in intArrayOf(
          R.id.editTextImapServer,
          R.id.editTextImapPort,
          R.id.editTextSmtpServer,
          R.id.editTextSmtpPort
        )
      ) {
        Espresso.onView(ViewMatchers.withText(getResString(R.string.network_error)))
          .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
      }

      Espresso.onView(ViewMatchers.withText(getResString(R.string.cancel)))
        .inRoot(RootMatchers.isDialog())
        .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        .perform(ViewActions.click())

      Espresso.onView(ViewMatchers.withId(fieldIdentifiersWithIncorrectData[i]))
        .perform(
          ViewActions.scrollTo(),
          ViewActions.clearText(),
          ViewActions.typeText(correctData[i]),
          ViewActions.closeSoftKeyboard()
        )
    }
  }
}
