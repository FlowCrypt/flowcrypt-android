/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;

import com.flowcrypt.email.R;
import com.flowcrypt.email.TestConstants;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.rules.AddAccountToDatabaseRule;
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.ui.activity.settings.KeysSettingsActivity;
import com.flowcrypt.email.util.TestGeneralUtil;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;

/**
 * @author Denis Bondarenko
 * Date: 20.02.2018
 * Time: 15:42
 * E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class KeysSettingsActivityTest extends BaseTest {

  private IntentsTestRule intentsTestRule = new IntentsTestRule<>(KeysSettingsActivity.class);
  @Rule
  public TestRule ruleChain = RuleChain
      .outerRule(new ClearAppSettingsRule())
      .around(new AddAccountToDatabaseRule())
      .around(new AddPrivateKeyToDatabaseRule())
      .around(intentsTestRule);

  @Override
  public ActivityTestRule getActivityTestRule() {
    return intentsTestRule;
  }

  @Test
  public void testAddNewKeys() throws Throwable {
    intending(hasComponent(new ComponentName(InstrumentationRegistry.getInstrumentation().getTargetContext(),
        ImportPrivateKeyActivity.class))).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

    TestGeneralUtil.saveKeyToDatabase(TestGeneralUtil.readFileFromAssetsAsString(InstrumentationRegistry
            .getInstrumentation().getContext(), "pgp/ben@flowcrypt.com-sec.asc"),
        TestConstants.DEFAULT_PASSWORD, KeyDetails.Type.EMAIL);

    onView(withId(R.id.floatActionButtonAddKey)).check(matches(isDisplayed())).perform(click());
    onView(withId(R.id.recyclerViewKeys)).check(matches(isDisplayed())).check(matches(matchRecyclerViewSize(2)));
  }

  @Test
  public void testKeyExists() {
    onView(withId(R.id.recyclerViewKeys)).check(matches(not(matchEmptyRecyclerView()))).check(matches(isDisplayed()));
    onView(withId(R.id.emptyView)).check(matches(not(isDisplayed())));
  }
}
