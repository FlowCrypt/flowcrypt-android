/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.junit.annotations.DependsOnMailServer
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.settings.SettingsActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 *         Date: 6/17/21
 *         Time: 3:13 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@DependsOnMailServer
class SearchBackupsInEmailFragmentDefaultAccountTest : BaseTest() {
  override val activityScenarioRule = activityScenarioRule<SettingsActivity>()

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(AddAccountToDatabaseRule())
    .around(AddPrivateKeyToDatabaseRule())
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Before
  fun goToSearchBackupsInEmailFragment() {
    onView(withText(getResString(R.string.backups)))
      .check(matches(isDisplayed()))
      .perform(click())
  }

  @Test
  fun testShowHelpScreen() {
    testHelpScreen()
  }

  @Test
  fun testNavigationToBackupKeysFragment() {
    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
  }
}
