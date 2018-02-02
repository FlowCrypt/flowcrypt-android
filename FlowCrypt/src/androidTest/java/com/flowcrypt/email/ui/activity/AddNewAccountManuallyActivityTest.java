/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.flowcrypt.email.R;
import com.flowcrypt.email.TestConstants;
import com.flowcrypt.email.api.email.model.AuthCredentials;
import com.flowcrypt.email.api.email.model.SecurityType;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.util.AuthCredentialsManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;

/**
 * A test for {@link AddNewAccountManuallyActivity}
 *
 * @author Denis Bondarenko
 *         Date: 01.02.2018
 *         Time: 13:28
 *         E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class AddNewAccountManuallyActivityTest extends BaseTest {

    private static final String IMAP_SERVER_PREFIX = "imap.";
    private static final String SMTP_SERVER_PREFIX = "smtp.";

    @Rule
    public TestRule ruleChain = RuleChain
            .outerRule(new ClearAppSettingsRule())
            .around(new ActivityTestRule<>(AddNewAccountManuallyActivity.class));

    private AuthCredentials authCredentials;

    @Before
    public void setUp() throws Exception {
        this.authCredentials = AuthCredentialsManager.getDefaultWithBackupAuthCredentials();
    }

    @Test
    public void testAllCredentialsCorrect() throws Exception {
        fillAllFields();
        onView(withId(R.id.buttonTryToConnect)).perform(scrollTo(), click());
        onView(withId(R.id.textViewSetupFlowCrypt)).check(matches(isDisplayed()));
    }

    @Test
    public void testShowSnackBarIfFieldEmpty() throws Exception {
        checkIsFieldEmptyWork(R.id.editTextEmail, R.string.e_mail);
        checkIsFieldEmptyWork(R.id.editTextUserName, R.string.username);
        checkIsFieldEmptyWork(R.id.editTextPassword, R.string.password);

        checkIsFieldEmptyWork(R.id.editTextImapServer, R.string.imap_server);
        checkIsFieldEmptyWork(R.id.editTextImapPort, R.string.imap_port);
        onView(withId(R.id.editTextImapPort)).perform(scrollTo(),
                typeText(String.valueOf(authCredentials.getImapPort())));

        checkIsFieldEmptyWork(R.id.editTextSmtpServer, R.string.smtp_server);
        checkIsFieldEmptyWork(R.id.editTextSmtpPort, R.string.smtp_port);
        onView(withId(R.id.editTextSmtpPort)).perform(scrollTo(),
                typeText(String.valueOf(authCredentials.getSmtpPort())));

        onView(withId(R.id.checkBoxRequireSignInForSmtp)).perform(scrollTo(), click());
        checkIsFieldEmptyWork(R.id.editTextSmtpUsername, R.string.smtp_username);
        checkIsFieldEmptyWork(R.id.editTextSmtpPassword, R.string.smtp_password);
    }

    @Test
    public void testIsPasswordFieldsAlwaysEmptyAtStart() throws Exception {
        onView(withId(R.id.editTextPassword)).check(matches(withText(isEmptyString())));
        onView(withId(R.id.checkBoxRequireSignInForSmtp)).perform(scrollTo(), click());
        onView(withId(R.id.editTextSmtpPassword)).check(matches(withText(isEmptyString())));
    }

    @Test
    public void testChangingImapPortWhenSelectSpinnerItem() throws Exception {
        String oldValueOfImapPort = String.valueOf(authCredentials.getImapPort());
        SecurityType.Option oldImapOption = authCredentials.getImapSecurityTypeOption();

        onView(withId(R.id.editTextImapPort)).perform(scrollTo(), clearText(), typeText(oldValueOfImapPort),
                closeSoftKeyboard());
        onView(withId(R.id.spinnerImapSecurityType)).perform(scrollTo(), click());
        onData(allOf(is(instanceOf(SecurityType.class)), matchOption(oldImapOption))).perform(click());
        onView(withId(R.id.spinnerImapSecurityType)).perform(scrollTo(), click());
        onData(instanceOf(SecurityType.class)).atPosition(getSpinnerItemPosition(oldImapOption)).perform(click());
        onView(withId(R.id.editTextImapPort)).check(matches(not(withText(oldValueOfImapPort))));
    }

    @Test
    public void testChangingSmtpPortWhenSelectSpinnerItem() throws Exception {
        String oldValueOfSmtpPort = String.valueOf(authCredentials.getSmtpPort());
        SecurityType.Option oldSmtpOption = authCredentials.getSmtpSecurityTypeOption();

        onView(withId(R.id.editTextSmtpPort)).perform(scrollTo(), clearText(), typeText(oldValueOfSmtpPort),
                closeSoftKeyboard());
        onView(withId(R.id.spinnerSmtpSecyrityType)).perform(scrollTo(), click());
        onData(allOf(is(instanceOf(SecurityType.class)), matchOption(oldSmtpOption))).perform(click());
        onView(withId(R.id.spinnerSmtpSecyrityType)).perform(scrollTo(), click());
        onData(instanceOf(SecurityType.class)).atPosition(getSpinnerItemPosition(oldSmtpOption)).perform(click());
        onView(withId(R.id.editTextSmtpPort)).check(matches(not(withText(oldValueOfSmtpPort))));
    }

    @Test
    public void testChangeFieldValuesWhenEmailChanged() {
        onView(withId(R.id.editTextEmail)).perform(clearText(), typeText(authCredentials.getEmail()),
                closeSoftKeyboard());
        onView(withId(R.id.editTextUserName)).perform(clearText(), typeText(authCredentials.getUsername()),
                closeSoftKeyboard());
        onView(withId(R.id.editTextImapServer)).perform(clearText(), typeText(authCredentials.getImapServer()),
                closeSoftKeyboard());
        onView(withId(R.id.editTextSmtpServer)).perform(clearText(), typeText(authCredentials.getSmtpServer()),
                closeSoftKeyboard());

        String newUserName = "test";
        String newHost = "test.com";

        onView(withId(R.id.editTextEmail)).perform(scrollTo(), clearText(), typeText(newUserName
                + TestConstants.COMMERCIAL_AT_SYMBOL + newHost), closeSoftKeyboard());
        onView(withId(R.id.editTextUserName)).perform(scrollTo()).check(matches(not(withText(authCredentials
                .getUsername()))));
        onView(withId(R.id.editTextUserName)).check(matches(withText(newUserName)));
        onView(withId(R.id.editTextImapServer)).perform(scrollTo()).check(matches(not(withText(authCredentials
                .getImapServer()))));
        onView(withId(R.id.editTextImapServer)).check(matches(withText(IMAP_SERVER_PREFIX + newHost)));
        onView(withId(R.id.editTextSmtpServer)).perform(scrollTo()).check(matches(not(withText(authCredentials
                .getSmtpServer()))));
        onView(withId(R.id.editTextSmtpServer)).check(matches(withText(SMTP_SERVER_PREFIX + newHost)));
        onView(withId(R.id.checkBoxRequireSignInForSmtp)).perform(scrollTo(), click());
        onView(withId(R.id.editTextSmtpUsername)).perform(scrollTo()).check(matches(withText(newUserName)));
    }

    @Test
    public void testVisibilityOfSmtpAuthField() {
        onView(withId(R.id.checkBoxRequireSignInForSmtp)).perform(scrollTo(), click());
        onView(withId(R.id.editTextSmtpUsername)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.editTextSmtpPassword)).perform(scrollTo()).check(matches(isDisplayed()))
                .check(matches(withText(isEmptyString())));

        onView(withId(R.id.checkBoxRequireSignInForSmtp)).perform(scrollTo(), click());
        onView(withId(R.id.editTextSmtpUsername)).check(matches(not(isDisplayed())));
        onView(withId(R.id.editTextSmtpPassword)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testFieldsAutoFilling() {
        String userName = authCredentials.getEmail().substring(0,
                authCredentials.getEmail().indexOf(TestConstants.COMMERCIAL_AT_SYMBOL));
        String host = authCredentials.getEmail().substring(authCredentials.getEmail().indexOf(TestConstants
                .COMMERCIAL_AT_SYMBOL) + 1, authCredentials.getEmail().length());

        String[] incorrectEmailAddresses = {"default",
                "default@",
                "default@denbond7",
                "default@denbond7.",
        };

        for (String invalidEmailAddress : incorrectEmailAddresses) {
            onView(withId(R.id.editTextEmail)).perform(scrollTo(), clearText(), typeText(invalidEmailAddress),
                    closeSoftKeyboard());
            onView(withId(R.id.editTextUserName)).perform(scrollTo()).check(matches(withText(isEmptyString())));
            onView(withId(R.id.editTextImapServer)).perform(scrollTo()).check(matches(withText(isEmptyString())));
            onView(withId(R.id.editTextSmtpServer)).perform(scrollTo()).check(matches(withText(isEmptyString())));
        }

        onView(withId(R.id.editTextEmail)).perform(scrollTo(), clearText(),
                typeText(userName + TestConstants.COMMERCIAL_AT_SYMBOL + host), closeSoftKeyboard());
        onView(withId(R.id.editTextUserName)).perform(scrollTo()).check(matches(withText(userName)));
        onView(withId(R.id.editTextImapServer)).perform(scrollTo()).check(matches(withText(IMAP_SERVER_PREFIX + host)));
        onView(withId(R.id.editTextSmtpServer)).perform(scrollTo()).check(matches(withText(SMTP_SERVER_PREFIX + host)));
    }

    @Test
    public void testWrongFormatOfEmailAddress() throws Exception {
        fillAllFields();

        String[] invalidEmailAddresses = {
                "default",
                "default@",
                "default@@denbond7.com",
                "@denbond7.com",
                "    "};

        for (String invalidEmailAddress : invalidEmailAddresses) {
            onView(withId(R.id.editTextEmail)).perform(scrollTo(), clearText(),
                    typeText(invalidEmailAddress), closeSoftKeyboard());
            onView(withId(R.id.buttonTryToConnect)).perform(scrollTo(), click());
            onView(withText(InstrumentationRegistry.getTargetContext().getString(R.string.error_email_is_not_valid)))
                    .check(matches(isDisplayed()));
            onView(withId(android.support.design.R.id.snackbar_action)).check(matches(isDisplayed())).perform(click());
        }
    }

    private int getSpinnerItemPosition(SecurityType.Option option) {
        if (option == SecurityType.Option.SSL_TLS) {
            return 0;
        } else {
            return 1;
        }
    }

    private void checkIsFieldEmptyWork(int viewId, int stringIdForError) {
        onView(withId(R.id.editTextEmail)).perform(scrollTo(), clearText(), typeText(authCredentials.getEmail()),
                closeSoftKeyboard());
        onView(withId(R.id.editTextPassword)).perform(clearText(), typeText(authCredentials.getPassword()),
                closeSoftKeyboard());

        onView(withId(viewId)).perform(scrollTo(), clearText());
        onView(withId(R.id.buttonTryToConnect)).perform(scrollTo(), click());

        onView(withText(InstrumentationRegistry.getTargetContext().getString(R.string.text_must_not_be_empty,
                InstrumentationRegistry.getTargetContext().getString(stringIdForError)))).check(matches(isDisplayed()));
        onView(withId(android.support.design.R.id.snackbar_action)).check(matches(isDisplayed())).perform(click());
    }

    private void fillAllFields() {
        onView(withId(R.id.editTextEmail)).perform(clearText(), typeText(authCredentials.getEmail()),
                closeSoftKeyboard());
        onView(withId(R.id.editTextUserName)).perform(clearText(), typeText(authCredentials.getUsername()),
                closeSoftKeyboard());
        onView(withId(R.id.editTextPassword)).perform(clearText(), typeText(authCredentials.getPassword()),
                closeSoftKeyboard());

        onView(withId(R.id.editTextImapServer)).perform(clearText(), typeText(authCredentials.getImapServer()),
                closeSoftKeyboard());
        onView(withId(R.id.spinnerImapSecurityType)).perform(scrollTo(), click());
        onData(allOf(is(instanceOf(SecurityType.class)),
                matchOption(authCredentials.getImapSecurityTypeOption()))).perform(click());
        onView(withId(R.id.editTextImapPort)).perform(clearText(),
                typeText(String.valueOf(authCredentials.getImapPort())), closeSoftKeyboard());

        onView(withId(R.id.editTextSmtpServer)).perform(clearText(), typeText(authCredentials.getSmtpServer()),
                closeSoftKeyboard());
        onView(withId(R.id.spinnerSmtpSecyrityType)).perform(scrollTo(), click());
        onData(allOf(is(instanceOf(SecurityType.class)),
                matchOption(authCredentials.getSmtpSecurityTypeOption()))).perform(click());
        onView(withId(R.id.editTextSmtpPort)).perform(clearText(),
                typeText(String.valueOf(authCredentials.getSmtpPort())), closeSoftKeyboard());

        if (authCredentials.isUseCustomSignInForSmtp()) {
            onView(withId(R.id.checkBoxRequireSignInForSmtp)).perform(click());
            onView(withId(R.id.editTextSmtpUsername)).perform(clearText(),
                    typeText(authCredentials.getSmtpSigInUsername()), closeSoftKeyboard());
            onView(withId(R.id.editTextSmtpPassword)).perform(clearText(),
                    typeText(authCredentials.getSmtpSignInPassword()), closeSoftKeyboard());
        }
    }
}