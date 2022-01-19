/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.hasTextColor
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withEmptyRecyclerView
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.settings.SettingsActivity
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 20.02.2018
 * Time: 15:42
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class PrivateKeysListFragmentTest : BaseTest() {

  override val useIntents: Boolean = true
  override val activityScenarioRule = activityScenarioRule<SettingsActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      uri = "flowcrypt://email.flowcrypt.com/settings/keys"
    )
  )

  private val addAccountToDatabaseRule = AddAccountToDatabaseRule()
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule()

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Before
  fun waitData() {
    //todo-denbond7 need to wait while activity lunches a fragment and loads data via ROOM.
    // Need to improve this code after espresso updates
    Thread.sleep(3000)
  }

  @Test
  fun testAddNewKeys() {
    intending(hasComponent(ComponentName(getTargetContext(), ImportPrivateKeyActivity::class.java)))
      .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

    val details =
      PrivateKeysManager.getPgpKeyDetailsFromAssets("pgp/default@flowcrypt.test_secondKey_prv_default.asc")
    PrivateKeysManager.saveKeyToDatabase(
      accountEntity = addAccountToDatabaseRule.account,
      pgpKeyDetails = details,
      passphrase = TestConstants.DEFAULT_PASSWORD,
      sourceType = KeyImportDetails.SourceType.EMAIL
    )

    onView(withId(R.id.floatActionButtonAddKey))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withId(R.id.recyclerViewKeys))
      .check(matches(isDisplayed()))
      .check(matches(withRecyclerViewItemCount(2)))
  }

  @Test
  fun testKeyExists() {
    onView(withId(R.id.recyclerViewKeys))
      .check(matches(not(withEmptyRecyclerView()))).check(matches(isDisplayed()))
    onView(withId(R.id.emptyView))
      .check(matches(not(isDisplayed())))
  }

  @Test
  fun testShowKeyDetailsScreen() {
    onView(withId(R.id.recyclerViewKeys))
      .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
  }

  @Test
  fun testKeyDetailsTestPassPhraseMismatch() {
    val details = PrivateKeysManager.getPgpKeyDetailsFromAssets(
      "pgp/default@flowcrypt.test_secondKey_prv_default.asc"
    )
    PrivateKeysManager.saveKeyToDatabase(
      accountEntity = addAccountToDatabaseRule.account,
      pgpKeyDetails = details,
      passphrase = "wrong passphrase",
      sourceType = KeyImportDetails.SourceType.EMAIL
    )

    onView(withId(R.id.recyclerViewKeys))
      .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

    onView(withId(R.id.tVPassPhraseVerification))
      .check(matches(withText(getResString(R.string.stored_pass_phrase_mismatch))))
      .check(matches(hasTextColor(R.color.red)))
  }
}
