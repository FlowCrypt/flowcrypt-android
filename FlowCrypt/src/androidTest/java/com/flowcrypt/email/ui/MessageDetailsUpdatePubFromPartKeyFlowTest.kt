/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.AddRecipientsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.ui.base.BaseMessageDetailsFlowTest
import com.flowcrypt.email.util.PrivateKeysManager
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
class MessageDetailsUpdatePubFromPartKeyFlowTest : BaseMessageDetailsFlowTest() {
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule()
  private val existingPgpKeyDetails =
    PrivateKeysManager.getPgpKeyDetailsFromAssets("pgp/denbond7@flowcrypt.test_pub_primary.asc")
  private val receivedPgpKeyDetails =
    PrivateKeysManager.getPgpKeyDetailsFromAssets("pgp/denbond7@flowcrypt.test_pub_primary_mod_11_16_2022.asc")

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
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(AddRecipientsToDatabaseRule(recipientWithPubKeys))
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  @Test
  fun testUpdatingPubKeyFromMessagePart() {
    val msgInfo = getMsgInfo(
      "messages/info/standard_msg_with_updated_pub_key.json",
      "messages/mime/standard_msg_with_updated_pub_key.txt"
    )

    baseCheck(msgInfo)

    val primaryAddress =
      requireNotNull(existingPgpKeyDetails.getPrimaryInternetAddress()).address.lowercase()
    val recipientBeforeSaving = runBlocking {
      roomDatabase.recipientDao().getRecipientWithPubKeysByEmailSuspend(primaryAddress)
    }

    val existingPubKeyBeforeUpdating = PgpKey.parseKeys(
      requireNotNull(recipientBeforeSaving?.publicKeys?.firstOrNull()?.publicKey)
    ).pgpKeyDetailsList.firstOrNull()

    assertEquals(existingPgpKeyDetails.lastModified, existingPubKeyBeforeUpdating?.lastModified)

    onView(withId(R.id.buttonKeyAction))
      .check(matches(isDisplayed()))
      .check(matches(withText(R.string.update_pub_key)))
      .perform(scrollTo(), click())
    onView(withId(R.id.buttonKeyAction))
      .check(matches(not(isDisplayed())))

    //need to wait database sync
    Thread.sleep(1000)

    val recipientAfterSaving = runBlocking {
      roomDatabase.recipientDao().getRecipientWithPubKeysByEmailSuspend(primaryAddress)
    }
    assertNotNull(recipientAfterSaving)
    val publicKeyByteArray =
      requireNotNull(recipientAfterSaving?.publicKeys?.firstOrNull()?.publicKey)
    val pgpKeyDetailsOfDatabaseEntity =
      PgpKey.parseKeys(publicKeyByteArray).pgpKeyDetailsList.firstOrNull()
    assertEquals(receivedPgpKeyDetails.lastModified, pgpKeyDetailsOfDatabaseEntity?.lastModified)
  }
}
