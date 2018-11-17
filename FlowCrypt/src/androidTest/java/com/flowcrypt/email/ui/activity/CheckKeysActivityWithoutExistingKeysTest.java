/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;

import com.flowcrypt.email.R;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.util.TestGeneralUtil;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;

import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.ActivityResultMatchers.hasResultCode;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

/**
 * @author Denis Bondarenko
 * Date: 23.02.2018
 * Time: 11:45
 * E-mail: DenBond7@gmail.com
 */

@LargeTest
@RunWith(AndroidJUnit4.class)
public class CheckKeysActivityWithoutExistingKeysTest extends BaseTest {
  private ActivityTestRule activityTestRule = new ActivityTestRule<CheckKeysActivity>(CheckKeysActivity.class) {
    @Override
    protected Intent getActivityIntent() {
      Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
      Intent result = new Intent(targetContext, CheckKeysActivity.class);
      ArrayList<KeyDetails> privateKeys = new ArrayList<>();
      try {
        KeyDetails keyDetails = new KeyDetails(null, TestGeneralUtil.readFileFromAssetsAsString
            (InstrumentationRegistry.getInstrumentation().getContext(), "pgp/default@denbond7.com_sec.asc"),
            KeyDetails.Type.EMAIL, true, null);
        privateKeys.add(keyDetails);
      } catch (IOException e) {
        e.printStackTrace();
      }
      result.putExtra(CheckKeysActivity.KEY_EXTRA_PRIVATE_KEYS, privateKeys);
      result.putExtra(CheckKeysActivity.KEY_EXTRA_SUB_TITLE,
          targetContext.getResources().getQuantityString(R.plurals.found_backup_of_your_account_key, 1, 1));
      result.putExtra(CheckKeysActivity.KEY_EXTRA_POSITIVE_BUTTON_TITLE,
          targetContext.getString(R.string.continue_));
      result.putExtra(CheckKeysActivity.KEY_EXTRA_NEUTRAL_BUTTON_TITLE, (Parcelable) null);
      result.putExtra(CheckKeysActivity.KEY_EXTRA_NEGATIVE_BUTTON_TITLE,
          targetContext.getString(R.string.use_another_account));
      return result;
    }
  };

  @Rule
  public TestRule ruleChain = RuleChain
      .outerRule(new ClearAppSettingsRule())
      .around(activityTestRule);

  @Test
  public void testShowMessageEmptyWarning() {
    Espresso.closeSoftKeyboard();
    onView(withId(R.id.buttonPositiveAction)).check(matches(isDisplayed())).perform(click());
    checkIsSnackbarDisplayed(InstrumentationRegistry.getInstrumentation().getTargetContext()
        .getString(R.string.passphrase_must_be_non_empty));
  }

  @Test
  public void testUseIncorrectPassPhrase() {
    onView(withId(R.id.editTextKeyPassword)).check(matches(isDisplayed()))
        .perform(typeText("some pass phrase"), closeSoftKeyboard());
    onView(withId(R.id.buttonPositiveAction)).check(matches(isDisplayed())).perform(click());
    checkIsSnackbarDisplayed(InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string
        .password_is_incorrect));
  }

  @Test
  public void testUseCorrectPassPhrase() throws Exception {
    onView(withId(R.id.editTextKeyPassword)).check(matches(isDisplayed()))
        .perform(typeText("android"), closeSoftKeyboard());
    onView(withId(R.id.buttonPositiveAction)).check(matches(isDisplayed())).perform(click());
    assertThat(activityTestRule.getActivityResult(), hasResultCode(Activity.RESULT_OK));
  }

  @Test
  public void testCheckClickButtonNeutral() throws Exception {
    Espresso.closeSoftKeyboard();
    onView(withId(R.id.buttonNeutralAction)).check(matches(not(isDisplayed())));
  }

  @Test
  public void testCheckClickButtonNegative() throws Exception {
    Espresso.closeSoftKeyboard();
    onView(withId(R.id.buttonNegativeAction)).check(matches(isDisplayed())).perform(scrollTo(), click());
    assertThat(activityTestRule.getActivityResult(), hasResultCode(CheckKeysActivity.RESULT_NEGATIVE));
  }
}
