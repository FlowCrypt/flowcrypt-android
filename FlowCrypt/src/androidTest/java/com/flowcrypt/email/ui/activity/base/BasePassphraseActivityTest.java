/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base;

import android.app.Activity;

import com.flowcrypt.email.R;
import com.flowcrypt.email.base.BaseTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.startsWith;

/**
 * @author Denis Bondarenko
 * Date: 3/13/19
 * Time: 3:44 PM
 * E-mail: DenBond7@gmail.com
 */
public abstract class BasePassphraseActivityTest extends BaseTest {
  protected static final String WEAK_PASSWORD = "weak";
  protected static final String POOR_PASSWORD = "weak, perfect, great";
  protected static final String REASONABLE_PASSWORD = "weak, poor, reasonable";
  protected static final String GOOD_PASSWORD = "weak, poor, good,";
  protected static final String GREAT_PASSWORD = "weak, poor, great, good";
  protected static final String PERFECT_PASSWORD = "unconventional blueberry unlike any other";

  @Before
  public void init() {
    ActivityTestRule activityTestRule = getActivityTestRule();
    if (activityTestRule != null) {
      Activity activity = activityTestRule.getActivity();
      if (activity instanceof BasePassPhraseManagerActivity) {
        IdlingRegistry.getInstance().register(
            ((BasePassPhraseManagerActivity) activity).getIdlingForPassphraseChecking());
      }
    }
  }

  @After
  public void unregisterDecryptionIdling() {
    ActivityTestRule activityTestRule = getActivityTestRule();
    if (activityTestRule != null) {
      Activity activity = activityTestRule.getActivity();
      if (activity instanceof BasePassPhraseManagerActivity) {
        IdlingRegistry.getInstance().unregister(
            ((BasePassPhraseManagerActivity) activity).getIdlingForPassphraseChecking());
      }
    }
  }

  @Test
  public void testShowDialogWithPasswordRecommendation() {
    onView(withId(R.id.imageButtonShowPasswordHint)).check(matches(isDisplayed())).perform(click());
    onView(withId(R.id.webView)).check(matches(isDisplayed()));
    onView(withId(R.id.buttonOk)).check(matches(isDisplayed())).perform(click());
    onView(withId(R.id.textViewFirstPasswordCheckTitle)).check(matches(isDisplayed()));
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
    onView(withId(R.id.editTextKeyPassword)).perform(scrollTo(), replaceText(PERFECT_PASSWORD), closeSoftKeyboard());
    onView(withId(R.id.buttonSetPassPhrase)).check(matches(isDisplayed())).perform(click());
    onView(withId(R.id.buttonConfirmPassPhrases)).check(matches(isDisplayed()));
  }

  @Test
  public void testShowMsgMismatchOfPassPhrase() {
    testShowRepeatingPassPhraseScreen();

    onView(withId(R.id.editTextKeyPasswordSecond)).check(matches(isDisplayed()))
        .perform(scrollTo(), replaceText("some text"), closeSoftKeyboard());
    onView(withId(R.id.buttonConfirmPassPhrases)).perform(click());

    onView(withText(InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string
        .pass_phrases_do_not_match)))
        .check(matches(isDisplayed()));
    onView(withId(com.google.android.material.R.id.snackbar_action)).check(matches(isDisplayed())).perform(click());
  }

  @Test
  public void testGoToUseAnotherPassPhrase() {
    testShowRepeatingPassPhraseScreen();

    onView(withId(R.id.buttonUseAnotherPassPhrase)).check(matches(isDisplayed())).perform(click());
    onView(withId(R.id.imageButtonShowPasswordHint)).check(matches(isDisplayed()));
  }

  @Test
  public void testCheckEraseOfRepeatingPassPhrase() {
    testShowRepeatingPassPhraseScreen();

    onView(withId(R.id.editTextKeyPasswordSecond)).check(
        matches(isDisplayed())).perform(replaceText(PERFECT_PASSWORD), closeSoftKeyboard());
    onView(withId(R.id.buttonUseAnotherPassPhrase)).check(matches(isDisplayed())).perform(click());
    onView(withId(R.id.buttonSetPassPhrase)).check(matches(isDisplayed()));

    testShowRepeatingPassPhraseScreen();
    onView(withId(R.id.editTextKeyPasswordSecond)).check(
        matches(isDisplayed())).check(matches(withText(isEmptyString())));
  }

  @Test
  public void testChangingQualityOfPassPhrase() {
    String[] passPhrases = {WEAK_PASSWORD, POOR_PASSWORD, REASONABLE_PASSWORD,
        GOOD_PASSWORD, GREAT_PASSWORD, PERFECT_PASSWORD};

    String[] degreeOfReliabilityOfPassPhrase = {
        InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string.password_quality_weak),
        InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string.password_quality_poor),
        InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string.password_quality_reasonable),
        InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string.password_quality_good),
        InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string.password_quality_great),
        InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string.password_quality_perfect),
    };

    for (int i = 0; i < passPhrases.length; i++) {
      onView(withId(R.id.editTextKeyPassword)).check(matches(isDisplayed())).perform(replaceText(passPhrases[i]));
      onView(withId(R.id.textViewPasswordQualityInfo)).check(matches(withText(
          startsWith(degreeOfReliabilityOfPassPhrase[i].toUpperCase()))));
      onView(withId(R.id.editTextKeyPassword)).check(matches(isDisplayed())).perform(clearText());
    }
  }

  @Test
  public void testShowDialogAboutBadPassPhrase() {
    String[] badPassPhrases = {WEAK_PASSWORD, POOR_PASSWORD};

    for (String passPhrase : badPassPhrases) {
      onView(withId(R.id.editTextKeyPassword)).check(matches(isDisplayed())).perform(replaceText(passPhrase),
          closeSoftKeyboard());
      onView(withId(R.id.buttonSetPassPhrase)).check(matches(isDisplayed())).perform(click());

      onView(withText(InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string
          .select_stronger_pass_phrase)))
          .check(matches(isDisplayed()));
      onView(withId(android.R.id.button1)).check(matches(isDisplayed())).perform(click());
      onView(withId(R.id.editTextKeyPassword)).check(matches(isDisplayed())).perform(clearText());
    }
  }

  protected void checkIsNonEmptyHintShown() {
    onView(withText(InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string
        .passphrase_must_be_non_empty)))
        .check(matches(isDisplayed()));
    onView(withId(com.google.android.material.R.id.snackbar_action)).check(matches(isDisplayed()))
        .perform(click());
  }
}
