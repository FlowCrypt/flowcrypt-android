/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.view.View
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.email.model.SecurityType
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withSecurityTypeOption
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.util.AuthCredentialsManager
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anyOf
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.isEmptyString
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.startsWith
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * A test for [AddNewAccountManuallyActivity]
 *
 * @author Denis Bondarenko
 * Date: 01.02.2018
 * Time: 13:28
 * E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class AddNewAccountManuallyActivityTest : BaseTest() {

  override val activityTestRule: ActivityTestRule<*>? = ActivityTestRule(AddNewAccountManuallyActivity::class.java)

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(activityTestRule)

  private val authCreds: AuthCredentials = AuthCredentialsManager.getDefaultWithBackupAuthCreds()

  @Before
  fun setUp() {
    IdlingPolicies.setMasterPolicyTimeout(60, TimeUnit.SECONDS)
  }

  @Test
  fun testAllCredsCorrect() {
    fillAllFields()
    onView(withId(R.id.buttonTryToConnect))
        .perform(scrollTo(), click())
    onView(withId(R.id.textViewTitle))
        .check(matches(isDisplayed()))
  }

  @Test
  fun testShowSnackBarIfFieldEmpty() {
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

    onView(withId(R.id.checkBoxRequireSignInForSmtp))
        .perform(scrollTo(), click())
    checkIsFieldEmptyWork(R.id.editTextSmtpUsername, R.string.smtp_username)
    checkIsFieldEmptyWork(R.id.editTextSmtpPassword, R.string.smtp_password)
  }

  @Test
  fun testIsPasswordFieldsAlwaysEmptyAtStart() {
    onView(withId(R.id.editTextPassword))
        .check(matches(withText(isEmptyString())))
    onView(withId(R.id.checkBoxRequireSignInForSmtp))
        .perform(scrollTo(), click())
    onView(withId(R.id.editTextSmtpPassword))
        .check(matches(withText(isEmptyString())))
  }

  @Test
  fun testChangingImapPortWhenSelectSpinnerItem() {
    checkSecurityTypeOpt(R.id.editTextImapPort, R.id.spinnerImapSecurityType,
        SecurityType.Option.STARTLS, JavaEmailConstants.DEFAULT_IMAP_PORT.toString())
    checkSecurityTypeOpt(R.id.editTextImapPort, R.id.spinnerImapSecurityType,
        SecurityType.Option.SSL_TLS, JavaEmailConstants.SSL_IMAP_PORT.toString())
    checkSecurityTypeOpt(R.id.editTextImapPort, R.id.spinnerImapSecurityType,
        SecurityType.Option.NONE, JavaEmailConstants.DEFAULT_IMAP_PORT.toString())
  }

  @Test
  fun testChangingSmtpPortWhenSelectSpinnerItem() {
    checkSecurityTypeOpt(R.id.editTextSmtpPort, R.id.spinnerSmtpSecyrityType,
        SecurityType.Option.STARTLS, JavaEmailConstants.STARTTLS_SMTP_PORT.toString())
    checkSecurityTypeOpt(R.id.editTextSmtpPort, R.id.spinnerSmtpSecyrityType,
        SecurityType.Option.SSL_TLS, JavaEmailConstants.SSL_SMTP_PORT.toString())
    checkSecurityTypeOpt(R.id.editTextSmtpPort, R.id.spinnerSmtpSecyrityType,
        SecurityType.Option.NONE, JavaEmailConstants.DEFAULT_SMTP_PORT.toString())
  }

  @Test
  fun testChangeFieldValuesWhenEmailChanged() {
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
    val text = newUserName + TestConstants.COMMERCIAL_AT_SYMBOL + newHost

    onView(withId(R.id.editTextEmail))
        .perform(scrollTo(), clearText(), typeText(text), closeSoftKeyboard())
    onView(withId(R.id.editTextUserName))
        .perform(scrollTo())
        .check(matches(not<View>(withText(authCreds.username))))
    onView(withId(R.id.editTextUserName))
        .check(matches(withText(newUserName)))
    onView(withId(R.id.editTextImapServer))
        .perform(scrollTo())
        .check(matches(not<View>(withText(authCreds.imapServer))))
    onView(withId(R.id.editTextImapServer))
        .check(matches(withText(IMAP_SERVER_PREFIX + newHost)))
    onView(withId(R.id.editTextSmtpServer))
        .perform(scrollTo())
        .check(matches(not<View>(withText(authCreds.smtpServer))))
    onView(withId(R.id.editTextSmtpServer))
        .check(matches(withText(SMTP_SERVER_PREFIX + newHost)))
    onView(withId(R.id.checkBoxRequireSignInForSmtp))
        .perform(scrollTo(), click())
    onView(withId(R.id.editTextSmtpUsername))
        .perform(scrollTo())
        .check(matches(withText(newUserName)))
  }

  @Test
  fun testVisibilityOfSmtpAuthField() {
    onView(withId(R.id.checkBoxRequireSignInForSmtp))
        .perform(scrollTo(), click())
    onView(withId(R.id.editTextSmtpUsername))
        .perform(scrollTo())
        .check(matches(isDisplayed()))
    onView(withId(R.id.editTextSmtpPassword))
        .perform(scrollTo())
        .check(matches(isDisplayed())).check(matches(withText(isEmptyString())))

    onView(withId(R.id.checkBoxRequireSignInForSmtp))
        .perform(scrollTo(), click())
    onView(withId(R.id.editTextSmtpUsername))
        .check(matches(not<View>(isDisplayed())))
    onView(withId(R.id.editTextSmtpPassword))
        .check(matches(not<View>(isDisplayed())))
  }

  @Test
  fun testFieldsAutoFilling() {

    val userName = authCreds.email.substring(0, authCreds.email.indexOf(TestConstants.COMMERCIAL_AT_SYMBOL))
    val host = authCreds.email.substring(authCreds.email.indexOf(TestConstants.COMMERCIAL_AT_SYMBOL) + 1)

    val incorrectEmailAddresses = arrayOf("default", "default@", "default@denbond7", "default@denbond7.")

    for (invalidEmailAddress in incorrectEmailAddresses) {
      onView(withId(R.id.editTextEmail))
          .perform(scrollTo(), clearText(), typeText(invalidEmailAddress), closeSoftKeyboard())
      onView(withId(R.id.editTextUserName))
          .perform(scrollTo())
          .check(matches(withText(isEmptyString())))
      onView(withId(R.id.editTextImapServer))
          .perform(scrollTo())
          .check(matches(withText(isEmptyString())))
      onView(withId(R.id.editTextSmtpServer))
          .perform(scrollTo())
          .check(matches(withText(isEmptyString())))
    }

    val text = userName + TestConstants.COMMERCIAL_AT_SYMBOL + host

    onView(withId(R.id.editTextEmail))
        .perform(scrollTo(), clearText(), typeText(text), closeSoftKeyboard())
    onView(withId(R.id.editTextUserName))
        .perform(scrollTo())
        .check(matches(withText(userName)))
    onView(withId(R.id.editTextImapServer))
        .perform(scrollTo())
        .check(matches(withText(IMAP_SERVER_PREFIX + host)))
    onView(withId(R.id.editTextSmtpServer))
        .perform(scrollTo())
        .check(matches(withText(SMTP_SERVER_PREFIX + host)))
  }

  @Test
  fun testWrongFormatOfEmailAddress() {
    fillAllFields()

    val invalidEmailAddresses = arrayOf("default", "default@", "default@@denbond7.com", "@denbond7.com", "    ")

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

  @Ignore//todo-denbond7 need to think about it
  @Test
  fun testShowWarningIfAuthFail() {
    IdlingPolicies.setMasterPolicyTimeout(5, TimeUnit.MINUTES)
    fillAllFields()
    val someFailTextToChangeRightValue = "123"

    val fieldIdentifiersWithIncorrectData = intArrayOf(R.id.editTextEmail, R.id.editTextUserName,
        R.id.editTextPassword, R.id.editTextImapServer, R.id.editTextImapPort, R.id.editTextSmtpServer,
        R.id.editTextSmtpPort, R.id.editTextSmtpUsername, R.id.editTextSmtpPassword)

    val correctData = arrayOf(authCreds.email, authCreds.username, authCreds.password, authCreds.imapServer,
        authCreds.imapPort.toString(), authCreds.smtpServer, authCreds.smtpPort.toString(),
        authCreds.smtpSigInUsername, authCreds.smtpSignInPassword)

    val numberOfChecks = if (authCreds.hasCustomSignInForSmtp) {
      fieldIdentifiersWithIncorrectData.size
    } else {
      fieldIdentifiersWithIncorrectData.size - 2
    }

    for (i in 0 until numberOfChecks) {
      onView(withId(fieldIdentifiersWithIncorrectData[i]))
          .perform(scrollTo(), typeText(someFailTextToChangeRightValue), closeSoftKeyboard())
      onView(withId(R.id.buttonTryToConnect))
          .perform(scrollTo(), click())

      onView(anyOf<View>(withText(startsWith(TestConstants.IMAP)), withText(startsWith(TestConstants.SMTP))))
          .check(matches(isDisplayed()))
      onView(withId(com.google.android.material.R.id.snackbar_action))
          .check(matches(isDisplayed()))
          .perform(click())

      onView(withId(fieldIdentifiersWithIncorrectData[i]))
          .perform(scrollTo(), clearText(), typeText(correctData[i]), closeSoftKeyboard())
    }
  }

  private fun checkSecurityTypeOpt(portViewId: Int, spinnerViewId: Int, option: SecurityType.Option, portValue: String) {
    val someValue = "111"
    onView(withId(portViewId))
        .perform(scrollTo(), clearText(), typeText(someValue), closeSoftKeyboard())
    onView(withId(spinnerViewId))
        .perform(scrollTo(), click())
    onData(allOf(`is`(instanceOf<Any>(SecurityType::class.java)), withSecurityTypeOption(option)))
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

  private fun fillAllFields() {
    onView(withId(R.id.editTextEmail))
        .perform(clearText(), typeText(authCreds.email), closeSoftKeyboard())
    onView(withId(R.id.editTextUserName))
        .perform(clearText(), typeText(authCreds.username), closeSoftKeyboard())
    onView(withId(R.id.editTextPassword))
        .perform(clearText(), typeText(authCreds.password), closeSoftKeyboard())

    onView(withId(R.id.editTextImapServer))
        .perform(clearText(), typeText(authCreds.imapServer), closeSoftKeyboard())
    onView(withId(R.id.spinnerImapSecurityType))
        .perform(scrollTo(), click())
    onData(allOf(`is`(instanceOf<Any>(SecurityType::class.java)), withSecurityTypeOption(authCreds.imapOpt)))
        .perform(click())
    onView(withId(R.id.editTextImapPort))
        .perform(clearText(), typeText(authCreds.imapPort.toString()), closeSoftKeyboard())

    onView(withId(R.id.editTextSmtpServer))
        .perform(clearText(), typeText(authCreds.smtpServer), closeSoftKeyboard())
    onView(withId(R.id.spinnerSmtpSecyrityType))
        .perform(scrollTo(), click())
    onData(allOf(`is`(instanceOf<Any>(SecurityType::class.java)), withSecurityTypeOption(authCreds.smtpOpt)))
        .perform(click())
    onView(withId(R.id.editTextSmtpPort))
        .perform(clearText(), typeText(authCreds.smtpPort.toString()), closeSoftKeyboard())

    if (authCreds.hasCustomSignInForSmtp) {
      onView(withId(R.id.checkBoxRequireSignInForSmtp))
          .perform(click())
      onView(withId(R.id.editTextSmtpUsername))
          .perform(clearText(), typeText(authCreds.smtpSigInUsername), closeSoftKeyboard())
      onView(withId(R.id.editTextSmtpPassword))
          .perform(clearText(), typeText(authCreds.smtpSignInPassword), closeSoftKeyboard())
    }
  }

  companion object {
    private const val IMAP_SERVER_PREFIX = "imap."
    private const val SMTP_SERVER_PREFIX = "smtp."
  }
}