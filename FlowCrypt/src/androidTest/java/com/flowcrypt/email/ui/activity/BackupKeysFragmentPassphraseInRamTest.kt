/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.content.Intent
import androidx.test.core.app.ActivityScenario
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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.junit.annotations.DependsOnMailServer
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.rules.lazyActivityScenarioRule
import com.flowcrypt.email.ui.activity.base.BaseBackupKeysFragmentTest
import com.flowcrypt.email.ui.activity.settings.SettingsActivity
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 *         Date: 6/17/21
 *         Time: 5:05 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@DependsOnMailServer
class BackupKeysFragmentPassphraseInRamTest : BaseBackupKeysFragmentTest() {
  override val useLazyInit: Boolean = true
  override val activeActivityRule =
    lazyActivityScenarioRule<SettingsActivity>(launchActivity = false)
  override val activityScenario: ActivityScenario<*>?
    get() = activeActivityRule.scenario

  val addPrivateKeyToDatabaseRule =
    AddPrivateKeyToDatabaseRule(passphraseType = KeyEntity.PassphraseType.RAM)

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(ClearAppSettingsRule())
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(RetryRule.DEFAULT)
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  @Test
  fun testNeedPassphraseEmailOptionSingleFingerprint() {
    doPreparationAndGoToBackupKeysFragment { }

    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())

    checkSingleFingerprintWithSuccess()

    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withText(getResString(R.string.title_activity_settings)))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testNeedPassphraseDownloadOptionSingleFingerprint() {
    doPreparationAndGoToBackupKeysFragment { }

    onView(withId(R.id.rBDownloadOption))
      .check(matches(isDisplayed()))
      .perform(click())

    val file = TestGeneralUtil.createFileAndFillWithContent("key.asc", "")

    intendingFileChoose(file)
    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())

    checkSingleFingerprintWithSuccess()

    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())

    TestGeneralUtil.deleteFiles(listOf(file))

    onView(withText(getResString(R.string.title_activity_settings)))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testNeedPassphraseEmailOptionMultiplyFingerprints() {
    doPreparationAndGoToBackupKeysFragment {
      val secondKeyDetails = PrivateKeysManager.getPgpKeyDetailsFromAssets(
        "pgp/default@flowcrypt.test_secondKey_prv_strong_second.asc"
      )
      PrivateKeysManager.saveKeyToDatabase(
        accountEntity = addAccountToDatabaseRule.account,
        pgpKeyDetails = secondKeyDetails,
        passphrase = TestConstants.DEFAULT_STRONG_PASSWORD,
        sourceType = KeyImportDetails.SourceType.EMAIL,
        passphraseType = KeyEntity.PassphraseType.RAM
      )
    }

    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())

    checkMultiplyFingerprintsWithSuccess()

    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())

    checkIsSnackbarDisplayedAndClick(getResString(R.string.different_pass_phrases))
  }

  @Test
  fun testNeedPassphraseDownloadOptionMultiplyFingerprints() {
    doPreparationAndGoToBackupKeysFragment {
      val secondKeyDetails = PrivateKeysManager.getPgpKeyDetailsFromAssets(
        "pgp/default@flowcrypt.test_secondKey_prv_strong_second.asc"
      )
      PrivateKeysManager.saveKeyToDatabase(
        accountEntity = addAccountToDatabaseRule.account,
        pgpKeyDetails = secondKeyDetails,
        passphrase = TestConstants.DEFAULT_STRONG_PASSWORD,
        sourceType = KeyImportDetails.SourceType.EMAIL,
        passphraseType = KeyEntity.PassphraseType.RAM
      )
    }

    onView(withId(R.id.rBDownloadOption))
      .check(matches(isDisplayed()))
      .perform(click())

    val file = TestGeneralUtil.createFileAndFillWithContent("key.asc", "")

    intendingFileChoose(file)
    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())

    checkMultiplyFingerprintsWithSuccess()

    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())

    TestGeneralUtil.deleteFiles(listOf(file))

    checkIsSnackbarDisplayedAndClick(getResString(R.string.different_pass_phrases))
  }

  private fun checkSingleFingerprintWithSuccess() {
    val fingerprint = addPrivateKeyToDatabaseRule.pgpKeyDetails.fingerprint
    val fingerprintFormatted = GeneralUtil.doSectionsInText(
      originalString = fingerprint, groupSize = 4
    )

    val tVStatusMessageText = getQuantityString(
      R.plurals.please_provide_passphrase_for_all_following_keys,
      1
    )
    onView(withId(R.id.tVStatusMessage))
      .check(matches(withText(tVStatusMessageText)))

    onView(withId(R.id.rVKeys))
      .check(matches(isDisplayed()))
      .check(matches(withRecyclerViewItemCount(1)))

    onView(withText(fingerprintFormatted))
      .check(matches(isDisplayed()))

    onView(withId(R.id.eTKeyPassword))
      .inRoot(isDialog())
      .perform(
        clearText(),
        replaceText(TestConstants.DEFAULT_STRONG_PASSWORD),
        pressImeActionButton()
      )
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

  private fun doPreparationAndGoToBackupKeysFragment(action: () -> Unit) {
    action.invoke()

    activeActivityRule.launch(Intent(getTargetContext(), SettingsActivity::class.java))
    registerAllIdlingResources()

    goToBackupKeysFragmentInternal()
  }
}
