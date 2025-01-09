/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.gmailapi

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.Constants
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.extensions.kotlin.asInternetAddress
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.rules.AddRecipientsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.adapter.GmailApiLabelsListAdapter
import com.flowcrypt.email.ui.adapter.PgpBadgeListAdapter
import com.flowcrypt.email.ui.gmailapi.base.BaseThreadDetailsGmailApiFlowTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@FlowCryptTestSettings(useCommonIdling = false)
class ThreadDetailsGmailApiFlowTest : BaseThreadDetailsGmailApiFlowTest() {

  private val recipientWithPubKeys = listOf(
    RecipientWithPubKeys(
      RecipientEntity(
        email = addAccountToDatabaseRule.account.email,
        name = "Default"
      ),
      listOf(
        addPrivateKeyToDatabaseRule.pgpKeyRingDetails
          .toPublicKeyEntity(addAccountToDatabaseRule.account.email)
          .copy(id = 1)
      )
    ),
    requireNotNull(DEFAULT_FROM_RECIPIENT.asInternetAddress()).let {
      RecipientWithPubKeys(
        RecipientEntity(
          email = it.address,
          name = it.personal
        ),
        listOf(
          defaultFromPgpKeyDetails
            .toPublicKeyEntity(defaultFromPgpKeyDetails.getUserIdsAsSingleString())
            .copy(id = 2)
        )
      )
    },
    RecipientWithPubKeys(
      RecipientEntity(email = EXISTING_MESSAGE_CC_RECIPIENT),
      listOf(
        existingCcPgpKeyDetails
          .toPublicKeyEntity(existingCcPgpKeyDetails.getUserIdsAsSingleString())
          .copy(id = 3)
      )
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(mockWebServerRule)
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(AddRecipientsToDatabaseRule(recipientWithPubKeys))
    .around(addLabelsToDatabaseRule)
    .around(customLabelsRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Before
  fun waitForThreadsList() {
    //need to wait while the app loads the thread list
    waitForObjectWithText(SUBJECT_EXISTING_STANDARD, TimeUnit.SECONDS.toMillis(20))
  }

  @Test
  fun testThreadDetailsWithSingleStandardMessage() {
    openThreadBasedOnPosition(4)
    checkCorrectThreadDetails(
      messagesCount = 1,
      threadSubject = SUBJECT_SINGLE_STANDARD,
      labels = listOf(
        GmailApiLabelsListAdapter.Label("Inbox")
      )
    )
    checkBaseMessageDetailsInTread(
      fromAddress = "From",
      datetimeInMilliseconds = DATE_EXISTING_STANDARD
    )
    checkPgpBadges(
      2,
      PgpBadgeListAdapter.PgpBadge.Type.NOT_SIGNED,
      PgpBadgeListAdapter.PgpBadge.Type.NOT_ENCRYPTED
    )
    checkWebViewText(MESSAGE_EXISTING_STANDARD)
    checkAttachments(
      listOf(
        Pair(ATTACHMENT_NAME_1, attachmentsDataCache[0].size.toLong())
      )
    )
    checkReplyButtons(isEncryptedMode = false)
  }

  @Test
  fun testThreadDetailsWithSingleEncryptedMessage() {
    openThreadBasedOnPosition(1)
    checkCorrectThreadDetails(
      messagesCount = 1,
      threadSubject = SUBJECT_SINGLE_ENCRYPTED,
      labels = listOf(
        GmailApiLabelsListAdapter.Label("Inbox")
      )
    )
    checkBaseMessageDetailsInTread(
      fromAddress = "From",
      datetimeInMilliseconds = DATE_EXISTING_ENCRYPTED
    )
    checkPgpBadges(
      2,
      PgpBadgeListAdapter.PgpBadge.Type.ENCRYPTED,
      PgpBadgeListAdapter.PgpBadge.Type.SIGNED
    )
    checkWebViewText(MESSAGE_EXISTING_ENCRYPTED)
    checkAttachments(
      listOf(
        Pair(ATTACHMENT_NAME_1 + "." + Constants.PGP_FILE_EXT, -1),
        Pair(ATTACHMENT_NAME_3 + "." + Constants.PGP_FILE_EXT, -1)
      )
    )
    checkReplyButtons(isEncryptedMode = true)
  }
}