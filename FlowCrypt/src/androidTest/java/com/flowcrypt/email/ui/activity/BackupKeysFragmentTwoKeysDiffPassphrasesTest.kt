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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.junit.annotations.DependsOnMailServer
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.base.BaseBackupKeysFragmentTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.File

/**
 * @author Denis Bondarenko
 *         Date: 6/22/21
 *         Time: 11:30 AM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class BackupKeysFragmentTwoKeysDiffPassphrasesTest : BaseBackupKeysFragmentTest() {

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(ClearAppSettingsRule())
    .around(addAccountToDatabaseRule)
    .around(AddPrivateKeyToDatabaseRule())
    .around(
      AddPrivateKeyToDatabaseRule(
        accountEntity = addAccountToDatabaseRule.account,
        keyPath = "pgp/default@flowcrypt.test_secondKey_prv_strong_second.asc",
        passphrase = TestConstants.DEFAULT_SECOND_STRONG_PASSWORD,
        sourceType = KeyImportDetails.SourceType.EMAIL
      )
    )
    .around(RetryRule.DEFAULT)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  @DependsOnMailServer
  fun testDiffPassphrasesForEmailOption() {
    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())
    intending(hasComponent(ComponentName(getTargetContext(), ChangePassPhraseActivity::class.java)))
      .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    checkIsSnackbarDisplayedAndClick(getResString(R.string.different_pass_phrases))
    assertTrue(activityScenarioRule.scenario.state == Lifecycle.State.RESUMED)
  }

  @Test
  fun testDiffPassphrasesForDownloadOption() {
    onView(withId(R.id.rBDownloadOption))
      .check(matches(isDisplayed()))
      .perform(click())
    intendingFileChoose(File(""))
    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())
    intending(hasComponent(ComponentName(getTargetContext(), ChangePassPhraseActivity::class.java)))
      .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    checkIsSnackbarDisplayedAndClick(getResString(R.string.different_pass_phrases))
    assertTrue(activityScenarioRule.scenario.state == Lifecycle.State.RESUMED)
  }
}

