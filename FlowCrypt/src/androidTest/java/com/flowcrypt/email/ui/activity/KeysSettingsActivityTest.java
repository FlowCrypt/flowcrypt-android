/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Environment;
import android.text.TextUtils;
import android.text.format.DateFormat;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.TestConstants;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.matchers.ToastMatcher;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.model.PgpContact;
import com.flowcrypt.email.rules.AddAccountToDatabaseRule;
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.ui.activity.base.BaseActivity;
import com.flowcrypt.email.ui.activity.settings.KeysSettingsActivity;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.PrivateKeysManager;
import com.flowcrypt.email.util.TestGeneralUtil;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.core.content.FileProvider;
import androidx.test.espresso.contrib.RecyclerViewActions;
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
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.AllOf.allOf;

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
  private AddPrivateKeyToDatabaseRule addPrivateKeyToDatabaseRule = new AddPrivateKeyToDatabaseRule();

  @Rule
  public TestRule ruleChain = RuleChain
      .outerRule(new ClearAppSettingsRule())
      .around(new AddAccountToDatabaseRule())
      .around(addPrivateKeyToDatabaseRule)
      .around(intentsTestRule);

  @Override
  public ActivityTestRule getActivityTestRule() {
    return intentsTestRule;
  }

  @Test
  public void testAddNewKeys() throws Throwable {
    intending(hasComponent(new ComponentName(InstrumentationRegistry.getInstrumentation().getTargetContext(),
        ImportPrivateKeyActivity.class))).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

    NodeKeyDetails nodeKeyDetails =
        PrivateKeysManager.getNodeKeyDetailsFromAssets("node/default@denbond7.com_secondKey_prv_default.json");

    PrivateKeysManager.saveKeyToDatabase(nodeKeyDetails, TestConstants.DEFAULT_PASSWORD, KeyDetails.Type.EMAIL,
        (BaseActivity) getActivityTestRule().getActivity());

    onView(withId(R.id.floatActionButtonAddKey)).check(matches(isDisplayed())).perform(click());
    onView(withId(R.id.recyclerViewKeys)).check(matches(isDisplayed())).check(matches(matchRecyclerViewSize(2)));
  }

  @Test
  public void testKeyExists() {
    onView(withId(R.id.recyclerViewKeys)).check(matches(not(matchEmptyRecyclerView()))).check(matches(isDisplayed()));
    onView(withId(R.id.emptyView)).check(matches(not(isDisplayed())));
  }

  @Test
  public void testShowKeyDetailsScreen() {
    selectFirstKey();
  }

  @Test
  public void testKeyDetailsShowPubKey() {
    selectFirstKey();
    NodeKeyDetails keyDetails = addPrivateKeyToDatabaseRule.getNodeKeyDetails();
    onView(withId(R.id.btnShowPubKey)).check(matches(isDisplayed())).perform(click());
    onView(withText(TestGeneralUtil.replaceVersionInKey(keyDetails.getPublicKey())));
  }

  @Test
  public void testKeyDetailsCopyToClipBoard() {
    selectFirstKey();
    NodeKeyDetails details = addPrivateKeyToDatabaseRule.getNodeKeyDetails();
    onView(withId(R.id.btnCopyToClipboard)).check(matches(isDisplayed())).perform(click());
    onView(withText(getResString(R.string.copied))).inRoot(new ToastMatcher()).check(matches(isDisplayed()));
    checkClipboardText(TestGeneralUtil.replaceVersionInKey(details.getPublicKey()));
  }

  @Test
  public void testKeyDetailsShowPrivateKey() {
    selectFirstKey();
    onView(withId(R.id.btnShowPrKey)).check(matches(isDisplayed())).perform(click());
    onView(withText(getResString(R.string.see_backups_to_save_your_private_keys)))
        .inRoot(new ToastMatcher()).check(matches(isDisplayed()));
  }

  @Test
  public void testKeyDetailsCheckDetails() {
    selectFirstKey();
    NodeKeyDetails details = addPrivateKeyToDatabaseRule.getNodeKeyDetails();
    onView(withId(R.id.textViewKeyWords)).check(matches(withText(
        getHtmlString(getResString(R.string.template_key_words, details.getKeywords())))));

    onView(withId(R.id.textViewFingerprint)).check(matches(withText(
        getHtmlString(getResString(R.string.template_fingerprint,
            GeneralUtil.doSectionsInText(" ", details.getFingerprint(), 4))))));

    onView(withId(R.id.textViewLongId)).check(matches(withText(getResString(R.string.template_longid,
        details.getLongId()))));

    onView(withId(R.id.textViewDate)).check(matches(withText(
        getHtmlString(getResString(R.string.template_date,
            DateFormat.getMediumDateFormat(getTargetContext()).format(new Date(details.getCreated())))))));

    List<PgpContact> pgpContacts = details.getPgpContacts();
    ArrayList<String> emails = new ArrayList<>();

    for (PgpContact pgpContact : pgpContacts) {
      emails.add(pgpContact.getEmail());
    }

    onView(withId(R.id.textViewUsers)).check(matches(withText(getResString(R.string.template_users, TextUtils.join(
        ", ", emails)))));
  }

  @Test
  public void testKeyDetailsSavePubKeyToFileWhenFileIsNotExist() {
    selectFirstKey();
    NodeKeyDetails details = addPrivateKeyToDatabaseRule.getNodeKeyDetails();

    File file = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(Environment
        .DIRECTORY_DOCUMENTS), "0x" + details.getLongId() + ".asc");

    if (file.exists()) {
      file.delete();
    }

    Intent resultData = new Intent();
    resultData.setData(FileProvider.getUriForFile(getTargetContext(), Constants.FILE_PROVIDER_AUTHORITY, file));

    intending(allOf(hasAction(Intent.ACTION_CREATE_DOCUMENT),
        hasCategories(hasItem(equalTo(Intent.CATEGORY_OPENABLE))),
        hasType(Constants.MIME_TYPE_PGP_KEY)))
        .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData));

    onView(withId(R.id.btnSaveToFile)).check(matches(isDisplayed())).perform(click());
    onView(withText(getResString(R.string.saved))).inRoot(new ToastMatcher()).check(matches(isDisplayed()));
  }

  private void selectFirstKey() {
    onView(withId(R.id.recyclerViewKeys)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
  }
}
