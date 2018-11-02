/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.flowcrypt.email.R;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.rules.AddAccountToDatabaseRule;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.ui.activity.settings.LegalSettingsActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isSelected;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

/**
 * @author Denis Bondarenko
 * Date: 23.02.2018
 * Time: 10:25
 * E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class LegalSettingsActivityTest extends BaseTest {

  @Rule
  public TestRule ruleChain = RuleChain
      .outerRule(new ClearAppSettingsRule())
      .around(new AddAccountToDatabaseRule())
      .around(new ActivityTestRule<>(LegalSettingsActivity.class));

  private String[] titleNames;

  @Before
  public void setUp() {
    titleNames = new String[]{
        InstrumentationRegistry.getTargetContext().getString(R.string.privacy),
        InstrumentationRegistry.getTargetContext().getString(R.string.terms),
        InstrumentationRegistry.getTargetContext().getString(R.string.licence),
        InstrumentationRegistry.getTargetContext().getString(R.string.sources)
    };
  }

  @Test
  public void testClickToTitleViewPager() {
    for (String titleName : titleNames) {
      onView(allOf(withParent(withParent(withParent(withId(R.id.tabLayout)))), withText(titleName)))
          .check(matches(isDisplayed())).perform(click());
      onView(allOf(withParent(withParent(withParent(withId(R.id.tabLayout)))), withText(titleName)))
          .check(matches(isDisplayed())).check(matches(isSelected()));
    }
  }

  @Test
  public void testShowHelpScreen() throws InterruptedException {
    //Added a timeout because an initialization of WebViews needs more time.
    Thread.sleep(5000);
    testHelpScreen();
  }

  @Test
  public void testSwipeInViewPager() {
    onView(allOf(withParent(withParent(withParent(withId(R.id.tabLayout)))), withText(titleNames[0])))
        .check(matches(isDisplayed())).check(matches(isSelected()));
    for (int i = 1; i < titleNames.length; i++) {
      onView(withId(R.id.viewPager)).perform(swipeLeft());
      onView(allOf(withParent(withParent(withParent(withId(R.id.tabLayout)))), withText(titleNames[i])))
          .check(matches(isDisplayed())).check(matches(isSelected()));
    }
  }
}
