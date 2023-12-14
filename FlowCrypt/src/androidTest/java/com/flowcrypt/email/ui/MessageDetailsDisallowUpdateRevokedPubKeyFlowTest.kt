/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.AddRecipientsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.base.BaseMessageDetailsFlowTest
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.PrivateKeysManager
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
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
class MessageDetailsDisallowUpdateRevokedPubKeyFlowTest : BaseMessageDetailsFlowTest() {
  override val addAccountToDatabaseRule: AddAccountToDatabaseRule
    get() = AddAccountToDatabaseRule(
      AccountDaoManager.getUserFromBaseSettings(ACCOUNT)
    )

  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule(
    keyPath = "pgp/denbond7@flowcrypt.test_prv_strong_primary.asc",
    accountEntity = addAccountToDatabaseRule.account
  )

  val pgpKeyRingDetails = PrivateKeysManager.getPgpKeyDetailsFromAssets(
    "pgp/default@flowcrypt.test_fisrtKey_pub_revoked.asc"
  )

  private val addRecipientsToDatabaseRule = AddRecipientsToDatabaseRule(
    listOf(
      RecipientWithPubKeys(
        RecipientEntity(email = RECIPIENT_WITH_REVOKED_KEY),
        listOf(pgpKeyRingDetails.toPublicKeyEntity(RECIPIENT_WITH_REVOKED_KEY).copy(id = 12))
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
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  @Test
  fun testDisallowUpdateRevokedPubKeyFromMessageDetails() {
    val msgInfo = getMsgInfo(
      path = "messages/info/encrypted_msg_info_text_with_pub_key_mod_after_revoked.json",
      mimeMsgPath = "messages/mime/encrypted_msg_info_text_with_pub_key_mod_after_revoked.txt",
      accountEntity = addAccountToDatabaseRule.accountEntityWithDecryptedInfo
    )

    baseCheck(msgInfo)

    onView(withId(R.id.textViewManualImportWarning))
      .check(matches(not(isDisplayed())))

    onView(allOf(withId(R.id.textViewStatus), hasSibling(withId(R.id.switchShowPublicKey))))
      .check(matches(withText(getResString(R.string.key_is_revoked_unable_to_update))))
  }

  companion object {
    const val ACCOUNT = "denbond7@flowcrypt.test"
    const val RECIPIENT_WITH_REVOKED_KEY = "default@flowcrypt.test"
  }
}
