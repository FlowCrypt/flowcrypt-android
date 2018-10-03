/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.content.ContentValues;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.text.TextUtils;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.api.email.model.ServiceInfo;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.model.MessageEncryptionType;
import com.flowcrypt.email.model.MessageType;
import com.flowcrypt.email.rules.AddAccountToDatabaseRule;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.rules.UpdateAccountRule;
import com.flowcrypt.email.util.AccountDaoManager;
import com.flowcrypt.email.util.MessageUtil;
import com.flowcrypt.email.util.TestGeneralUtil;
import com.hootsuite.nachos.tokenizer.SpanChipTokenizer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isFocusable;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;

/**
 * This class tests a case when we want to send a reply with {@link ServiceInfo}
 *
 * @author Denis Bondarenko
 *         Date: 14.05.2018
 *         Time: 16:34
 *         E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class StandardReplyWithServiceInfoAndOneFileTest extends BaseTest {
    private static final String STRING = "Some short string";
    private ServiceInfo serviceInfo;
    private IncomingMessageInfo incomingMessageInfo;

    private IntentsTestRule intentsTestRule = new IntentsTestRule<CreateMessageActivity>(CreateMessageActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            try {
                incomingMessageInfo =
                        MessageUtil.getIncomingMessageInfoWithOutBody(
                                new Js(InstrumentationRegistry.getTargetContext(), null),
                                TestGeneralUtil.readFileFromAssetsAsString(InstrumentationRegistry.getContext(),
                                        "messages/mime_message.txt"));

                AttachmentInfo attachmentInfo = new AttachmentInfo();
                attachmentInfo.setName("test.txt");
                attachmentInfo.setEncodedSize(STRING.length());
                attachmentInfo.setRawData(STRING);
                attachmentInfo.setType("text/plain");
                attachmentInfo.setId(EmailUtil.generateContentId());
                attachmentInfo.setCanBeDeleted(false);

                List<AttachmentInfo> attachmentInfoList = new ArrayList<>();
                attachmentInfoList.add(attachmentInfo);

                serviceInfo = new ServiceInfo.Builder()
                        .setIsFromFieldEditEnable(false)
                        .setIsToFieldEditEnable(false)
                        .setIsSubjectEditEnable(false)
                        .setIsMessageTypeCanBeSwitched(false)
                        .setIsAddNewAttachmentsEnable(false)
                        .setSystemMessage(InstrumentationRegistry.getTargetContext()
                                .getString(R.string.message_was_encrypted_for_wrong_key))
                        .setAttachmentInfoList(attachmentInfoList)
                        .createServiceInfo();

                return CreateMessageActivity.generateIntent(InstrumentationRegistry.getTargetContext(),
                        incomingMessageInfo, MessageType.REPLY, MessageEncryptionType.STANDARD, serviceInfo);
            } catch (IOException e) {
                e.printStackTrace();
                throw new IllegalStateException(e);
            }
        }
    };

    @Rule
    public TestRule ruleChain = RuleChain
            .outerRule(new ClearAppSettingsRule())
            .around(new AddAccountToDatabaseRule())
            .around(new UpdateAccountRule(AccountDaoManager.getDefaultAccountDao(), generateContentValues()))
            .around(intentsTestRule);

    @Test
    public void testFrom() {
        onView(withId(R.id.editTextFrom)).perform(scrollTo()).check(matches(allOf(
                isDisplayed(), serviceInfo.isFromFieldEditEnable() ? isFocusable() : not(isFocusable()))));
    }

    @Test
    public void testToRecipients() {
        String chipSeparator = Character.toString(SpanChipTokenizer.CHIP_SPAN_SEPARATOR);
        String autoCorrectSeparator = Character.toString(SpanChipTokenizer.AUTOCORRECT_SEPARATOR);
        CharSequence textWithSeparator = autoCorrectSeparator
                + chipSeparator
                + incomingMessageInfo.getFrom().get(0)
                + chipSeparator
                + autoCorrectSeparator;

        onView(withId(R.id.editTextRecipientTo)).perform(scrollTo()).check(matches(allOf(
                isDisplayed(), withText(textWithSeparator.toString()),
                serviceInfo.isToFieldEditEnable() ? isFocusable() : not(isFocusable()))));
    }

    @Test
    public void testSubject() {
        onView(withId(R.id.editTextEmailSubject)).check(matches(allOf(
                isDisplayed(),
                serviceInfo.isSubjectEditEnable() ? isFocusable() : not(isFocusable()))));
    }

    @Test
    public void testEmailMessage() {
        onView(withId(R.id.editTextEmailMessage)).check(matches(
                allOf(isDisplayed(), TextUtils.isEmpty(serviceInfo.getSystemMessage())
                                ? withText(isEmptyString())
                                : withText(serviceInfo.getSystemMessage()),
                        serviceInfo.isMessageEditEnable() ? isFocusable() : not(isFocusable()))));

        if (serviceInfo.isMessageEditEnable()) {
            onView(withId(R.id.editTextEmailMessage)).perform(typeText(STRING));
        }
    }

    @Test
    public void testAvailabilityAddingAttachments() {
        if (!serviceInfo.isAddNewAttachmentsEnable()) {
            onView(withId(R.id.menuActionAttachFile)).check(doesNotExist());
        }
    }

    @Test
    public void testDisabledSwitchingBetweenEncryptionTypes() {
        if (!serviceInfo.isMessageTypeCanBeSwitched()) {
            onView(withText(R.string.switch_to_standard_email)).check(doesNotExist());
            onView(withText(R.string.switch_to_secure_email)).check(doesNotExist());
        }
    }

    @Test
    public void testShowHelpScreen() {
        testHelpScreen();
    }

    private ContentValues generateContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(AccountDaoSource.COL_IS_CONTACTS_LOADED, true);
        return contentValues;
    }
}
