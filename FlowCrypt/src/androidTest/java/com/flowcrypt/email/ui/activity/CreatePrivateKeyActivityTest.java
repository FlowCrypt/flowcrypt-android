/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.content.Context;
import android.content.Intent;

import com.flowcrypt.email.R;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.ui.activity.base.BasePassphraseActivityTest;
import com.flowcrypt.email.util.AccountDaoManagerKt;

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
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

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
public class CreatePrivateKeyActivityTest extends BasePassphraseActivityTest {

  private ActivityTestRule activityTestRule =
      new ActivityTestRule<CreatePrivateKeyActivity>(CreatePrivateKeyActivity.class) {
        @Override
        protected Intent getActivityIntent() {
          Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
          Intent result = new Intent(targetContext, CreatePrivateKeyActivity.class);
          result.putExtra(CreatePrivateKeyActivity.KEY_EXTRA_ACCOUNT_DAO,
              AccountDaoManagerKt.getDefaultAccountDao());
          return result;
        }
      };

  @Rule
  public TestRule ruleChain = RuleChain
      .outerRule(new ClearAppSettingsRule())
      .around(activityTestRule);

  @Override
  public ActivityTestRule getActivityTestRule() {
    return activityTestRule;
  }

  @Test
  public void testUseCorrectPassPhrase() {
    onView(withId(R.id.editTextKeyPassword)).check(matches(isDisplayed())).perform(replaceText(PERFECT_PASSWORD),
        closeSoftKeyboard());
    onView(withId(R.id.buttonSetPassPhrase)).check(matches(isDisplayed())).perform(click());

    onView(withId(R.id.editTextKeyPasswordSecond)).check(matches(isDisplayed())).perform(replaceText(PERFECT_PASSWORD),
        closeSoftKeyboard());
    onView(withId(R.id.buttonConfirmPassPhrases)).check(matches(isDisplayed())).perform(click());

    onView(withId(R.id.buttonSuccess)).check(matches(isDisplayed()));
  }
}
