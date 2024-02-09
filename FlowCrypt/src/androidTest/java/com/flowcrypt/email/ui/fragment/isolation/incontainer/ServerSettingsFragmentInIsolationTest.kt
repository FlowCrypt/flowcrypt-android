/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.ServerSettingsFragment
import com.flowcrypt.email.ui.base.AddAccountToDatabaseRuleInterface
import org.hamcrest.Matchers.emptyString
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@FlowCryptTestSettings(useCommonIdling = false)
class ServerSettingsFragmentInIsolationTest : BaseTest(), AddAccountToDatabaseRuleInterface {
  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(AddPrivateKeyToDatabaseRule())
    .around(ScreenshotTestRule())

  @Before
  fun launchFragmentInContainerWithPredefinedArgs() {
    launchFragmentInContainer<ServerSettingsFragment>()
  }

  @Test
  fun testBaseInfo() {
    Thread.sleep(1000)

    val account = addAccountToDatabaseRule.account

    onView(withId(R.id.editTextEmail))
      .check(matches(isNotEnabled()))
      .check(matches(withText(account.email)))
    onView(withId(R.id.editTextUserName))
      .check(matches(isNotEnabled()))
      .check(matches(withText(account.username)))
    onView(withId(R.id.editTextPassword))
      .check(matches(withText(`is`(emptyString()))))
    onView(withId(R.id.editTextImapServer))
      .check(matches(withText(account.imapServer)))
    onView(withId(R.id.editTextImapPort))
      .check(matches(withText(account.imapPort.toString())))

    onView(withId(R.id.editTextSmtpServer))
      .check(matches(withText(account.smtpServer)))
    onView(withId(R.id.editTextSmtpPort))
      .check(matches(withText(account.smtpPort.toString())))
    onView(withId(R.id.checkBoxRequireSignInForSmtp))
      .check(matches(isChecked()))
    onView(withId(R.id.editTextSmtpUsername))
      .check(matches(withText(account.smtpUsername)))
    onView(withId(R.id.editTextSmtpPassword))
      .check(matches(withText(`is`(emptyString()))))
  }
}
