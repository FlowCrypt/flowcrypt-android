/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.app.Instrumentation;

import com.flowcrypt.email.R;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.rules.ClearAppSettingsRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.isInternal;
import static androidx.test.espresso.intent.matcher.IntentMatchers.toPackage;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

/**
 * A test for {@link SplashActivity}
 *
 * @author Denis Bondarenko
 * Date: 13.02.2018
 * Time: 11:12
 * E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class SplashActivityTest extends BaseTest {
  private IntentsTestRule intentsTestRule = new IntentsTestRule<>(SplashActivity.class);

  @Rule
  public TestRule ruleChain = RuleChain
      .outerRule(new ClearAppSettingsRule())
      .around(intentsTestRule);

  @Override
  public ActivityTestRule getActivityTestRule() {
    return intentsTestRule;
  }

  @Before
  public void stubAllExternalIntents() {
    // All external Intents will be blocked.
    intending(not(isInternal())).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
  }

  @Test
  public void testUseOtherEmailProviders() {
    onView(withId(R.id.buttonOtherEmailProvider)).check(matches(isDisplayed())).perform(click());
    onView(allOf(withText(R.string.adding_new_account), withParent(withId(R.id.toolbar)))).check(matches
        (isDisplayed()));
  }

  @Test
  public void testUseGmail() {
    onView(withId(R.id.buttonSignInWithGmail)).check(matches(isDisplayed())).perform(click());
    //check that the Google Sign-in screen displayed
    intended(toPackage("com.google.android.gms"));
  }

  @Test
  public void testShowPrivacyScreen() {
    onView(withId(R.id.buttonPrivacy)).check(matches(isDisplayed())).perform(click());
    onView(allOf(withText(R.string.privacy), withParent(withId(R.id.toolbar)))).check(matches(isDisplayed()));
  }

  @Test
  public void testShowTermsScreen() {
    onView(withId(R.id.buttonTerms)).check(matches(isDisplayed())).perform(click());
    onView(allOf(withText(R.string.terms), withParent(withId(R.id.toolbar)))).check(matches(isDisplayed()));
  }

  @Test
  public void testShowSecurityScreen() {
    onView(withId(R.id.buttonSecurity)).check(matches(isDisplayed())).perform(click());
    onView(allOf(withText(R.string.security), withParent(withId(R.id.toolbar)))).check(matches(isDisplayed()));
  }
}
