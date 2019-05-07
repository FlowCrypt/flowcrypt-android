/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.view.View
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withChild
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.dao.source.ContactsDaoSource
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withEmptyListView
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddContactsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.*


/**
 * @author Denis Bondarenko
 * Date: 23.02.2018
 * Time: 10:34
 * E-mail: DenBond7@gmail.com
 */

@LargeTest
@RunWith(AndroidJUnit4::class)
class SelectContactsActivityTest : BaseTest() {

  override val activityTestRule: ActivityTestRule<*>? = ActivityTestRule(SelectContactsActivity::class.java)

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(AddAccountToDatabaseRule())
      .around(AddContactsToDatabaseRule(CONTACTS))
      .around(activityTestRule)

  @Test
  fun testShowEmptyView() {
    clearContactsFromDatabase()

    //Need to wait a little while data will be updated
    Thread.sleep(2000)

    onView(withId(R.id.listViewContacts))
        .check(matches(withEmptyListView())).check(matches(not<View>(isDisplayed())))
    onView(withId(R.id.emptyView))
        .check(matches(isDisplayed())).check(matches(withText(R.string.no_results)))
  }

  @Test
  fun testShowListContacts() {
    onView(withId(R.id.listViewContacts))
        .check(matches(isDisplayed())).check(matches(not<View>(withEmptyListView())))
    onView(withId(R.id.emptyView))
        .check(matches(not<View>(isDisplayed())))

    for (i in EMAILS.indices) {
      if (i % 2 == 0) {
        checkIsDataItemDisplayed(i, R.id.textViewName, getUserName(EMAILS[i]))
      } else {
        checkIsDataItemDisplayed(i, R.id.textViewOnlyEmail, EMAILS[i])
      }
    }
  }

  @Test
  fun testCheckSearchExistingContact() {
    onView(withId(R.id.menuSearch))
        .check(matches(isDisplayed()))
        .perform(click())

    for (i in EMAILS.indices) {
      if (i % 2 == 0) {
        checkIsTypedUserFound(R.id.textViewName, getUserName(EMAILS[i]))
      } else {
        checkIsTypedUserFound(R.id.textViewOnlyEmail, EMAILS[i])
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
    onView(withId(R.id.listViewContacts))
        .check(matches(withEmptyListView()))
    onView(withId(R.id.emptyView))
        .check(matches(isDisplayed())).check(matches(withText(R.string.no_results)))
  }

  private fun clearContactsFromDatabase() {
    val contactsDaoSource = ContactsDaoSource()
    for (email in EMAILS) {
      contactsDaoSource.deletePgpContact(getTargetContext(), email)
    }
  }

  private fun checkIsTypedUserFound(viewId: Int, viewText: String) {
    onView(withId(com.google.android.material.R.id.search_src_text))
        .perform(clearText(), typeText(viewText))
    closeSoftKeyboard()
    onView(withId(viewId))
        .check(matches(isDisplayed())).check(matches(withText(viewText)))
  }

  private fun checkIsDataItemDisplayed(index: Int, viewId: Int, viewText: String) {
    onData(anything())
        .inAdapterView(withId(R.id.listViewContacts))
        .onChildView(withChild(withId(viewId)))
        .atPosition(index)
        .check(matches(withChild(withText(viewText))))
  }

  companion object {
    private val EMAILS = arrayOf("contact_0@denbond7.com", "contact_1@denbond7.com", "contact_2@denbond7.com", "contact_3@denbond7.com")
    private val CONTACTS = ArrayList<PgpContact>()

    init {
      for (i in EMAILS.indices) {
        val email = EMAILS[i]
        val pgpContact: PgpContact
        if (i % 2 == 0) {
          pgpContact = PgpContact(email, getUserName(email), "publicKey", true, null, false, null, null, null, 0)
        } else {
          pgpContact = PgpContact(email, null, "publicKey", true, null,
              false, null, null, null, 0)
        }
        CONTACTS.add(pgpContact)
      }
    }

    private fun getUserName(email: String): String {
      return email.substring(0, email.indexOf(TestConstants.COMMERCIAL_AT_SYMBOL))
    }
  }
}
