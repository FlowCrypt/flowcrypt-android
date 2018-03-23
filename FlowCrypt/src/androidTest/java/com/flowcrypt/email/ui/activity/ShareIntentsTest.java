/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;

import com.flowcrypt.email.R;
import com.flowcrypt.email.TestConstants;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.rules.AddAccountToDatabaseRule;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.util.TestGeneralUtil;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

import static android.support.test.espresso.Espresso.closeSoftKeyboard;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;

/**
 * @author Denis Bondarenko
 *         Date: 22.03.2018
 *         Time: 08:55
 *         E-mail: DenBond7@gmail.com
 */

public class ShareIntentsTest extends BaseTest {
    private static final int ATTACHMENTS_COUNT = 3;
    private static final String ENCODED_SUBJECT = "some%20subject";
    private static final String ENCODED_BODY = "some%20body";

    private static File[] attachments;
    private static String[] recipients;

    private ActivityTestRule activityTestRule = new ActivityTestRule<>
            (CreateMessageActivity.class, false, false);

    @Rule
    public TestRule ruleChain = RuleChain
            .outerRule(new ClearAppSettingsRule())
            .around(new AddAccountToDatabaseRule())
            .around(activityTestRule);

    @BeforeClass
    public static void setUp() {
        createFilesForAttachments();
        recipients = new String[]{
                TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER,
                TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER};
    }

    @AfterClass
    public static void cleanResources() {
        TestGeneralUtil.deleteFiles(Arrays.asList(attachments));
    }

    @Test
    public void testEmptyUri() {
        activityTestRule.launchActivity(generateIntentForUri(getRandomActionForRFC6068(), null));
        checkViewsOnScreen(0, null, null, 0);
    }

    @Test
    public void testTo_Subject_Body() {
        activityTestRule.launchActivity(generateIntentForUri(getRandomActionForRFC6068(),
                "mailto:" + recipients[0] + "?subject=" + ENCODED_SUBJECT + "&body=" + ENCODED_BODY));
        checkViewsOnScreen(1, ENCODED_SUBJECT, ENCODED_BODY, 0);
    }

    @Test
    public void testToParam_Subject_Body() {
        activityTestRule.launchActivity(generateIntentForUri(getRandomActionForRFC6068(),
                "mailto:?to=" + recipients[0] + "&subject=" + ENCODED_SUBJECT + "&body=" + ENCODED_BODY));
        checkViewsOnScreen(1, ENCODED_SUBJECT, ENCODED_BODY, 0);
    }

    @Test
    public void testTo_ToParam_Subject_Body() {
        activityTestRule.launchActivity(generateIntentForUri(getRandomActionForRFC6068(),
                "mailto:" + recipients[0] + "?to=" + recipients[1] + "&subject=" + ENCODED_SUBJECT + "&body=" +
                        ENCODED_BODY));
        checkViewsOnScreen(2, ENCODED_SUBJECT, ENCODED_BODY, 0);
    }

    @Test
    public void testToParam_To_Subject_Body() {
        activityTestRule.launchActivity(generateIntentForUri(getRandomActionForRFC6068(),
                "mailto:?to=" + recipients[0] + "," + recipients[1] + "&subject=" + ENCODED_SUBJECT + "&body=" +
                        ENCODED_BODY));
        checkViewsOnScreen(2, ENCODED_SUBJECT, ENCODED_BODY, 0);
    }

    @Test
    public void testMultiTo_Subject_Body() {
        activityTestRule.launchActivity(generateIntentForUri(getRandomActionForRFC6068(),
                "mailto:" + recipients[0] + "," + recipients[1] + "?subject=" + ENCODED_SUBJECT + "&body=" +
                        ENCODED_BODY));
        checkViewsOnScreen(2, ENCODED_SUBJECT, ENCODED_BODY, 0);
    }

