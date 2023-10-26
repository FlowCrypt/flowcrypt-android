/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
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
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.File

/**
 * @author Denys Bondarenko
 */
@FlowCryptTestSettings(useIntents = true)
@MediumTest
@RunWith(AndroidJUnit4::class)
class PrivateKeysListFragmentFlowTest : BaseTest() {
  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      destinationId = R.id.privateKeysListFragment
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
  fun testNavigationToImportKeyScreen() {
    onView(withId(R.id.floatActionButtonAddKey))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withText(R.string.import_private_key))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testShowKeyDetailsScreen() {
    onView(withId(R.id.recyclerViewKeys))
      .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
  }

  @Test
  fun testImportFromFileUnprotectedKey() {
    testImportUnprotectedPrivateKey { pgpKeyDetails ->
      val fileWithPublicKey: File = TestGeneralUtil.createFileWithTextContent(
        TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER + "_prv.asc",
        requireNotNull(pgpKeyDetails.privateKey)
      )

      val resultData =
        TestGeneralUtil.genIntentWithPersistedReadPermissionForFile(fileWithPublicKey)
      intendingActivityResultContractsGetContent(resultData = resultData)

      onView(withId(R.id.buttonLoadFromFile))
        .check(matches(isDisplayed()))
        .perform(click())
    }
  }

  @Test
  fun testImportFromClipboardUnprotectedKey() {
    testImportUnprotectedPrivateKey { pgpKeyDetails ->
      addTextToClipboard("unprotected private key", requireNotNull(pgpKeyDetails.privateKey))

      onView(withId(R.id.buttonLoadFromClipboard))
        .check(matches(isDisplayed()))
        .perform(click())
    }
  }

  private fun testImportUnprotectedPrivateKey(preparation: (pgpKeyDetails: PgpKeyDetails) -> Unit) {
    val pgpKeyDetails =
      PrivateKeysManager.getPgpKeyDetailsFromAssets("pgp/unprotected@flowcrypt.test_prv.asc")

    //check that we don't have a key with an owner == unprotected@flowcrypt.test
    onView(
      allOf(
        withId(R.id.textViewKeyOwner),
        withText(pgpKeyDetails.getPrimaryInternetAddress()?.address)
      )
    ).check(doesNotExist())

    onView(withId(R.id.recyclerViewKeys))
      .check(matches(withRecyclerViewItemCount(1)))

    onView(withId(R.id.floatActionButtonAddKey))
      .check(matches(isDisplayed()))
      .perform(click())

    //do additional preparation depends on a test
    preparation.invoke(pgpKeyDetails)

    isDialogWithTextDisplayed(
      decorView,
      getResString(R.string.this_key_is_unprotected_please_protect_it_with_pass_phrase_before_import)
    )

    onView(withText(R.string.continue_))
      .check(matches(isDisplayed()))
      .perform(click())

    //protect a key with a pass phrase
    onView(withId(R.id.eTPassphrase))
      .check(matches(isDisplayed()))
      .perform(
        replaceText(TestConstants.DEFAULT_STRONG_PASSWORD),
        closeSoftKeyboard()
      )
    onView(withId(R.id.btSetPassphrase))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withId(R.id.eTRepeatedPassphrase))
      .check(matches(isDisplayed()))
      .perform(
        replaceText(TestConstants.DEFAULT_STRONG_PASSWORD),
        closeSoftKeyboard()
      )
    onView(withId(R.id.btConfirmPassphrase))
      .perform(click())

    //recheck and import already protected key
    onView(withId(R.id.editTextKeyPassword))
      .perform(
        ViewActions.scrollTo(),
        replaceText(TestConstants.DEFAULT_STRONG_PASSWORD),
        closeSoftKeyboard()
      )
    onView(withId(R.id.buttonPositiveAction))
      .perform(ViewActions.scrollTo(), click())

    //check that we successfully imported the key
    onView(withId(R.id.recyclerViewKeys))
      .check(matches(withRecyclerViewItemCount(2)))

    onView(withId(R.id.recyclerViewKeys))
      .perform(
        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
          hasDescendant(withText(requireNotNull(pgpKeyDetails.getPrimaryInternetAddress()?.address)))
        )
      )
  }
}
