/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withChild
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.junit.annotations.NotReadyForCI
import com.flowcrypt.email.matchers.CustomMatchers.Companion.hasItem
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.AddRecipientsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.ParseAndSavePubKeysFragment
import com.flowcrypt.email.ui.activity.fragment.ParseAndSavePubKeysFragmentArgs
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import com.flowcrypt.email.viewaction.ClickOnViewInRecyclerViewItem
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
class ParseAndSavePubKeysFragmentInIsolationTest : BaseTest() {

  private val existingPgpKeyDetails =
    PrivateKeysManager.getPgpKeyDetailsFromAssets("pgp/denbond7@flowcrypt.test_pub_primary.asc")

  private val recipientWithPubKeys = listOf(
    RecipientWithPubKeys(
      RecipientEntity(email = existingPgpKeyDetails.getUserIdsAsSingleString()),
      listOf(
        existingPgpKeyDetails
          .toPublicKeyEntity(existingPgpKeyDetails.getUserIdsAsSingleString())
          .copy(id = 2)
      )
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(AddAccountToDatabaseRule())
    .around(AddPrivateKeyToDatabaseRule())
    .around(AddRecipientsToDatabaseRule(recipientWithPubKeys))
    .around(ScreenshotTestRule())

  @Test
  fun testParsingStringSourceWithUnsupportedPublicKeys() {
    launchFragmentInContainer<ParseAndSavePubKeysFragment>(
      fragmentArgs = ParseAndSavePubKeysFragmentArgs(
        source = TestGeneralUtil.readFileFromAssetsAsString(
          "pgp/keys/pub_keys_5_good_1_unsupported.asc"
        )
      ).toBundle()
    )

    onView(withId(R.id.rVPubKeys))
      .check(matches(withRecyclerViewItemCount(5)))
  }

  @Test
  fun testIsDisplayedSingleItem() {
    launchFragmentInContainer<ParseAndSavePubKeysFragment>(
      fragmentArgs = ParseAndSavePubKeysFragmentArgs(
        source = TestGeneralUtil.readFileFromAssetsAsString(
          "pgp/default@flowcrypt.test_fisrtKey_pub.asc"
        )
      ).toBundle()
    )

    onView(withId(R.id.rVPubKeys))
      .check(matches(withRecyclerViewItemCount(1)))

    onView(withId(R.id.rVPubKeys))
      .check(
        matches(
          hasItem(
            withChild(
              hasSibling(
                withText(
                  getResString(
                    R.string.template_message_part_public_key_owner,
                    "default@flowcrypt.test"
                  )
                )
              )
            )
          )
        )
      )
  }

  @Test
  @FlakyTest
  @NotReadyForCI
  fun testIsDisplayedLabelAlreadyImported() {
    launchFragmentInContainer<ParseAndSavePubKeysFragment>(
      fragmentArgs = ParseAndSavePubKeysFragmentArgs(
        source = TestGeneralUtil.readFileFromAssetsAsString(
          "pgp/denbond7@flowcrypt.test_pub_primary.asc"
        )
      ).toBundle()
    )

    onView(withId(R.id.rVPubKeys))
      .check(matches(withRecyclerViewItemCount(1)))

    onView(withId(R.id.rVPubKeys))
      .check(
        matches(
          hasItem(
            withChild(
              hasSibling(
                withText(
                  getResString(R.string.already_imported)
                )
              )
            )
          )
        )
      )
  }

  @Test
  fun testSaveButtonForSingleContact() {
    launchFragmentInContainer<ParseAndSavePubKeysFragment>(
      fragmentArgs = ParseAndSavePubKeysFragmentArgs(
        source = TestGeneralUtil.readFileFromAssetsAsString(
          "pgp/user_without_letters@flowcrypt.test_pub.asc"
        )
      ).toBundle()
    )

    onView(withId(R.id.rVPubKeys))
      .check(matches(withRecyclerViewItemCount(1)))

    onView(withId(R.id.rVPubKeys))
      .perform(
        RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
          0,
          ClickOnViewInRecyclerViewItem(R.id.buttonSaveContact)
        )
      )
    Thread.sleep(1000)
    onView(withId(R.id.rVPubKeys))
      .check(
        matches(
          hasItem(
            withChild(
              hasSibling(
                withText(
                  getResString(R.string.already_imported)
                )
              )
            )
          )
        )
      )
  }

  @Test
  fun testIsImportAllButtonDisplayed() {
    launchFragmentInContainer<ParseAndSavePubKeysFragment>(
      fragmentArgs = ParseAndSavePubKeysFragmentArgs(
        source = TestGeneralUtil.readFileFromAssetsAsString(
          "pgp/keys/10_pub_keys_armored_own_header.asc"
        )
      ).toBundle()
    )

    onView(withId(R.id.btImportAll))
      .check(matches(isDisplayed()))
  }
}
