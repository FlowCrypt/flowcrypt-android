/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.content.ContentValues;
import android.content.Intent;
import android.widget.EditText;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.LocalFolder;
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

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.flowcrypt.email.matchers.CustomMatchers.withEmptyListView;
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
      return SearchMessagesActivity.newIntent(InstrumentationRegistry.getInstrumentation().getTargetContext(), QUERY,
          new LocalFolder(FOLDER_NAME, FOLDER_NAME, 0, null, false));
    }
  };

  @Rule
  public TestRule ruleChain = RuleChain
      .outerRule(new ClearAppSettingsRule())
      .around(new AddAccountToDatabaseRule())
      .around(new UpdateAccountRule(AccountDaoManager.getDefaultAccountDao(), generateContentValues()))
      .around(intentsTestRule);

  @Override
  public ActivityTestRule getActivityTestRule() {
    return intentsTestRule;
  }

  @Before
  public void registerIdlingResource() {
    IdlingRegistry.getInstance().register(((SearchMessagesActivity) intentsTestRule.getActivity())
        .getMsgsCountingIdlingResource());
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
    onView(withId(R.id.listViewMessages)).check(matches(not(withEmptyListView())));
  }

  @Test
  public void testOpenSomeMsg() {
    testShowNotEmptyList();
    testRunMsgDetailsActivity(0);
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
    onView(withId(androidx.appcompat.R.id.search_close_btn)).perform(click());
    onView(isAssignableFrom(EditText.class)).check(matches(withText(isEmptyString())))
        .check(matches(withHint(InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string
            .search))));
  }

  private static ContentValues generateContentValues() {
    ContentValues contentValues = new ContentValues();
    contentValues.put(AccountDaoSource.COL_IS_CONTACTS_LOADED, true);
    return contentValues;
  }
}
