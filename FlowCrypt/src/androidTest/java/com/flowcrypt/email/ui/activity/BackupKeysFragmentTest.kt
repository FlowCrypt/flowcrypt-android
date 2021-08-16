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
import com.flowcrypt.email.junit.annotations.DependsOnMailServer
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.base.BaseBackupKeysFragmentTest
import com.flowcrypt.email.util.TestGeneralUtil
import org.hamcrest.CoreMatchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 17.08.2018
 * Time: 16:28
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class BackupKeysFragmentTest : BaseBackupKeysFragmentTest() {

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(ClearAppSettingsRule())
    .around(addAccountToDatabaseRule)
    .around(AddPrivateKeyToDatabaseRule())
    .around(RetryRule.DEFAULT)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testEmailOptionHint() {
    onView(withId(R.id.rBEmailOption))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withText(getResString(R.string.backup_as_email_hint)))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testDownloadOptionHint() {
    onView(withId(R.id.rBDownloadOption))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withText(getResString(R.string.backup_as_download_hint)))
      .check(matches(isDisplayed()))
  }

  @Test
  @DependsOnMailServer
  fun testSuccessEmailOption() {
    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withId(R.id.btBackup))
      .check(matches(not(isDisplayed())))
    isToastDisplayed(getResString(R.string.backed_up_successfully))
  }

  @Test
  fun testSuccessDownloadOption() {
    onView(withId(R.id.rBDownloadOption))
      .check(matches(isDisplayed()))
      .perform(click())

    val file = TestGeneralUtil.createFileAndFillWithContent("key.asc", "")

    intendingFileChoose(file)
    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())

    TestGeneralUtil.deleteFiles(listOf(file))

    onView(withId(R.id.btBackup))
      .check(matches(not(isDisplayed())))
    isToastDisplayed(getResString(R.string.backed_up_successfully))
  }
}
