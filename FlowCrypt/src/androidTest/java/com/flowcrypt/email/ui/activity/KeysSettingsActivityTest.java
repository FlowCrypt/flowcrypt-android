/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import com.flowcrypt.email.R;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.rules.AddAccountToDatabaseRule;
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.ui.activity.settings.KeysSettingsActivity;
import com.flowcrypt.email.util.TestGeneralUtil;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;

/**
 * @author Denis Bondarenko
 *         Date: 20.02.2018
 *         Time: 15:42
 *         E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class KeysSettingsActivityTest extends BaseTest {

    private IntentsTestRule intentsTestRule = new IntentsTestRule<>(KeysSettingsActivity.class);
    @Rule
    public TestRule ruleChain = RuleChain
            .outerRule(new ClearAppSettingsRule())
            .around(new AddAccountToDatabaseRule())
            .around(new AddPrivateKeyToDatabaseRule())
            .around(intentsTestRule);

    @Test
    public void testAddNewKeys() throws Throwable {
        intending(hasComponent(new ComponentName(InstrumentationRegistry.getTargetContext(), ImportPrivateKeyActivity
                .class))).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

        TestGeneralUtil.saveKeyToDatabase(TestGeneralUtil.readFileFromAssetsAsString(InstrumentationRegistry
                .getContext(), "pgp/ben@flowcrypt.com-sec.asc"), KeyDetails.Type.EMAIL);

        onView(withId(R.id.floatActionButtonAddKey)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.listViewKeys)).check(matches(isDisplayed()))
                .check(matches(matchListSize(2)));
        checkIsToastDisplayed(intentsTestRule.getActivity(),
                InstrumentationRegistry.getTargetContext().getString(R.string.key_successfully_imported));
    }

    @Test
    public void testIsKeyExists() {
        onView(withId(R.id.listViewKeys)).check(matches(not(matchEmptyList()))).check(matches(isDisplayed()));
        onView(withId(R.id.emptyView)).check(matches(not(isDisplayed())));
    }
}