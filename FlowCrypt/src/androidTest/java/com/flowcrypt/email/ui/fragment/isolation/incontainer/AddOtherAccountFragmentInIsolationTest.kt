/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.email.model.SecurityType
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withSecurityTypeOption
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.AddOtherAccountFragment
import com.flowcrypt.email.ui.base.AddOtherAccountBaseTest
import com.flowcrypt.email.util.AuthCredentialsManager
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.emptyString
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denys Bondarenko
 */
@FlowCryptTestSettings(useIntents = true)
@MediumTest
@RunWith(AndroidJUnit4::class)
class AddOtherAccountFragmentInIsolationTest : AddOtherAccountBaseTest() {
  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(ScreenshotTestRule())

  private val authCreds: AuthCredentials = AuthCredentialsManager.getLocalWithOneBackupAuthCreds()

  @Before
  fun launchFragmentInContainerWithPredefinedArgs() {
    launchFragmentInContainer<AddOtherAccountFragment>()
  }

  @Test
  fun testShowSnackBarIfFieldEmpty() {
    onView(withId(R.id.checkBoxAdvancedMode))
      .perform(scrollTo(), click())

    clearAllFields()

    checkIsFieldEmptyWork(R.id.editTextEmail, R.string.e_mail)
    checkIsFieldEmptyWork(R.id.editTextUserName, R.string.username)
    checkIsFieldEmptyWork(R.id.editTextPassword, R.string.password)

    checkIsFieldEmptyWork(R.id.editTextImapServer, R.string.imap_server)
    checkIsFieldEmptyWork(R.id.editTextImapPort, R.string.imap_port)
    onView(withId(R.id.editTextImapPort))
      .perform(scrollTo(), typeText(authCreds.imapPort.toString()))

    checkIsFieldEmptyWork(R.id.editTextSmtpServer, R.string.smtp_server)
    checkIsFieldEmptyWork(R.id.editTextSmtpPort, R.string.smtp_port)
    onView(withId(R.id.editTextSmtpPort))
      .perform(scrollTo(), typeText(authCreds.smtpPort.toString()))

    checkIsFieldEmptyWork(R.id.editTextSmtpUsername, R.string.smtp_username)
    checkIsFieldEmptyWork(R.id.editTextSmtpPassword, R.string.smtp_password)
  }

  @Test
  fun testIsPasswordFieldsAlwaysEmptyAtStart() {
    onView(withId(R.id.editTextPassword))
      .check(matches(withText(`is`(emptyString()))))
    enableAdvancedMode()
    onView(withId(R.id.checkBoxRequireSignInForSmtp))
      .perform(scrollTo(), click())
    onView(withId(R.id.editTextSmtpPassword))
      .check(matches(withText(`is`(emptyString()))))
  }

  @Test
  fun testChangingImapPortWhenSelectSpinnerItem() {
    enableAdvancedMode()
    checkSecurityTypeOpt(
      R.id.editTextImapPort, R.id.spinnerImapSecurityType,
      SecurityType.Option.STARTLS, JavaEmailConstants.DEFAULT_IMAP_PORT.toString()
    )
    checkSecurityTypeOpt(
      R.id.editTextImapPort, R.id.spinnerImapSecurityType,
      SecurityType.Option.SSL_TLS, JavaEmailConstants.SSL_IMAP_PORT.toString()
    )
    checkSecurityTypeOpt(
      R.id.editTextImapPort, R.id.spinnerImapSecurityType,
      SecurityType.Option.NONE, JavaEmailConstants.DEFAULT_IMAP_PORT.toString()
    )
  }

  @Test
  fun testChangingSmtpPortWhenSelectSpinnerItem() {
    enableAdvancedMode()
    checkSecurityTypeOpt(
      R.id.editTextSmtpPort, R.id.spinnerSmtpSecyrityType,
      SecurityType.Option.STARTLS, JavaEmailConstants.STARTTLS_SMTP_PORT.toString()
    )
    checkSecurityTypeOpt(
      R.id.editTextSmtpPort, R.id.spinnerSmtpSecyrityType,
      SecurityType.Option.SSL_TLS, JavaEmailConstants.SSL_SMTP_PORT.toString()
    )
    checkSecurityTypeOpt(
      R.id.editTextSmtpPort, R.id.spinnerSmtpSecyrityType,
      SecurityType.Option.NONE, JavaEmailConstants.DEFAULT_SMTP_PORT.toString()
    )
  }

