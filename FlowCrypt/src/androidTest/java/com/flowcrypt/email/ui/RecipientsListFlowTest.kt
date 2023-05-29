/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.ui.base.BaseRecipientsListTest
import com.flowcrypt.email.util.TestGeneralUtil
import org.hamcrest.Matchers.allOf
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
class RecipientsListFlowTest : BaseRecipientsListTest() {
  override val useIntents: Boolean = true
  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      destinationId = R.id.recipientsListFragment
    )
  )

  private val addAccountToDatabaseRule = AddAccountToDatabaseRule()
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule()

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testShowContactsWithPgpOnly() {
    unregisterCountingIdlingResource()
    addContactsToDatabase()
    Thread.sleep(2000)
    onView(withId(R.id.switchView))
      .perform(click())
    Thread.sleep(1000)
    onView(
      allOf(
        withId(R.id.imageViewPgp),
        withEffectiveVisibility(ViewMatchers.Visibility.GONE)
      )
    ).check(doesNotExist())
    clearContactsFromDatabase()
  }

  @Test
  fun testShowAllContactsAfterSwitch() {
    unregisterCountingIdlingResource()
    val contactWithoutPgp = "no_pgp@flowcrypt.test"
    addContactsToDatabase()
    addContactToDatabase(email = contactWithoutPgp, hasPgp = false)
    Thread.sleep(2000)
    onView(withId(R.id.rVRecipients))
      .check(matches(withRecyclerViewItemCount(EMAILS.size)))
    onView(withId(R.id.switchView))
      .perform(click())
    onView(withId(R.id.rVRecipients))
      .check(matches(withRecyclerViewItemCount(EMAILS.size + 1)))
    Thread.sleep(1000)

    onView(withId(R.id.rVRecipients))
      .perform(
        scrollTo<RecyclerView.ViewHolder>(
          allOf(
            hasDescendant(
              allOf(
                withId(R.id.imageViewPgp),
                withEffectiveVisibility(ViewMatchers.Visibility.GONE)
              )
            ),
            hasDescendant(withText(contactWithoutPgp)),
          )
        )
      )

    clearContactsFromDatabase()
  }
}
