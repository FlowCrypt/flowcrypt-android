/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.CICandidateAnnotation
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.lazyActivityScenarioRule
import com.flowcrypt.email.util.PrivateKeysManager
import org.junit.Assert
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 02.03.2018
 * Time: 19:00
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@CICandidateAnnotation
class CheckKeysActivityTestMultiBackups : BaseTest() {
  override val activeActivityRule = lazyActivityScenarioRule<CheckKeysActivity>(launchActivity = false)
  override val activityScenario: ActivityScenario<*>?
    get() = activeActivityRule.scenario

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(activeActivityRule)

  /**
   * There are two keys (all keys are different and have different pass phrases). Only one key from two keys is using.
   */
  @Test
  @Ignore("fix me")
  fun testUseTwoKeysFirstCombination() {
    val keysPaths = arrayOf(
        "node/key_testing@denbond7.com_keyA_strong.json",
        "node/key_testing@denbond7.com_keyB_default.json")
    launchActivity(keysPaths)

    checkKeysTitleAtStart(2)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)
    checkKeysTitle(1, 2, 1)
    checkSkipRemainingBackupsButton()
  }

  /**
   * There are two keys (all keys are different and have different pass phrases). All keys are checking in the queue.
   */
  @Test
  fun testUseTwoKeysSecondCombination() {
    val keysPaths = arrayOf(
        "node/key_testing@denbond7.com_keyA_strong.json",
        "node/key_testing@denbond7.com_keyB_default.json")
    launchActivity(keysPaths)

    checkKeysTitleAtStart(2)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)
    checkKeysTitle(1, 2, 1)
    typePassword(TestConstants.DEFAULT_PASSWORD)

    Assert.assertTrue(activeActivityRule.getNonNullScenario().state == Lifecycle.State.DESTROYED)
    Assert.assertTrue(activeActivityRule.getNonNullScenario().result.resultCode == Activity.RESULT_OK)
  }

  /**
   * There are two keys with the same pass phrase. All keys will be imported per one transaction.
   */
  @Test
  fun testUseTwoKeysWithSamePasswordThirdCombination() {
    val keysPaths = arrayOf(
        "node/key_testing@denbond7.com_keyA_strong.json",
        "node/key_testing@denbond7.com_keyC_strong.json")
    launchActivity(keysPaths)

    checkKeysTitleAtStart(2)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)

    Assert.assertTrue(activeActivityRule.getNonNullScenario().state == Lifecycle.State.DESTROYED)
    Assert.assertTrue(activeActivityRule.getNonNullScenario().result.resultCode == Activity.RESULT_OK)
  }

  /**
   * There are two keys (the identical keys with different pass phrases). A key will be imported using
   * [TestConstants.DEFAULT_PASSWORD].
   */
  @Test
  fun testUseTwoKeysFourthCombination() {
    val keysPaths = arrayOf(
        "node/key_testing@denbond7.com_keyC_default.json",
        "node/key_testing@denbond7.com_keyC_strong.json")
    launchActivity(keysPaths)

    checkKeysTitleAtStart(1)
    typePassword(TestConstants.DEFAULT_PASSWORD)

    Assert.assertTrue(activeActivityRule.getNonNullScenario().state == Lifecycle.State.DESTROYED)
    Assert.assertTrue(activeActivityRule.getNonNullScenario().result.resultCode == Activity.RESULT_OK)
  }

  /**
   * There are two keys (the identical keys with different pass phrases). A key will be imported using
   * [TestConstants.DEFAULT_STRONG_PASSWORD]
   */
  @Test
  @Ignore("fix me")
  fun testUseTwoKeysFifthCombination() {
    val keysPaths = arrayOf(
        "node/key_testing@denbond7.com_keyC_default.json",
        "node/key_testing@denbond7.com_keyC_strong.json")
    launchActivity(keysPaths)

    checkKeysTitleAtStart(1)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)

    Assert.assertTrue(activeActivityRule.getNonNullScenario().state == Lifecycle.State.DESTROYED)
    Assert.assertTrue(activeActivityRule.getNonNullScenario().result.resultCode == Activity.RESULT_OK)
  }

  /**
   * There are three keys (all keys are different, two keys have the identical pass phrase). Will be used only one
   * key with a unique pass phrase.
   */
  @Test
  fun testUseThreeFirstCombination() {
    val keysPaths = arrayOf(
        "node/key_testing@denbond7.com_keyA_strong.json",
        "node/key_testing@denbond7.com_keyB_default.json",
        "node/key_testing@denbond7.com_keyC_default.json")
    launchActivity(keysPaths)

    checkKeysTitleAtStart(3)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)
    checkKeysTitle(1, 3, 2)
    checkSkipRemainingBackupsButton()
  }

  /**
   * There are three keys (all keys are different, two keys have the identical pass phrase). Will be used two keys
   * with the same pass phrase.
   */
  @Test
  fun testUseThreeKeysSecondCombination() {
    val keysPaths = arrayOf(
        "node/key_testing@denbond7.com_keyA_strong.json",
        "node/key_testing@denbond7.com_keyB_default.json",
        "node/key_testing@denbond7.com_keyC_strong.json")
    launchActivity(keysPaths)

    checkKeysTitleAtStart(3)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)
    checkKeysTitle(2, 3, 1)
    checkSkipRemainingBackupsButton()
  }

  /**
   * There are three keys (all keys are different, two keys have the identical pass phrase). First will be used a key
   * with a unique pass phrase, and then the remaining keys.
   */
  @Test
  fun testUseThreeKeysThirdCombination() {
    val keysPaths = arrayOf(
        "node/key_testing@denbond7.com_keyA_strong.json",
        "node/key_testing@denbond7.com_keyB_default.json",
        "node/key_testing@denbond7.com_keyC_default.json")
    launchActivity(keysPaths)

    checkKeysTitleAtStart(3)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)
    checkKeysTitle(1, 3, 2)
    typePassword(TestConstants.DEFAULT_PASSWORD)

    Assert.assertTrue(activeActivityRule.getNonNullScenario().state == Lifecycle.State.DESTROYED)
    Assert.assertTrue(activeActivityRule.getNonNullScenario().result.resultCode == Activity.RESULT_OK)
  }

  /**
   * There are three keys (all keys are different, two keys have the identical pass phrase). First will be used two
   * keys with the same pass phrase, and then the remaining key.
   */
  @Test
  fun testUseThreeKeysFourthCombination() {
    val keysPaths = arrayOf(
        "node/key_testing@denbond7.com_keyA_strong.json",
        "node/key_testing@denbond7.com_keyB_default.json",
        "node/key_testing@denbond7.com_keyC_strong.json")
    launchActivity(keysPaths)

    checkKeysTitleAtStart(3)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)
    checkKeysTitle(2, 3, 1)
    typePassword(TestConstants.DEFAULT_PASSWORD)

    Assert.assertTrue(activeActivityRule.getNonNullScenario().state == Lifecycle.State.DESTROYED)
    Assert.assertTrue(activeActivityRule.getNonNullScenario().result.resultCode == Activity.RESULT_OK)
  }

  /**
   * There are three keys (one unique and two identical, the unique key and the identical key have the same
   * pass phrase). Will be used one of the identical keys with a unique pass phrase.
   */
  @Test
  fun testUseThreeKeysFifthCombination() {
    val keysPaths = arrayOf(
        "node/key_testing@denbond7.com_keyB_default.json",
        "node/key_testing@denbond7.com_keyC_default.json",
        "node/key_testing@denbond7.com_keyC_strong.json")
    launchActivity(keysPaths)

    checkKeysTitleAtStart(2)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)
    checkKeysTitle(1, 2, 1)
    checkSkipRemainingBackupsButton()
  }

  /**
   * There are three keys (one unique and two identical, the unique key and the identical key have the same
   * pass phrase). All keys will be imported per one transaction using [TestConstants.DEFAULT_STRONG_PASSWORD].
   */
  @Test
  fun testUseThreeKeysSixthCombination() {
    val keysPaths = arrayOf(
        "node/key_testing@denbond7.com_keyA_strong.json",
        "node/key_testing@denbond7.com_keyC_default.json",
        "node/key_testing@denbond7.com_keyC_strong.json")
    launchActivity(keysPaths)

    checkKeysTitleAtStart(2)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)

    Assert.assertTrue(activeActivityRule.getNonNullScenario().state == Lifecycle.State.DESTROYED)
    Assert.assertTrue(activeActivityRule.getNonNullScenario().result.resultCode == Activity.RESULT_OK)
  }

  /**
   * There are three keys (one unique and two identical, the unique key and the identical key have the same
   * pass phrase). First will be used one key of the identical keys with a unique passphrase, and then the other keys.
   */
  @Test
  fun testUseThreeKeysSeventhCombination() {
    val keysPaths = arrayOf(
        "node/key_testing@denbond7.com_keyB_default.json",
        "node/key_testing@denbond7.com_keyC_default.json",
        "node/key_testing@denbond7.com_keyC_strong.json")
    launchActivity(keysPaths)

    checkKeysTitleAtStart(2)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)
    checkKeysTitle(1, 2, 1)
    typePassword(TestConstants.DEFAULT_PASSWORD)

    Assert.assertTrue(activeActivityRule.getNonNullScenario().state == Lifecycle.State.DESTROYED)
    Assert.assertTrue(activeActivityRule.getNonNullScenario().result.resultCode == Activity.RESULT_OK)
  }

  /**
   * There are four keys (three keys are different and one is the same as one of the other; identical keys have
   * different pass phrases; each of the keys with identical pass phrase are unique). Will be used only
   * two keys with the same pass phrase.
   */
  @Test
  fun testUseFourKeysFirstCombination() {
    val keysPaths = arrayOf(
        "node/key_testing@denbond7.com_keyA_strong.json",
        "node/key_testing@denbond7.com_keyB_default.json",
        "node/key_testing@denbond7.com_keyC_default.json",
        "node/key_testing@denbond7.com_keyC_strong.json")
    launchActivity(keysPaths)

    checkKeysTitleAtStart(3)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)
    checkKeysTitle(2, 3, 1)
    checkSkipRemainingBackupsButton()
  }

  /**
   * There are four keys (three keys are different and one is the same as one of the other; identical keys have
   * different pass phrases; each of the keys with identical pass phrase are unique). Will be used all keys (two
   * keys per one pass phrase typing).
   */
  @Test
  fun testUseFourKeysSecondCombination() {
    val keysPaths = arrayOf(
        "node/key_testing@denbond7.com_keyA_strong.json",
        "node/key_testing@denbond7.com_keyB_default.json",
        "node/key_testing@denbond7.com_keyC_default.json",
        "node/key_testing@denbond7.com_keyC_strong.json")
    launchActivity(keysPaths)

    checkKeysTitleAtStart(3)
    typePassword(TestConstants.DEFAULT_STRONG_PASSWORD)
    checkKeysTitle(2, 3, 1)
    typePassword(TestConstants.DEFAULT_PASSWORD)

    Assert.assertTrue(activeActivityRule.getNonNullScenario().state == Lifecycle.State.DESTROYED)
    Assert.assertTrue(activeActivityRule.getNonNullScenario().result.resultCode == Activity.RESULT_OK)
  }

  private fun launchActivity(keysPaths: Array<String>) {
    activeActivityRule.launch(getStartCheckKeysActivityIntent(keysPaths))
    registerCountingIdlingResource()
    registerNodeIdling()
    registerSyncServiceCountingIdlingResource()
  }

  private fun checkSkipRemainingBackupsButton() {
    onView(withId(R.id.buttonSkipRemainingBackups))
        .perform(scrollTo(), click())

    Assert.assertTrue(activeActivityRule.getNonNullScenario().state == Lifecycle.State.DESTROYED)
    Assert.assertTrue(activeActivityRule.getNonNullScenario().result.resultCode == CheckKeysActivity.RESULT_SKIP_REMAINING_KEYS)
  }

  /**
   * Type a password and click on the "CONTINUE" button.
   *
   * @param password The input password.
   */
  private fun typePassword(password: String) {
    onView(withId(R.id.editTextKeyPassword))
        .perform(scrollTo(), typeText(password), closeSoftKeyboard())
    onView(withId(R.id.buttonPositiveAction))
        .perform(scrollTo(), click())
  }

  private fun checkKeysTitle(quantityOfKeysUsed: Int, totalQuantityOfKeys: Int, quantityOfRemainingKeys: Int) {
    onView(withId(R.id.textViewSubTitle))
        .check(matches(isDisplayed()))
        .check(matches(withText(getTargetContext().resources.getQuantityString(R.plurals.not_recovered_all_keys,
            quantityOfRemainingKeys, quantityOfKeysUsed, totalQuantityOfKeys, quantityOfRemainingKeys))))
  }

  private fun checkKeysTitleAtStart(totalQuantityOfKeys: Int) {
    onView(withId(R.id.textViewSubTitle))
        .check(matches(isDisplayed()))
        .check(matches(withText(getTargetContext().resources
            .getQuantityString(R.plurals.found_backup_of_your_account_key, totalQuantityOfKeys, totalQuantityOfKeys))))
  }

  private fun getStartCheckKeysActivityIntent(keysPaths: Array<String>): Intent {
    return CheckKeysActivity.newIntent(getTargetContext(),
        PrivateKeysManager.getKeysFromAssets(keysPaths),
        KeyDetails.Type.EMAIL,
        getTargetContext().resources.getQuantityString(R.plurals.found_backup_of_your_account_key,
            keysPaths.size, keysPaths.size),
        getTargetContext().getString(R.string.continue_),
        getTargetContext().getString(R.string.use_another_account))
  }
}