    @Test
    public void testMultiToParam_Subject_Body() {
        activityTestRule.launchActivity(generateIntentForUri(getRandomActionForRFC6068(),
                "mailto:?to=" + recipients[0] + "&to=" + recipients[1] + "&subject=" + ENCODED_SUBJECT +
                        "&body=" + ENCODED_BODY));
        checkViewsOnScreen(2, ENCODED_SUBJECT, ENCODED_BODY, 0);
    }

    @Test
    public void testSendEmptyExtras() {
        activityTestRule.launchActivity(generateIntentWithExtras(Intent.ACTION_SEND, null,
                null, 0));
        checkViewsOnScreen(0, null, null, 0);
    }

    @Test
    public void testSend_ExtSubject() {
        activityTestRule.launchActivity(generateIntentWithExtras(Intent.ACTION_SEND, Intent.EXTRA_SUBJECT,
                null, 0));
        checkViewsOnScreen(0, Intent.EXTRA_SUBJECT, null, 0);
    }

    @Test
    public void testSend_ExtBody() {
        activityTestRule.launchActivity(generateIntentWithExtras(Intent.ACTION_SEND, null,
                Intent.EXTRA_TEXT, 0));
        checkViewsOnScreen(0, null, Intent.EXTRA_TEXT, 0);
    }

    @Test
    public void testSend_Att() {
        activityTestRule.launchActivity(generateIntentWithExtras(Intent.ACTION_SEND, null,
                null, 1));
        checkViewsOnScreen(0, null, null, 1);
    }

    @Test
    public void testSend_ExtSubject_ExtBody() {
        activityTestRule.launchActivity(generateIntentWithExtras(Intent.ACTION_SEND, Intent.EXTRA_SUBJECT,
                Intent.EXTRA_TEXT, 0));
        checkViewsOnScreen(0, Intent.EXTRA_SUBJECT, Intent.EXTRA_TEXT, 0);
    }

    @Test
    public void testSend_ExtSubject_Att() {
        activityTestRule.launchActivity(generateIntentWithExtras(Intent.ACTION_SEND, Intent.EXTRA_SUBJECT,
                null, 1));
        checkViewsOnScreen(0, Intent.EXTRA_SUBJECT, null, 1);
    }

    @Test
    public void testSend_ExtBody_Att() {
        activityTestRule.launchActivity(generateIntentWithExtras(Intent.ACTION_SEND, null,
                Intent.EXTRA_TEXT, 1));
        checkViewsOnScreen(0, null, Intent.EXTRA_TEXT, 1);
    }

    @Test
    public void testSend_ExtSubject_ExtBody_Att() {
        activityTestRule.launchActivity(generateIntentWithExtras(Intent.ACTION_SEND, Intent.EXTRA_SUBJECT,
                Intent.EXTRA_TEXT, 1));
        checkViewsOnScreen(0, Intent.EXTRA_SUBJECT, Intent.EXTRA_TEXT, 1);
    }

    @Test
    public void testSendMultiple_MultiAtt() {
        activityTestRule.launchActivity(generateIntentWithExtras(Intent.ACTION_SEND_MULTIPLE, null,
                null, attachments.length));
        checkViewsOnScreen(0, null, null, attachments.length);
    }

    @Test
    public void testSendMultiple_ExtSubject_ExtBody_MultiAtt() {
        activityTestRule.launchActivity(generateIntentWithExtras(Intent.ACTION_SEND_MULTIPLE, Intent.EXTRA_SUBJECT,
                Intent.EXTRA_TEXT, attachments.length));
        checkViewsOnScreen(0, Intent.EXTRA_SUBJECT, Intent.EXTRA_TEXT, attachments.length);
    }

    private static void createFilesForAttachments() {
        attachments = new File[ATTACHMENTS_COUNT];
        for (int i = 0; i < attachments.length; i++) {
            attachments[i] = TestGeneralUtil.createFile(i + ".txt", UUID.randomUUID().toString());
        }
    }

