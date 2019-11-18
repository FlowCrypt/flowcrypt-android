/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.view.View
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
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.ui.activity.base.BaseCheckKeysActivityTest
import com.flowcrypt.email.util.PrivateKeysManager
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.not
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
@LargeTest
@RunWith(AndroidJUnit4::class)
@DoesNotNeedMailserver
class CheckKeysActivityWithoutExistingKeysTest : BaseCheckKeysActivityTest() {
  override val activityTestRule: ActivityTestRule<*>? =
      object : ActivityTestRule<CheckKeysActivity>(CheckKeysActivity::class.java) {
        override fun getActivityIntent(): Intent {
          val privateKeys = PrivateKeysManager.getKeysFromAssets(
              arrayOf("node/default@denbond7.com_fisrtKey_prv_default.json"))
          return CheckKeysActivity.newIntent(getTargetContext(),
              privateKeys,
              KeyDetails.Type.EMAIL,
              getTargetContext().resources.getQuantityString(R.plurals.found_backup_of_your_account_key,
                  privateKeys.size, privateKeys.size),
              getTargetContext().getString(R.string.continue_),
              getTargetContext().getString(R.string.use_another_account))
        }
      }

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(activityTestRule)

  @Test
  fun testShowMsgEmptyWarning() {
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
        .perform(typeText("android"), closeSoftKeyboard())
    onView(withId(R.id.buttonPositiveAction))
        .check(matches(isDisplayed()))
        .perform(click())
    assertThat<Instrumentation.ActivityResult>(activityTestRule?.activityResult, hasResultCode(Activity.RESULT_OK))
  }

  @Test
  fun testCheckClickButtonNeutral() {
    Espresso.closeSoftKeyboard()
    onView(withId(R.id.buttonUseExistingKeys))
        .check(matches(not<View>(isDisplayed())))
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
