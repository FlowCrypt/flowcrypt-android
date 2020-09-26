/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.widget.EditText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withEmptyRecyclerView
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.ui.activity.base.BaseEmailListActivityTest
import com.flowcrypt.email.util.AccountDaoManager
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.isEmptyString
import org.hamcrest.Matchers.not
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 03.05.2018
 * Time: 13:59
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@Ignore("fix me")
class SearchMessagesActivityTest : BaseEmailListActivityTest() {

  private val accountRule = AddAccountToDatabaseRule(AccountDaoManager.getDefaultAccountDao().copy(areContactsLoaded = true))

  override val activityScenarioRule = activityScenarioRule<SearchMessagesActivity>(
      intent = SearchMessagesActivity.newIntent(getTargetContext(), DEFAULT_QUERY_TEXT, LocalFolder(
          account = accountRule.account.email,
          fullName = FOLDER_NAME,
          folderAlias = FOLDER_NAME)))

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(accountRule)
      .around(RetryRule())
      .around(activityScenarioRule)

  @Test
  fun testDefaultSearchQueryAtStart() {
    onView(allOf(withId(R.id.menuSearch), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
        .check(matches(isDisplayed()))
    onView(isAssignableFrom(EditText::class.java))
        .check(matches(withText(DEFAULT_QUERY_TEXT)))
    onView(withId(R.id.recyclerViewMsgs))
        .check(matches(not(withEmptyRecyclerView())))
  }

  @Test
  @Ignore("fix me")
  fun testSearchQuery() {
    onView(withId(R.id.recyclerViewMsgs))
        .check(matches(withRecyclerViewItemCount(1))).check(matches(isDisplayed()))

    onView(allOf(withId(R.id.menuSearch), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
        .check(matches(isDisplayed()))
    onView(isAssignableFrom(EditText::class.java))
        .check(matches(withText(DEFAULT_QUERY_TEXT)))
        .perform(clearText(), typeText(SECOND_QUERY_TEXT), pressImeActionButton())
    onView(withId(R.id.recyclerViewMsgs))
        .check(matches(withRecyclerViewItemCount(2))).check(matches(isDisplayed()))
  }

  @Test
  fun testSearchOverSubjectBodyFrom() {
    onView(allOf(withId(R.id.menuSearch), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
        .check(matches(isDisplayed()))
    onView(isAssignableFrom(EditText::class.java))
        .check(matches(withText(DEFAULT_QUERY_TEXT)))
        .perform(clearText(), typeText(QUERY_TEXT_FOR_SUBJECT_BODY_FROM), pressImeActionButton())
    onView(withId(R.id.recyclerViewMsgs))
        .check(matches(withRecyclerViewItemCount(4))).check(matches(isDisplayed()))
  }

  @Test
  fun testShowNotEmptyList() {
    onView(withId(R.id.recyclerViewMsgs))
        .check(matches(isDisplayed()))
    onView(withId(R.id.recyclerViewMsgs))
        .check(matches(not(withEmptyRecyclerView())))
  }

  @Test
  fun testOpenSomeMsg() {
    testShowNotEmptyList()
    testRunMsgDetailsActivity(0)
  }

  @Test
  fun testCheckNoResults() {
    onView(allOf(withId(R.id.menuSearch), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(isAssignableFrom(EditText::class.java))
        .perform(clearText(), typeText("The string with no results"), pressImeActionButton())
    onView(withId(R.id.emptyView))
        .check(matches(isDisplayed()))
  }

  @Test
  fun testClearSearchView() {
    onView(allOf(withId(R.id.menuSearch), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(withId(androidx.appcompat.R.id.search_close_btn))
        .perform(click())
    onView(isAssignableFrom(EditText::class.java))
        .check(matches(withText(isEmptyString())))
        .check(matches(withHint(InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.search))))
  }

  companion object {
    private const val FOLDER_NAME = "INBOX"
    private const val DEFAULT_QUERY_TEXT = "Search"
    private const val SECOND_QUERY_TEXT = "Espresso"
    private const val QUERY_TEXT_FOR_SUBJECT_BODY_FROM = "android"
  }
}
