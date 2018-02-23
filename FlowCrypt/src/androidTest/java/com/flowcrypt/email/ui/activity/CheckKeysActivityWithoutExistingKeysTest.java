/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.flowcrypt.email.R;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.util.TestGeneralUtil;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.ActivityResultMatchers.hasResultCode;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

/**
 * @author Denis Bondarenko
 *         Date: 23.02.2018
 *         Time: 11:45
 *         E-mail: DenBond7@gmail.com
 */

@LargeTest
@RunWith(AndroidJUnit4.class)
public class CheckKeysActivityWithoutExistingKeysTest extends BaseTest {
    private ActivityTestRule activityTestRule = new ActivityTestRule<CheckKeysActivity>(CheckKeysActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            Context targetContext = InstrumentationRegistry.getTargetContext();
            Intent result = new Intent(targetContext, CheckKeysActivity.class);
            ArrayList<KeyDetails> privateKeys = new ArrayList<>();
            try {
                KeyDetails keyDetails = new KeyDetails(null, TestGeneralUtil.readFileFromAssetsAsString
                        (InstrumentationRegistry.getContext(), "pgp/default@denbond7.com_sec.asc"), null,
                        KeyDetails.Type.EMAIL, true, null);
                privateKeys.add(keyDetails);
            } catch (IOException e) {
                e.printStackTrace();
            }
            result.putExtra(CheckKeysActivity.KEY_EXTRA_PRIVATE_KEYS, privateKeys);
            result.putExtra(CheckKeysActivity.KEY_EXTRA_BOTTOM_TITLE,
                    targetContext.getString(R.string.found_backup_of_your_account_key));
            result.putExtra(CheckKeysActivity.KEY_EXTRA_POSITIVE_BUTTON_TITLE,
                    targetContext.getString(R.string.continue_));
            result.putExtra(CheckKeysActivity.KEY_EXTRA_NEUTRAL_BUTTON_TITLE, (Parcelable) null);
            result.putExtra(CheckKeysActivity.KEY_EXTRA_NEGATIVE_BUTTON_TITLE,
                    targetContext.getString(R.string.use_another_account));
            result.putExtra(CheckKeysActivity.KEY_EXTRA_IS_THROW_ERROR_IF_DUPLICATE_FOUND, true);
            return result;
        }
    };

    @Rule
    public TestRule ruleChain = RuleChain
            .outerRule(new ClearAppSettingsRule())
            .around(activityTestRule);

    @Test
    public void testShowMessageEmptyWarning() {
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.buttonPositiveAction)).check(matches(isDisplayed())).perform(click());
        checkIsSnackbarDisplayed(InstrumentationRegistry.getTargetContext()
                .getString(R.string.passphrase_must_be_non_empty));
    }

    @Test
    public void testUseIncorrectPassPhrase() {
        onView(withId(R.id.editTextKeyPassword)).check(matches(isDisplayed()))
                .perform(typeText("some pass phrase"), closeSoftKeyboard());
        onView(withId(R.id.buttonPositiveAction)).check(matches(isDisplayed())).perform(click());
        checkIsSnackbarDisplayed(InstrumentationRegistry.getTargetContext().getString(R.string.password_is_incorrect));
    }

    @Test
    public void testUseCorrectPassPhrase() throws Exception {
        onView(withId(R.id.editTextKeyPassword)).check(matches(isDisplayed()))
                .perform(typeText("android"), closeSoftKeyboard());
        onView(withId(R.id.buttonPositiveAction)).check(matches(isDisplayed())).perform(click());
        assertThat(activityTestRule.getActivityResult(), hasResultCode(Activity.RESULT_OK));
    }

    @Test
    public void testCheckClickButtonNeutral() throws Exception {
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.buttonNeutralAction)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testCheckClickButtonNegative() throws Exception {
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.buttonNegativeAction)).check(matches(isDisplayed())).perform(scrollTo(), click());
        assertThat(activityTestRule.getActivityResult(), hasResultCode(CheckKeysActivity.RESULT_NEGATIVE));
    }

    private void checkIsSnackbarDisplayed(String message) {
        onView(withText(message)).check(matches(isDisplayed()));
        onView(withId(android.support.design.R.id.snackbar_action)).check(matches(isDisplayed())).perform(click());
    }
}
