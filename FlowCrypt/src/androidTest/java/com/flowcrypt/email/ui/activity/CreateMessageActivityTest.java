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
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.flowcrypt.email.R;
import com.flowcrypt.email.TestConstants;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.model.MessageEncryptionType;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.util.AccountDaoManager;
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

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasType;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withChild;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;

/**
 * A test for {@link CreateMessageActivity}
 *
 * @author Denis Bondarenko
 *         Date: 06.02.2018
 *         Time: 10:11
 *         E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class CreateMessageActivityTest extends BaseTest {

    private static final int ATTACHMENTS_COUNT = 3;
    private static final String EMAIL_SUBJECT = "Test subject";
    private static final String EMAIL_MESSAGE = "Test message";

    private static File[] attachments;

    private IntentsTestRule intentsTestRule = new IntentsTestRule<CreateMessageActivity>(CreateMessageActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            Context targetContext = InstrumentationRegistry.getTargetContext();
            AccountDao accountDao = AccountDaoManager.getDefaultAccountDao();
            AccountDaoSource accountDaoSource = new AccountDaoSource();
            try {
                accountDaoSource.addRow(targetContext, accountDao.getAuthCredentials());
            } catch (Exception e) {
                e.printStackTrace();
            }

            return CreateMessageActivity.generateIntent(targetContext, null,
                    MessageEncryptionType.ENCRYPTED);
        }
    };

    @Rule
    public TestRule ruleChain = RuleChain
            .outerRule(new ClearAppSettingsRule())
            .around(intentsTestRule);

    @BeforeClass
    public static void setUp() {
        createFilesForAttachments();
    }

    @AfterClass
    public static void cleanResources() {
        TestGeneralUtil.deleteFiles(Arrays.asList(attachments));
    }

    @Test
    public void testEmptyRecipient() {
        onView(withId(R.id.editTextRecipientTo)).check(matches(isDisplayed())).check(matches(withText(isEmptyString()
        )));
        onView(withId(R.id.menuActionSend)).check(matches(isDisplayed())).perform(click());
        onView(withText(InstrumentationRegistry.getTargetContext().getString(R.string.text_must_not_be_empty,
                InstrumentationRegistry.getTargetContext().getString(R.string.prompt_recipients_to)))).check(matches
                (isDisplayed()));
    }

    @Test
    public void testEmptyEmailSubject() {
        onView(withId(R.id.editTextRecipientTo)).check(matches(isDisplayed())).perform(typeText
                (TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER));
        onView(withId(R.id.editTextEmailSubject)).check(matches(isDisplayed())).perform(scrollTo(), typeText
                ("subject"), clearText()).check(matches(withText(isEmptyString())));
        onView(withId(R.id.menuActionSend)).check(matches(isDisplayed())).perform(click());
        onView(withText(InstrumentationRegistry.getTargetContext().getString(R.string.text_must_not_be_empty,
                InstrumentationRegistry.getTargetContext().getString(R.string.prompt_subject))))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testEmptyEmailMessage() {
        onView(withId(R.id.editTextRecipientTo)).check(matches(isDisplayed())).perform(typeText
                (TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER));
        onView(withId(R.id.editTextEmailSubject)).check(matches(isDisplayed())).perform(typeText(EMAIL_SUBJECT));
        onView(withId(R.id.editTextEmailMessage)).check(matches(isDisplayed()))
                .check(matches(withText(isEmptyString())));
        onView(withId(R.id.menuActionSend)).check(matches(isDisplayed())).perform(click());
        onView(withText(InstrumentationRegistry.getTargetContext().getString(R.string.your_message_must_be_non_empty)))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testUsingStandardMessageEncryptionType() {
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());
        onView(withText(R.string.switch_to_standard_email)).check(matches(isDisplayed())).perform(click());
        checkIsDisplayedStandardAttributes();
    }

    @Test
    public void testUsingSecureMessageEncryptionType() {
        testUsingStandardMessageEncryptionType();
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());
        onView(withText(R.string.switch_to_secure_email)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.underToolbarTextTextView)).check(doesNotExist());
        onView(withId(R.id.appBarLayout)).check(matches(matchAppBarLayoutBackgroundColor(
                UIUtil.getColor(InstrumentationRegistry.getTargetContext(), R.color.colorPrimary))));
    }

    @Test
    public void testShowHelpScreen() {
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());
        onView(withText(R.string.help)).check(matches(isDisplayed())).perform(click());

        onView(withId(R.id.textViewAuthorHint)).check(matches(isDisplayed()))
                .check(matches(withText(R.string.i_will_usually_reply_within_an_hour_except_when_i_sleep_tom)));
        onView(withText(R.string.help_feedback_or_question)).check(matches(isDisplayed()));
    }

    @Test
    public void testIsScreenOfComposeNewMessage() {
        onView(withText(R.string.compose)).check(matches(isDisplayed()));
        onView(withId(R.id.editTextFrom)).check(matches(isDisplayed())).check(matches(withText(not(isEmptyString()))));
        onView(withId(R.id.editTextRecipientTo)).check(matches(isDisplayed())).check(matches(withText(isEmptyString()
        )));
        onView(withId(R.id.editTextEmailSubject)).check(matches(isDisplayed()))
                .check(matches(withText(isEmptyString())));
    }

    @Test
    public void testWrongFormatOfRecipientEmailAddress() throws Exception {
        String[] invalidEmailAddresses = {
                "test",
                "test@",
                "test@@denbond7.com",
                "@denbond7.com"};

        for (String invalidEmailAddress : invalidEmailAddresses) {
            onView(withId(R.id.editTextRecipientTo)).perform(clearText(), typeText(invalidEmailAddress),
                    closeSoftKeyboard());
            onView(withId(R.id.menuActionSend)).check(matches(isDisplayed())).perform(click());

            onView(withText(InstrumentationRegistry.getTargetContext().getString(R.string.error_some_email_is_not_valid,
                    invalidEmailAddress))).check(matches(isDisplayed()));
            onView(withId(android.support.design.R.id.snackbar_action)).check(matches(isDisplayed())).perform(click());
        }
    }

    @Test
    public void testShowMessageAboutUpdateRecipientInformation() {
        onView(withId(R.id.editTextEmailSubject)).check(matches(isDisplayed())).perform(typeText(EMAIL_SUBJECT),
                closeSoftKeyboard());
        onView(withId(R.id.editTextEmailMessage)).check(matches(isDisplayed())).perform(typeText(EMAIL_MESSAGE),
                closeSoftKeyboard());
        onView(withId(R.id.editTextRecipientTo)).check(matches(isDisplayed())).perform(typeText
                (TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER));
        onView(withId(R.id.menuActionSend)).check(matches(isDisplayed())).perform(click());

        onView(withText(InstrumentationRegistry.getTargetContext().getString(R.string
                .please_update_information_about_contacts))).check(matches(isDisplayed()));
        onView(withId(android.support.design.R.id.snackbar_action)).check(matches(isDisplayed())).perform(click());
    }

    @Test
    public void testAddingAttachments() {
        for (File attachment : attachments) {
            addAttachment(attachment);
        }
    }

    @Test
    public void testDeletingAttachments() {
        testAddingAttachments();
        for (File attachment : attachments) {
            deleteAttachment(attachment);
        }
        onView(withId(R.id.textViewAttchmentName)).check(doesNotExist());
    }

    @Test
    public void testSelectImportPublicKeyFromPopUp() throws IOException {
        fillInAllFields(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER);
        intending(hasComponent(new ComponentName(InstrumentationRegistry.getTargetContext(), ImportPublicKeyActivity
                .class))).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
        onView(withId(R.id.menuActionSend)).check(matches(isDisplayed())).perform(click());
        savePublicKeyInDatabase();
        onView(withText(R.string.import_their_public_key)).check(matches(isDisplayed())).perform(click());
    }

    @Test
    public void testSelectedStandardEncryptionTypeFromPopUp() {
        fillInAllFields(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER);
        onView(withId(R.id.menuActionSend)).check(matches(isDisplayed())).perform(click());
        onView(withText(R.string.switch_to_standard_email)).check(matches(isDisplayed())).perform(click());
        checkIsDisplayedStandardAttributes();
    }

    @Test
    public void testSelectedRemoveRecipientFromPopUp() {
        fillInAllFields(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER);
        onView(withId(R.id.menuActionSend)).check(matches(isDisplayed())).perform(click());
        onView(withText(InstrumentationRegistry.getTargetContext().getString(R.string.template_remove_recipient,
                TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER))).check(matches(isDisplayed())).perform(click
                ());
        onView(withId(R.id.editTextRecipientTo)).check(matches(isDisplayed())).check(matches(withText(not
                (containsString(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER)))));
    }

    @Test
    public void testSelectedCopyFromOtherContactFromPopUp() throws IOException {
        fillInAllFields(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER);
        Intent result = new Intent();
        result.putExtra(SelectContactsActivity.KEY_EXTRA_PGP_CONTACT, getPgpContact());
        intending(hasComponent(new ComponentName(InstrumentationRegistry.getTargetContext(), SelectContactsActivity
                .class))).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, result));
        onView(withId(R.id.menuActionSend)).check(matches(isDisplayed())).perform(click());
        onView(withText(R.string.copy_from_other_contact)).check(matches(isDisplayed())).perform(click());
        checkIsToastDisplayed(intentsTestRule.getActivity(), InstrumentationRegistry.getTargetContext().getString(R
                .string.key_successfully_copied));
    }

    private static void createFilesForAttachments() {
        attachments = new File[ATTACHMENTS_COUNT];
        for (int i = 0; i < attachments.length; i++) {
            attachments[i] = TestGeneralUtil.createFile(i + ".txt", "Text for filling the attached file");
        }
    }

    private void savePublicKeyInDatabase() throws IOException {
        ContactsDaoSource contactsDaoSource = new ContactsDaoSource();
        PgpContact pgpContact = getPgpContact();
        contactsDaoSource.addRow(InstrumentationRegistry.getTargetContext(), pgpContact);
    }

    @NonNull
    private PgpContact getPgpContact() throws IOException {
        String publicKey = TestGeneralUtil.readFileFromAssetsAsString(InstrumentationRegistry.getContext(),
                "pgp/not_attester_user@denbond7.com-pub.asc");
        return new PgpContact(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER, null, publicKey, true, null,
                false, null, null, null, 0);
    }

    private void deleteAttachment(File attachment) {
        onView(allOf(withId(R.id.imageButtonClearAttachment),
                hasSibling(withChild(withText(attachment.getName())))))
                .check(matches(isDisplayed()))
                .perform(click());
        onView(withText(attachment.getName())).check(doesNotExist());
    }

    private void addAttachment(File attachment) {
        Intent resultData = new Intent();
        resultData.setData(Uri.fromFile(attachment));
        intending(allOf(hasAction(Intent.ACTION_CHOOSER), hasExtra(is(Intent.EXTRA_INTENT), allOf(hasAction(Intent
                .ACTION_GET_CONTENT), hasType("*/*"))))).respondWith(new Instrumentation.ActivityResult(Activity
                .RESULT_OK, resultData));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.menuActionAttachFile)).check(matches(isDisplayed())).perform(click());
        onView(withText(attachment.getName())).check(matches(isDisplayed()));
    }

    private void checkIsDisplayedStandardAttributes() {
        onView(withId(R.id.underToolbarTextTextView)).check(matches(isDisplayed())).
                check(matches(withText(R.string.this_message_will_not_be_encrypted)));
        onView(withId(R.id.appBarLayout)).check(matches(matchAppBarLayoutBackgroundColor(UIUtil.getColor
                (InstrumentationRegistry.getTargetContext(), R.color.red))));
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