/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.toPackage
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.DoesNotNeedMailserver
import com.flowcrypt.email.R
import com.flowcrypt.email.ReadyForCIAnnotation
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * A test for [SignInActivity]
 *
 * @author Denis Bondarenko
 * Date: 13.02.2018
 * Time: 11:12
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class SignInActivityTest : BaseTest() {
  override val useIntents: Boolean = true
  override val activityScenarioRule = activityScenarioRule<SignInActivity>()

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(RetryRule.DEFAULT)
      .around(activityScenarioRule)
      .around(ScreenshotTestRule())

  @Test
  @DoesNotNeedMailserver
  @ReadyForCIAnnotation
  fun testUseOtherEmailProviders() {
    onView(withId(R.id.buttonOtherEmailProvider))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(withText(R.string.or_use_your_credentials_to_connect))
        .check(matches(isDisplayed()))
  }

  @Test
  @DoesNotNeedMailserver
  @ReadyForCIAnnotation
  fun testUseGmail() {
    onView(withId(R.id.buttonSignInWithGmail))
        .check(matches(isDisplayed()))
        .perform(click())
    //check that the Google Sign-in screen displayed
    intended(toPackage("com.google.android.gms"))
  }

  @Test
  @DoesNotNeedMailserver
  @ReadyForCIAnnotation
  fun testShowSecurityScreen() {
    onView(withId(R.id.buttonSecurity))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(allOf(withText(R.string.security), withParent(withId(R.id.toolbar))))
        .check(matches(isDisplayed()))
  }
}
