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
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.intent.rule.IntentsTestRule;

import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.matchers.ToolBarTitleMatcher;
import com.flowcrypt.email.rules.AddAccountToDatabaseRule;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.ui.activity.base.BaseEmailListActivityTest;
import com.flowcrypt.email.ui.activity.settings.SettingsActivity;
import com.flowcrypt.email.util.AccountDaoManager;
import com.flowcrypt.email.viewaction.CustomNavigationViewActions;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.action.ViewActions.swipeUp;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.open;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.not;

/**
 * @author Denis Bondarenko
 * Date: 23.03.2018
 * Time: 16:16
 * E-mail: DenBond7@gmail.com
 */

public class EmailManagerActivityTest extends BaseEmailListActivityTest {
    private AccountDao userWithoutLetters = AccountDaoManager.getAccountDao("user_without_letters.json");
    private AccountDao userWithMoreThan21LettersAccount = AccountDaoManager.getUserWitMoreThan21Letters();

    private IntentsTestRule intentsTestRule = new IntentsTestRule<>(EmailManagerActivity.class);

    @Rule
    public TestRule ruleChain = RuleChain
            .outerRule(new ClearAppSettingsRule())
            .around(new AddAccountToDatabaseRule(userWithoutLetters))
            .around(new AddAccountToDatabaseRule(userWithMoreThan21LettersAccount))
            .around(intentsTestRule);

    @Before
    public void registerIdlingResource() {
        IdlingRegistry.getInstance().register(((EmailManagerActivity) intentsTestRule.getActivity())
                .getCountingIdlingResourceForMessages());
        IdlingRegistry.getInstance().register(((EmailManagerActivity) intentsTestRule.getActivity())
                .getCountingIdlingResourceForLabel());
    }

    @After
    public void unregisterIdlingResource() {
        for (IdlingResource idlingResource : IdlingRegistry.getInstance().getResources()) {
            IdlingRegistry.getInstance().unregister(idlingResource);
        }
    }

    @Test
    public void testComposeFloatButton() {
        onView(withId(R.id.floatActionButtonCompose)).check(matches(isDisplayed())).perform(click());
        intended(hasComponent(CreateMessageActivity.class.getName()));
        onView(allOf(withText(R.string.compose), withParent(withId(R.id.toolbar)))).check(matches(isDisplayed()));
    }

    @Test
    public void testRunMessageDetailsActivity() {
        testRunMessageDetailsActivity(20);
    }

    @Test
    public void testForceLoadMessages() {
        onData(anything())
                .inAdapterView(withId(R.id.listViewMessages))
                .atPosition(0)
                .perform(scrollTo());
        onView(withId(R.id.listViewMessages)).check(matches(isDisplayed())).perform(swipeDown());
        onView(withId(R.id.listViewMessages)).check(matches(not(matchEmptyList()))).check(matches(isDisplayed()));
    }

    @Test
    public void testSwipeNavigationView() {
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withId(R.id.navigationView)).perform(swipeUp());
    }

    @Test
    public void testShowSplashActivityAfterLogout() {
        clickLogOut();
        clickLogOut();
        intended(hasComponent(SplashActivity.class.getName()));
    }

    @Test
    public void testClickLogOutIfMoreAccounts() {
        clickLogOut();
        onView(withId(R.id.floatActionButtonCompose)).check(matches(isDisplayed()));
    }

    @Test
    public void testGoToSettingsActivity() {
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withId(R.id.navigationView)).perform(swipeUp());
        onView(withText(R.string.action_settings)).check(matches(isDisplayed())).perform(click());
        intended(hasComponent(SettingsActivity.class.getName()));
    }

    @Test
    public void testSwitchLabels() {
        String menuItem = "Sent";
        onView(withId(R.id.toolbar)).check(matches(anyOf(
                ToolBarTitleMatcher.withText("INBOX"),
                ToolBarTitleMatcher.withText(InstrumentationRegistry.getTargetContext().getString(R.string.loading)))));



        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withId(R.id.navigationView)).perform(CustomNavigationViewActions.navigateTo(menuItem));
        onView(withId(R.id.toolbar)).check(matches(ToolBarTitleMatcher.withText(menuItem)));
    }

    @Test
    public void testAddNewAccount() {
        Context targetContext = InstrumentationRegistry.getTargetContext();
        AccountDao accountDao = AccountDaoManager.getDefaultAccountDao();
        Intent result = new Intent();
        result.putExtra(AddNewAccountActivity.KEY_EXTRA_NEW_ACCOUNT, accountDao);
        intending(hasComponent(new ComponentName(targetContext, AddNewAccountActivity.class)))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, result));

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withId(R.id.layoutUserDetails)).check(matches(isDisplayed())).perform(click(), click());

        try {
            AccountDaoSource accountDaoSource = new AccountDaoSource();
            accountDaoSource.addRow(targetContext, accountDao.getAuthCredentials());
            accountDaoSource.setActiveAccount(targetContext, accountDao.getEmail());
        } catch (Exception e) {
            e.printStackTrace();
        }

        onView(withId(R.id.viewIdAddNewAccount)).check(matches(isDisplayed())).perform(click());

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withId(R.id.textViewActiveUserEmail)).check(matches(isDisplayed()))
                .check(matches(withText(accountDao.getEmail())));
    }

    @Test
    public void testChooseAnotherAccount() {
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withId(R.id.textViewActiveUserEmail)).check(matches(isDisplayed())).check(matches(withText
                (userWithMoreThan21LettersAccount.getEmail())));
        onView(withId(R.id.layoutUserDetails)).check(matches(isDisplayed())).perform(click(), click());
        onView(withText(userWithoutLetters.getEmail())).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withId(R.id.textViewActiveUserEmail)).check(matches(isDisplayed())).check(
                matches(withText(userWithoutLetters.getEmail())));
    }

    private void clickLogOut() {
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withId(R.id.navigationView)).perform(swipeUp());
        onView(withText(R.string.log_out)).check(matches(isDisplayed())).perform(click());
    }
}
