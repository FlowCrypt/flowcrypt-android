/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import android.view.KeyEvent
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressKey
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
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
import java.util.concurrent.TimeUnit

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@FlowCryptTestSettings(useIntents = true, useCommonIdling = false)
class RecipientsListFlowTest : BaseRecipientsListTest() {
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
    addContactsToDatabase()
    waitForObjectWithText(EMAILS[3], TimeUnit.SECONDS.toMillis(5))
    onView(withId(R.id.switchView))
      .perform(click())
    Thread.sleep(1000)
    onView(
      allOf(
        withId(R.id.imageViewPgp),
        withEffectiveVisibility(Visibility.GONE)
      )
    ).check(doesNotExist())
    clearContactsFromDatabase()
  }

  @Test
  fun testShowAllContactsAfterSwitch() {
    val contactWithoutPgp = "no_pgp@flowcrypt.test"
    addContactsToDatabase()
    addContactToDatabase(email = contactWithoutPgp, hasPgp = false)
    waitForObjectWithText(EMAILS[3], TimeUnit.SECONDS.toMillis(5))
    onView(withId(R.id.rVRecipients))
      .check(matches(withRecyclerViewItemCount(EMAILS.size)))
    onView(withId(R.id.switchView))
      .perform(click())
    waitForObjectWithText(contactWithoutPgp, TimeUnit.SECONDS.toMillis(5))
    onView(withId(R.id.rVRecipients))
      .check(matches(withRecyclerViewItemCount(EMAILS.size + 1)))
    Thread.sleep(1000)
    testSomeContact(contactWithoutPgp, Visibility.GONE)
    clearContactsFromDatabase()
  }

  @Test
  fun testFilterContacts() {
    val contactWithPgp = "has_pgp_00@flowcrypt.test"
    val contactsWithPgp = arrayOf(
      contactWithPgp,
      "has_pgp_01@flowcrypt.test",
      "has_pgp_02@flowcrypt.test",
      "has_pgp_03@flowcrypt.test"
    )

    val contactWithoutPgp = "no_pgp_00@flowcrypt.test"
    val contactsWithoutPgp = arrayOf(
      contactWithoutPgp,
      "no_pgp_1@flowcrypt.test",
      "no_pgp_2@flowcrypt.test",
      "no_pgp_3@flowcrypt.test"
    )

    contactsWithPgp.forEach { addContactToDatabase(email = it) }
    contactsWithoutPgp.forEach { addContactToDatabase(email = it, hasPgp = false) }

    waitForObjectWithText(contactWithPgp, TimeUnit.SECONDS.toMillis(5))
    onView(withId(R.id.rVRecipients))
      .check(matches(withRecyclerViewItemCount(contactsWithPgp.size)))

    onView(withId(R.id.menuSearch))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withId(com.google.android.material.R.id.search_src_text))
      .perform(clearText(), replaceText("00"))
      .perform(pressKey(KeyEvent.KEYCODE_ENTER))
    closeSoftKeyboard()

    waitForObjectWithText(contactWithPgp, TimeUnit.SECONDS.toMillis(5))
    onView(withId(R.id.rVRecipients))
      .check(matches(withRecyclerViewItemCount(1)))
    testSomeContact(contactWithPgp, Visibility.VISIBLE)
    onView(
      allOf(
        withId(R.id.imageViewPgp),
        withEffectiveVisibility(Visibility.GONE)
      )
    ).check(doesNotExist())

    onView(withId(R.id.switchView))
      .perform(click())
    waitForObjectWithText(contactWithoutPgp, TimeUnit.SECONDS.toMillis(5))
    onView(withId(R.id.rVRecipients))
      .check(matches(withRecyclerViewItemCount(2)))
    testSomeContact(contactWithPgp, Visibility.VISIBLE)
    testSomeContact(contactWithoutPgp, Visibility.GONE)
    Thread.sleep(1000)

    clearContactsFromDatabase()
  }
}
