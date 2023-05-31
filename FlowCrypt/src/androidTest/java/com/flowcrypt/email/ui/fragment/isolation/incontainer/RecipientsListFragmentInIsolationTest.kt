/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withEmptyRecyclerView
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.RecipientsListFragment
import com.flowcrypt.email.ui.base.BaseRecipientsListTest
import com.flowcrypt.email.viewaction.ClickOnViewInRecyclerViewItem
import org.hamcrest.Matchers.allOf
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
class RecipientsListFragmentInIsolationTest : BaseRecipientsListTest() {
  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(AddAccountToDatabaseRule())
    .around(ScreenshotTestRule())

  @Before
  fun launchFragmentInContainerWithPredefinedArgs() {
    launchFragmentInContainer<RecipientsListFragment>()
  }

  @Test
  fun testEmptyList() {
    unregisterCountingIdlingResource()
    Thread.sleep(2000)
    onView(withId(R.id.rVRecipients))
      .check(matches(withEmptyRecyclerView())).check(matches(not(isDisplayed())))
    onView(withId(R.id.emptyView))
      .check(matches(isDisplayed())).check(matches(withText(R.string.no_results)))
  }

  @Test
  fun testDeleteContacts() {
    unregisterCountingIdlingResource()
    addContactsToDatabase()
    Thread.sleep(2000)
    for (ignored in EMAILS) {
      onView(withId(R.id.rVRecipients))
        .perform(
          actionOnItemAtPosition<RecyclerView.ViewHolder>(
            0,
            ClickOnViewInRecyclerViewItem(R.id.iBtDeleteContact)
          )
        )
    }
    Thread.sleep(2000)
    onView(withId(R.id.emptyView))
      .check(matches(isDisplayed())).check(matches(withText(R.string.no_results)))
    clearContactsFromDatabase()
  }

  @Test
  fun testShowContactsWithPgp() {
    unregisterCountingIdlingResource()
    addContactsToDatabase()
    Thread.sleep(2000)

    onView(
      allOf(
        withId(R.id.imageViewPgp),
        withEffectiveVisibility(ViewMatchers.Visibility.GONE)
      )
    ).check(doesNotExist())

    clearContactsFromDatabase()
  }
}
