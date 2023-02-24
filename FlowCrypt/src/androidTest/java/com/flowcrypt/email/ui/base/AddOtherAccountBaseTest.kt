/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.base

import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.email.model.SecurityType
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withSecurityTypeOption
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf

/**
 * @author Denys Bondarenko
 */
abstract class AddOtherAccountBaseTest : BaseTest() {
  protected fun fillAllFields(authCredentials: AuthCredentials) {
    onView(withId(R.id.editTextEmail))
      .perform(
        clearText(),
        typeText(authCredentials.email),
        closeSoftKeyboard()
      )
    onView(withId(R.id.editTextUserName))
      .perform(
        clearText(),
        typeText(authCredentials.username),
        closeSoftKeyboard()
      )
    onView(withId(R.id.editTextPassword))
      .perform(
        clearText(),
        typeText(authCredentials.password),
        closeSoftKeyboard()
      )

    onView(withId(R.id.editTextImapServer))
      .perform(
        clearText(),
        typeText(authCredentials.imapServer),
        closeSoftKeyboard()
      )
    onView(withId(R.id.spinnerImapSecurityType))
      .perform(scrollTo(), click())
    onData(
      allOf(
        `is`(instanceOf<Any>(SecurityType::class.java)),
        withSecurityTypeOption(authCredentials.imapOpt)
      )
    ).perform(click())
    onView(withId(R.id.editTextImapPort))
      .perform(
        clearText(),
        typeText(authCredentials.imapPort.toString()),
        closeSoftKeyboard()
      )

    onView(withId(R.id.editTextSmtpServer))
      .perform(
        clearText(),
        typeText(authCredentials.smtpServer),
        closeSoftKeyboard()
      )
    onView(withId(R.id.spinnerSmtpSecyrityType))
      .perform(scrollTo(), click())
    onData(
      allOf(
        `is`(instanceOf<Any>(SecurityType::class.java)),
        withSecurityTypeOption(authCredentials.smtpOpt)
      )
    ).perform(click())
    onView(withId(R.id.editTextSmtpPort))
      .perform(
        clearText(),
        typeText(authCredentials.smtpPort.toString()),
        closeSoftKeyboard()
      )

    if (authCredentials.hasCustomSignInForSmtp) {
      onView(withId(R.id.checkBoxRequireSignInForSmtp))
        .perform(click())
      onView(withId(R.id.editTextSmtpUsername))
        .perform(
          clearText(),
          typeText(authCredentials.smtpSigInUsername),
          closeSoftKeyboard()
        )
      onView(withId(R.id.editTextSmtpPassword))
        .perform(
          clearText(),
          typeText(authCredentials.smtpSignInPassword),
          closeSoftKeyboard()
        )
    }
  }

  protected fun enableAdvancedMode() {
    onView(withId(R.id.checkBoxAdvancedMode))
      .check(matches(isNotChecked()))
      .perform(scrollTo(), click())
  }
}
