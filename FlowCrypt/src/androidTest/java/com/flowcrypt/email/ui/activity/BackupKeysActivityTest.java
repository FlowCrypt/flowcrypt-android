/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.TestConstants;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.rules.AddAccountToDatabaseRule;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.util.PrivateKeysManager;
import com.flowcrypt.email.util.TestGeneralUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Collections;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasCategories;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasType;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertTrue;

/**
 * @author Denis Bondarenko
 * Date: 17.08.2018
 * Time: 16:28
 * E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
//todo-denbond7 Not completed
public class BackupKeysActivityTest extends BaseTest {

  private IntentsTestRule activityTestRule = new IntentsTestRule<>(BackupKeysActivity.class);

  @Rule
  public TestRule ruleChain = RuleChain
      .outerRule(new ClearAppSettingsRule())
      .around(new AddAccountToDatabaseRule())
      .around(activityTestRule);

  @Override
  public ActivityTestRule getActivityTestRule() {
    return activityTestRule;
  }

  @Before
  public void registerIdlingResource() {
    IdlingRegistry.getInstance().register(((BackupKeysActivity) activityTestRule.getActivity())
        .getCountingIdlingResource());
  }

  @After
  public void unregisterIdlingResource() {
    IdlingRegistry.getInstance().unregister(((BackupKeysActivity) activityTestRule.getActivity())
        .getCountingIdlingResource());
  }

  @Test
  public void testSuccessDownloadOption() throws Throwable {
    addKeyWithStrongPassword();
    File file = selectDownloadOption();
    assertTrue(activityTestRule.getActivity().isFinishing());
    TestGeneralUtil.deleteFiles(Collections.singletonList(file));
  }

  @Test
  public void testSuccessEmailOption() throws Throwable {
    addKeyWithStrongPassword();
    onView(withId(R.id.buttonBackupAction)).check(matches(isDisplayed())).perform(click());
    assertTrue(activityTestRule.getActivity().isFinishing());
  }

  @Test
  public void testShowWeakPasswordHintForDownloadOption() throws Throwable {
    addKeyWithDefaultPassword();
    selectDownloadOption();
    onView(withText(getResString(R.string.pass_phrase_is_too_weak))).check(matches(isDisplayed()));
  }

  @Test
  public void testShowWeakPasswordHintForEmailOption() throws Throwable {
    addKeyWithDefaultPassword();
    onView(withId(R.id.buttonBackupAction)).check(matches(isDisplayed())).perform(click());
    onView(withText(getResString(R.string.pass_phrase_is_too_weak))).check(matches(isDisplayed()));
  }

  private File selectDownloadOption() {
    onView(withId(R.id.radioButtonDownload)).check(matches(isDisplayed())).perform(click());
    Intent resultData = new Intent();
    File file = TestGeneralUtil.createFile("key.asc", "");
    resultData.setData(Uri.fromFile(file));
    intending(allOf(hasAction(Intent.ACTION_CREATE_DOCUMENT),
        hasCategories(hasItem(equalTo(Intent.CATEGORY_OPENABLE))),
        hasType(Constants.MIME_TYPE_PGP_KEY)))
        .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData));
    onView(withId(R.id.buttonBackupAction)).check(matches(isDisplayed())).perform(click());
    return file;
  }

  private void addKeyWithDefaultPassword() throws Throwable {
    PrivateKeysManager.saveKeyFromAssetsToDatabase("node/default@denbond7.com_fisrtKey_prv_default.json",
        TestConstants.DEFAULT_PASSWORD,
        KeyDetails.Type.EMAIL);
  }

  private void addKeyWithStrongPassword() throws Throwable {
    PrivateKeysManager.saveKeyFromAssetsToDatabase("node/default@denbond7.com_fisrtKey_prv_strong.json",
        TestConstants.DEFAULT_STRONG_PASSWORD,
        KeyDetails.Type.EMAIL);
  }
}
