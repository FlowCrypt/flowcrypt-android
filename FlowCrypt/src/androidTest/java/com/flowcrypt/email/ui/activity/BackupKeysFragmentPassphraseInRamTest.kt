/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

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
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.base.BaseBackupKeysFragmentTest
import com.flowcrypt.email.util.GeneralUtil
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
class BackupKeysFragmentPassphraseInRamTest : BaseBackupKeysFragmentTest() {
  val addPrivateKeyToDatabaseRule =
    AddPrivateKeyToDatabaseRule(passphraseType = KeyEntity.PassphraseType.RAM)

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(ClearAppSettingsRule())
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(RetryRule.DEFAULT)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testNeedPassphraseEmailOptionSingleFingerprint() {
    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())

    checkSingleFingerprintWithSuccess()

    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())

    isToastDisplayed(getResString(R.string.backed_up_successfully))
  }

  @Test
  fun testNeedPassphraseDownloadOptionSingleFingerprint() {
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

    isToastDisplayed(getResString(R.string.backed_up_successfully))
  }

  private fun checkSingleFingerprintWithSuccess() {
    val fingerprint = addPrivateKeyToDatabaseRule.pgpKeyDetails.fingerprint
    val fingerprintFormatted = GeneralUtil.doSectionsInText(
      originalString = fingerprint, groupSize = 4
    )

    val tVStatusMessageText = getQuantityString(
      R.plurals.please_provide_passphrase_for_following_keys,
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
}
