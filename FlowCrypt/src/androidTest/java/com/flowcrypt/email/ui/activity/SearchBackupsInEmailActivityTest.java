/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import com.flowcrypt.email.R;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.rules.AddAccountToDatabaseRule;
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.ui.activity.settings.SearchBackupsInEmailActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

/**
 * @author Denis Bondarenko
 * Date: 07.03.2018
 * Time: 12:39
 * E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class SearchBackupsInEmailActivityTest extends BaseTest {

  private IntentsTestRule activityTestRule = new IntentsTestRule<>(SearchBackupsInEmailActivity.class);

  @Rule
  public TestRule ruleChain = RuleChain
      .outerRule(new ClearAppSettingsRule())
      .around(new AddAccountToDatabaseRule())
      .around(new AddPrivateKeyToDatabaseRule())
      .around(activityTestRule);

  @Before
  public void registerIdling() {
    IdlingRegistry.getInstance().register(((SearchBackupsInEmailActivity) activityTestRule.getActivity())
        .getCountingIdlingResource());
  }

  @After
  public void unregisterIdling() {
    IdlingRegistry.getInstance().unregister(((SearchBackupsInEmailActivity) activityTestRule.getActivity())
        .getCountingIdlingResource());
  }

  @Test
  public void testShowHelpScreen() {
    testHelpScreen();
  }

  @Test
  public void testIsBackupFound() {
    onView(withId(R.id.buttonSeeMoreBackupOptions)).check(matches(isDisplayed()));
    onView(withId(R.id.textViewBackupFound)).check(matches(isDisplayed()));
  }

  @Test
  public void testShowBackupOptions() {
    testIsBackupFound();
    onView(withId(R.id.buttonSeeMoreBackupOptions)).perform(click());
    onView(withId(R.id.buttonBackupAction)).check(matches(isDisplayed()));
  }

  @Test
  public void testSelectEmailForSavingBackup() {
    testShowBackupOptions();
    onView(withId(R.id.radioButtonEmail)).check(matches(isDisplayed()))
        .perform(click()).check(matches(isChecked()));
    onView(withId(R.id.textViewOptionsHint)).check(matches(isDisplayed()))
        .check(matches(withText(R.string.backup_as_email_hint)));
    onView(withId(R.id.buttonBackupAction)).check(matches(withText(R.string.backup_as_email)));
    onView(withId(R.id.radioButtonDownload)).check(matches(isDisplayed()))
        .check(matches(not(isChecked())));
  }

  @Test
  public void testSelectDownloadToFileForSavingBackup() {
    testShowBackupOptions();
    onView(withId(R.id.radioButtonDownload)).check(matches(isDisplayed()))
        .perform(click()).check(matches(isChecked()));
    onView(withId(R.id.textViewOptionsHint)).check(matches(isDisplayed()))
        .check(matches(withText(R.string.backup_as_download_hint)));
    onView(withId(R.id.buttonBackupAction)).check(matches(withText(R.string.backup_as_a_file)));
    onView(withId(R.id.radioButtonEmail)).check(matches(isDisplayed()))
        .check(matches(not(isChecked())));
  }
}
