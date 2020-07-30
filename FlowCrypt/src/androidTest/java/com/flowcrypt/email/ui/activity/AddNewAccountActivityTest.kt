/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.matcher.IntentMatchers.toPackage
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.flowcrypt.email.DoesNotNeedMailserver
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.StubAllExternalIntentsRule
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 23.02.2018
 * Time: 10:30
 * E-mail: DenBond7@gmail.com
 */
@DoesNotNeedMailserver
@LargeTest
@Ignore("Fix me after 1.0.9")
@RunWith(AndroidJUnit4::class)
class AddNewAccountActivityTest : BaseTest() {
  override val activityTestRule: ActivityTestRule<*>? = IntentsTestRule(SignInActivity::class.java)

  @get:Rule
  val ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(AddAccountToDatabaseRule())
      .around(activityTestRule)
      .around(StubAllExternalIntentsRule())

  @Test
  fun testUseOtherEmailProviders() {
    onView(withId(R.id.buttonOtherEmailProvider))
        .check(matches(isDisplayed()))

    Thread.sleep(1000)

    onView(withId(R.id.buttonOtherEmailProvider)).perform(click())
    intended(IntentMatchers.hasComponent(SignInActivity::class.java.name))
  }

  @Test
  fun testUseGmail() {
    onView(withId(R.id.buttonSignInWithGmail))
        .check(matches(isDisplayed()))
        .perform(click())
    //check that the Google Sign-in screen is displayed
    intended(toPackage("com.google.android.gms"))
  }
}
