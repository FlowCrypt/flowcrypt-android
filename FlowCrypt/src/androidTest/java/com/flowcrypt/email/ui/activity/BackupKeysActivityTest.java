/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.rules.AddAccountToDatabaseRule;
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.util.TestGeneralUtil;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Collections;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasCategories;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasType;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

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
      .around(new AddPrivateKeyToDatabaseRule())
      .around(activityTestRule);

  @Test
  public void testSuccessDownloadBackup() {
    onView(withId(R.id.radioButtonDownload)).check(matches(isDisplayed())).perform(click());
    Intent resultData = new Intent();
    File file = TestGeneralUtil.createFile("key.asc", "");
    resultData.setData(Uri.fromFile(file));
    intending(allOf(hasAction(Intent.ACTION_CREATE_DOCUMENT),
        hasCategories(hasItem(equalTo(Intent.CATEGORY_OPENABLE))),
        hasType(Constants.MIME_TYPE_PGP_KEY)))
        .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData));
    onView(withId(R.id.buttonBackupAction)).check(matches(isDisplayed())).perform(click());
    TestGeneralUtil.deleteFiles(Collections.singletonList(file));
  }

  @Test
  public void testSuccessEmailBackup() {
    onView(withId(R.id.buttonBackupAction)).check(matches(isDisplayed())).perform(click());
  }
}