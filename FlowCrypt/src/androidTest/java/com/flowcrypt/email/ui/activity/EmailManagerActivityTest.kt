/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import android.view.View
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions.open
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withEmptyListView
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withToolBarText
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddLabelsToDatabaseRule
import com.flowcrypt.email.rules.AddMessageToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.ui.activity.base.BaseEmailListActivityTest
import com.flowcrypt.email.ui.activity.settings.SettingsActivity
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.viewaction.CustomViewActions.Companion.navigateToItemWithName
import org.hamcrest.CoreMatchers.anyOf
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.*

/**
 * @author Denis Bondarenko
 * Date: 23.03.2018
 * Time: 16:16
 * E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class EmailManagerActivityTest : BaseEmailListActivityTest() {

  override val activityTestRule: ActivityTestRule<*>? = IntentsTestRule(EmailManagerActivity::class.java)

  private val userWithoutLetters = AccountDaoManager.getAccountDao("user_without_letters.json")
  private val userWithMoreThan21LettersAccount = AccountDaoManager.getUserWitMoreThan21Letters()

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(AddAccountToDatabaseRule(userWithoutLetters))
      .around(AddAccountToDatabaseRule(userWithMoreThan21LettersAccount))
      .around(AddLabelsToDatabaseRule(userWithMoreThan21LettersAccount, LOCAL_FOLDERS))
      .around(AddMessageToDatabaseRule(userWithMoreThan21LettersAccount, INBOX_USER_WITH_MORE_THAN_21_LETTERS_ACCOUNT))
      .around(activityTestRule)

  @Before
  fun registerIdlingResource() {
    IdlingRegistry.getInstance().register((activityTestRule?.activity as EmailManagerActivity).msgsCountingIdlingResource)
    IdlingRegistry.getInstance().register((activityTestRule.activity as EmailManagerActivity).countingIdlingResourceForLabel)
  }

  @After
  fun unregisterIdlingResource() {
    for (idlingResource in IdlingRegistry.getInstance().resources) {
      IdlingRegistry.getInstance().unregister(idlingResource)
    }
  }

  @Test
  fun testComposeFloatButton() {
    onView(withId(R.id.floatActionButtonCompose))
        .check(matches(isDisplayed()))
        .perform(click())
    intended(hasComponent(CreateMessageActivity::class.java.name))
    onView(allOf<View>(withText(R.string.compose), withParent(withId(R.id.toolbar))))
        .check(matches(isDisplayed()))
  }

  @Test
  fun testRunMsgDetailsActivity() {
    testRunMsgDetailsActivity(0)
  }

  @Test
  fun testForceLoadMsgs() {
    onData(anything())
        .inAdapterView(withId(R.id.listViewMessages))
        .atPosition(0)
        .perform(scrollTo())
    onView(withId(R.id.listViewMessages))
        .check(matches(isDisplayed()))
        .perform(swipeDown())
    onView(withId(R.id.listViewMessages))
        .check(matches(not<View>(withEmptyListView())))
        .check(matches(isDisplayed()))
  }

  @Test
  fun testOpenAndSwipeNavigationView() {
    onView(withId(R.id.drawer_layout))
        .perform(open())
    onView(withId(R.id.navigationView))
        .perform(swipeUp())
  }

  @Test
  fun testShowSplashActivityAfterLogout() {
    clickLogOut()
    clickLogOut()
    intended(hasComponent(SignInActivity::class.java.name))
  }

  @Test
  fun testClickLogOutIfMoreAccounts() {
    clickLogOut()
    onView(withId(R.id.floatActionButtonCompose))
        .check(matches(isDisplayed()))
  }

  @Test
  fun testGoToSettingsActivity() {
    onView(withId(R.id.drawer_layout))
        .perform(open())
    onView(withId(R.id.navigationView))
        .perform(swipeUp())
    onView(withText(R.string.action_settings))
        .check(matches(isDisplayed()))
        .perform(click())
    intended(hasComponent(SettingsActivity::class.java.name))
  }

  @Test
  fun testSwitchLabels() {
    val menuItem = "Sent"
    onView(withId(R.id.toolbar)).check(matches(anyOf<View>(
        withToolBarText("INBOX"),
        withToolBarText(InstrumentationRegistry.getInstrumentation().targetContext.getString(R
            .string.loading)))))

    onView(withId(R.id.drawer_layout))
        .perform(open())
    onView(withId(R.id.navigationView))
        .perform(navigateToItemWithName(menuItem))
    onView(withId(R.id.toolbar))
        .check(matches(withToolBarText(menuItem)))
  }

  @Test
  fun testAddNewAccount() {
    val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    val account = AccountDaoManager.getDefaultAccountDao()
    val result = Intent()
    result.putExtra(AddNewAccountActivity.KEY_EXTRA_NEW_ACCOUNT, account)
    intending(hasComponent(ComponentName(targetContext, AddNewAccountActivity::class.java)))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, result))

    onView(withId(R.id.drawer_layout))
        .perform(open())
    onView(withId(R.id.layoutUserDetails))
        .check(matches(isDisplayed()))
        .perform(click(), click())

    try {
      val accountDaoSource = AccountDaoSource()
      accountDaoSource.addRow(targetContext, account.authCreds)
      accountDaoSource.setActiveAccount(targetContext, account.email)
    } catch (e: Exception) {
      e.printStackTrace()
    }

    onView(withId(R.id.viewIdAddNewAccount))
        .check(matches(isDisplayed())).perform(click())

    onView(withId(R.id.drawer_layout))
        .perform(open())
    onView(withId(R.id.textViewActiveUserEmail))
        .check(matches(isDisplayed())).check(matches(withText(account.email)))
  }

  @Test
  fun testChooseAnotherAccount() {
    onView(withId(R.id.drawer_layout))
        .perform(open())
    onView(withId(R.id.textViewActiveUserEmail))
        .check(matches(isDisplayed())).check(matches(withText(userWithMoreThan21LettersAccount.email)))
    onView(withId(R.id.layoutUserDetails))
        .check(matches(isDisplayed()))
        .perform(click(), click())
    onView(withText(userWithoutLetters.email))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(withId(R.id.drawer_layout))
        .perform(open())
    onView(withId(R.id.textViewActiveUserEmail))
        .check(matches(isDisplayed())).check(matches(withText(userWithoutLetters.email)))
  }

  private fun clickLogOut() {
    onView(withId(R.id.drawer_layout))
        .perform(open())
    onView(withId(R.id.navigationView))
        .perform(swipeUp())
    onView(withText(R.string.log_out)
    ).check(matches(isDisplayed()))
        .perform(click())
  }

  companion object {
    private val LOCAL_FOLDERS: MutableList<LocalFolder>
    private val INBOX_USER_WITH_MORE_THAN_21_LETTERS_ACCOUNT = LocalFolder(
        fullName = "INBOX",
        folderAlias = "INBOX",
        attributes = listOf("\\HasNoChildren"),
        isCustom = true)

    init {
      LOCAL_FOLDERS = ArrayList()
      LOCAL_FOLDERS.add(INBOX_USER_WITH_MORE_THAN_21_LETTERS_ACCOUNT)
      LOCAL_FOLDERS.add(LocalFolder(
          fullName = "Drafts",
          folderAlias = "Drafts",
          attributes = listOf("\\HasNoChildren", "\\Drafts")))
      LOCAL_FOLDERS.add(LocalFolder(
          fullName = "Sent",
          folderAlias = "Sent",
          attributes = listOf("\\HasNoChildren", "\\Sent")))
      LOCAL_FOLDERS.add(LocalFolder(
          fullName = "Junk",
          folderAlias = "Junk",
          attributes = listOf("\\HasNoChildren", "\\Junk")))
    }
  }
}