  @Test
  fun testChangeFieldValuesWhenEmailChanged() {
    enableAdvancedMode()

    onView(withId(R.id.editTextEmail))
      .perform(clearText(), typeText(authCreds.email), closeSoftKeyboard())
    onView(withId(R.id.editTextUserName))
      .perform(clearText(), typeText(authCreds.username), closeSoftKeyboard())
    onView(withId(R.id.editTextImapServer))
      .perform(clearText(), typeText(authCreds.imapServer), closeSoftKeyboard())
    onView(withId(R.id.editTextSmtpServer))
      .perform(clearText(), typeText(authCreds.smtpServer), closeSoftKeyboard())

    val newUserName = "test"
    val newHost = "test.com"
    val fullUserName = newUserName + TestConstants.COMMERCIAL_AT_SYMBOL + newHost

    onView(withId(R.id.editTextEmail))
      .perform(scrollTo(), clearText(), typeText(fullUserName), closeSoftKeyboard())
    onView(withId(R.id.editTextUserName))
      .perform(scrollTo())
      .check(matches(not(withText(authCreds.username))))
    onView(withId(R.id.editTextUserName))
      .check(matches(withText(fullUserName)))
    onView(withId(R.id.editTextImapServer))
      .perform(scrollTo())
      .check(matches(not(withText(authCreds.imapServer))))
    onView(withId(R.id.editTextImapServer))
      .check(matches(withText(IMAP_SERVER_PREFIX + newHost)))
    onView(withId(R.id.editTextSmtpServer))
      .perform(scrollTo())
      .check(matches(not(withText(authCreds.smtpServer))))
    onView(withId(R.id.editTextSmtpServer))
      .check(matches(withText(SMTP_SERVER_PREFIX + newHost)))
    onView(withId(R.id.checkBoxRequireSignInForSmtp))
      .perform(scrollTo(), click())
    onView(withId(R.id.editTextSmtpUsername))
      .perform(scrollTo())
      .check(matches(withText(fullUserName)))
  }

  @Test
  fun testVisibilityOfSmtpAuthField() {
    enableAdvancedMode()

    onView(withId(R.id.checkBoxRequireSignInForSmtp))
      .perform(scrollTo(), click())
    onView(withId(R.id.editTextSmtpUsername))
      .perform(scrollTo())
      .check(matches(isDisplayed()))
    onView(withId(R.id.editTextSmtpPassword))
      .perform(scrollTo())
      .check(matches(isDisplayed())).check(matches(withText(`is`(emptyString()))))

    onView(withId(R.id.checkBoxRequireSignInForSmtp))
      .perform(scrollTo(), click())
    onView(withId(R.id.editTextSmtpUsername))
      .check(matches(not(isDisplayed())))
    onView(withId(R.id.editTextSmtpPassword))
      .check(matches(not(isDisplayed())))
  }

  @Test
  fun testFieldsAutoFilling() {
    enableAdvancedMode()

    val userName =
      authCreds.email.substring(0, authCreds.email.indexOf(TestConstants.COMMERCIAL_AT_SYMBOL))
    val host =
      authCreds.email.substring(authCreds.email.indexOf(TestConstants.COMMERCIAL_AT_SYMBOL) + 1)

    val incorrectEmailAddresses =
      arrayOf("default", "default@", "default@denbond7", "default@denbond7.")

    for (invalidEmailAddress in incorrectEmailAddresses) {
      onView(withId(R.id.editTextEmail))
        .perform(scrollTo(), clearText(), typeText(invalidEmailAddress), closeSoftKeyboard())
      onView(withId(R.id.editTextUserName))
        .perform(scrollTo())
        .check(matches(withText(`is`(emptyString()))))
      onView(withId(R.id.editTextImapServer))
        .perform(scrollTo())
        .check(matches(withText(`is`(emptyString()))))
      onView(withId(R.id.editTextSmtpServer))
        .perform(scrollTo())
        .check(matches(withText(`is`(emptyString()))))
    }

    val text = userName + TestConstants.COMMERCIAL_AT_SYMBOL + host

    onView(withId(R.id.editTextEmail))
      .perform(scrollTo(), clearText(), typeText(text), closeSoftKeyboard())
    onView(withId(R.id.editTextUserName))
      .perform(scrollTo())
      .check(matches(withText(text)))
    onView(withId(R.id.editTextImapServer))
      .perform(scrollTo())
      .check(matches(withText(IMAP_SERVER_PREFIX + host)))
    onView(withId(R.id.editTextSmtpServer))
      .perform(scrollTo())
      .check(matches(withText(SMTP_SERVER_PREFIX + host)))
  }

