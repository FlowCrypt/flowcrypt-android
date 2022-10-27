/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.junit.annotations.NotReadyForCI
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withEmptyRecyclerView
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.RecipientsListFragment
import com.flowcrypt.email.viewaction.ClickOnViewInRecyclerViewItem
import org.hamcrest.Matchers.not
import org.junit.AfterClass
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 20.02.2018
 * Time: 15:43
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class RecipientsListFragmentInIsolationTest : BaseTest() {
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
    onView(withId(R.id.rVRecipients))
      .check(matches(withEmptyRecyclerView())).check(matches(not(isDisplayed())))
    onView(withId(R.id.emptyView))
      .check(matches(isDisplayed())).check(matches(withText(R.string.no_results)))
  }

  @Test
  @NotReadyForCI
  fun testDeleteContacts() {
    addContactsToDatabase()
    //todo-denbond7 improve this in the future. When we have a good solution for ROOM, coroutines and Espresso
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

  private fun addContactsToDatabase() {
    for (email in EMAILS) {
      roomDatabase.recipientDao().insert(RecipientEntity(email = email))
      roomDatabase.pubKeyDao().insert(
        PublicKeyEntity(
          recipient = email,
          fingerprint = "FINGER",
          publicKey = "KEY".toByteArray()
        )
      )
    }
  }

  companion object {
    private val EMAILS = arrayOf(
      "contact_0@flowcrypt.test",
      "contact_1@flowcrypt.test",
      "contact_2@flowcrypt.test",
      "contact_3@flowcrypt.test"
    )

    @AfterClass
    fun clearContactsFromDatabase() {
      for (email in EMAILS) {
        val dao = FlowCryptRoomDatabase.getDatabase(ApplicationProvider.getApplicationContext())
          .recipientDao()

        val contact = dao.getRecipientByEmail(email) ?: continue
        dao.delete(contact)
      }
    }
  }
}
