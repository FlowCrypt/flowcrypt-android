/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.flowcrypt.email.R
import com.flowcrypt.email.junit.annotations.DependsOnMailServer
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.base.BaseBackupKeysFragmentTest
import com.flowcrypt.email.util.AccountDaoManager
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import java.io.File

/**
 * @author Denis Bondarenko
 *         Date: 6/17/21
 *         Time: 5:13 PM
 *         E-mail: DenBond7@gmail.com
 */
class BackupKeysFragmentNoKeysTest : BaseBackupKeysFragmentTest() {

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(ClearAppSettingsRule())
    .around(addAccountToDatabaseRule)
    .around(RetryRule.DEFAULT)
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

  @Test
  fun testShowWeakPasswordHintForDownloadOption() {
    addFirstKeyWithDefaultPassword()
    onView(withId(R.id.rBDownloadOption))
      .check(matches(isDisplayed()))
      .perform(click())
    intendingFileChoose(File(""))
    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withText(getResString(R.string.pass_phrase_is_too_weak)))
      .check(matches(isDisplayed()))
  }

  @Test
  @DependsOnMailServer
  fun testFixWeakPasswordForEmailOption() {
    addFirstKeyWithDefaultPassword()
    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())
    intending(hasComponent(ComponentName(getTargetContext(), ChangePassPhraseActivity::class.java)))
      .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    checkIsSnackbarDisplayedAndClick(getResString(R.string.pass_phrase_is_too_weak))
    assertTrue(activityScenarioRule.scenario.state == Lifecycle.State.RESUMED)
  }

  @Test
  fun testFixWeakPasswordForDownloadOption() {
    addFirstKeyWithDefaultPassword()
    onView(withId(R.id.rBDownloadOption))
      .check(matches(isDisplayed()))
      .perform(click())
    intendingFileChoose(File(""))
    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())
    intending(hasComponent(ComponentName(getTargetContext(), ChangePassPhraseActivity::class.java)))
      .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    checkIsSnackbarDisplayedAndClick(getResString(R.string.pass_phrase_is_too_weak))
    assertTrue(activityScenarioRule.scenario.state == Lifecycle.State.RESUMED)
  }

  @Test
  @DependsOnMailServer
  fun testShowWeakPasswordHintForEmailOption() {
    addFirstKeyWithDefaultPassword()
    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withText(getResString(R.string.pass_phrase_is_too_weak)))
      .check(matches(isDisplayed()))
  }
}

