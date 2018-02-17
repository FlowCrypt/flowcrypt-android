/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.app.Instrumentation;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import com.flowcrypt.email.R;
import com.flowcrypt.email.rules.ClearAppSettingsRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.isInternal;
import static android.support.test.espresso.intent.matcher.IntentMatchers.toPackage;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

/**
 * A test for {@link SplashActivity}
 *
 * @author Denis Bondarenko
 *         Date: 13.02.2018
 *         Time: 11:12
 *         E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class SplashActivityTest {

    @Rule
    public TestRule ruleChain = RuleChain
            .outerRule(new ClearAppSettingsRule())
            .around(new IntentsTestRule<>(SplashActivity.class));

    @Before
    public void stubAllExternalIntents() {
        // All external Intents will be blocked.
        intending(not(isInternal())).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
    }

    @Test
    public void testUseOtherEmailProviders() {
        onView(withId(R.id.buttonOtherEmailProvider)).check(matches(isDisplayed())).perform(click());
        onView(withText(R.string.adding_new_account)).check(matches(isDisplayed()));
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
        onView(withText(R.string.privacy)).check(matches(isDisplayed()));
    }

    @Test
    public void testShowTermsScreen() {
        onView(withId(R.id.buttonTerms)).check(matches(isDisplayed())).perform(click());
        onView(withText(R.string.terms)).check(matches(isDisplayed()));
    }

    @Test
    public void testShowSecurityScreen() {
        onView(withId(R.id.buttonSecurity)).check(matches(isDisplayed())).perform(click());
        onView(withText(R.string.security)).check(matches(isDisplayed()));
    }
}