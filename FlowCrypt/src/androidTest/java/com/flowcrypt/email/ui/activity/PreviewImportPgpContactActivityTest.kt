/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.assertions.RecyclerViewItemCountAssertion
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import com.flowcrypt.email.viewaction.ClickOnViewInRecyclerViewItem
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 21.05.2018
 * Time: 14:50
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@Ignore("We've migrated this functionality to NavController. Need to review this class.")
class PreviewImportPgpContactActivityTest : BaseTest() {
  /*override val activeActivityRule =
    lazyActivityScenarioRule<PreviewImportPgpContactActivity>(launchActivity = false)*/
  /*override val activityScenario: ActivityScenario<*>?
    get() = activeActivityRule.scenario*/

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(AddAccountToDatabaseRule())
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  private val singlePublicKeyForUnsavedContact: String? =
    PrivateKeysManager.getPgpKeyDetailsFromAssets(
      "pgp/default@flowcrypt.test_fisrtKey_pub.asc"
    ).publicKey

  private val tenPubKeys: String =
    TestGeneralUtil.readFileFromAssetsAsString("pgp/keys/10_pub_keys_armored_own_header.asc")

  @Test
  fun testIsDisplayedSingleItem() {
    /*val pgpContact = PgpContact(
      "default@flowcrypt.test", null,
      singlePublicKeyForUnsavedContact, true, null, null, 0
    )*/
    /*FlowCryptRoomDatabase.getDatabase(getTargetContext()).recipientDao()
      .insert(pgpContact.toRecipientEntity())*/
    /*activeActivityRule.launch(
      PreviewImportPgpContactActivity.newIntent(
        getTargetContext(),
        singlePublicKeyForUnsavedContact
      )
    )*/
    registerAllIdlingResources()
    onView(withId(R.id.recyclerViewContacts))
      .check(RecyclerViewItemCountAssertion(1))
    onView(
      withText(
        getResString(
          R.string.template_message_part_public_key_owner,
          "default@flowcrypt.test"
        )
      )
    )
      .check(matches(isDisplayed()))
  }

  @Test
  fun testIsDisplayedLabelAlreadyImported() {
    /*activeActivityRule.launch(
      PreviewImportPgpContactActivity.newIntent(
        getTargetContext(),
        singlePublicKeyForUnsavedContact
      )
    )*/
    registerAllIdlingResources()
    onView(withId(R.id.recyclerViewContacts))
      .check(RecyclerViewItemCountAssertion(1))
  }

  @Test
  fun testSaveButtonForSingleContact() {
    /*activeActivityRule.launch(
      PreviewImportPgpContactActivity.newIntent(
        getTargetContext(),
        singlePublicKeyForUnsavedContact
      )
    )*/
    registerAllIdlingResources()
    onView(withId(R.id.recyclerViewContacts))
      .check(RecyclerViewItemCountAssertion(1))
    onView(withId(R.id.recyclerViewContacts))
      .perform(
        RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
          0,
          ClickOnViewInRecyclerViewItem(R.id.buttonSaveContact)
        )
      )
    isToastDisplayed(getResString(R.string.pub_key_successfully_imported))
  }

  @Test
  fun testIsImportAllButtonDisplayed() {
    /*activeActivityRule.launch(
      PreviewImportPgpContactActivity.newIntent(
        getTargetContext(),
        tenPubKeys
      )
    )*/
    registerAllIdlingResources()
    onView(withId(R.id.btImportAll))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testLoadLotOfContacts() {
    val countOfKeys = 10

    /*activeActivityRule.launch(
      PreviewImportPgpContactActivity.newIntent(
        getTargetContext(),
        tenPubKeys
      )
    )*/
    registerAllIdlingResources()
    onView(withId(R.id.recyclerViewContacts))
      .check(RecyclerViewItemCountAssertion(countOfKeys))
      .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(countOfKeys - 1))
  }
}
