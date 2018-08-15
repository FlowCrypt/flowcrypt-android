/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.flowcrypt.email.R;
import com.flowcrypt.email.TestConstants;
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
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.ActivityResultMatchers.hasResultCode;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Denis Bondarenko
 *         Date: 02.03.2018
 *         Time: 19:00
 *         E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class CheckKeysActivityTestMultiBackups extends BaseTest {
    private ActivityTestRule activityTestRule = new ActivityTestRule<>
            (CheckKeysActivity.class, false, false);

    @Rule
    public TestRule ruleChain = RuleChain
            .outerRule(new ClearAppSettingsRule())
            .around(activityTestRule);

    /**
     * There are two keys (all keys are different and have different pass phrases). Only one key from two keys is using.
     */
    @Test
    public void testTwoKeysFirstCombination() throws IOException {
        String[] keysPaths = {"pgp/default@denbond7.com_keyA_strong.key",
                "pgp/default@denbond7.com_keyB_default.key"};
        activityTestRule.launchActivity(getStartCheckKeysActivityIntent(keysPaths));

        checkKeysTitleAtStart(2);
        typePassword(TestConstants.DEFAULT_STRONG_PASSWORD);
        checkKeysTitle(1, 2, 1);
        checkSkipRemainingBackupsButton();
    }

    /**
     * There are two keys (all keys are different and have different pass phrases). All keys are checking in the queue.
     */
    @Test
    public void testTwoKeysSecondCombination() throws IOException {
        String[] keysPaths = {"pgp/default@denbond7.com_keyA_strong.key",
                "pgp/default@denbond7.com_keyB_default.key"};
        activityTestRule.launchActivity(getStartCheckKeysActivityIntent(keysPaths));

        checkKeysTitleAtStart(2);
        typePassword(TestConstants.DEFAULT_STRONG_PASSWORD);
        checkKeysTitle(1, 2, 1);
        typePassword(TestConstants.DEFAULT_PASSWORD);
        assertThat(activityTestRule.getActivityResult(), hasResultCode(CheckKeysActivity.RESULT_OK));
    }

    /**
     * There are two keys with the same pass phrase. All keys will be imported per one
     * transaction.
     */
    @Test
    public void testTwoKeysWithSamePasswordThirdCombination() throws IOException {
        String[] keysPaths = {"pgp/default@denbond7.com_keyA_strong.key",
                "pgp/default@denbond7.com_keyC_strong.key"};
        activityTestRule.launchActivity(getStartCheckKeysActivityIntent(keysPaths));

        checkKeysTitleAtStart(2);
        typePassword(TestConstants.DEFAULT_STRONG_PASSWORD);
        assertThat(activityTestRule.getActivityResult(), hasResultCode(CheckKeysActivity.RESULT_OK));
    }

    /**
     * There are two keys (the identical keys with different pass phrases). A key will be imported using
     * {@link TestConstants#DEFAULT_PASSWORD}.
     */
    @Test
    public void testUseTwoKeysFourthCombination() throws IOException {
        String[] keysPaths = {"pgp/default@denbond7.com_keyC_default.key",
                "pgp/default@denbond7.com_keyC_strong.key"};
        activityTestRule.launchActivity(getStartCheckKeysActivityIntent(keysPaths));

        checkKeysTitleAtStart(1);
        typePassword(TestConstants.DEFAULT_PASSWORD);
        assertThat(activityTestRule.getActivityResult(), hasResultCode(CheckKeysActivity.RESULT_OK));
    }

    /**
     * There are two keys (the identical keys with different pass phrases). A key will be imported using
     * {@link TestConstants#DEFAULT_STRONG_PASSWORD}
     */
    @Test
    public void testUseTwoKeysFifthCombination() throws IOException {
        String[] keysPaths = {"pgp/default@denbond7.com_keyC_default.key",
                "pgp/default@denbond7.com_keyC_strong.key"};
        activityTestRule.launchActivity(getStartCheckKeysActivityIntent(keysPaths));

        checkKeysTitleAtStart(1);
        typePassword(TestConstants.DEFAULT_STRONG_PASSWORD);
        assertThat(activityTestRule.getActivityResult(), hasResultCode(CheckKeysActivity.RESULT_OK));
    }

    /**
     * There are three keys (all keys are different, two keys have the identical pass phrase). Will be used only one
     * key with a unique pass phrase.
     */
    @Test
    public void testUseThreeFirstCombination() throws IOException {
        String[] keysPaths = {"pgp/default@denbond7.com_keyA_strong.key",
                "pgp/default@denbond7.com_keyB_default.key",
                "pgp/default@denbond7.com_keyC_default.key"};
        activityTestRule.launchActivity(getStartCheckKeysActivityIntent(keysPaths));

        checkKeysTitleAtStart(3);
        typePassword(TestConstants.DEFAULT_STRONG_PASSWORD);
        checkKeysTitle(1, 3, 2);
        checkSkipRemainingBackupsButton();
    }

    /**
     * There are three keys (all keys are different, two keys have the identical pass phrase). Will be used two keys
     * with the same pass phrase.
     */
    @Test
    public void testUseThreeKeysSecondCombination() throws IOException {
        String[] keysPaths = {"pgp/default@denbond7.com_keyA_strong.key",
                "pgp/default@denbond7.com_keyB_default.key",
                "pgp/default@denbond7.com_keyC_strong.key"};
        activityTestRule.launchActivity(getStartCheckKeysActivityIntent(keysPaths));

        checkKeysTitleAtStart(3);
        typePassword(TestConstants.DEFAULT_STRONG_PASSWORD);
        checkKeysTitle(2, 3, 1);
        checkSkipRemainingBackupsButton();
    }

    /**
     * There are three keys (all keys are different, two keys have the identical pass phrase). First will be used a key
     * with a unique pass phrase, and then the remaining keys.
     */
    @Test
    public void testUseThreeKeysThirdCombination() throws IOException {
        String[] keysPaths = {"pgp/default@denbond7.com_keyA_strong.key",
                "pgp/default@denbond7.com_keyB_default.key",
                "pgp/default@denbond7.com_keyC_default.key"};
        activityTestRule.launchActivity(getStartCheckKeysActivityIntent(keysPaths));

        checkKeysTitleAtStart(3);
        typePassword(TestConstants.DEFAULT_STRONG_PASSWORD);
        checkKeysTitle(1, 3, 2);
        typePassword(TestConstants.DEFAULT_PASSWORD);
        assertThat(activityTestRule.getActivityResult(), hasResultCode(CheckKeysActivity.RESULT_OK));
    }

    /**
     * There are three keys (all keys are different, two keys have the identical pass phrase). First will be used two
     * keys with the same pass phrase, and then the remaining key.
     */
    @Test
    public void testUseThreeKeysFourthCombination() throws IOException {
        String[] keysPaths = {"pgp/default@denbond7.com_keyA_strong.key",
                "pgp/default@denbond7.com_keyB_default.key",
                "pgp/default@denbond7.com_keyC_strong.key"};
        activityTestRule.launchActivity(getStartCheckKeysActivityIntent(keysPaths));

        checkKeysTitleAtStart(3);
        typePassword(TestConstants.DEFAULT_STRONG_PASSWORD);
        checkKeysTitle(2, 3, 1);
        typePassword(TestConstants.DEFAULT_PASSWORD);
        assertThat(activityTestRule.getActivityResult(), hasResultCode(CheckKeysActivity.RESULT_OK));
    }

    /**
     * There are three keys (one unique and two identical, the unique key and the identical key have the same
     * pass phrase). Will be used one of the identical keys with a unique pass phrase.
     */
    @Test
    public void testUseThreeKeysFifthCombination() throws IOException {
        String[] keysPaths = {"pgp/default@denbond7.com_keyB_default.key",
                "pgp/default@denbond7.com_keyC_default.key",
                "pgp/default@denbond7.com_keyC_strong.key"};
        activityTestRule.launchActivity(getStartCheckKeysActivityIntent(keysPaths));

        checkKeysTitleAtStart(2);
        typePassword(TestConstants.DEFAULT_STRONG_PASSWORD);
        checkKeysTitle(1, 2, 1);
        checkSkipRemainingBackupsButton();
    }

    /**
     * There are three keys (one unique and two identical, the unique key and the identical key have the same
     * pass phrase). All keys will be imported per one transaction using {@link TestConstants#DEFAULT_STRONG_PASSWORD}.
     */
    @Test
    public void testUseThreeKeysSixthCombination() throws IOException {
        String[] keysPaths = {"pgp/default@denbond7.com_keyA_strong.key",
                "pgp/default@denbond7.com_keyC_default.key",
                "pgp/default@denbond7.com_keyC_strong.key"};
        activityTestRule.launchActivity(getStartCheckKeysActivityIntent(keysPaths));

        checkKeysTitleAtStart(2);
        typePassword(TestConstants.DEFAULT_STRONG_PASSWORD);
        assertThat(activityTestRule.getActivityResult(), hasResultCode(CheckKeysActivity.RESULT_OK));
    }

    /**
     * There are three keys (one unique and two identical, the unique key and the identical key have the same
     * pass phrase). First will be used one key of the identical keys with a unique passphrase, and then the other keys.
     */
    @Test
    public void testUseThreeKeysSeventhCombination() throws IOException {
        String[] keysPaths = {"pgp/default@denbond7.com_keyB_default.key",
                "pgp/default@denbond7.com_keyC_default.key",
                "pgp/default@denbond7.com_keyC_strong.key"};
        activityTestRule.launchActivity(getStartCheckKeysActivityIntent(keysPaths));

        checkKeysTitleAtStart(2);
        typePassword(TestConstants.DEFAULT_STRONG_PASSWORD);
        checkKeysTitle(1, 2, 1);
        typePassword(TestConstants.DEFAULT_PASSWORD);
        assertThat(activityTestRule.getActivityResult(), hasResultCode(CheckKeysActivity.RESULT_OK));
    }

    /**
     * There are four keys (three keys are different and one is the same as one of the other; identical keys have
     * different pass phrases; each of the keys with identical pass phrase are unique). Will be used only
     * two keys with the same pass phrase.
     */
    @Test
    public void testUseFourKeysFirstCombination() throws IOException {
        String[] keysPaths = {"pgp/default@denbond7.com_keyA_strong.key",
                "pgp/default@denbond7.com_keyB_default.key",
                "pgp/default@denbond7.com_keyC_default.key",
                "pgp/default@denbond7.com_keyC_strong.key"};
        activityTestRule.launchActivity(getStartCheckKeysActivityIntent(keysPaths));

        checkKeysTitleAtStart(3);
        typePassword(TestConstants.DEFAULT_STRONG_PASSWORD);
        checkKeysTitle(2, 3, 1);
        checkSkipRemainingBackupsButton();
    }

    /**
     * There are four keys (three keys are different and one is the same as one of the other; identical keys have
     * different pass phrases; each of the keys with identical pass phrase are unique). Will be used all keys (two
     * keys per one pass phrase typing).
     */
    @Test
    public void testUseFourKeysSecondCombination() throws IOException {
        String[] keysPaths = {"pgp/default@denbond7.com_keyA_strong.key",
                "pgp/default@denbond7.com_keyB_default.key",
                "pgp/default@denbond7.com_keyC_default.key",
                "pgp/default@denbond7.com_keyC_strong.key"};
        activityTestRule.launchActivity(getStartCheckKeysActivityIntent(keysPaths));

        checkKeysTitleAtStart(3);
        typePassword(TestConstants.DEFAULT_STRONG_PASSWORD);
        checkKeysTitle(2, 3, 1);
        typePassword(TestConstants.DEFAULT_PASSWORD);
        assertThat(activityTestRule.getActivityResult(), hasResultCode(CheckKeysActivity.RESULT_OK));
    }

    private void checkSkipRemainingBackupsButton() {
        onView(withId(R.id.buttonNeutralAction)).check(matches(isDisplayed())).perform(click());
        assertThat(activityTestRule.getActivityResult(), hasResultCode(CheckKeysActivity.RESULT_NEUTRAL));
    }

    /**
     * Type a password and click on the "CONTINUE" button.
     *
     * @param password The input password.
     */
    private void typePassword(String password) {
        onView(withId(R.id.editTextKeyPassword)).check(matches(isDisplayed()))
                .perform(typeText(password), closeSoftKeyboard());
        onView(withId(R.id.buttonPositiveAction)).check(matches(isDisplayed())).perform(click());
    }

    private void checkKeysTitle(int quantityOfKeysUsed, int totalQuantityOfKeys, int quantityOfRemainingKeys) {
        onView(withId(R.id.textViewCheckKeysTitle)).check(matches(isDisplayed()))
                .check(matches(withText(InstrumentationRegistry.getTargetContext().getResources()
                        .getQuantityString(R.plurals.not_recovered_all_keys, quantityOfRemainingKeys,
                                quantityOfKeysUsed, totalQuantityOfKeys, quantityOfRemainingKeys))));
    }

    private void checkKeysTitleAtStart(int totalQuantityOfKeys) {
        onView(withId(R.id.textViewCheckKeysTitle)).check(matches(isDisplayed()))
                .check(matches(withText(InstrumentationRegistry.getTargetContext().getResources()
                        .getQuantityString(R.plurals.found_backup_of_your_account_key, totalQuantityOfKeys,
                                totalQuantityOfKeys))));
    }

    @NonNull
    private Intent getStartCheckKeysActivityIntent(String[] keysPaths) throws IOException {
        Context targetContext = InstrumentationRegistry.getTargetContext();
        Intent startCheckKeysActivity = new Intent(targetContext, CheckKeysActivity.class);
        startCheckKeysActivity.putExtra(CheckKeysActivity.KEY_EXTRA_PRIVATE_KEYS, getKeyDetailsListFromAssets
                (keysPaths));
        startCheckKeysActivity.putExtra(CheckKeysActivity.KEY_EXTRA_BOTTOM_TITLE,
                targetContext.getResources().getQuantityString(R.plurals.found_backup_of_your_account_key, 1, 1));
        startCheckKeysActivity.putExtra(CheckKeysActivity.KEY_EXTRA_POSITIVE_BUTTON_TITLE,
                targetContext.getString(R.string.continue_));
        startCheckKeysActivity.putExtra(CheckKeysActivity.KEY_EXTRA_NEUTRAL_BUTTON_TITLE, (Parcelable) null);
        startCheckKeysActivity.putExtra(CheckKeysActivity.KEY_EXTRA_NEGATIVE_BUTTON_TITLE,
                targetContext.getString(R.string.use_another_account));
        return startCheckKeysActivity;
    }

    @NonNull
    private ArrayList<KeyDetails> getKeyDetailsListFromAssets(String[] keysPaths) throws IOException {
        ArrayList<KeyDetails> privateKeys = new ArrayList<>();
        for (String path : keysPaths) {
            KeyDetails keyDetails = new KeyDetails(null, TestGeneralUtil.readFileFromAssetsAsString
                    (InstrumentationRegistry.getContext(), path), KeyDetails.Type.EMAIL,
                    true, null);
            privateKeys.add(keyDetails);
        }
        return privateKeys;
    }
}
