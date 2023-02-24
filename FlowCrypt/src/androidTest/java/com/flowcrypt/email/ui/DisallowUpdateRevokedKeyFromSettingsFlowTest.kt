/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.AddRecipientsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.ui.activity.fragment.ParseAndSavePubKeysFragmentArgs
import com.flowcrypt.email.util.AccountDaoManager
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
class DisallowUpdateRevokedKeyFromSettingsFlowTest : BaseTest() {
  override val useIntents: Boolean = true
  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      destinationId = R.id.parseAndSavePubKeysFragment,
      extras = ParseAndSavePubKeysFragmentArgs(
        TestGeneralUtil.readFileFromAssetsAsString(
          "pgp/default@flowcrypt.test_fisrtKey_pub_default_mod_06_17_2022.asc"
        )
      ).toBundle()
    )
  )

  private val addAccountToDatabaseRule =
    AddAccountToDatabaseRule(AccountDaoManager.getUserFromBaseSettings(ACCOUNT))

  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule(
    keyPath = "pgp/denbond7@flowcrypt.test_prv_strong_primary.asc",
    accountEntity = addAccountToDatabaseRule.account
  )

  val pgpKeyDetails = PrivateKeysManager.getPgpKeyDetailsFromAssets(
    "pgp/default@flowcrypt.test_fisrtKey_pub_revoked.asc"
  )

  private val addRecipientsToDatabaseRule = AddRecipientsToDatabaseRule(
    listOf(
      RecipientWithPubKeys(
        RecipientEntity(email = RECIPIENT_WITH_REVOKED_KEY),
        listOf(pgpKeyDetails.toPublicKeyEntity(RECIPIENT_WITH_REVOKED_KEY).copy(id = 12))
      )
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(addRecipientsToDatabaseRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testDisallowUpdateRevokedKey() {
    onView(withId(R.id.rVPubKeys))
      .perform(
        actionOnItemAtPosition<RecyclerView.ViewHolder>(
          0,
          ClickOnViewInRecyclerViewItem(R.id.buttonUpdateContact)
        )
      )
    isDialogWithTextDisplayed(
      decorView, getResString(R.string.key_is_revoked_unable_to_update)
    )
  }

  companion object {
    const val ACCOUNT = "denbond7@flowcrypt.test"
    const val RECIPIENT_WITH_REVOKED_KEY = "default@flowcrypt.test"
  }
}
