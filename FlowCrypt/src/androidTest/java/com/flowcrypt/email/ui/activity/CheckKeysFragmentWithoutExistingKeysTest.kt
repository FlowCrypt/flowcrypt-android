/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.os.Bundle
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 23.02.2018
 * Time: 11:45
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class CheckKeysFragmentWithoutExistingKeysTest : BaseTest() {

  private val privateKeys = PrivateKeysManager.getKeysFromAssets(
    arrayOf("pgp/default@flowcrypt.test_fisrtKey_prv_default.asc"),
    true
  )

  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      uri = "flowcrypt://email.flowcrypt.com/check_private_keys",
      extras = Bundle().apply {
        putParcelableArray("privateKeys", privateKeys.toTypedArray())
        putParcelable("sourceType", KeyImportDetails.SourceType.EMAIL)
        putString("positiveBtnTitle", getTargetContext().getString(R.string.continue_))
        putString("negativeBtnTitle", getTargetContext().getString(R.string.use_another_account))
        putInt("initSubTitlePlurals", R.plurals.found_backup_of_your_account_key)
      }
    ))

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testShowMsgEmptyWarning() {
    Espresso.closeSoftKeyboard()
    onView(withId(R.id.buttonPositiveAction))
      .perform(scrollTo(), click())
    checkIsSnackbarDisplayedAndClick(getResString(R.string.passphrase_must_be_non_empty))
  }

  @Test
  fun testUseIncorrectPassPhrase() {
    onView(withId(R.id.editTextKeyPassword))
      .perform(scrollTo(), typeText("some pass phrase"), closeSoftKeyboard())
    onView(withId(R.id.buttonPositiveAction))
      .perform(scrollTo(), click())
    checkIsSnackbarDisplayedAndClick(getResString(R.string.password_is_incorrect))
  }

  @Test
  fun testUseCorrectPassPhrase() {
    onView(withId(R.id.editTextKeyPassword))
      .perform(scrollTo(), typeText("android"), closeSoftKeyboard())
    onView(withId(R.id.buttonPositiveAction))
      .perform(scrollTo(), click())

    onView(ViewMatchers.withText(R.string.enter_your_pass_phrase))
      .check(ViewAssertions.doesNotExist())
  }

  @Test
  fun testCheckClickButtonNegative() {
    Espresso.closeSoftKeyboard()
    onView(withId(R.id.buttonNegativeAction))
      .perform(scrollTo(), click())

    onView(ViewMatchers.withText(R.string.enter_your_pass_phrase))
      .check(ViewAssertions.doesNotExist())
  }
}
