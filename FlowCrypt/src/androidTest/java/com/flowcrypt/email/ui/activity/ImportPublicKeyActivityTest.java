/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcelable;

import com.flowcrypt.email.R;
import com.flowcrypt.email.TestConstants;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.rules.AddAccountToDatabaseRule;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.ui.activity.base.BaseImportKeyActivity;
import com.flowcrypt.email.util.TestGeneralUtil;

import org.junit.AfterClass;
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

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.ActivityResultMatchers.hasResultCode;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasCategories;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasType;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

/**
 * @author Denis Bondarenko
 * Date: 23.02.2018
 * Time: 16:53
 * E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class ImportPublicKeyActivityTest extends BaseTest {
  private static final String SOME_TEXT = "Some text";
  private static File fileWithPublicKey;
  private static File fileWithoutPublicKey;
  private static String publicKey;

  private IntentsTestRule intentsTestRule =
      new IntentsTestRule<ImportPublicKeyActivity>(ImportPublicKeyActivity.class) {
        @Override
        protected Intent getActivityIntent() {
          Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
          PgpContact pgpContact = new PgpContact(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER, null, null,
              false, null, false, null, null, null, 0);
          Intent result = new Intent(targetContext, ImportPublicKeyActivity.class);
          result.putExtra(BaseImportKeyActivity.KEY_EXTRA_IS_SYNC_ENABLE, true);
          result.putExtra(BaseImportKeyActivity.KEY_EXTRA_TITLE, targetContext.getString(R.string.import_public_key));
          result.putExtra(BaseImportKeyActivity.KEY_EXTRA_PRIVATE_KEY_IMPORT_MODEL_FROM_CLIPBOARD, (Parcelable) null);
          result.putExtra(BaseImportKeyActivity.KEY_EXTRA_IS_THROW_ERROR_IF_DUPLICATE_FOUND, false);
          result.putExtra(ImportPublicKeyActivity.KEY_EXTRA_PGP_CONTACT, pgpContact);
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
    publicKey = TestGeneralUtil.readFileFromAssetsAsString(InstrumentationRegistry.getInstrumentation().getContext(),
        "pgp/" + TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER + "-pub.asc");
    fileWithPublicKey = TestGeneralUtil.createFile(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER
        + "_pub.asc", publicKey);
    fileWithoutPublicKey = TestGeneralUtil.createFile(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER
        + ".txt", SOME_TEXT);
  }

  @AfterClass
  public static void cleanResources() {
    List<File> files = new ArrayList<>();
    files.add(fileWithPublicKey);
    files.add(fileWithoutPublicKey);
    TestGeneralUtil.deleteFiles(files);
  }

  @Test
  public void testImportKeyFromFile() {
    Intent resultData = new Intent();
    resultData.setData(Uri.fromFile(fileWithPublicKey));
    intending(allOf(hasAction(Intent.ACTION_CHOOSER), hasExtra(is(Intent.EXTRA_INTENT), allOf(hasAction(Intent
        .ACTION_GET_CONTENT), hasCategories(hasItem(equalTo(Intent.CATEGORY_OPENABLE))), hasType("*/*")))))
        .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData));
    onView(withId(R.id.buttonLoadFromFile)).check(matches(isDisplayed())).perform(click());
    assertThat(intentsTestRule.getActivityResult(), hasResultCode(Activity.RESULT_OK));
  }

  @Test
  public void testShowErrorWhenImportingKeyFromFile() {
    Intent resultData = new Intent();
    resultData.setData(Uri.fromFile(fileWithoutPublicKey));
    intending(allOf(hasAction(Intent.ACTION_CHOOSER), hasExtra(is(Intent.EXTRA_INTENT), allOf(hasAction(Intent
        .ACTION_GET_CONTENT), hasCategories(hasItem(equalTo(Intent.CATEGORY_OPENABLE))), hasType("*/*")))))
        .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData));
    onView(withId(R.id.buttonLoadFromFile)).check(matches(isDisplayed())).perform(click());
    checkIsSnackbarDisplayed(InstrumentationRegistry.getInstrumentation().getTargetContext().getString(
        R.string.file_has_wrong_pgp_structure,
        InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string.public_)));
  }

  @Test
  public void testImportKeyFromClipboard() throws Throwable {
    addTextToClipboard("public key", publicKey);
    onView(withId(R.id.buttonLoadFromClipboard)).check(matches(isDisplayed())).perform(click());
    assertThat(intentsTestRule.getActivityResult(), hasResultCode(Activity.RESULT_OK));
  }

  @Test
  public void testShowErrorWhenImportKeyFromClipboard() throws Throwable {
    addTextToClipboard("not public key", SOME_TEXT);
    onView(withId(R.id.buttonLoadFromClipboard)).check(matches(isDisplayed())).perform(click());
    checkIsSnackbarDisplayed(InstrumentationRegistry.getInstrumentation().getTargetContext().getString(
        R.string.clipboard_has_wrong_structure,
        InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string.public_)));
  }
}
