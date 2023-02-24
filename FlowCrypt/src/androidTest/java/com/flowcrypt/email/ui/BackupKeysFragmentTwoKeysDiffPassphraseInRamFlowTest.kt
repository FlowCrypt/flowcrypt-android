/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.junit.annotations.DependsOnMailServer
import com.flowcrypt.email.junit.annotations.NotReadyForCI
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.ui.base.BaseBackupKeysFragmentTest
import com.flowcrypt.email.util.TestGeneralUtil
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
class BackupKeysFragmentTwoKeysDiffPassphraseInRamFlowTest : BaseBackupKeysFragmentTest() {
  val addPrivateKeyToDatabaseRule =
    AddPrivateKeyToDatabaseRule(passphraseType = KeyEntity.PassphraseType.RAM)
  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      destinationId = R.id.backupKeysFragment
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(
      AddPrivateKeyToDatabaseRule(
        accountEntity = addAccountToDatabaseRule.account,
        keyPath = "pgp/default@flowcrypt.test_secondKey_prv_strong_second.asc",
        passphrase = TestConstants.DEFAULT_SECOND_STRONG_PASSWORD,
        sourceType = KeyImportDetails.SourceType.EMAIL,
        passphraseType = KeyEntity.PassphraseType.RAM
      )
    )
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  @DependsOnMailServer
  @NotReadyForCI
  fun testNeedPassphraseEmailOptionMultiplyFingerprints() {
    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())

    checkMultiplyFingerprintsWithSuccess()

    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())

    checkIsSnackBarDisplayed(getResString(R.string.different_pass_phrases))
  }

  @Test
  @NotReadyForCI
  fun testNeedPassphraseDownloadOptionMultiplyFingerprints() {
    onView(withId(R.id.rBDownloadOption))
      .check(matches(isDisplayed()))
      .perform(click())

    val file = TestGeneralUtil.createFileWithTextContent("key.asc", "")

    intendingFileChoose(file)
    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())

    checkMultiplyFingerprintsWithSuccess()

    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())

    TestGeneralUtil.deleteFiles(listOf(file))

    checkIsSnackBarDisplayed(getResString(R.string.different_pass_phrases))
  }

  private fun checkMultiplyFingerprintsWithSuccess() {
    val tVStatusMessageText = getQuantityString(
      resId = R.plurals.please_provide_passphrase_for_all_following_keys,
      quantity = 2
    )
    onView(withId(R.id.tVStatusMessage))
      .check(matches(withText(tVStatusMessageText)))

    onView(withId(R.id.rVKeys))
      .check(matches(isDisplayed()))
      .check(matches(withRecyclerViewItemCount(2)))

    onView(withId(R.id.eTKeyPassword))
      .inRoot(isDialog())
      .perform(
        clearText(),
        replaceText(TestConstants.DEFAULT_STRONG_PASSWORD),
        pressImeActionButton()
      )

    onView(withId(R.id.rVKeys))
      .check(matches(isDisplayed()))
      .check(matches(withRecyclerViewItemCount(1)))

    onView(withId(R.id.eTKeyPassword))
      .inRoot(isDialog())
      .perform(
        clearText(),
        replaceText(TestConstants.DEFAULT_SECOND_STRONG_PASSWORD),
        pressImeActionButton()
      )
  }
}

