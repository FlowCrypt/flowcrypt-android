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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.junit.annotations.DependsOnMailServer
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.BackupKeysFragment
import com.flowcrypt.email.ui.base.BaseBackupKeysFragmentTest
import com.flowcrypt.email.util.TestGeneralUtil
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 *         Date: 6/22/21
 *         Time: 11:27 AM
 *         E-mail: DenBond7@gmail.com
 */

@MediumTest
@RunWith(AndroidJUnit4::class)
class BackupKeysFragmentTwoKeysSamePassphraseTest : BaseBackupKeysFragmentTest() {

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(AddPrivateKeyToDatabaseRule())
    .around(
      AddPrivateKeyToDatabaseRule(
        accountEntity = addAccountToDatabaseRule.account,
        keyPath = TestConstants.DEFAULT_SECOND_KEY_PRV_STRONG,
        passphrase = TestConstants.DEFAULT_STRONG_PASSWORD,
        sourceType = KeyImportDetails.SourceType.EMAIL
      )
    )
    .around(ScreenshotTestRule())

  @Before
  fun launchFragmentInContainerWithPredefinedArgs() {
    launchFragmentInContainer<BackupKeysFragment>()
  }

  @Test
  @DependsOnMailServer
  fun testSuccessWithTwoKeysEmailOption() {
    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withId(R.id.btBackup))
      .check(matches(not(isDisplayed())))

    isToastDisplayed(getResString(R.string.backed_up_successfully))
  }

  @Test
  fun testSuccessWithTwoKeysDownloadOption() {
    onView(withId(R.id.rBDownloadOption))
      .check(matches(isDisplayed()))
      .perform(click())

    val file = TestGeneralUtil.createFileWithTextContent("key.asc", "")

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
