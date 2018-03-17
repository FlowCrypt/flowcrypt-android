/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.rules.AddAccountToDatabaseRule;
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.ui.activity.settings.BackupSettingsActivity;
import com.flowcrypt.email.util.TestGeneralUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import java.io.File;
import java.util.Collections;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasCategories;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasType;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

/**
 * @author Denis Bondarenko
 *         Date: 07.03.2018
 *         Time: 12:39
 *         E-mail: DenBond7@gmail.com
 */

public class BackupSettingsActivityTest extends BaseTest {

    private IntentsTestRule activityTestRule = new IntentsTestRule<>(BackupSettingsActivity.class);

    @Rule
    public TestRule ruleChain = RuleChain
            .outerRule(new ClearAppSettingsRule())
            .around(new AddAccountToDatabaseRule())
            .around(new AddPrivateKeyToDatabaseRule())
            .around(activityTestRule);

    @Before
    public void registerIdling() {
        IdlingRegistry.getInstance().register(((BackupSettingsActivity) activityTestRule.getActivity())
                .getCountingIdlingResource());
    }

    @After
    public void unregisterIdling() {
        IdlingRegistry.getInstance().unregister(((BackupSettingsActivity) activityTestRule.getActivity())
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

    @Test
    public void testUseEmailForSavingBackup() {
        testSelectEmailForSavingBackup();
        onView(withId(R.id.buttonBackupAction)).check(matches(isDisplayed())).perform(click());
        checkIsToastDisplayed(activityTestRule.getActivity(), InstrumentationRegistry.getTargetContext()
                .getString(R.string.backup_was_sent_successfully));
    }

    @Test
    public void testUseDownloadToFileForSavingBackup() {
        Intent resultData = new Intent();
        File file = TestGeneralUtil.createFile("key.asc", "");
        resultData.setData(Uri.fromFile(file));
        intending(allOf(hasAction(Intent.ACTION_CREATE_DOCUMENT),
                hasCategories(hasItem(equalTo(Intent.CATEGORY_OPENABLE))),
                hasType(Constants.MIME_TYPE_PGP_KEY)))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData));

        testSelectDownloadToFileForSavingBackup();
        onView(withId(R.id.buttonBackupAction)).check(matches(isDisplayed())).perform(click());
        checkIsToastDisplayed(activityTestRule.getActivity(), InstrumentationRegistry.getTargetContext()
                .getString(R.string.key_successfully_saved));
        TestGeneralUtil.deleteFiles(Collections.singletonList(file));
    }
}
