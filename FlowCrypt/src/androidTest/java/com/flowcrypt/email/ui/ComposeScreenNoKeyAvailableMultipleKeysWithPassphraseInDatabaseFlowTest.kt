/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasTextColor
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.extensions.kotlin.asInternetAddress
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.base.BaseComposeScreenNoKeyAvailableTest
import org.hamcrest.CoreMatchers.not
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
class ComposeScreenNoKeyAvailableMultipleKeysWithPassphraseInDatabaseFlowTest :
  BaseComposeScreenNoKeyAvailableTest() {
  private val addPrivateKeyToDatabaseRuleFirst = AddPrivateKeyToDatabaseRule(
    keyPath = "pgp/key_testing@flowcrypt.test_keyA_strong.asc"
  )

  private val addPrivateKeyToDatabaseRuleSecond = AddPrivateKeyToDatabaseRule(
    keyPath = "pgp/key_testing@flowcrypt.test_keyC_strong.asc"
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRuleFirst)
    .around(addPrivateKeyToDatabaseRuleSecond)
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  @Test
  fun testAddEmailToExistingKey() {
    doTestAddEmailToExistingKey {
      waitForObjectWithText(getResString(android.R.string.ok), 2000)

      onView(withId(R.id.buttonOk))
        .check(matches(isDisplayed()))
        .perform(click())
    }
  }
}
