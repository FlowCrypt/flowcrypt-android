/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.retrofit.response.model.VerificationResult
import com.flowcrypt.email.database.entity.AccountAliasesEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.matchers.CustomMatchers.Companion.hasItem
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.rules.AddAccountAliasToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.CreateMessageActivity
import com.flowcrypt.email.ui.activity.fragment.CreateMessageFragmentArgs
import com.flowcrypt.email.ui.base.BaseComposeScreenTest
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
class CreateMessageTestRecipientsDuringReplyAllFlowTest : BaseComposeScreenTest() {
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule()
  private val addAccountAliasToDatabaseRule = AddAccountAliasToDatabaseRule(
    listOf(
      AccountAliasesEntity(
      email = addAccountToDatabaseRule.account.email,
      accountType = requireNotNull(addAccountToDatabaseRule.account.accountType),
      sendAsEmail = ALIAS.lowercase(),
      displayName = ALIAS,
      isDefault = false,
      verificationStatus = "accepted"
      )
    )
  )
  private val INBOX = LocalFolder(
    addAccountToDatabaseRule.account.email,
    fullName = "INBOX",
    folderAlias = "INBOX",
    msgCount = 1,
    attributes = listOf("\\HasNoChildren")
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(addAccountAliasToDatabaseRule)
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  @Test
  fun testRecipientsForSentAndOutboxFolder() {
    val toRecipient = "to@flowcrypt.test"
    val ccRecipient = "cc@flowcrypt.test"

    val localFolder = LocalFolder(
      addAccountToDatabaseRule.account.email,
      fullName = "SENT",
      folderAlias = "SENT",
      msgCount = 1,
      attributes = listOf("\\HasNoChildren")
    )

    val incomingMessageInfo = IncomingMessageInfo(
      localFolder = localFolder,
      msgEntity = MessageEntity(
        account = addAccountToDatabaseRule.account.email,
        accountType = addAccountToDatabaseRule.account.accountType,
        folder = localFolder.fullName,
        uid = 123,
        toAddresses = toRecipient,
        ccAddresses = ccRecipient
      ),
      encryptionType = MessageEncryptionType.STANDARD,
      verificationResult = VERIFICATION_RESULT
    )

    activeActivityRule?.launch(getIntent(incomingMessageInfo))
    registerAllIdlingResources()
    Thread.sleep(1000)

    onView(withId(R.id.recyclerViewChipsTo))
      .check(matches(withRecyclerViewItemCount(2)))//two items: CHIP + ADD
    onView(withId(R.id.recyclerViewChipsTo))
      .check(matches(hasItem(withText(toRecipient))))

    onView(withId(R.id.recyclerViewChipsCc))
      .check(matches(withRecyclerViewItemCount(2)))//two items: CHIP + ADD
    onView(withId(R.id.recyclerViewChipsCc))
      .check(matches(hasItem(withText(ccRecipient))))
  }

  @Test
  fun testReplyAllForToRecipientsWithoutAliases() {
    val replyToRecipient = "replyto@flowcrypt.test"
    val to1Recipient = addAccountToDatabaseRule.account.email
    val to2Recipient = "to@flowcrypt.test"

    val incomingMessageInfo = IncomingMessageInfo(
      localFolder = INBOX,
      msgEntity = MessageEntity(
        account = addAccountToDatabaseRule.account.email,
        accountType = addAccountToDatabaseRule.account.accountType,
        folder = INBOX.fullName,
        uid = 123,
        replyToAddresses = replyToRecipient,
        toAddresses = listOf(to1Recipient, to2Recipient).joinToString()
      ),
      encryptionType = MessageEncryptionType.STANDARD,
      verificationResult = VERIFICATION_RESULT
    )

    activeActivityRule?.launch(getIntent(incomingMessageInfo))
    registerAllIdlingResources()
    Thread.sleep(1000)

    onView(withId(R.id.recyclerViewChipsTo))
      .check(matches(withRecyclerViewItemCount(2)))//two items: CHIP + ADD
    onView(withId(R.id.recyclerViewChipsTo))
      .check(matches(hasItem(withText(replyToRecipient))))
    onView(withId(R.id.recyclerViewChipsTo))
      .check(matches(not(hasItem(withText(to1Recipient)))))

    onView(withId(R.id.recyclerViewChipsCc))
      .check(matches(withRecyclerViewItemCount(2)))//two items: CHIP + ADD
    onView(withId(R.id.recyclerViewChipsCc))
      .check(matches(hasItem(withText(to2Recipient))))
    onView(withId(R.id.recyclerViewChipsCc))
      .check(matches(not(hasItem(withText(to1Recipient)))))
  }

  @Test
  fun testReplyAllForToRecipientsWithAliases() {
    val replyToRecipient = "replyto@flowcrypt.test"
    val to1Recipient = addAccountToDatabaseRule.account.email
    val to2Recipient = "to@flowcrypt.test"

    val incomingMessageInfo = IncomingMessageInfo(
      localFolder = INBOX,
      msgEntity = MessageEntity(
        account = addAccountToDatabaseRule.account.email,
        accountType = addAccountToDatabaseRule.account.accountType,
        folder = INBOX.fullName,
        uid = 123,
        replyToAddresses = replyToRecipient,
        toAddresses = listOf(to1Recipient, to2Recipient, ALIAS).joinToString()
      ),
      encryptionType = MessageEncryptionType.STANDARD,
      verificationResult = VERIFICATION_RESULT
    )

    activeActivityRule?.launch(getIntent(incomingMessageInfo))
    registerAllIdlingResources()
    Thread.sleep(1000)

    onView(withId(R.id.recyclerViewChipsTo))
      .check(matches(withRecyclerViewItemCount(2)))//two items: CHIP + ADD
    onView(withId(R.id.recyclerViewChipsTo))
      .check(matches(hasItem(withText(replyToRecipient))))
    onView(withId(R.id.recyclerViewChipsTo))
      .check(matches(not(hasItem(withText(to1Recipient)))))
    onView(withId(R.id.recyclerViewChipsTo))
      .check(matches(not(hasItem(withText(ALIAS)))))

    onView(withId(R.id.recyclerViewChipsCc))
      .check(matches(withRecyclerViewItemCount(2)))//two items: CHIP + ADD
    onView(withId(R.id.recyclerViewChipsCc))
      .check(matches(hasItem(withText(to2Recipient))))
    onView(withId(R.id.recyclerViewChipsCc))
      .check(matches(not(hasItem(withText(to1Recipient)))))
    onView(withId(R.id.recyclerViewChipsCc))
      .check(matches(not(hasItem(withText(ALIAS)))))
  }

  @Test
  fun testReplyAllForToCcRecipientsWithAliases() {
    val replyToRecipient = "replyto@flowcrypt.test"
    val to1Recipient = addAccountToDatabaseRule.account.email
    val to2Recipient = "to@flowcrypt.test"

    val incomingMessageInfo = IncomingMessageInfo(
      localFolder = INBOX,
      msgEntity = MessageEntity(
        account = addAccountToDatabaseRule.account.email,
        accountType = addAccountToDatabaseRule.account.accountType,
        folder = INBOX.fullName,
        uid = 123,
        replyToAddresses = replyToRecipient,
        toAddresses = listOf(to1Recipient, to2Recipient).joinToString(),
        ccAddresses = listOf(ALIAS).joinToString()
      ),
      encryptionType = MessageEncryptionType.STANDARD,
      verificationResult = VERIFICATION_RESULT
    )

    activeActivityRule?.launch(getIntent(incomingMessageInfo))
    registerAllIdlingResources()
    Thread.sleep(1000)

    onView(withId(R.id.recyclerViewChipsTo))
      .check(matches(withRecyclerViewItemCount(2)))//two items: CHIP + ADD
    onView(withId(R.id.recyclerViewChipsTo))
      .check(matches(hasItem(withText(replyToRecipient))))
    onView(withId(R.id.recyclerViewChipsTo))
      .check(matches(not(hasItem(withText(to1Recipient)))))
    onView(withId(R.id.recyclerViewChipsTo))
      .check(matches(not(hasItem(withText(ALIAS)))))

    onView(withId(R.id.recyclerViewChipsCc))
      .check(matches(withRecyclerViewItemCount(2)))//two items: CHIP + ADD
    onView(withId(R.id.recyclerViewChipsCc))
      .check(matches(hasItem(withText(to2Recipient))))
    onView(withId(R.id.recyclerViewChipsCc))
      .check(matches(not(hasItem(withText(to1Recipient)))))
    onView(withId(R.id.recyclerViewChipsCc))
      .check(matches(not(hasItem(withText(ALIAS)))))
  }

  private fun getIntent(incomingMessageInfo: IncomingMessageInfo): Intent =
    Intent(getTargetContext(), CreateMessageActivity::class.java).apply {
      putExtras(
        CreateMessageFragmentArgs(
          messageType = MessageType.REPLY_ALL,
          incomingMessageInfo = incomingMessageInfo
        ).toBundle()
      )
    }

  companion object {
    private val VERIFICATION_RESULT = VerificationResult(
      hasEncryptedParts = false,
      hasSignedParts = false,
      hasMixedSignatures = false,
      isPartialSigned = false,
      keyIdOfSigningKeys = emptyList(),
      hasBadSignatures = false
    )

    private const val ALIAS = "alias@flowcrypt.test"
  }
}