    private String getRandomActionForRFC6068() {
        return new Random().nextBoolean() ? Intent.ACTION_SENDTO : Intent.ACTION_VIEW;
    }

    private Intent generateIntentForUri(String action, String stringUri) {
        Context targetContext = InstrumentationRegistry.getTargetContext();
        Intent intent = new Intent(targetContext, CreateMessageActivity.class);

        if (action != null) {
            intent.setAction(action);
        }

        if (stringUri != null) {
            intent.setData(Uri.parse(stringUri));
        }
        return intent;
    }


    private Intent generateIntentWithExtras(String action, String extraSubject, CharSequence extraMessage,
                                            int attachmentsCount) {
        Context targetContext = InstrumentationRegistry.getTargetContext();
        Intent intent = new Intent(targetContext, CreateMessageActivity.class);

        if (action != null) {
            intent.setAction(action);
        }

        if (extraSubject != null) {
            intent.putExtra(Intent.EXTRA_SUBJECT, extraSubject);
        }

        if (extraMessage != null) {
            intent.putExtra(Intent.EXTRA_TEXT, extraMessage);
        }

        if (attachmentsCount > 0) {
            if (attachmentsCount == 1) {
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(attachments[1]));
            } else {
                ArrayList<Uri> urisFromAttachments = new ArrayList<>();
                for (File attachment : attachments) {
                    urisFromAttachments.add(Uri.fromFile(attachment));
                }
                intent.putExtra(Intent.EXTRA_STREAM, urisFromAttachments);
            }
        }
        return intent;
    }

    private void checkViewsOnScreen(int recipientsCount, String subject, CharSequence body, int attachmentsCount) {
        onView(withText(R.string.compose)).check(matches(isDisplayed()));
        onView(withId(R.id.editTextFrom)).check(matches(isDisplayed())).check(matches(withText(not(isEmptyString()))));
        closeSoftKeyboard();

        checkRecipients(recipientsCount);
        checkSubject(subject);
        checkBody(body);
        checkAttachments(attachmentsCount);
    }

    private void checkAttachments(int attachmentsCount) {
        if (attachmentsCount > 0) {
            if (attachmentsCount == 1) {
                onView(withText(attachments[1].getName())).check(matches(isDisplayed()));
            } else {
                for (File attachment : attachments) {
                    onView(withText(attachment.getName())).check(matches(isDisplayed()));
                }
            }
        }
    }

    private void checkBody(CharSequence body) {
        if (body != null) {
            onView(withId(R.id.editTextEmailMessage)).check(matches(isDisplayed()))
                    .check(matches(withText(getRidOfCharacterSubstitutes(body.toString()))));
        } else {
            onView(withId(R.id.editTextEmailMessage)).check(matches(isDisplayed()))
                    .check(matches(withText(isEmptyString())));
        }
    }

    private void checkSubject(String subject) {
        if (subject != null) {
            onView(withId(R.id.editTextEmailSubject)).check(matches(isDisplayed()))
                    .check(matches(withText(getRidOfCharacterSubstitutes(subject))));
        } else {
            onView(withId(R.id.editTextEmailSubject)).check(matches(isDisplayed()))
                    .check(matches(withText(isEmptyString())));
        }
    }

    private void checkRecipients(int recipientsCount) {
        if (recipientsCount > 0) {
            for (int i = 0; i < recipientsCount; i++) {
                onView(withId(R.id.editTextRecipient)).check(matches(isDisplayed()))
                        .check(matches(withText(containsString(recipients[i]))));
            }
        } else {
            onView(withId(R.id.editTextRecipient)).check(matches(isDisplayed()))
                    .check(matches(withText(isEmptyString())));
        }
    }

    private String getRidOfCharacterSubstitutes(String message) {
        try {
            return URLDecoder.decode(message, StandardCharsets.UTF_8.displayName());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return message;
        }
    }
}
