/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.content.Context;
import android.content.Intent;

import com.flowcrypt.email.R;
import com.flowcrypt.email.rules.AddAccountToDatabaseRule;
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.ui.activity.base.BasePassphraseActivityTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertTrue;

/**
 * @author Denis Bondarenko
 * Date: 3/13/19
 * Time: 12:15 PM
 * E-mail: DenBond7@gmail.com
 */
public class ChangePassPhraseActivityTest extends BasePassphraseActivityTest {
  private AddAccountToDatabaseRule addAccountToDatabaseRule = new AddAccountToDatabaseRule();

  private IntentsTestRule activityTestRule =
      new IntentsTestRule<ChangePassPhraseActivity>(ChangePassPhraseActivity.class) {
        @Override
        protected Intent getActivityIntent() {
          Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
          return ChangePassPhraseActivity.newIntent(targetContext, addAccountToDatabaseRule.getAccount());
        }
      };

  @Rule
  public TestRule ruleChain = RuleChain
      .outerRule(new ClearAppSettingsRule())
      .around(addAccountToDatabaseRule)
      .around(new AddPrivateKeyToDatabaseRule())
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
    assertTrue(activityTestRule.getActivity().isFinishing());
  }
}