  @Test
  fun testWrongFormatOfEmailAddress() {
    enableAdvancedMode()
    fillAllFields(authCreds)

    val invalidEmailAddresses =
      arrayOf("default", "default@", "default@@flowcrypt.test", "@flowcrypt.test", "    ")

    for (invalidEmailAddress in invalidEmailAddresses) {
      onView(withId(R.id.editTextEmail))
        .perform(scrollTo(), clearText(), typeText(invalidEmailAddress), closeSoftKeyboard())
      onView(withId(R.id.buttonTryToConnect))
        .perform(scrollTo(), click())
      onView(withText(getResString(R.string.error_email_is_not_valid)))
        .check(matches(isDisplayed()))
      onView(withId(com.google.android.material.R.id.snackbar_action))
        .check(matches(isDisplayed()))
        .perform(click())
    }
  }

  private fun checkSecurityTypeOpt(
    portViewId: Int, spinnerViewId: Int, option: SecurityType.Option,
    portValue: String
  ) {
    val someValue = "111"
    onView(withId(portViewId))
      .perform(scrollTo(), clearText(), typeText(someValue), closeSoftKeyboard())
    onView(withId(spinnerViewId))
      .perform(scrollTo(), click())
    onData(allOf(`is`(instanceOf(SecurityType::class.java)), withSecurityTypeOption(option)))
      .perform(click())
    onView(withId(portViewId))
      .check(matches(withText(portValue)))
  }

  private fun checkIsFieldEmptyWork(viewId: Int, stringIdForError: Int) {
    onView(withId(R.id.editTextEmail))
      .perform(scrollTo(), clearText(), typeText(authCreds.email), closeSoftKeyboard())
    onView(withId(R.id.editTextPassword))
      .perform(clearText(), typeText(authCreds.password), closeSoftKeyboard())

    onView(withId(viewId))
      .perform(scrollTo(), clearText())
    onView(withId(R.id.buttonTryToConnect))
      .perform(scrollTo(), click())

    val text = getResString(R.string.text_must_not_be_empty, getResString(stringIdForError))

    onView(withText(text))
      .check(matches(isDisplayed()))
    onView(withId(com.google.android.material.R.id.snackbar_action))
      .check(matches(isDisplayed())).perform(click())
  }

  private fun clearAllFields() {
    onView(withId(R.id.editTextEmail))
      .perform(scrollTo(), clearText(), closeSoftKeyboard())
    onView(withId(R.id.editTextUserName))
      .perform(scrollTo(), clearText(), closeSoftKeyboard())
    onView(withId(R.id.editTextPassword))
      .perform(scrollTo(), clearText(), closeSoftKeyboard())

    onView(withId(R.id.editTextImapServer))
      .perform(scrollTo(), clearText(), closeSoftKeyboard())
    onView(withId(R.id.spinnerImapSecurityType))
      .perform(scrollTo(), click())
    onData(
      allOf(
        `is`(instanceOf<Any>(SecurityType::class.java)),
        withSecurityTypeOption(SecurityType.Option.NONE)
      )
    )
      .perform(scrollTo(), click())
    onView(withId(R.id.editTextImapPort))
      .perform(scrollTo(), clearText(), closeSoftKeyboard())

    onView(withId(R.id.editTextSmtpServer))
      .perform(scrollTo(), clearText(), closeSoftKeyboard())
    onView(withId(R.id.spinnerSmtpSecyrityType))
      .perform(scrollTo(), click())
    onData(
      allOf(
        `is`(instanceOf<Any>(SecurityType::class.java)),
        withSecurityTypeOption(SecurityType.Option.NONE)
      )
    )
      .perform(click())
    onView(withId(R.id.editTextSmtpPort))
      .perform(clearText(), closeSoftKeyboard())

    onView(withId(R.id.checkBoxRequireSignInForSmtp))
      .check(matches(isNotChecked()))
      .perform(scrollTo(), click())
    onView(withId(R.id.editTextSmtpUsername))
      .perform(scrollTo(), clearText(), closeSoftKeyboard())
    onView(withId(R.id.editTextSmtpPassword))
      .perform(scrollTo(), clearText(), closeSoftKeyboard())
  }

  companion object {
    private const val IMAP_SERVER_PREFIX = "imap."
    private const val SMTP_SERVER_PREFIX = "smtp."
  }
}
