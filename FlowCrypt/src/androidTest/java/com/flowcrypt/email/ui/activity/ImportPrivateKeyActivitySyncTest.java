/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcelable;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.filters.LargeTest;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;

import com.flowcrypt.email.R;
import com.flowcrypt.email.TestConstants;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.rules.AddAccountToDatabaseRule;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.ui.activity.base.BaseImportKeyActivity;
import com.flowcrypt.email.util.TestGeneralUtil;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.ActivityResultMatchers.hasResultCode;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasCategories;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasType;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

/**
 * @author Denis Bondarenko
 *         Date: 17.03.2018
 *         Time: 13:33
 *         E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class ImportPrivateKeyActivitySyncTest extends BaseTest {

    private static final String SOME_TEXT = "Some text";
    private static File fileWithPrivateKey;
    private static File fileWithoutPrivateKey;
    private static String privateKey;

    private IntentsTestRule intentsTestRule = new IntentsTestRule<ImportPrivateKeyActivity>
            (ImportPrivateKeyActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            Context targetContext = InstrumentationRegistry.getTargetContext();
            Intent result = new Intent(targetContext, ImportPrivateKeyActivity.class);
            result.putExtra(BaseImportKeyActivity.KEY_EXTRA_IS_SYNC_ENABLE, true);
            result.putExtra(BaseImportKeyActivity.KEY_EXTRA_TITLE, targetContext.getString(R.string
                    .import_private_key));
            result.putExtra(BaseImportKeyActivity.KEY_EXTRA_PRIVATE_KEY_IMPORT_MODEL_FROM_CLIPBOARD, (Parcelable) null);
            result.putExtra(BaseImportKeyActivity.KEY_EXTRA_IS_THROW_ERROR_IF_DUPLICATE_FOUND, true);
            return result;
        }
    };

    @Rule
    public TestRule ruleChain = RuleChain
            .outerRule(new ClearAppSettingsRule())
            .around(new AddAccountToDatabaseRule())
            .around(GrantPermissionRule.grant(android.Manifest.permission.READ_EXTERNAL_STORAGE))
            .around(intentsTestRule);

    @BeforeClass
    public static void createResources() throws IOException {
        privateKey = TestGeneralUtil.readFileFromAssetsAsString(InstrumentationRegistry.getContext(),
                "pgp/" + TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER + "-sec.asc");
        fileWithPrivateKey = TestGeneralUtil.createFile(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER
                + "_sec.asc", privateKey);
        fileWithoutPrivateKey = TestGeneralUtil.createFile(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER
                + ".txt", SOME_TEXT);
    }

    @AfterClass
    public static void cleanResources() {
        List<File> files = new ArrayList<>();
        files.add(fileWithPrivateKey);
        files.add(fileWithoutPrivateKey);
        TestGeneralUtil.deleteFiles(files);
    }

    @Before
    public void registerIdlingResource() {
        IdlingRegistry.getInstance().register(((ImportPrivateKeyActivity) intentsTestRule.getActivity())
                .getCountingIdlingResource());
    }

    @After
    public void unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(((ImportPrivateKeyActivity) intentsTestRule.getActivity())
                .getCountingIdlingResource());
    }

    @Test
    public void testImportKeyFromBackup() {
        useIntentionFromRunCheckKeysActivity();

        onView(withId(R.id.buttonImportBackup)).check(matches(isDisplayed())).perform(click());
        assertThat(intentsTestRule.getActivityResult(), hasResultCode(Activity.RESULT_OK));
    }

    @Test
    public void testImportKeyFromFile() {
        useIntentionToRunActivityToSelectFile(fileWithPrivateKey);
        useIntentionFromRunCheckKeysActivity();

        onView(withId(R.id.buttonLoadFromFile)).check(matches(isDisplayed())).perform(click());
        assertThat(intentsTestRule.getActivityResult(), hasResultCode(Activity.RESULT_OK));
    }

    @Test
    public void testShowErrorWhenImportingKeyFromFile() {
        useIntentionToRunActivityToSelectFile(fileWithoutPrivateKey);

        onView(withId(R.id.buttonLoadFromFile)).check(matches(isDisplayed())).perform(click());
        checkIsSnackbarDisplayed(InstrumentationRegistry.getTargetContext().getString(
                R.string.file_has_wrong_pgp_structure,
                InstrumentationRegistry.getTargetContext().getString(R.string.private_)));
    }

    @Test
    public void testImportKeyFromClipboard() throws Throwable {
        useIntentionFromRunCheckKeysActivity();

        addTextToClipboard("private key", privateKey);
        onView(withId(R.id.buttonLoadFromClipboard)).check(matches(isDisplayed())).perform(click());
        assertThat(intentsTestRule.getActivityResult(), hasResultCode(Activity.RESULT_OK));
    }

    @Test
    public void testShowErrorWhenImportKeyFromClipboard() throws Throwable {
        addTextToClipboard("not private key", SOME_TEXT);
        onView(withId(R.id.buttonLoadFromClipboard)).check(matches(isDisplayed())).perform(click());
        checkIsSnackbarDisplayed(InstrumentationRegistry.getTargetContext().getString(
                R.string.clipboard_has_wrong_structure,
                InstrumentationRegistry.getTargetContext().getString(R.string.private_)));
    }

    private void useIntentionToRunActivityToSelectFile(File file) {
        Intent resultData = new Intent();
        resultData.setData(Uri.fromFile(file));
        intending(allOf(hasAction(Intent.ACTION_CHOOSER), hasExtra(is(Intent.EXTRA_INTENT), allOf(hasAction(Intent
                .ACTION_GET_CONTENT), hasCategories(hasItem(equalTo(Intent.CATEGORY_OPENABLE))), hasType("*/*")))))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData));
    }


    private void useIntentionFromRunCheckKeysActivity() {
        intending(hasComponent(new ComponentName(InstrumentationRegistry.getTargetContext(),
                CheckKeysActivity.class))).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK,
                null));
    }
}
