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
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.junit.annotations.DependsOnMailServer
import com.flowcrypt.email.junit.annotations.NotReadyForCI
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.base.BaseBackupKeysFragmentTest
import com.flowcrypt.email.ui.activity.fragment.BackupKeysFragment
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.File

/**
 * @author Denis Bondarenko
 *         Date: 6/22/21
 *         Time: 11:18 AM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class BackupKeysFragmentSingleKeyWeakPassphraseInIsolationTest : BaseBackupKeysFragmentTest() {

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(addAccountToDatabaseRule)
    .around(
      AddPrivateKeyToDatabaseRule(
        accountEntity = addAccountToDatabaseRule.account,
        keyPath = "pgp/default@flowcrypt.test_fisrtKey_prv_default.asc",
        passphrase = TestConstants.DEFAULT_PASSWORD,
        sourceType = KeyImportDetails.SourceType.EMAIL
      )
    )
    .around(ScreenshotTestRule())

  @Before
  fun launchFragmentInContainerWithPredefinedArgs() {
    launchFragmentInContainer<BackupKeysFragment>()
  }

  @Test
  fun testShowWeakPasswordHintForDownloadOption() {
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
  @NotReadyForCI
  fun testShowWeakPasswordHintForEmailOption() {
    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withText(getResString(R.string.pass_phrase_is_too_weak)))
      .check(matches(isDisplayed()))
  }
}
