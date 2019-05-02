/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.TestConstants;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.rules.AddAccountToDatabaseRule;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.util.AccountDaoManagerKt;
import com.flowcrypt.email.util.PrivateKeysManagerKt;
import com.flowcrypt.email.util.TestGeneralUtilKt;

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
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasCategories;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasType;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Denis Bondarenko
 * Date: 17.08.2018
 * Time: 16:28
 * E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
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
  public void testEmailOptionHint() {
    onView(withId(R.id.radioButtonEmail)).check(matches(isDisplayed())).perform(click());
    onView(withText(getResString(R.string.backup_as_email_hint))).check(matches(isDisplayed()));
  }

  @Test
  public void testDownloadOptionHint() {
    onView(withId(R.id.radioButtonDownload)).check(matches(isDisplayed())).perform(click());
    onView(withText(getResString(R.string.backup_as_download_hint))).check(matches(isDisplayed()));
  }

  @Test
  public void testNoKeysEmailOption() {
    onView(withId(R.id.radioButtonEmail)).check(matches(isDisplayed())).perform(click());
    onView(withId(R.id.buttonBackupAction)).check(matches(isDisplayed())).perform(click());
    onView(withText(getResString(R.string.there_are_no_private_keys,
        AccountDaoManagerKt.getDefaultAccountDao().getEmail()))).check(matches(isDisplayed()));
  }

  @Test
  public void testNoKeysDownloadOption() {
    onView(withId(R.id.radioButtonDownload)).check(matches(isDisplayed())).perform(click());
    onView(withId(R.id.buttonBackupAction)).check(matches(isDisplayed())).perform(click());
    onView(withText(getResString(R.string.there_are_no_private_keys,
        AccountDaoManagerKt.getDefaultAccountDao().getEmail()))).check(matches(isDisplayed()));
  }

  @Test
  public void testSuccessEmailOption() throws Throwable {
    addFirstKeyWithStrongPassword();
    onView(withId(R.id.buttonBackupAction)).check(matches(isDisplayed())).perform(click());
    assertTrue(activityTestRule.getActivity().isFinishing());
  }

  @Test
  public void testSuccessWithTwoKeysEmailOption() throws Throwable {
    addSecondKeyWithStrongPassword();
    testSuccessEmailOption();
  }

  @Test
  public void testSuccessDownloadOption() throws Throwable {
    addFirstKeyWithStrongPassword();
    onView(withId(R.id.radioButtonDownload)).check(matches(isDisplayed())).perform(click());
    File file = TestGeneralUtilKt.createFile("key.asc", "");
    intendingFileChoose(file);
    onView(withId(R.id.buttonBackupAction)).check(matches(isDisplayed())).perform(click());
    assertTrue(activityTestRule.getActivity().isFinishing());
    TestGeneralUtilKt.deleteFiles(Collections.singletonList(file));
  }

  @Test
  public void testSuccessWithTwoKeysDownloadOption() throws Throwable {
    addSecondKeyWithStrongPassword();
    testSuccessDownloadOption();
  }

  @Test
  public void testShowWeakPasswordHintForDownloadOption() throws Throwable {
    addFirstKeyWithDefaultPassword();
    onView(withId(R.id.radioButtonDownload)).check(matches(isDisplayed())).perform(click());
    intendingFileChoose(new File(""));
    onView(withId(R.id.buttonBackupAction)).check(matches(isDisplayed())).perform(click());
    onView(withText(getResString(R.string.pass_phrase_is_too_weak))).check(matches(isDisplayed()));
  }

  @Test
  public void testShowWeakPasswordHintForEmailOption() throws Throwable {
    addFirstKeyWithDefaultPassword();
    onView(withId(R.id.buttonBackupAction)).check(matches(isDisplayed())).perform(click());
    onView(withText(getResString(R.string.pass_phrase_is_too_weak))).check(matches(isDisplayed()));
  }

  @Test
  public void testFixWeakPasswordForDownloadOption() throws Throwable {
    addFirstKeyWithDefaultPassword();
    onView(withId(R.id.radioButtonDownload)).check(matches(isDisplayed())).perform(click());
    intendingFileChoose(new File(""));
    onView(withId(R.id.buttonBackupAction)).check(matches(isDisplayed())).perform(click());
    intending(hasComponent(new ComponentName(InstrumentationRegistry.getInstrumentation().getTargetContext(),
        ChangePassPhraseActivity.class))).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
    checkIsSnackbarDisplayedAndClick(getResString(R.string.pass_phrase_is_too_weak));
    assertFalse(activityTestRule.getActivity().isFinishing());
  }

  @Test
  public void testFixWeakPasswordForEmailOption() throws Throwable {
    addFirstKeyWithDefaultPassword();
    onView(withId(R.id.buttonBackupAction)).check(matches(isDisplayed())).perform(click());
    intending(hasComponent(new ComponentName(InstrumentationRegistry.getInstrumentation().getTargetContext(),
        ChangePassPhraseActivity.class))).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
    checkIsSnackbarDisplayedAndClick(getResString(R.string.pass_phrase_is_too_weak));
    assertFalse(activityTestRule.getActivity().isFinishing());
  }

  @Test
  public void testDiffPassphrasesForEmailOption() throws Throwable {
    addFirstKeyWithStrongPassword();
    addSecondKeyWithStrongSecondPassword();
    onView(withId(R.id.buttonBackupAction)).check(matches(isDisplayed())).perform(click());
    intending(hasComponent(new ComponentName(InstrumentationRegistry.getInstrumentation().getTargetContext(),
        ChangePassPhraseActivity.class))).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
    checkIsSnackbarDisplayedAndClick(getResString(R.string.different_pass_phrases));
    assertFalse(activityTestRule.getActivity().isFinishing());
  }

  @Test
  public void testDiffPassphrasesForDownloadOption() throws Throwable {
    addFirstKeyWithStrongPassword();
    addSecondKeyWithStrongSecondPassword();
    onView(withId(R.id.radioButtonDownload)).check(matches(isDisplayed())).perform(click());
    intendingFileChoose(new File(""));
    onView(withId(R.id.buttonBackupAction)).check(matches(isDisplayed())).perform(click());
    intending(hasComponent(new ComponentName(InstrumentationRegistry.getInstrumentation().getTargetContext(),
        ChangePassPhraseActivity.class))).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
    checkIsSnackbarDisplayedAndClick(getResString(R.string.different_pass_phrases));
    assertFalse(activityTestRule.getActivity().isFinishing());
  }

  private void intendingFileChoose(File file) {
    Intent resultData = new Intent();
    resultData.setData(Uri.fromFile(file));
    intending(allOf(hasAction(Intent.ACTION_CREATE_DOCUMENT),
        hasCategories(hasItem(equalTo(Intent.CATEGORY_OPENABLE))),
        hasType(Constants.MIME_TYPE_PGP_KEY)))
        .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData));
  }

  private void addFirstKeyWithDefaultPassword() throws Throwable {
    PrivateKeysManagerKt.saveKeyFromAssetsToDatabase("node/default@denbond7.com_fisrtKey_prv_default.json",
        TestConstants.DEFAULT_PASSWORD, KeyDetails.Type.EMAIL);
  }

  private void addFirstKeyWithStrongPassword() throws Throwable {
    PrivateKeysManagerKt.saveKeyFromAssetsToDatabase("node/default@denbond7.com_fisrtKey_prv_strong.json",
        TestConstants.DEFAULT_STRONG_PASSWORD, KeyDetails.Type.EMAIL);
  }

  private void addSecondKeyWithStrongPassword() throws Throwable {
    PrivateKeysManagerKt.saveKeyFromAssetsToDatabase("node/default@denbond7.com_secondKey_prv_strong.json",
        TestConstants.DEFAULT_STRONG_PASSWORD, KeyDetails.Type.EMAIL);
  }

  private void addSecondKeyWithStrongSecondPassword() throws Throwable {
    PrivateKeysManagerKt.saveKeyFromAssetsToDatabase("node/default@denbond7.com_secondKey_prv_strong_second.json",
        TestConstants.DEFAULT_SECOND_STRONG_PASSWORD, KeyDetails.Type.EMAIL);
  }
}
