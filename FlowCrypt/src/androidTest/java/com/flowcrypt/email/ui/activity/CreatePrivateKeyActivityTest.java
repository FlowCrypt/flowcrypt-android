/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.text.Html;

import com.flowcrypt.email.R;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.util.AccountDaoManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.equalTo;

/**
 * A test for {@link CreatePrivateKeyActivity}
 *
 * @author Denis Bondarenko
 *         Date: 15.01.2018
 *         Time: 09:21
 *         E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class CreatePrivateKeyActivityTest {

    private static final String PERFECT_PASSWORD = "unconventional blueberry unlike any other";

    @Rule
    public TestRule ruleChain = RuleChain
            .outerRule(new ClearAppSettingsRule())
            .around(new ActivityTestRule<CreatePrivateKeyActivity>(CreatePrivateKeyActivity.class) {
                @Override
                protected Intent getActivityIntent() {
                    Context targetContext = InstrumentationRegistry.getTargetContext();
                    Intent result = new Intent(targetContext, CreatePrivateKeyActivity.class);
                    result.putExtra(CreatePrivateKeyActivity.KEY_EXTRA_ACCOUNT_DAO,
                            AccountDaoManager.getDefaultAccountDao());
                    return result;
                }
            });

    @Test
    public void testShowDialogWithPasswordRecommendation() {
        onView(withId(R.id.imageButtonShowPasswordHint)).check(matches(isDisplayed())).perform(click());

        String messageFromHint = Html.fromHtml(InstrumentationRegistry.getTargetContext()
                .getString(R.string.password_recommendation)).toString();

        onView(withText(equalTo(messageFromHint))).check(matches(isDisplayed()));
        onView(withId(android.R.id.button1)).check(matches(isDisplayed())).perform(click());

        onView(withId(R.id.textViewSetupFlowCrypt)).check(matches(isDisplayed()));
    }

    @Test
    public void testEmptyFirstPassPhrase() {
        closeSoftKeyboard();
        onView(withId(R.id.buttonSetPassPhrase)).check(matches(isDisplayed())).perform(click());

        checkIsNonEmptyHintShown();
    }

    @Test
    public void testEmptySecondPassPhrase() {
        testShowRepeatingPassPhraseScreen();
        onView(withId(R.id.buttonConfirmPassPhrases)).perform(click());

        checkIsNonEmptyHintShown();
    }

    @Test
    public void testShowRepeatingPassPhraseScreen() {
        onView(withId(R.id.editTextKeyPassword)).perform(scrollTo(), typeText(PERFECT_PASSWORD), closeSoftKeyboard());
        onView(withId(R.id.buttonSetPassPhrase)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.buttonConfirmPassPhrases)).check(matches(isDisplayed()));
    }

    @Test
    public void testShowMessageMismatchOfPassPhrase() {
        testShowRepeatingPassPhraseScreen();

        onView(withId(R.id.editTextKeyPasswordSecond)).check(matches(isDisplayed()))
                .perform(scrollTo(), typeText("some text"), closeSoftKeyboard());
        onView(withId(R.id.buttonConfirmPassPhrases)).perform(click());

        onView(withText(InstrumentationRegistry.getTargetContext().getString(R.string.pass_phrases_do_not_match)))
                .check(matches(isDisplayed()));
        onView(withId(android.support.design.R.id.snackbar_action)).check(matches(isDisplayed())).perform(click());
    }

    @Test
    public void testGoToUseAnotherPassPhrase() {
        testShowRepeatingPassPhraseScreen();

        onView(withId(R.id.buttonUseAnotherPassPhrase)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.imageButtonShowPasswordHint)).check(matches(isDisplayed()));
    }

    @Test
    public void testUseCorrectPassPhrase() throws Exception {
        onView(withId(R.id.editTextKeyPassword)).check(matches(isDisplayed())).perform(typeText(PERFECT_PASSWORD),
                closeSoftKeyboard());
        onView(withId(R.id.buttonSetPassPhrase)).check(matches(isDisplayed())).perform(click());

        onView(withId(R.id.editTextKeyPasswordSecond)).check(matches(isDisplayed())).perform(typeText(PERFECT_PASSWORD),
                closeSoftKeyboard());
        onView(withId(R.id.buttonConfirmPassPhrases)).check(matches(isDisplayed())).perform(click());

        onView(withId(R.id.buttonContinue)).check(matches(isDisplayed()));
    }

    private void checkIsNonEmptyHintShown() {
        onView(withText(InstrumentationRegistry.getTargetContext().getString(R.string.passphrase_must_be_non_empty)))
                .check(matches(isDisplayed()));
        onView(withId(android.support.design.R.id.snackbar_action)).check(matches(isDisplayed()))
                .perform(click());
    }
}