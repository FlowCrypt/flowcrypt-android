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

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.matchers.ToolBarTitleMatcher;
import com.flowcrypt.email.rules.AddAccountToDatabaseRule;
import com.flowcrypt.email.rules.AddLabelsToDatabaseRule;
import com.flowcrypt.email.rules.AddMessageToDatabaseRule;
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
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.swipeDown;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.DrawerActions.open;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
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
@LargeTest
@RunWith(AndroidJUnit4.class)
public class EmailManagerActivityTest extends BaseEmailListActivityTest {
  private static final List<Folder> folders;
  private static final Folder INBOX_USER_WITH_MORE_THAN_21_LETTERS_ACCOUNT =
      new Folder("INBOX", "INBOX", 0, new String[]{"\\HasNoChildren"}, false);

  static {
    folders = new ArrayList<>();
    folders.add(INBOX_USER_WITH_MORE_THAN_21_LETTERS_ACCOUNT);
    folders.add(new Folder("Drafts", "Drafts", 0, new String[]{"\\HasNoChildren", "\\Drafts"}, false));
    folders.add(new Folder("Sent", "Sent", 0, new String[]{"\\HasNoChildren", "\\Sent"}, false));
    folders.add(new Folder("Junk", "Junk", 0, new String[]{"\\HasNoChildren", "\\Junk"}, false));
  }

  private AccountDao userWithoutLetters = AccountDaoManager.getAccountDao("user_without_letters.json");
  private AccountDao userWithMoreThan21LettersAccount = AccountDaoManager.getUserWitMoreThan21Letters();
  private IntentsTestRule intentsTestRule = new IntentsTestRule<>(EmailManagerActivity.class);
  @Rule
  public TestRule ruleChain = RuleChain
      .outerRule(new ClearAppSettingsRule())
      .around(new AddAccountToDatabaseRule(userWithoutLetters))
      .around(new AddAccountToDatabaseRule(userWithMoreThan21LettersAccount))
      .around(new AddLabelsToDatabaseRule(userWithMoreThan21LettersAccount, folders))
      .around(new AddMessageToDatabaseRule(userWithMoreThan21LettersAccount,
          INBOX_USER_WITH_MORE_THAN_21_LETTERS_ACCOUNT))
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
    testRunMessageDetailsActivity(0);
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
  public void testOpenAndSwipeNavigationView() {
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
        ToolBarTitleMatcher.withText(InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R
            .string.loading)))));

    onView(withId(R.id.drawer_layout)).perform(open());
    onView(withId(R.id.navigationView)).perform(CustomNavigationViewActions.navigateTo(menuItem));
    onView(withId(R.id.toolbar)).check(matches(ToolBarTitleMatcher.withText(menuItem)));
  }

  @Test
  public void testAddNewAccount() {
    Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
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
