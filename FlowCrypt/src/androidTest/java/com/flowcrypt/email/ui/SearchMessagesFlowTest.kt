/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

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
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.junit.annotations.DependsOnMailServer
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withEmptyRecyclerView
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.util.AccountDaoManager
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.emptyString
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@DependsOnMailServer
class SearchMessagesFlowTest : BaseTest() {

  private val addAccountToDatabaseRule = AddAccountToDatabaseRule(
    AccountDaoManager.getDefaultAccountDao().copy(contactsLoaded = true)
  )

  override val activityScenarioRule = activityScenarioRule<MainActivity>()

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Before
  fun waitData() {
    //todo-denbond7 Need to improve this code after espresso updates
    Thread.sleep(2000)
  }

  @Before
  fun openMenuSearch() {
    onView(withId(R.id.menuSearch))
      .check(matches(isDisplayed()))
      .perform(click())
  }

  @Test
  fun testSearchQuery() {
    onView(allOf(withId(R.id.menuSearch), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
      .check(matches(isDisplayed()))
    onView(isAssignableFrom(EditText::class.java))
      .perform(clearText(), typeText(SECOND_QUERY_TEXT), pressImeActionButton())
    Thread.sleep(2000)
    onView(withId(R.id.recyclerViewMsgs))
      .check(matches(withRecyclerViewItemCount(2))).check(matches(isDisplayed()))
  }

  @Test
  fun testSearchOverSubjectBodyFrom() {
    onView(allOf(withId(R.id.menuSearch), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
      .check(matches(isDisplayed()))
    onView(isAssignableFrom(EditText::class.java))
      .perform(clearText(), typeText(QUERY_TEXT_FOR_SUBJECT_BODY_FROM), pressImeActionButton())
    Thread.sleep(2000)
    onView(withId(R.id.recyclerViewMsgs))
      .check(matches(withRecyclerViewItemCount(3))).check(matches(isDisplayed()))
  }

  @Test
  fun testShowNotEmptyList() {
    onView(withId(R.id.recyclerViewMsgs))
      .check(matches(isDisplayed()))
    onView(withId(R.id.recyclerViewMsgs))
      .check(matches(not(withEmptyRecyclerView())))
  }

  @Test
  fun testCheckNoResults() {
    onView(allOf(withId(R.id.menuSearch), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(isAssignableFrom(EditText::class.java))
      .perform(
        clearText(),
        typeText("The string with no results"),
        pressImeActionButton()
      )
    Thread.sleep(2000)
    onView(withId(R.id.tVEmpty))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testClearSearchView() {
    onView(isAssignableFrom(EditText::class.java))
      .perform(typeText(QUERY_TEXT_FOR_SUBJECT_BODY_FROM))
    onView(allOf(withId(R.id.menuSearch), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withId(androidx.appcompat.R.id.search_close_btn))
      .perform(click())
    onView(isAssignableFrom(EditText::class.java))
      .check(matches(withText(`is`(emptyString()))))
  }

  companion object {
    private const val SECOND_QUERY_TEXT = "Espresso"
    private const val QUERY_TEXT_FOR_SUBJECT_BODY_FROM = "android"
  }
}
