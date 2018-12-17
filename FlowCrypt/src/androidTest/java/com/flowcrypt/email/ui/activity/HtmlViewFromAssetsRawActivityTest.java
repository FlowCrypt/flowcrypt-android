/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.content.Context;
import android.content.Intent;

import com.flowcrypt.email.R;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.rules.ClearAppSettingsRule;

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
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

/**
 * @author Denis Bondarenko
 * Date: 24.02.2018
 * Time: 14:56
 * E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class HtmlViewFromAssetsRawActivityTest extends BaseTest {
  private ActivityTestRule activityTestRule = new ActivityTestRule<>(HtmlViewFromAssetsRawActivity.class, false, false);

  @Rule
  public TestRule ruleChain = RuleChain
      .outerRule(new ClearAppSettingsRule())
      .around(activityTestRule);

  @Test
  public void testShowPrivacyTitle() {
    startActivity(InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string.privacy));
    onView(allOf(withText(R.string.privacy), withParent(withId(R.id.toolbar)))).check(matches(isDisplayed()));
  }

  @Test
  public void testShowTermsTitle() {
    startActivity(InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string.terms));
    onView(allOf(withText(R.string.terms), withParent(withId(R.id.toolbar)))).check(matches(isDisplayed()));
  }

  @Test
  public void testShowSecurityTitle() {
    startActivity(InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string.security));
    onView(allOf(withText(R.string.security), withParent(withId(R.id.toolbar)))).check(matches(isDisplayed()));
  }

  private void startActivity(String title) {
    Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    Intent intent = new Intent(targetContext, HtmlViewFromAssetsRawActivity.class);
    intent.putExtra(HtmlViewFromAssetsRawActivity.EXTRA_KEY_ACTIVITY_TITLE, title);
    intent.putExtra(HtmlViewFromAssetsRawActivity.EXTRA_KEY_HTML_RESOURCES_ID, "html/privacy.htm");
    activityTestRule.launchActivity(intent);
  }
}

