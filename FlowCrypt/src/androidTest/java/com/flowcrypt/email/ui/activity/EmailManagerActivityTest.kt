/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions.open
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.junit.annotations.DependsOnMailServer
import com.flowcrypt.email.junit.annotations.NotReadyForCI
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withEmptyRecyclerView
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withToolBarText
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddLabelsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.base.BaseEmailListActivityTest
import com.flowcrypt.email.ui.activity.settings.SettingsActivity
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.viewaction.CustomViewActions.Companion.navigateToItemWithName
import org.hamcrest.CoreMatchers.anyOf
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 23.03.2018
 * Time: 16:16
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class EmailManagerActivityTest : BaseEmailListActivityTest() {
  override val useIntents: Boolean = true
  override val activityScenarioRule = activityScenarioRule<EmailManagerActivity>()

  private val userWithoutLetters = AccountDaoManager.getUserWithoutLetters()
  private val userWithMoreThan21LettersAccount = AccountDaoManager.getUserWithMoreThan21Letters()

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(AddAccountToDatabaseRule(userWithoutLetters))
    .around(AddAccountToDatabaseRule(userWithMoreThan21LettersAccount))
    .around(AddLabelsToDatabaseRule(userWithMoreThan21LettersAccount, LOCAL_FOLDERS))
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  @DependsOnMailServer
  @NotReadyForCI
  fun testComposeFloatButton() {
    onView(withId(R.id.floatActionButtonCompose))
      .check(matches(isDisplayed()))
      .perform(click())
    intended(hasComponent(CreateMessageActivity::class.java.name))
    onView(allOf(withText(R.string.compose), withParent(withId(R.id.toolbar))))
      .check(matches(isDisplayed()))
  }

  @Test
  @DependsOnMailServer
  @NotReadyForCI
  fun testRunMsgDetailsActivity() {
    //need to add some timeout while database will be updated
    Thread.sleep(1000)
    testRunMsgDetailsActivity(0)
  }

  @Test
  @DependsOnMailServer
  @NotReadyForCI
  fun testForceLoadMsgs() {
    //need to add some timeout while database will be updated
    Thread.sleep(1000)

    onView(withId(R.id.rVMsgs))
      .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, scrollTo()))
    onView(withId(R.id.rVMsgs))
      .check(matches(isDisplayed()))
      .perform(swipeDown())
    onView(withId(R.id.rVMsgs))
      .check(matches(not(withEmptyRecyclerView())))
      .check(matches(isDisplayed()))
  }

  @Test
  @DependsOnMailServer
  @NotReadyForCI
  fun testOpenAndSwipeNavigationView() {
    onView(withId(R.id.drawer_layout))
      .perform(open())
    onView(withId(R.id.navigationView))
      .perform(swipeUp())
  }

  @Test
  @NotReadyForCI
  fun testShowSplashActivityAfterLogout() {
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(getTargetContext())
    val nonActiveAccounts = roomDatabase.accountDao().getAllNonactiveAccounts()
    roomDatabase.accountDao().delete(nonActiveAccounts)
    clickLogOut()
    intended(hasComponent(MainActivity::class.java.name))
  }

  @Test
  @DependsOnMailServer
  @NotReadyForCI
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
  @DependsOnMailServer
  @NotReadyForCI
  fun testSwitchLabels() {
    val menuItem = "Sent"
    onView(withId(R.id.toolbar)).check(
      matches(
        anyOf(
          withToolBarText("INBOX"),
          withToolBarText(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(
              R.string.loading
            )
          )
        )
      )
    )

    onView(withId(R.id.drawer_layout))
      .perform(open())
    onView(withId(R.id.navigationView))
      .perform(navigateToItemWithName(menuItem))
    onView(withId(R.id.toolbar))
      .check(matches(withToolBarText(menuItem)))
  }

  @Test
  @NotReadyForCI
  fun testShowAnotherAccounts() {
    onView(withId(R.id.drawer_layout))
      .perform(open())
    onView(withId(R.id.textViewActiveUserEmail))
      .check(matches(isDisplayed()))
      .check(matches(withText(userWithMoreThan21LettersAccount.email)))
    onView(withId(R.id.layoutUserDetails))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withText(userWithoutLetters.email))
      .check(matches(isDisplayed()))
  }

  private fun clickLogOut() {
    onView(withId(R.id.drawer_layout))
      .perform(open())
    onView(withId(R.id.navigationView))
      .perform(swipeUp())
    onView(withText(R.string.log_out))
      .check(matches(isDisplayed()))
      .perform(click())
  }

  companion object {
    private val LOCAL_FOLDERS: MutableList<LocalFolder>
    private val userWithMoreThan21LettersAccount = AccountDaoManager.getUserWithMoreThan21Letters()
    private val INBOX_USER_WITH_MORE_THAN_21_LETTERS_ACCOUNT = LocalFolder(
      account = userWithMoreThan21LettersAccount.email,
      fullName = "INBOX",
      folderAlias = "INBOX",
      attributes = listOf("\\HasNoChildren"),
      isCustom = false
    )

    init {
      LOCAL_FOLDERS = ArrayList()
      LOCAL_FOLDERS.add(INBOX_USER_WITH_MORE_THAN_21_LETTERS_ACCOUNT)
      LOCAL_FOLDERS.add(
        LocalFolder(
          account = userWithMoreThan21LettersAccount.email,
          fullName = "Drafts",
          folderAlias = "Drafts",
          attributes = listOf("\\HasNoChildren", "\\Drafts")
        )
      )
      LOCAL_FOLDERS.add(
        LocalFolder(
          account = userWithMoreThan21LettersAccount.email,
          fullName = "Sent",
          folderAlias = "Sent",
          attributes = listOf("\\HasNoChildren", "\\Sent")
        )
      )
      LOCAL_FOLDERS.add(
        LocalFolder(
          account = userWithMoreThan21LettersAccount.email,
          fullName = "Junk",
          folderAlias = "Junk",
          attributes = listOf("\\HasNoChildren", "\\Junk")
        )
      )
    }
  }
}
