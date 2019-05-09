/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.flowcrypt.email.R
import com.flowcrypt.email.assertions.RecyclerViewItemCountAssertion
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.dao.source.ContactsDaoSource
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import com.flowcrypt.email.viewaction.ClickOnViewInRecyclerViewItem
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
@LargeTest
@RunWith(AndroidJUnit4::class)
class PreviewImportPgpContactActivityTest : BaseTest() {
  override val activityTestRule: ActivityTestRule<*>? =
      ActivityTestRule<PreviewImportPgpContactActivity>(PreviewImportPgpContactActivity::class.java, false, false)

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(AddAccountToDatabaseRule())
      .around(activityTestRule)

  private val singlePublicKeyForUnsavedContact: String
    get() = PrivateKeysManager.getNodeKeyDetailsFromAssets("node/default@denbond7.com_fisrtKey_pub.json")
        .publicKey

  private val tenPubKeys: String =
      TestGeneralUtil.readFileFromAssetsAsString(getContext(), "pgp/pub_keys_2048_bits_10.asc")

  @Test
  fun testShowHelpScreen() {
    activityTestRule?.launchActivity(
        PreviewImportPgpContactActivity.newIntent(getTargetContext(), singlePublicKeyForUnsavedContact))
    testHelpScreen()
  }

  @Test
  fun testIsDisplayedSingleItem() {
    val pgpContact = PgpContact("default@denbond7.com", null,
        singlePublicKeyForUnsavedContact, true, null, null, null, null, 0)
    ContactsDaoSource().addRow(getTargetContext(), pgpContact)
    activityTestRule?.launchActivity(
        PreviewImportPgpContactActivity.newIntent(getTargetContext(), singlePublicKeyForUnsavedContact))
    onView(withId(R.id.recyclerViewContacts))
        .check(RecyclerViewItemCountAssertion(1))
    onView(withText(getResString(R.string.template_message_part_public_key_owner, "default@denbond7.com")))
        .check(matches(isDisplayed()))
  }

  @Test
  fun testIsDisplayedLabelAlreadyImported() {
    activityTestRule?.launchActivity(
        PreviewImportPgpContactActivity.newIntent(getTargetContext(), singlePublicKeyForUnsavedContact))
    onView(withId(R.id.recyclerViewContacts))
        .check(RecyclerViewItemCountAssertion(1))
  }

  @Test
  fun testSaveButtonForSingleContact() {
    activityTestRule?.launchActivity(
        PreviewImportPgpContactActivity.newIntent(getTargetContext(), singlePublicKeyForUnsavedContact))
    onView(withId(R.id.recyclerViewContacts))
        .check(RecyclerViewItemCountAssertion(1))
    onView(withId(R.id.recyclerViewContacts))
        .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0,
            ClickOnViewInRecyclerViewItem(R.id.buttonSaveContact)))
    checkIsToastDisplayed(activityTestRule?.activity, getResString(R.string.contact_successfully_saved))
  }

  @Test
  fun testIsImportAllButtonDisplayed() {
    activityTestRule?.launchActivity(PreviewImportPgpContactActivity.newIntent(getTargetContext(), tenPubKeys))
    onView(withId(R.id.buttonImportAll))
        .check(matches(isDisplayed()))
  }

  @Test
  fun testLoadLotOfContacts() {
    val countOfKeys = 10

    activityTestRule?.launchActivity(PreviewImportPgpContactActivity.newIntent(getTargetContext(), tenPubKeys))
    onView(withId(R.id.recyclerViewContacts))
        .check(RecyclerViewItemCountAssertion(countOfKeys))
        .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(countOfKeys - 1))
  }
}
