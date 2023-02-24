/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddRecipientsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.CreateMessageActivity
import com.flowcrypt.email.util.TestGeneralUtil
import com.flowcrypt.email.viewaction.CustomViewActions.doNothing
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
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
class SelectRecipientsFragmentFlowTest : BaseTest() {
  override val activityScenarioRule = activityScenarioRule<CreateMessageActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      navGraphId = R.navigation.create_msg_graph,
      activityClass = CreateMessageActivity::class.java,
      destinationId = R.id.selectRecipientsFragment
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(AddAccountToDatabaseRule())
    .around(AddRecipientsToDatabaseRule(CONTACTS))
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testShowListContacts() {
    onView(withId(R.id.tVEmpty))
      .check(matches(not(isDisplayed())))

    for (i in EMAILS.indices) {
      if (i % 2 == 0) {
        onView(withId(R.id.recyclerViewContacts)).perform(
          actionOnItem<RecyclerView.ViewHolder>
            (
            hasDescendant(allOf(withId(R.id.tVName), withText(getUserName(EMAILS[i])))),
            doNothing()
          )
        )
      } else {
        onView(withId(R.id.recyclerViewContacts)).perform(
          actionOnItem<RecyclerView.ViewHolder>
            (hasDescendant(allOf(withId(R.id.tVOnlyEmail), withText(EMAILS[i]))), doNothing())
        )
      }
    }
  }

  @Test
  fun testSearchExistingContact() {
    onView(withId(R.id.menuSearch))
      .check(matches(isDisplayed()))
      .perform(click())

    for (i in EMAILS.indices) {
      if (i % 2 == 0) {
        checkIsTypedUserFound(R.id.tVName, getUserName(EMAILS[i]))
      } else {
        checkIsTypedUserFound(R.id.tVOnlyEmail, EMAILS[i])
      }
    }
  }

  @Test
  fun testNoResults() {
    onView(withId(R.id.menuSearch))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withId(com.google.android.material.R.id.search_src_text))
      .perform(clearText(), typeText("some email"))
    closeSoftKeyboard()
    onView(withId(R.id.tVEmpty))
      .check(matches(isDisplayed())).check(matches(withText(R.string.no_results)))
  }

  private fun checkIsTypedUserFound(viewId: Int, viewText: String) {
    onView(withId(com.google.android.material.R.id.search_src_text))
      .perform(clearText(), typeText(viewText))
    closeSoftKeyboard()
    onView(withId(viewId))
      .check(matches(isDisplayed())).check(matches(withText(viewText)))
  }

  companion object {
    private val EMAILS = arrayOf(
      "contact_0@flowcrypt.test",
      "contact_1@flowcrypt.test",
      "contact_2@flowcrypt.test",
      "contact_3@flowcrypt.test"
    )
    private val CONTACTS = ArrayList<RecipientWithPubKeys>()

    init {
      for (i in EMAILS.indices) {
        val email = EMAILS[i]
        val recipientWithPubKeys = RecipientWithPubKeys(
          RecipientEntity(
            email = email,
            name = if (i % 2 == 0) getUserName(email) else null
          ),
          listOf(
            PublicKeyEntity(
              recipient = email,
              fingerprint = "FINGERPRINT",
              publicKey = "PUBLIC_KEY".toByteArray()
            )
          )
        )
        CONTACTS.add(recipientWithPubKeys)
      }
    }

    private fun getUserName(email: String): String {
      return email.substring(0, email.indexOf(TestConstants.COMMERCIAL_AT_SYMBOL))
    }
  }
}
