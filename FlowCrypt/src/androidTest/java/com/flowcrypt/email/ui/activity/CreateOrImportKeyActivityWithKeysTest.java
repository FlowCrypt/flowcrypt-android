/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import com.flowcrypt.email.R;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.ui.activity.base.BaseImportKeyActivity;
import com.flowcrypt.email.util.AccountDaoManager;
import com.flowcrypt.email.util.GeneralUtil;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.ActivityResultMatchers.hasResultCode;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtraWithKey;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;

/**
 * @author Denis Bondarenko
 *         Date: 23.02.2018
 *         Time: 16:51
 *         E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class CreateOrImportKeyActivityWithKeysTest extends BaseTest {
    private static final String KEY_IS_SHOW_USE_ANOTHER_ACCOUNT_BUTTON =
            GeneralUtil.generateUniqueExtraKey("KEY_IS_SHOW_USE_ANOTHER_ACCOUNT_BUTTON",
                    CreateOrImportKeyActivity.class);

    private IntentsTestRule activityTestRule = new IntentsTestRule<CreateOrImportKeyActivity>
            (CreateOrImportKeyActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            Context targetContext = InstrumentationRegistry.getTargetContext();
            AccountDao accountDao = AccountDaoManager.getDefaultAccountDao();
            Intent result = new Intent(targetContext, CreateOrImportKeyActivity.class);
            result.putExtra(CreateOrImportKeyActivity.EXTRA_KEY_ACCOUNT_DAO, accountDao);
            result.putExtra(KEY_IS_SHOW_USE_ANOTHER_ACCOUNT_BUTTON, true);
            return result;
        }
    };

    @Rule
    public TestRule ruleChain = RuleChain
            .outerRule(new ClearAppSettingsRule())
            .around(new AddPrivateKeyToDatabaseRule())
            .around(activityTestRule);

    @Test
    public void testClickOnButtonCreateNewKey() {
        intending(allOf(hasComponent(new ComponentName(InstrumentationRegistry.getTargetContext(),
                CreatePrivateKeyActivity.class)), hasExtraWithKey(CreatePrivateKeyActivity.KEY_EXTRA_ACCOUNT_DAO)))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
        onView(withId(R.id.buttonCreateNewKey)).check(matches(isDisplayed())).perform(click());
        assertThat(activityTestRule.getActivityResult(), hasResultCode(Activity.RESULT_OK));
    }

    @Test
    public void testClickOnButtonImportMyKey() {
        intending(allOf(hasComponent(new ComponentName(InstrumentationRegistry.getTargetContext(),
                        ImportPrivateKeyActivity.class)),
                hasExtraWithKey(BaseImportKeyActivity.KEY_EXTRA_IS_SYNC_ENABLE),
                hasExtraWithKey(BaseImportKeyActivity.KEY_EXTRA_TITLE),
                hasExtraWithKey(BaseImportKeyActivity.KEY_EXTRA_PRIVATE_KEY_DETAILS_FROM_CLIPBOARD),
                hasExtraWithKey(BaseImportKeyActivity.KEY_EXTRA_IS_THROW_ERROR_IF_DUPLICATE_FOUND)))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
        onView(withId(R.id.buttonImportMyKey)).check(matches(isDisplayed())).perform(click());
        assertThat(activityTestRule.getActivityResult(), hasResultCode(Activity.RESULT_OK));
    }

    @Test
    public void testClickOnButtonSelectAnotherAccount() {
        onView(withId(R.id.buttonSelectAnotherAccount)).check(matches(isDisplayed())).perform(click());
        assertThat(activityTestRule.getActivityResult(), hasResultCode(CreateOrImportKeyActivity
                .RESULT_CODE_USE_ANOTHER_ACCOUNT));
    }

    @Test
    public void testClickOnButtonSkipSetup() {
        onView(withId(R.id.buttonSkipSetup)).check(matches(isDisplayed())).perform(click());
        assertThat(activityTestRule.getActivityResult(), hasResultCode(Activity.RESULT_OK));
    }
}
