/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.os.Bundle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.util.TestGeneralUtil
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 *         Date: 6/29/21
 *         Time: 3:54 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class RecheckProvidedPassphraseFragmentTest : BaseTest() {
  private val addAccountToDatabaseRule = AddAccountToDatabaseRule()

  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      uri = "flowcrypt://email.flowcrypt.com/settings/security/recheck_passphrase",
      extras = Bundle().apply {
        putInt("popBackStackIdIfSuccess", R.id.securitySettingsFragment)
        putString("title", getResString(R.string.change_pass_phrase))
        putString("passphrase", PERFECT_PASSWORD)
      }
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(addAccountToDatabaseRule)
    .around(AddPrivateKeyToDatabaseRule())
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testEmptyPassphrase() {
    onView(withId(R.id.btConfirmPassphrase))
      .perform(click())
    onView(withText(getResString(R.string.passphrase_must_be_non_empty)))
      .check(matches(isDisplayed()))
    onView(withId(com.google.android.material.R.id.snackbar_action))
      .check(matches(isDisplayed()))
      .perform(click())
  }

  @Test
  fun testShowMsgMismatchOfPassphrase() {
    onView(withId(R.id.eTRepeatedPassphrase))
      .check(matches(isDisplayed()))
      .perform(replaceText("some text"), closeSoftKeyboard())
    onView(withId(R.id.btConfirmPassphrase))
      .perform(click())
    onView(withText(getResString(R.string.pass_phrases_do_not_match)))
      .check(matches(isDisplayed()))
    onView(withId(com.google.android.material.R.id.snackbar_action))
      .check(matches(isDisplayed())).perform(click())
  }

  companion object {
    internal const val GREAT_PASSWORD = "weak, poor, great, good"
    internal const val PERFECT_PASSWORD = "unconventional blueberry unlike any other"
  }
}
