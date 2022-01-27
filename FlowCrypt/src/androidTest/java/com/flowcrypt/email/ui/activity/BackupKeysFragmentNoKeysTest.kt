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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.base.BaseBackupKeysFragmentTest
import com.flowcrypt.email.util.AccountDaoManager
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 *         Date: 6/17/21
 *         Time: 5:13 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class BackupKeysFragmentNoKeysTest : BaseBackupKeysFragmentTest() {

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(addAccountToDatabaseRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testNoKeysEmailOption() {
    onView(withId(R.id.rBEmailOption))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(
      withText(
        getResString(
          R.string.there_are_no_private_keys,
          AccountDaoManager.getDefaultAccountDao().email
        )
      )
    )
      .check(matches(isDisplayed()))
  }

  @Test
  fun testNoKeysDownloadOption() {
    onView(withId(R.id.rBDownloadOption))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(
      withText(
        getResString(
          R.string.there_are_no_private_keys,
          AccountDaoManager.getDefaultAccountDao().email
        )
      )
    )
      .check(matches(isDisplayed()))
  }
}
