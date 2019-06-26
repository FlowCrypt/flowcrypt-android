/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.isInternal
import androidx.test.espresso.intent.matcher.IntentMatchers.toPackage
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.rules.ClearAppSettingsRule
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.Before
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
@LargeTest
@RunWith(AndroidJUnit4::class)
class SignInActivityTest : BaseTest() {
  override val activityTestRule: ActivityTestRule<*>? = IntentsTestRule(SignInActivity::class.java)

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(activityTestRule)

  @Before
  fun stubAllExternalIntents() {
    // All external Intents will be blocked.
    intending(not<Intent>(isInternal())).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
  }

  @Test
  fun testUseOtherEmailProviders() {
    onView(withId(R.id.buttonOtherEmailProvider))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(allOf<View>(withText(R.string.adding_new_account), withParent(withId(R.id.toolbar))))
        .check(matches(isDisplayed()))
  }

  @Test
  fun testUseGmail() {
    onView(withId(R.id.buttonSignInWithGmail))
        .check(matches(isDisplayed()))
        .perform(click())
    //check that the Google Sign-in screen displayed
    intended(toPackage("com.google.android.gms"))
  }

  @Test
  fun testShowSecurityScreen() {
    onView(withId(R.id.buttonSecurity))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(allOf<View>(withText(R.string.security), withParent(withId(R.id.toolbar))))
        .check(matches(isDisplayed()))
  }
}
