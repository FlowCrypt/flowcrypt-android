/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.DoesNotNeedMailserver
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.ui.activity.settings.SettingsActivity
import org.hamcrest.Matchers.allOf
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 20.02.2018
 * Time: 15:42
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@DoesNotNeedMailserver
class SettingsActivityTest : BaseTest() {
  override val activityScenarioRule = activityScenarioRule<SettingsActivity>()

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(AddAccountToDatabaseRule())
      .around(RetryRule())
      .around(activityScenarioRule)
      .around(ScreenshotTestRule())

  @Test
  fun testShowHelpScreen() {
    testHelpScreen()
  }

  @Test
  fun testShowBackupsScreen() {
    checkIsScreenDisplaying(getResString(R.string.backups))
  }

  @Test
  fun testShowSecurityScreen() {
    checkIsScreenDisplaying(getResString(R.string.security_and_privacy))
  }

  @Test
  fun testShowContactsScreen() {
    checkIsScreenDisplaying(getResString(R.string.contacts))
  }

  @Test
  fun testShowKeysScreen() {
    checkIsScreenDisplaying(getResString(R.string.keys))
  }

  @Test
  fun testShowAttesterScreen() {
    checkIsScreenDisplaying(getResString(R.string.attester))
  }

  @Test
  @Ignore("Failed on CI")
  fun testShowLegalScreen() {
    checkIsScreenDisplaying(getResString(R.string.experimental), getResString(R.string.experimental_settings))
  }

  private fun checkIsScreenDisplaying(screenName: String) {
    checkIsScreenDisplaying(screenName, screenName)
  }

  private fun checkIsScreenDisplaying(commandName: String, screenName: String) {
    onView(withText(commandName))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(allOf<View>(withText(screenName), withParent(withId(R.id.toolbar))))
        .check(matches(isDisplayed()))
  }
}
