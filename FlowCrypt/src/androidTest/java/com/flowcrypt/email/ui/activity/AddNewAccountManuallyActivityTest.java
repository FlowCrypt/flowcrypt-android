/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import com.flowcrypt.email.R;
import com.flowcrypt.email.TestConstants;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.model.AuthCredentials;
import com.flowcrypt.email.api.email.model.SecurityType;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.util.AuthCredentialsManager;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.IdlingPolicies;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

/**
 * A test for {@link AddNewAccountManuallyActivity}
 *
 * @author Denis Bondarenko
 * Date: 01.02.2018
 * Time: 13:28
 * E-mail: DenBond7@gmail.com
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
    IdlingPolicies.setMasterPolicyTimeout(60, TimeUnit.SECONDS);
  }

  @Test
  public void testAllCredentialsCorrect() throws Exception {
    fillAllFields();
    onView(withId(R.id.buttonTryToConnect)).perform(scrollTo(), click());
    onView(withId(R.id.textViewTitle)).check(matches(isDisplayed()));
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
    checkSecurityTypeOption(R.id.editTextImapPort, R.id.spinnerImapSecurityType,
        SecurityType.Option.STARTLS, String.valueOf(JavaEmailConstants.DEFAULT_IMAP_PORT));
    checkSecurityTypeOption(R.id.editTextImapPort, R.id.spinnerImapSecurityType,
        SecurityType.Option.SSL_TLS, String.valueOf(JavaEmailConstants.SSL_IMAP_PORT));
    checkSecurityTypeOption(R.id.editTextImapPort, R.id.spinnerImapSecurityType,
        SecurityType.Option.NONE, String.valueOf(JavaEmailConstants.DEFAULT_IMAP_PORT));
  }

  @Test
  public void testChangingSmtpPortWhenSelectSpinnerItem() throws Exception {
    checkSecurityTypeOption(R.id.editTextSmtpPort, R.id.spinnerSmtpSecyrityType,
        SecurityType.Option.STARTLS, String.valueOf(JavaEmailConstants.STARTTLS_SMTP_PORT));
    checkSecurityTypeOption(R.id.editTextSmtpPort, R.id.spinnerSmtpSecyrityType,
        SecurityType.Option.SSL_TLS, String.valueOf(JavaEmailConstants.SSL_SMTP_PORT));
    checkSecurityTypeOption(R.id.editTextSmtpPort, R.id.spinnerSmtpSecyrityType,
        SecurityType.Option.NONE, String.valueOf(JavaEmailConstants.DEFAULT_SMTP_PORT));
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
      onView(withId(com.google.android.material.R.id.snackbar_action)).check(matches(isDisplayed())).perform(click());
    }
  }

  @Ignore//todo-denbond7 need to think about it
  @Test
  public void testShowWarningIfAuthFail() {
    IdlingPolicies.setMasterPolicyTimeout(5, TimeUnit.MINUTES);
    fillAllFields();
    String someFailTextToChangeRightValue = "123";

    int[] fieldIdentifiersWithIncorrectData = {R.id.editTextEmail,
        R.id.editTextUserName, R.id.editTextPassword,
        R.id.editTextImapServer, R.id.editTextImapPort,
        R.id.editTextSmtpServer, R.id.editTextSmtpPort,
        R.id.editTextSmtpUsername, R.id.editTextSmtpPassword};

    String[] correctData = {authCredentials.getEmail(),
        authCredentials.getUsername(), authCredentials.getPassword(),
        authCredentials.getImapServer(), String.valueOf(authCredentials.getImapPort()),
        authCredentials.getSmtpServer(), String.valueOf(authCredentials.getSmtpPort()),
        authCredentials.getSmtpSigInUsername(), authCredentials.getSmtpSignInPassword()};

    int numberOfChecks = authCredentials.isUseCustomSignInForSmtp() ?
        fieldIdentifiersWithIncorrectData.length : fieldIdentifiersWithIncorrectData.length - 2;

    for (int i = 0; i < numberOfChecks; i++) {
      onView(withId(fieldIdentifiersWithIncorrectData[i])).perform(scrollTo(),
          typeText(someFailTextToChangeRightValue), closeSoftKeyboard());
      onView(withId(R.id.buttonTryToConnect)).perform(scrollTo(), click());

      onView(anyOf(withText(startsWith(TestConstants.IMAP)), withText(startsWith(TestConstants.SMTP))))
          .check(matches(isDisplayed()));
      onView(withId(com.google.android.material.R.id.snackbar_action)).check(matches(isDisplayed()))
          .perform(click());

      onView(withId(fieldIdentifiersWithIncorrectData[i])).perform(scrollTo(), clearText(),
          typeText(correctData[i]), closeSoftKeyboard());
    }
  }

  private void checkSecurityTypeOption(int portViewId, int spinnerViewId, SecurityType.Option option,
                                       String portValue) {
    String someValue = "111";
    onView(withId(portViewId)).perform(scrollTo(), clearText(), typeText(someValue), closeSoftKeyboard());
    onView(withId(spinnerViewId)).perform(scrollTo(), click());
    onData(allOf(is(instanceOf(SecurityType.class)), matchOption(option))).perform(click());
    onView(withId(portViewId)).check(matches(withText(portValue)));
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
    onView(withId(com.google.android.material.R.id.snackbar_action)).check(matches(isDisplayed())).perform(click());
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