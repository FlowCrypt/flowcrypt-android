/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.database.Cursor
import android.text.format.DateFormat
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.flowcrypt.email.DoesNotNeedMailserver
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.dao.source.ContactsDaoSource
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddContactsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.ui.activity.settings.ContactsSettingsActivity
import com.flowcrypt.email.util.PrivateKeysManager
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.AfterClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author Denis Bondarenko
 *         Date: 10/2/19
 *         Time: 1:32 PM
 *         E-mail: DenBond7@gmail.com
 */
@LargeTest
@DoesNotNeedMailserver
@RunWith(AndroidJUnit4::class)
class PublicKeyDetailsFragmentTest : BaseTest() {

  override val activityTestRule: ActivityTestRule<*>? = ActivityTestRule(ContactsSettingsActivity::class.java)

  private val keyDetails = PrivateKeysManager.getNodeKeyDetailsFromAssets("node/denbond7@denbond7.com_pub.json")

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(AddAccountToDatabaseRule())
      .around(AddContactsToDatabaseRule(listOf(PgpContact(EMAIL_DENBOND7, USER_DENBOND7,
          keyDetails.publicKey, true, null, null, null, null, 0))))
      .around(activityTestRule)

  @Test
  fun testPubKeyDetails() {
    onData(withItemContent(EMAIL_DENBOND7))
        .inAdapterView(withId(R.id.listViewContacts))
        .perform(click())

    keyDetails.users!!.forEachIndexed { index, s ->
      onView(withText(getResString(R.string.template_user, index + 1, s)))
          .check(matches(isDisplayed()))
    }

    keyDetails.ids!!.forEachIndexed { index, s ->
      onView(withText(getResString(R.string.template_long_id, index + 1, s.longId!!)))
          .check(matches(isDisplayed()))

      onView(withText(s.keywords))
          .check(matches(isDisplayed()))
    }

    onView(withId(R.id.textViewAlgorithm))
        .check(matches(withText(getResString(R.string.template_algorithm, keyDetails.algo!!.algorithm!!))))
    onView(withId(R.id.textViewCreated))
        .check(matches(withText(getResString(R.string.template_created,
            DateFormat.getMediumDateFormat(getTargetContext()).format(
                Date(TimeUnit.MILLISECONDS.convert(keyDetails.created, TimeUnit.SECONDS)))))))
  }

  /**
   * Match an item in an adapter which has the given text
   */
  private fun withItemContent(itemTextMatcher: String): Matcher<Any> {
    // use preconditions to fail fast when a test is creating an invalid matcher.
    return object : BoundedMatcher<Any, Cursor>(Cursor::class.java) {
      public override fun matchesSafely(cursor: Cursor): Boolean {
        return cursor.getString(cursor.getColumnIndex(ContactsDaoSource.COL_EMAIL)) == itemTextMatcher
      }

      override fun describeTo(description: Description) {
        description.appendText("with item content: ")
      }
    }
  }

  companion object {
    private const val EMAIL_DENBOND7 = "denbond7@denbond7.com"
    private const val USER_DENBOND7 = "DenBond7"

    @AfterClass
    fun removeContactFromDatabase() {
      ContactsDaoSource().deletePgpContact(InstrumentationRegistry.getInstrumentation().targetContext, EMAIL_DENBOND7)
    }
  }
}
