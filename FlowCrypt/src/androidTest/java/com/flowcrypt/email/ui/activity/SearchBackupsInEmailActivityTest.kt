/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.ui.activity.settings.SearchBackupsInEmailActivity
import org.hamcrest.Matchers.not
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 07.03.2018
 * Time: 12:39
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class SearchBackupsInEmailActivityTest : BaseTest() {
  override val activityScenarioRule = activityScenarioRule<SearchBackupsInEmailActivity>()

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(AddAccountToDatabaseRule())
      .around(AddPrivateKeyToDatabaseRule())
      .around(RetryRule())
      .around(activityScenarioRule)
      .around(ScreenshotTestRule())

  @Test
  fun testShowHelpScreen() {
    testHelpScreen()
  }

  @Test
  fun testIsBackupFound() {
    onView(withId(R.id.buttonSeeMoreBackupOptions))
        .check(matches(isDisplayed()))
    onView(withId(R.id.textViewBackupFound))
        .check(matches(isDisplayed()))
  }

  @Test
  @Ignore("fix me")
  fun testShowBackupOptions() {
    testIsBackupFound()
    onView(withId(R.id.buttonSeeMoreBackupOptions))
        .perform(click())
    onView(withId(R.id.buttonBackupAction))
        .check(matches(isDisplayed()))
  }

  @Test
  @Ignore("fix me")
  fun testSelectEmailForSavingBackup() {
    testShowBackupOptions()
    onView(withId(R.id.radioButtonEmail))
        .check(matches(isDisplayed()))
        .perform(click()).check(matches(isChecked()))
    onView(withId(R.id.textViewOptionsHint))
        .check(matches(isDisplayed())).check(matches(withText(R.string.backup_as_email_hint)))
    onView(withId(R.id.buttonBackupAction))
        .check(matches(withText(R.string.backup_as_email)))
    onView(withId(R.id.radioButtonDownload))
        .check(matches(isDisplayed())).check(matches(not<View>(isChecked())))
  }

  @Test
  @Ignore("fix me")
  fun testSelectDownloadToFileForSavingBackup() {
    testShowBackupOptions()
    onView(withId(R.id.radioButtonDownload))
        .check(matches(isDisplayed()))
        .perform(click()).check(matches(isChecked()))
    onView(withId(R.id.textViewOptionsHint))
        .check(matches(isDisplayed())).check(matches(withText(R.string.backup_as_download_hint)))
    onView(withId(R.id.buttonBackupAction))
        .check(matches(withText(R.string.backup_as_a_file)))
    onView(withId(R.id.radioButtonEmail))
        .check(matches(isDisplayed())).check(matches(not<View>(isChecked())))
  }
}
