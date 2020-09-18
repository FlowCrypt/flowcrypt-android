/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.ActivityResultMatchers.hasResultCode
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.flowcrypt.email.DoesNotNeedMailserver
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.ui.activity.base.BaseCheckKeysActivityTest
import com.flowcrypt.email.util.PrivateKeysManager
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 02.03.2018
 * Time: 16:17
 * E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@DoesNotNeedMailserver
@Ignore("Failed on CI")
class CheckKeysActivityWithExistingKeysTest : BaseCheckKeysActivityTest() {
  override val activityTestRule: ActivityTestRule<*>? =
      object : ActivityTestRule<CheckKeysActivity>(CheckKeysActivity::class.java) {
        override fun getActivityIntent(): Intent {
          val privateKeys =
              PrivateKeysManager.getKeysFromAssets(arrayOf("node/default@denbond7.com_fisrtKey_prv_default.json"))
          return CheckKeysActivity.newIntent(getTargetContext(),
              privateKeys = privateKeys,
              type = KeyDetails.Type.EMAIL,
              subTitle = getTargetContext().resources.getQuantityString(R.plurals.found_backup_of_your_account_key,
                  privateKeys.size, privateKeys.size),
              positiveBtnTitle = getTargetContext().getString(R.string.continue_),
              negativeBtnTitle = getTargetContext().getString(R.string.use_another_account))
        }
      }

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(AddPrivateKeyToDatabaseRule("node/not_attester_user@denbond7.com_prv_default.json",
          TestConstants.DEFAULT_PASSWORD, KeyDetails.Type.EMAIL))
      .around(activityTestRule)

  @Test
  @Ignore("Failed on CI")
  fun testShowMsgEmptyPassPhrase() {
    Espresso.closeSoftKeyboard()
    onView(withId(R.id.buttonPositiveAction))
        .check(matches(isDisplayed()))
        .perform(click())
    checkIsSnackbarDisplayedAndClick(getResString(R.string.passphrase_must_be_non_empty))
  }

  @Test
  fun testUseIncorrectPassPhrase() {
    onView(withId(R.id.editTextKeyPassword))
        .check(matches(isDisplayed()))
        .perform(typeText("some pass phrase"), closeSoftKeyboard())
    onView(withId(R.id.buttonPositiveAction))
        .check(matches(isDisplayed()))
        .perform(click())
    checkIsSnackbarDisplayedAndClick(getResString(R.string.password_is_incorrect))
  }

  @Test
  fun testUseCorrectPassPhrase() {
    onView(withId(R.id.editTextKeyPassword))
        .check(matches(isDisplayed()))
        .perform(typeText(TestConstants.DEFAULT_PASSWORD), closeSoftKeyboard())
    onView(withId(R.id.buttonPositiveAction))
        .check(matches(isDisplayed()))
        .perform(click())
    assertThat<Instrumentation.ActivityResult>(activityTestRule?.activityResult, hasResultCode(Activity.RESULT_OK))
  }

  @Test
  fun testCheckClickButtonNeutral() {
    Espresso.closeSoftKeyboard()
    onView(withId(R.id.buttonUseExistingKeys))
        .check(matches(isDisplayed()))
        .perform(scrollTo(), click())
    assertThat<Instrumentation.ActivityResult>(activityTestRule?.activityResult,
        hasResultCode(CheckKeysActivity.RESULT_USE_EXISTING_KEYS))
  }

  @Test
  fun testCheckClickButtonNegative() {
    Espresso.closeSoftKeyboard()
    onView(withId(R.id.buttonNegativeAction))
        .check(matches(isDisplayed()))
        .perform(scrollTo(), click())
    assertThat<Instrumentation.ActivityResult>(activityTestRule?.activityResult,
        hasResultCode(CheckKeysActivity.RESULT_NEGATIVE))
  }
}
