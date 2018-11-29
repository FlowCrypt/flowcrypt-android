/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.scenarios.setup;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.AuthCredentials;
import com.flowcrypt.email.api.email.model.SecurityType;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.ui.activity.SplashActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * The base test for testing the standard login using email provider settings.
 *
 * @author Denis Bondarenko
 * Date: 25.12.2017
 * Time: 16:49
 * E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public abstract class SignInWithStandardAuthTest extends BaseTest {

  @Rule
  public TestRule ruleChain = RuleChain
      .outerRule(new ClearAppSettingsRule())
      .around(new ActivityTestRule<>(SplashActivity.class));

  protected AuthCredentials authCredentials;

  abstract AuthCredentials getAuthCredentials();

  @Before
  public void setUp() throws Exception {
    this.authCredentials = getAuthCredentials();
  }

  protected void fillAllFields() {
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
    onView(withId(R.id.editTextImapPort)).perform(clearText(), typeText(String.valueOf(authCredentials
        .getImapPort())), closeSoftKeyboard());

    onView(withId(R.id.editTextSmtpServer)).perform(clearText(), typeText(authCredentials.getSmtpServer()),
        closeSoftKeyboard());
    onView(withId(R.id.spinnerSmtpSecyrityType)).perform(scrollTo(), click());
    onData(allOf(is(instanceOf(SecurityType.class)),
        matchOption(authCredentials.getSmtpSecurityTypeOption()))).perform(click());
    onView(withId(R.id.editTextSmtpPort)).perform(clearText(), typeText(String.valueOf(authCredentials
        .getSmtpPort())), closeSoftKeyboard());

    if (authCredentials.isUseCustomSignInForSmtp()) {
      onView(withId(R.id.checkBoxRequireSignInForSmtp)).perform(click());
      onView(withId(R.id.editTextSmtpUsername)).perform(clearText(),
          typeText(authCredentials.getSmtpSigInUsername()), closeSoftKeyboard());
      onView(withId(R.id.editTextSmtpPassword)).perform(clearText(),
          typeText(authCredentials.getSmtpSignInPassword()), closeSoftKeyboard());
    }
  }
}
