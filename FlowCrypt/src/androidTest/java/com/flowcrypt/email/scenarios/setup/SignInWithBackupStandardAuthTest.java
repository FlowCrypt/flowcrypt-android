/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.scenarios.setup;


import com.flowcrypt.email.R;
import com.flowcrypt.email.util.PrivateKeysManager;

import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

/**
 * This class tests the condition when a user has some private key backups (email).
 *
 * @author Denis Bondarenko
 *         Date: 27.12.2017
 *         Time: 13:05
 *         E-mail: DenBond7@gmail.com
 */

public abstract class SignInWithBackupStandardAuthTest extends SignInWithStandardAuthTest {

    private static final String DEFAULT_PRIVATE_KEY_PASSWORD = "android";

    @Test
    public void testAllConditionsTrue() throws Exception {
        onView(withId(R.id.buttonOtherEmailProvider)).perform(click());
        fillAllFields();
        onView(withId(R.id.buttonTryToConnect)).perform(click());

        checkRightHeader();
        typeAndCheckPrivateKeyPassword(DEFAULT_PRIVATE_KEY_PASSWORD);
        onView(withId(R.id.textViewUserEmail)).check(matches(withText(authCredentials.getEmail())));
    }

    @Test
    public void testPasswordIncorrect() throws Exception {
        onView(withId(R.id.buttonOtherEmailProvider)).perform(click());
        fillAllFields();
        onView(withId(R.id.buttonTryToConnect)).perform(click());

        checkRightHeader();
        typeAndCheckPrivateKeyPassword("password");
        onView(withText(R.string.password_is_incorrect)).check(matches(isDisplayed()));

        typeAndCheckPrivateKeyPassword(DEFAULT_PRIVATE_KEY_PASSWORD);
        onView(withId(R.id.textViewUserEmail)).check(matches(withText(authCredentials.getEmail())));
    }

    @Test
    public void testUseAnotherAccount() throws Exception {
        onView(withId(R.id.buttonOtherEmailProvider)).perform(click());
        fillAllFields();
        onView(withId(R.id.buttonTryToConnect)).perform(click());

        checkRightHeader();
        onView(withId(R.id.editTextKeyPassword)).perform(closeSoftKeyboard());
        onView(withText(R.string.use_another_account)).perform(click());
        onView(withId(R.id.buttonOtherEmailProvider)).check(matches(isDisplayed()));
    }

    @Test
    public void testUseExistingKey() throws Exception {
        PrivateKeysManager.addTempPrivateKey();

        onView(withId(R.id.buttonOtherEmailProvider)).perform(click());
        fillAllFields();
        onView(withId(R.id.buttonTryToConnect)).perform(click());

        checkRightHeader();
        onView(withId(R.id.editTextKeyPassword)).perform(closeSoftKeyboard());
        onView(withText(R.string.use_existing_keys)).perform(click());

        onView(withId(R.id.textViewUserEmail)).check(matches(withText(authCredentials.getEmail())));
    }

    private void typeAndCheckPrivateKeyPassword(String password) {
        onView(withId(R.id.editTextKeyPassword)).perform(clearText(), typeText(password),
                closeSoftKeyboard());
        onView(withId(R.id.buttonPositiveAction)).perform(click());
    }

    private void checkRightHeader() {
        onView(withText(R.string.found_backup_of_your_account_key)).check(matches(isDisplayed()));
    }
}
