/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

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
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.BackupKeysFragment
import com.flowcrypt.email.ui.base.BaseBackupKeysFragmentTest
import com.flowcrypt.email.util.AccountDaoManager
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
class BackupKeysFragmentNoKeysInIsolationTest : BaseBackupKeysFragmentTest() {

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(ScreenshotTestRule())

  @Before
  fun launchFragmentInContainerWithPredefinedArgs() {
    launchFragmentInContainer<BackupKeysFragment>()
  }

  @Test
  fun testNoKeysEmailOption() {
    launchFragmentInContainer<BackupKeysFragment>()
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
