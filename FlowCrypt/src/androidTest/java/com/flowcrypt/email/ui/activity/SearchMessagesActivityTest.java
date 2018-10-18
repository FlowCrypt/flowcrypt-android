/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.content.ContentValues;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.widget.EditText;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.rules.AddAccountToDatabaseRule;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.rules.UpdateAccountRule;
import com.flowcrypt.email.ui.activity.base.BaseEmailListActivityTest;
import com.flowcrypt.email.util.AccountDaoManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withHint;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;

/**
 * @author Denis Bondarenko
 * Date: 03.05.2018
 * Time: 13:59
 * E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class SearchMessagesActivityTest extends BaseEmailListActivityTest {

    private static final String FOLDER_NAME = "INBOX";
    private static final String QUERY = "Search";

    private IntentsTestRule intentsTestRule = new IntentsTestRule<SearchMessagesActivity>(SearchMessagesActivity
            .class) {
        @Override
        protected Intent getActivityIntent() {
            return SearchMessagesActivity.newIntent(InstrumentationRegistry.getTargetContext(), QUERY,
                    new Folder(FOLDER_NAME, FOLDER_NAME, 0, null, false));
        }
    };

    @Rule
    public TestRule ruleChain = RuleChain
            .outerRule(new ClearAppSettingsRule())
            .around(new AddAccountToDatabaseRule())
            .around(new UpdateAccountRule(AccountDaoManager.getDefaultAccountDao(), generateContentValues()))
            .around(intentsTestRule);

    @Before
    public void registerIdlingResource() {
        IdlingRegistry.getInstance().register(((SearchMessagesActivity) intentsTestRule.getActivity())
                .getCountingIdlingResourceForMessages());
    }

    @After
    public void unregisterIdlingResource() {
        for (IdlingResource idlingResource : IdlingRegistry.getInstance().getResources()) {
            IdlingRegistry.getInstance().unregister(idlingResource);
        }
    }

    @Test
    public void testSearchQueryAtStart() {
        onView(allOf(withId(R.id.menuSearch), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .check(matches(isDisplayed()));
        onView(isAssignableFrom(EditText.class)).check(matches(withText(QUERY)));
    }

    @Test
    public void testShowNotEmptyList() {
        onView(withId(R.id.listViewMessages)).check(matches(isDisplayed()));
        onView(withId(R.id.listViewMessages)).check(matches(not(matchEmptyList())));
    }

    @Test
    public void testOpenSomeMessage() {
        testShowNotEmptyList();
        testRunMessageDetailsActivity(0);
    }

    @Test
    public void testCheckNoResults() {
        onView(allOf(withId(R.id.menuSearch), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .check(matches(isDisplayed())).perform(click());
        onView(isAssignableFrom(EditText.class)).perform(clearText(), typeText("The string with no results"),
                pressImeActionButton());
        onView(withId(R.id.emptyView)).check(matches(isDisplayed()));
    }

    @Test
    public void testClearSearchView() {
        onView(allOf(withId(R.id.menuSearch), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .check(matches(isDisplayed())).perform(click());
        onView(withId(android.support.v7.appcompat.R.id.search_close_btn)).perform(click());
        onView(isAssignableFrom(EditText.class)).check(matches(withText(isEmptyString())))
                .check(matches(withHint(InstrumentationRegistry.getTargetContext().getString(R.string.search))));
    }

    private static ContentValues generateContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(AccountDaoSource.COL_IS_CONTACTS_LOADED, true);
        return contentValues;
    }
}