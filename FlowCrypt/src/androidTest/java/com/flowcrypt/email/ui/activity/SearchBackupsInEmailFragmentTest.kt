/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.junit.annotations.DependsOnMailServer
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.rules.lazyActivityScenarioRule
import com.flowcrypt.email.ui.activity.settings.SettingsActivity
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.PrivateKeysManager
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
@DependsOnMailServer
class SearchBackupsInEmailFragmentTest : BaseTest() {
  override val activeActivityRule =
    lazyActivityScenarioRule<SettingsActivity>(launchActivity = false)
  override val activityScenario: ActivityScenario<*>?
    get() = activeActivityRule.scenario

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  @Test
  fun testBackupFound() {
    doPreparationAndGoToSearchBackupsInEmailFragment {
      val user = AccountDaoManager.getUserWithSingleBackup()
      FlowCryptRoomDatabase.getDatabase(getTargetContext()).accountDao().addAccount(user)
      PrivateKeysManager.saveKeyFromAssetsToDatabase(
        accountEntity = user,
        keyPath = "pgp/key_testing@flowcrypt.test_keyB_default.asc",
        passphrase = TestConstants.DEFAULT_PASSWORD,
        sourceType = KeyImportDetails.SourceType.EMAIL
      )
    }

    onView(withId(R.id.tVTitle))
      .check(matches(withText(getResString(R.string.backups_found, 1))))
    onView(withId(R.id.btBackup))
      .check(matches(withText(getResString(R.string.see_more_backup_options))))
  }

  @Test
  fun testBackupNotFound() {
    doPreparationAndGoToSearchBackupsInEmailFragment {
      val user = AccountDaoManager.getUserWithoutBackup()
      FlowCryptRoomDatabase.getDatabase(getTargetContext()).accountDao().addAccount(user)
      PrivateKeysManager.saveKeyFromAssetsToDatabase(
        accountEntity = user,
        keyPath = "pgp/key_testing@flowcrypt.test_keyB_default.asc",
        passphrase = TestConstants.DEFAULT_PASSWORD,
        sourceType = KeyImportDetails.SourceType.EMAIL
      )
    }

    onView(withId(R.id.tVTitle))
      .check(matches(withText(getResString(R.string.there_are_no_backups_on_this_account))))
    onView(withId(R.id.btBackup))
      .check(matches(withText(getResString(R.string.back_up_my_key))))
  }

  private fun doPreparationAndGoToSearchBackupsInEmailFragment(action: () -> Unit) {
    action.invoke()

    activeActivityRule.launch(Intent(getTargetContext(), SettingsActivity::class.java))
    registerAllIdlingResources()

    onView(withText(getResString(R.string.backups)))
      .check(matches(isDisplayed()))
      .perform(click())
  }
}
