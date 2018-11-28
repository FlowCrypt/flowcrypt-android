/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.content.Context;
import android.content.Intent;

import com.flowcrypt.email.R;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.util.AccountDaoManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.startsWith;

/**
 * A test for {@link CreatePrivateKeyActivity}
 *
 * @author Denis Bondarenko
 * Date: 15.01.2018
 * Time: 09:21
 * E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class CreatePrivateKeyActivityTest {

  private static final String WEAK_PASSWORD = "weak";
  private static final String POOR_PASSWORD = "weak, perfect, great";
  private static final String REASONABLE_PASSWORD = "weak, poor, reasonable";
  private static final String GOOD_PASSWORD = "weak, poor, good,";
  private static final String GREAT_PASSWORD = "weak, poor, great, good";
  private static final String PERFECT_PASSWORD = "unconventional blueberry unlike any other";

  @Rule
  public TestRule ruleChain = RuleChain
      .outerRule(new ClearAppSettingsRule())
      .around(new ActivityTestRule<CreatePrivateKeyActivity>(CreatePrivateKeyActivity.class) {
        @Override
        protected Intent getActivityIntent() {
          Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
          Intent result = new Intent(targetContext, CreatePrivateKeyActivity.class);
          result.putExtra(CreatePrivateKeyActivity.KEY_EXTRA_ACCOUNT_DAO,
              AccountDaoManager.getDefaultAccountDao());
          return result;
        }
      });

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
  public void testUseCorrectPassPhrase() {
    onView(withId(R.id.editTextKeyPassword)).check(matches(isDisplayed())).perform(typeText(PERFECT_PASSWORD),
        closeSoftKeyboard());
    onView(withId(R.id.buttonSetPassPhrase)).check(matches(isDisplayed())).perform(click());

    onView(withId(R.id.editTextKeyPasswordSecond)).check(matches(isDisplayed())).perform(typeText(PERFECT_PASSWORD),
        closeSoftKeyboard());
    onView(withId(R.id.buttonConfirmPassPhrases)).check(matches(isDisplayed())).perform(click());

    onView(withId(R.id.buttonSuccess)).check(matches(isDisplayed()));
  }

  @Test
  public void testCheckEraseOfRepeatingPassPhrase() {
    testShowRepeatingPassPhraseScreen();

    onView(withId(R.id.editTextKeyPasswordSecond)).check(
        matches(isDisplayed())).perform(typeText(PERFECT_PASSWORD), closeSoftKeyboard());
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
      onView(withId(R.id.editTextKeyPassword)).check(matches(isDisplayed())).perform(typeText
          (passPhrases[i]));
      onView(withId(R.id.textViewPasswordQualityInfo)).check(matches(withText(startsWith
          (degreeOfReliabilityOfPassPhrase[i].toUpperCase()))));
      onView(withId(R.id.editTextKeyPassword)).check(matches(isDisplayed())).perform(clearText());
    }
  }

  @Test
  public void testShowDialogAboutBadPassPhrase() {
    String[] badPassPhrases = {WEAK_PASSWORD, POOR_PASSWORD};

    for (String passPhrase : badPassPhrases) {
      onView(withId(R.id.editTextKeyPassword)).check(matches(isDisplayed())).perform(typeText(passPhrase),
          closeSoftKeyboard());
      onView(withId(R.id.buttonSetPassPhrase)).check(matches(isDisplayed())).perform(click());

      onView(withText(InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string
          .select_stronger_pass_phrase)))
          .check(matches(isDisplayed()));
      onView(withId(android.R.id.button1)).check(matches(isDisplayed())).perform(click());
      onView(withId(R.id.editTextKeyPassword)).check(matches(isDisplayed())).perform(clearText());
    }
  }

  private void checkIsNonEmptyHintShown() {
    onView(withText(InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string
        .passphrase_must_be_non_empty)))
        .check(matches(isDisplayed()));
    onView(withId(com.google.android.material.R.id.snackbar_action)).check(matches(isDisplayed()))
        .perform(click());
  }
}
