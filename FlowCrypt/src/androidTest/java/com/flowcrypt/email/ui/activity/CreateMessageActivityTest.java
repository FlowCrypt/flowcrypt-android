/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;

import com.flowcrypt.email.R;
import com.flowcrypt.email.TestConstants;
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.model.MessageEncryptionType;
import com.flowcrypt.email.model.MessageType;
import com.flowcrypt.email.rules.AddAccountToDatabaseRule;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.util.TestGeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasCategories;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasType;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withChild;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;

/**
 * A test for {@link CreateMessageActivity}. By default, this test describes running an activity with type
 * {@link MessageType#NEW} and empty {@link IncomingMessageInfo}
 *
 * @author Denis Bondarenko
 * Date: 06.02.2018
 * Time: 10:11
 * E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class CreateMessageActivityTest extends BaseTest {

  private static final int ATTACHMENTS_COUNT = 3;
  private static final String EMAIL_SUBJECT = "Test subject";
  private static final String EMAIL_MESSAGE = "Test message";

  private static File[] atts;

  private ActivityTestRule activityTestRule = new ActivityTestRule<>(CreateMessageActivity.class, false, false);

  @Rule
  public TestRule ruleChain = RuleChain
      .outerRule(new ClearAppSettingsRule())
      .around(new AddAccountToDatabaseRule())
      .around(activityTestRule);

  @BeforeClass
  public static void setUp() {
    createFilesForAtts();
  }

  @AfterClass
  public static void cleanResources() {
    TestGeneralUtil.deleteFiles(Arrays.asList(atts));
  }

  public Intent getIntent() {
    return CreateMessageActivity.generateIntent(InstrumentationRegistry.getInstrumentation().getTargetContext(), null,
        MessageEncryptionType.ENCRYPTED);
  }

  public MessageEncryptionType getDefaultMsgEncryptionType() {
    return MessageEncryptionType.ENCRYPTED;
  }

  @Test
  public void testEmptyRecipient() {
    activityTestRule.launchActivity(getIntent());

    onView(withId(R.id.editTextRecipientTo)).check(matches(isDisplayed())).check(
        matches(withText(isEmptyString())));
    onView(withId(R.id.menuActionSend)).check(matches(isDisplayed())).perform(click());
    onView(withText(InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string
            .text_must_not_be_empty,
        InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string.prompt_recipients_to))))
        .check(matches(isDisplayed()));
  }

  @Test
  public void testEmptyEmailSubject() {
    activityTestRule.launchActivity(getIntent());

    onView(withId(R.id.editTextRecipientTo)).check(matches(isDisplayed())).perform(typeText
        (TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER));
    onView(withId(R.id.editTextEmailSubject)).check(matches(isDisplayed())).perform(scrollTo(), typeText
        ("subject"), clearText()).check(matches(withText(isEmptyString())));
    onView(withId(R.id.menuActionSend)).check(matches(isDisplayed())).perform(click());
    onView(withText(InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string
            .text_must_not_be_empty,
        InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string.prompt_subject))))
        .check(matches(isDisplayed()));
  }

  @Test
  public void testEmptyEmailMsg() {
    activityTestRule.launchActivity(getIntent());

    onView(withId(R.id.editTextRecipientTo)).check(matches(isDisplayed())).perform(typeText
        (TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER));
    onView(withId(R.id.editTextEmailSubject)).check(matches(isDisplayed())).perform(typeText(EMAIL_SUBJECT));
    onView(withId(R.id.editTextEmailMessage)).check(matches(isDisplayed()))
        .check(matches(withText(isEmptyString())));
    onView(withId(R.id.menuActionSend)).check(matches(isDisplayed())).perform(click());
    onView(withText(InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string
        .your_message_must_be_non_empty)))
        .check(matches(isDisplayed()));
  }

  @Test
  public void testUsingStandardMsgEncryptionType() {
    activityTestRule.launchActivity(getIntent());

    if (getDefaultMsgEncryptionType() != MessageEncryptionType.STANDARD) {
      openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().getTargetContext());
      onView(withText(R.string.switch_to_standard_email)).check(matches(isDisplayed())).perform(click());
    }

    checkIsDisplayedStandardAttributes();
  }

  @Test
  public void testUsingSecureMsgEncryptionType() {
    activityTestRule.launchActivity(getIntent());

    if (getDefaultMsgEncryptionType() != MessageEncryptionType.ENCRYPTED) {
      openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().getTargetContext());
      onView(withText(R.string.switch_to_secure_email)).check(matches(isDisplayed())).perform(click());
    }
    checkIsDisplayedEncryptedAttributes();
  }

  @Test
  public void testSwitchBetweenEncryptionTypes() {
    activityTestRule.launchActivity(getIntent());

    MessageEncryptionType messageEncryptionType = getDefaultMsgEncryptionType();

    if (messageEncryptionType == MessageEncryptionType.ENCRYPTED) {
      checkIsDisplayedEncryptedAttributes();
      openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().getTargetContext());
      onView(withText(R.string.switch_to_standard_email)).check(matches(isDisplayed())).perform(click());
      checkIsDisplayedStandardAttributes();
    } else {
      checkIsDisplayedStandardAttributes();
      openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().getTargetContext());
      onView(withText(R.string.switch_to_secure_email)).check(matches(isDisplayed())).perform(click());
      checkIsDisplayedEncryptedAttributes();
    }
  }

  @Test
  public void testShowHelpScreen() {
    activityTestRule.launchActivity(getIntent());

    openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().getTargetContext());
    onView(withText(R.string.help)).check(matches(isDisplayed())).perform(click());

    onView(withId(R.id.textViewAuthorHint)).check(matches(isDisplayed()))
        .check(matches(withText(R.string.i_will_usually_reply_within_an_hour_except_when_i_sleep_tom)));
    onView(withText(R.string.help_feedback_or_question)).check(matches(isDisplayed()));
  }

  @Test
  public void testIsScreenOfComposeNewMsg() {
    activityTestRule.launchActivity(getIntent());

    onView(withText(R.string.compose)).check(matches(isDisplayed()));
    onView(withId(R.id.editTextFrom)).check(matches(isDisplayed())).check(matches(withText(not(isEmptyString()))));
    onView(withId(R.id.editTextRecipientTo)).check(matches(isDisplayed())).check(matches(withText(isEmptyString()
    )));
    onView(withId(R.id.editTextEmailSubject)).check(matches(isDisplayed()))
        .check(matches(withText(isEmptyString())));
  }

  @Test
  public void testWrongFormatOfRecipientEmailAddress() {
    activityTestRule.launchActivity(getIntent());

    String[] invalidEmailAddresses = {
        "test",
        "test@",
        "test@@denbond7.com",
        "@denbond7.com"};

    for (String invalidEmailAddress : invalidEmailAddresses) {
      onView(withId(R.id.editTextRecipientTo)).perform(clearText(), typeText(invalidEmailAddress),
          closeSoftKeyboard());
      onView(withId(R.id.menuActionSend)).check(matches(isDisplayed())).perform(click());

      onView(withText(InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string
              .error_some_email_is_not_valid,
          invalidEmailAddress))).check(matches(isDisplayed()));
      onView(withId(com.google.android.material.R.id.snackbar_action)).check(matches(isDisplayed())).perform(click());
    }
  }

  @Test
  public void testShowMsgAboutUpdateRecipientInformation() {
    activityTestRule.launchActivity(getIntent());

    onView(withId(R.id.editTextEmailSubject)).check(matches(isDisplayed())).perform(typeText(EMAIL_SUBJECT),
        closeSoftKeyboard());
    onView(withId(R.id.editTextEmailMessage)).check(matches(isDisplayed())).perform(typeText(EMAIL_MESSAGE),
        closeSoftKeyboard());
    onView(withId(R.id.editTextRecipientTo)).check(matches(isDisplayed())).perform(typeText
        (TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER));
    onView(withId(R.id.menuActionSend)).check(matches(isDisplayed())).perform(click());

    onView(withText(InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string
        .please_update_information_about_contacts))).check(matches(isDisplayed()));
    onView(withId(com.google.android.material.R.id.snackbar_action)).check(matches(isDisplayed())).perform(click());
  }

  @Test
  public void testAddingAtts() {
    Intents.init();
    activityTestRule.launchActivity(getIntent());

    for (File att : atts) {
      addAtt(att);
    }
    Intents.release();
  }

  @Test
  public void testDeletingAtts() {
    Intents.init();
    activityTestRule.launchActivity(getIntent());

    for (File att : atts) {
      addAtt(att);
    }

    for (File att : atts) {
      deleteAtt(att);
    }

    onView(withId(R.id.textViewAttchmentName)).check(doesNotExist());
    Intents.release();
  }

  @Test
  public void testSelectImportPublicKeyFromPopUp() throws IOException {
    Intents.init();
    activityTestRule.launchActivity(getIntent());

    fillInAllFields(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER);
    intending(hasComponent(new ComponentName(InstrumentationRegistry.getInstrumentation().getTargetContext(),
        ImportPublicKeyActivity.class)))
        .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
    onView(withId(R.id.menuActionSend)).check(matches(isDisplayed())).perform(click());
    savePublicKeyInDatabase();
    onView(withText(R.string.import_their_public_key)).check(matches(isDisplayed())).perform(click());
    Intents.release();
  }

  @Test
  public void testSelectedStandardEncryptionTypeFromPopUp() {
    activityTestRule.launchActivity(getIntent());

    fillInAllFields(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER);
    onView(withId(R.id.menuActionSend)).check(matches(isDisplayed())).perform(click());
    onView(withText(R.string.switch_to_standard_email)).check(matches(isDisplayed())).perform(click());
    checkIsDisplayedStandardAttributes();
  }

  @Test
  public void testSelectedRemoveRecipientFromPopUp() {
    activityTestRule.launchActivity(getIntent());

    onView(withId(R.id.editTextRecipientTo)).check(matches(isDisplayed()))
        .perform(typeText(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER), closeSoftKeyboard());
    //move the focus to the next view
    onView(withId(R.id.editTextEmailMessage)).check(matches(isDisplayed()))
        .perform(typeText(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER), closeSoftKeyboard());
    onView(withId(R.id.menuActionSend)).check(matches(isDisplayed())).perform(click());
    onView(withText(InstrumentationRegistry.getInstrumentation().getTargetContext().getString(
        R.string.template_remove_recipient, TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER))).check(
        matches(isDisplayed())).perform(click());
    onView(withId(R.id.editTextRecipientTo)).check(matches(isDisplayed())).check(matches(withText(not
        (containsString(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER)))));
  }

  @Test
  public void testSelectedCopyFromOtherContactFromPopUp() throws IOException {
    Intents.init();
    activityTestRule.launchActivity(getIntent());

    fillInAllFields(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER);
    Intent result = new Intent();
    result.putExtra(SelectContactsActivity.KEY_EXTRA_PGP_CONTACT, getPgpContact());
    intending(hasComponent(new ComponentName(InstrumentationRegistry.getInstrumentation().getTargetContext(),
        SelectContactsActivity.class))).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, result));
    onView(withId(R.id.menuActionSend)).check(matches(isDisplayed())).perform(click());
    onView(withText(R.string.copy_from_other_contact)).check(matches(isDisplayed())).perform(click());
    checkIsToastDisplayed(activityTestRule.getActivity(), InstrumentationRegistry.getInstrumentation()
        .getTargetContext().getString(R.string.key_successfully_copied));
    Intents.release();
  }

  private static void createFilesForAtts() {
    atts = new File[ATTACHMENTS_COUNT];
    for (int i = 0; i < atts.length; i++) {
      atts[i] = TestGeneralUtil.createFile(i + ".txt", "Text for filling the attached file");
    }
  }

  private void checkIsDisplayedEncryptedAttributes() {
    onView(withId(R.id.underToolbarTextTextView)).check(doesNotExist());
    onView(withId(R.id.appBarLayout)).check(matches(matchAppBarLayoutBackgroundColor(
        UIUtil.getColor(InstrumentationRegistry.getInstrumentation().getTargetContext(), R.color.colorPrimary))));
  }

  private void savePublicKeyInDatabase() throws IOException {
    ContactsDaoSource contactsDaoSource = new ContactsDaoSource();
    PgpContact pgpContact = getPgpContact();
    contactsDaoSource.addRow(InstrumentationRegistry.getInstrumentation().getTargetContext(), pgpContact);
  }

  @NonNull
  private PgpContact getPgpContact() throws IOException {
    String publicKey = TestGeneralUtil.readFileFromAssetsAsString(InstrumentationRegistry.getInstrumentation()
        .getContext(), "pgp/not_attester_user@denbond7.com-pub.asc");
    return new PgpContact(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER, null, publicKey, true, null,
        false, null, null, null, 0);
  }

  private void deleteAtt(File att) {
    onView(allOf(withId(R.id.imageButtonClearAtt),
        hasSibling(withChild(withText(att.getName())))))
        .check(matches(isDisplayed()))
        .perform(click());
    onView(withText(att.getName())).check(doesNotExist());
  }

  private void addAtt(File att) {
    Intent resultData = new Intent();
    resultData.setData(Uri.fromFile(att));
    intending(allOf(hasAction(Intent.ACTION_CHOOSER),
        hasExtra(is(Intent.EXTRA_INTENT),
            allOf(hasAction(Intent.ACTION_OPEN_DOCUMENT),
                hasType("*/*"),
                hasCategories(hasItem(equalTo(Intent.CATEGORY_OPENABLE)))))))
        .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData));
    Espresso.closeSoftKeyboard();
    onView(withId(R.id.menuActionAttachFile)).check(matches(isDisplayed())).perform(click());
    onView(withText(att.getName())).check(matches(isDisplayed()));
  }

  private void checkIsDisplayedStandardAttributes() {
    onView(withId(R.id.underToolbarTextTextView)).check(matches(isDisplayed())).
        check(matches(withText(R.string.this_message_will_not_be_encrypted)));
    onView(withId(R.id.appBarLayout)).check(matches(matchAppBarLayoutBackgroundColor(UIUtil.getColor
        (InstrumentationRegistry.getInstrumentation().getTargetContext(), R.color.red))));
  }

  private void fillInAllFields(String recipient) {
    onView(withId(R.id.editTextRecipientTo)).check(matches(isDisplayed()))
        .perform(typeText(recipient), closeSoftKeyboard());
    onView(withId(R.id.editTextEmailSubject)).check(matches(isDisplayed()))
        .perform(typeText(EMAIL_SUBJECT), closeSoftKeyboard());
    onView(withId(R.id.editTextEmailMessage)).check(matches(isDisplayed()))
        .perform(typeText(EMAIL_SUBJECT), closeSoftKeyboard());
  }
}
