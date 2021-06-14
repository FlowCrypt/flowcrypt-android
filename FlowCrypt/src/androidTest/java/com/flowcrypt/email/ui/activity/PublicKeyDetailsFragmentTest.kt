/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.database.Cursor
import android.os.Environment
import android.text.format.DateFormat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddContactsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.settings.ContactsSettingsActivity
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import org.hamcrest.CoreMatchers
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.core.AllOf
import org.junit.AfterClass
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.File
import java.util.Date

/**
 * @author Denis Bondarenko
 *         Date: 10/2/19
 *         Time: 1:32 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class PublicKeyDetailsFragmentTest : BaseTest() {
  override val useIntents: Boolean = true
  override val activityScenarioRule = activityScenarioRule<ContactsSettingsActivity>()

  private val keyDetails =
    PrivateKeysManager.getPgpKeyDetailsFromAssets("pgp/expired@flowcrypt.test_pub.asc")

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(ClearAppSettingsRule())
    .around(AddAccountToDatabaseRule())
    .around(
      AddContactsToDatabaseRule(
        listOf(
          PgpContact(
            EMAIL_DENBOND7, USER_DENBOND7,
            keyDetails.publicKey, true, null, null, 0
          )
        )
      )
    )
    .around(RetryRule.DEFAULT)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Before
  fun waitData() {
    //todo-denbond7 need to wait while activity lunches a fragment and loads data via ROOM.
    // Need to improve this code after espresso updates
    Thread.sleep(1000)
  }

  @Test
  fun testPubKeyDetails() {
    chooseContact()

    keyDetails.users.forEachIndexed { index, s ->
      onView(withText(getResString(R.string.template_user, index + 1, s)))
        .check(matches(isDisplayed()))
    }

    keyDetails.ids.forEachIndexed { index, s ->
      onView(withText(getResString(R.string.template_fingerprint_2, index + 1, s.fingerprint)))
        .check(matches(isDisplayed()))
    }

    onView(withId(R.id.textViewAlgorithm))
      .check(
        matches(
          withText(
            getResString(
              R.string.template_algorithm,
              keyDetails.algo.algorithm!!
            )
          )
        )
      )
    onView(withId(R.id.textViewCreated))
      .check(
        matches(
          withText(
            getResString(
              R.string.template_created,
              DateFormat.getMediumDateFormat(getTargetContext()).format(
                Date(keyDetails.created)
              )
            )
          )
        )
      )
  }

  @Test
  fun testActionCopy() {
    chooseContact()

    onView(withId(R.id.menuActionCopy))
      .check(matches(isDisplayed()))
      .perform(click())
    isToastDisplayed(getResString(R.string.public_key_copied_to_clipboard))
    UiThreadStatement.runOnUiThread {
      checkClipboardText(TestGeneralUtil.replaceVersionInKey(keyDetails.publicKey))
    }
  }

  @Test
  fun testActionSave() {
    chooseContact()

    val sanitizedEmail = EMAIL_DENBOND7.replace("[^a-z0-9]".toRegex(), "")
    val fileName = "0x" + keyDetails.fingerprint + "-" + sanitizedEmail + "-publickey" + ".asc"

    val file =
      File(getTargetContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

    if (file.exists()) {
      file.delete()
    }

    val resultData = Intent()
    resultData.data =
      FileProvider.getUriForFile(getTargetContext(), Constants.FILE_PROVIDER_AUTHORITY, file)

    Intents.intending(
      AllOf.allOf(
        IntentMatchers.hasAction(Intent.ACTION_CREATE_DOCUMENT),
        IntentMatchers.hasCategories(CoreMatchers.hasItem(Matchers.equalTo(Intent.CATEGORY_OPENABLE))),
        IntentMatchers.hasType(Constants.MIME_TYPE_PGP_KEY)
      )
    )
      .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultData))

    onView(withId(R.id.menuActionSave))
      .check(matches(isDisplayed()))
      .perform(click())

    isToastDisplayed(getResString(R.string.saved))
  }

  @Test
  fun testActionDelete() {
    chooseContact()

    openActionBarOverflowOrOptionsMenu(getTargetContext())
    onView(withText(R.string.delete))
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withText(R.string.no_results))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testActionHelp() {
    testHelpScreen()
  }

  private fun chooseContact() {
    onView(withId(R.id.recyclerViewContacts))
      .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
  }

  /**
   * Match an item in an adapter which has the given text
   */
  private fun withItemContent(itemTextMatcher: String): Matcher<Any> {
    // use preconditions to fail fast when a test is creating an invalid matcher.
    return object : BoundedMatcher<Any, Cursor>(Cursor::class.java) {
      public override fun matchesSafely(cursor: Cursor): Boolean {
        //todo-denbond7 - fix me
        return "cursor.getString(cursor.getColumnIndex(ContactsDaoSource.COL_EMAIL))" == itemTextMatcher
      }

      override fun describeTo(description: Description) {
        description.appendText("with item content: ")
      }
    }
  }

  companion object {
    private const val EMAIL_DENBOND7 = "denbond7@flowcrypt.test"
    private const val USER_DENBOND7 = "DenBond7"

    @AfterClass
    @JvmStatic
    fun removeContactFromDatabase() {
      val dao =
        FlowCryptRoomDatabase.getDatabase(ApplicationProvider.getApplicationContext()).contactsDao()
      dao.getContactByEmail(EMAIL_DENBOND7)?.let { dao.delete(it) }
    }
  }
}
