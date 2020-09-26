/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.CICandidateAnnotation
import com.flowcrypt.email.DoesNotNeedMailserver
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.util.PrivateKeysManager
import org.hamcrest.Matchers.not
import org.junit.Assert
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
class CheckKeysActivityWithoutExistingKeysTest : BaseTest() {

  private val privateKeys = PrivateKeysManager.getKeysFromAssets(arrayOf("node/default@denbond7.com_fisrtKey_prv_default.json"))

  override val activityScenarioRule = activityScenarioRule<CheckKeysActivity>(
      CheckKeysActivity.newIntent(getTargetContext(),
          privateKeys,
          KeyDetails.Type.EMAIL,
          getTargetContext().resources.getQuantityString(R.plurals.found_backup_of_your_account_key,
              privateKeys.size, privateKeys.size),
          getTargetContext().getString(R.string.continue_),
          getTargetContext().getString(R.string.use_another_account))
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(RetryRule())
      .around(activityScenarioRule)

  @Test
  @DoesNotNeedMailserver
  fun testShowMsgEmptyWarning() {
    Espresso.closeSoftKeyboard()
    onView(withId(R.id.buttonPositiveAction))
        .perform(scrollTo(), click())
    checkIsSnackbarDisplayedAndClick(getResString(R.string.passphrase_must_be_non_empty))
  }

  @Test
  @DoesNotNeedMailserver
  fun testUseIncorrectPassPhrase() {
    onView(withId(R.id.editTextKeyPassword))
        .perform(scrollTo(), typeText("some pass phrase"), closeSoftKeyboard())
    onView(withId(R.id.buttonPositiveAction))
        .perform(scrollTo(), click())
    checkIsSnackbarDisplayedAndClick(getResString(R.string.password_is_incorrect))
  }

  @Test
  @CICandidateAnnotation
  fun testUseCorrectPassPhrase() {
    onView(withId(R.id.editTextKeyPassword))
        .perform(scrollTo(), typeText("android"), closeSoftKeyboard())
    onView(withId(R.id.buttonPositiveAction))
        .perform(scrollTo(), click())

    Assert.assertTrue(activityScenarioRule.scenario.state == Lifecycle.State.DESTROYED)
    Assert.assertTrue(activityScenarioRule.scenario.result.resultCode == Activity.RESULT_OK)
  }

  @Test
  @DoesNotNeedMailserver
  fun testCheckClickButtonNeutral() {
    Espresso.closeSoftKeyboard()
    onView(withId(R.id.buttonUseExistingKeys))
        .check(matches(not(isDisplayed())))
  }

  @Test
  @DoesNotNeedMailserver
  fun testCheckClickButtonNegative() {
    Espresso.closeSoftKeyboard()
    onView(withId(R.id.buttonNegativeAction))
        .perform(scrollTo(), click())

    Assert.assertTrue(activityScenarioRule.scenario.state == Lifecycle.State.DESTROYED)
    Assert.assertTrue(activityScenarioRule.scenario.result.resultCode == CheckKeysActivity.RESULT_NEGATIVE)
  }
}
