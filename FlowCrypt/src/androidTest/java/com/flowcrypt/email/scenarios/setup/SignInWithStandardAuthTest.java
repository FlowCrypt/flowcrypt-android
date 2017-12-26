/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.scenarios.setup;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.AuthCredentials;
import com.flowcrypt.email.api.email.model.SecurityType;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.ui.activity.SplashActivity;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * The base test for testing the standard login using email provider settings.
 *
 * @author Denis Bondarenko
 *         Date: 25.12.2017
 *         Time: 16:49
 *         E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public abstract class SignInWithStandardAuthTest extends BaseTest {

    @Rule
    public ActivityTestRule<SplashActivity> activityTestRule = new ActivityTestRule<>(SplashActivity.class);

    private AuthCredentials authCredentials;

    abstract AuthCredentials getAuthCredentials();

    /**
     * Match the {@link SecurityType.Option}.
     *
     * @param option An input {@link SecurityType.Option}.
     */
    public static <T> Matcher<T> matchOption(final SecurityType.Option option) {
        return new BaseMatcher<T>() {
            @Override
            public boolean matches(Object item) {
                if (item instanceof SecurityType) {
                    SecurityType securityType = (SecurityType) item;
                    return securityType.getOption() == option;
                } else {
                    return false;
                }

            }

            @Override
            public void describeTo(Description description) {
                description.appendText("The input option = " + option);
            }
        };
    }

    @Before
    public void setUp() throws Exception {
        this.authCredentials = getAuthCredentials();
        clearApp(authCredentials.getEmail());
    }

    @Test
    public void testStandardLogin() throws Exception {
        onView(withId(R.id.buttonOtherEmailProvider)).perform(click());

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

        onView(withId(R.id.buttonTryToConnect)).perform(click());
    }
}
