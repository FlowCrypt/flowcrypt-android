/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.gmailapi

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.scrollTo
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
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
import com.flowcrypt.email.viewaction.ClickOnViewInRecyclerViewItem
import org.hamcrest.Matchers.allOf
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

  @Test
  fun testThreadDetailsWithSingleStandardMessage() {
    openThreadBasedOnPosition(5)
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
    checkCollapsedState(1, hasPgp = false, hasAttachments = true)
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
    checkCollapsedState(1, hasPgp = true, hasAttachments = true)
  }

  @Test
  fun testThreadDetailsWithFewEncryptedMessages() {
    openThreadBasedOnPosition(0)
    checkCorrectThreadDetails(
      messagesCount = 2,
      threadSubject = SUBJECT_EXISTING_ENCRYPTED,
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
    checkCollapsedState(1, hasPgp = true, hasAttachments = true)
    checkCollapsedState(
      2, hasPgp = true, hasAttachments = true, needToCollapseVisibleExpandedItem = false
    )
  }

  @Test
  fun testThreadDetailsWithFewStandardMessages() {
    openThreadBasedOnPosition(3)
    checkCorrectThreadDetails(
      messagesCount = 2,
      threadSubject = SUBJECT_EXISTING_STANDARD,
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
      PgpBadgeListAdapter.PgpBadge.Type.NOT_ENCRYPTED,
      PgpBadgeListAdapter.PgpBadge.Type.NOT_SIGNED
    )
    checkWebViewText(MESSAGE_EXISTING_STANDARD)
    checkAttachments(
      listOf(
        Pair(ATTACHMENT_NAME_1, attachmentsDataCache[0].size.toLong())
      )
    )
    checkReplyButtons(isEncryptedMode = false)
    checkCollapsedState(1, hasPgp = false, hasAttachments = true)
    checkCollapsedState(
      2,
      hasPgp = false,
      hasAttachments = true,
      needToCollapseVisibleExpandedItem = false
    )
  }

  /**
   * This conversation contains 1 standard + 1 encrypted + 1 deleted messages
   * The app should show only 2 messages when we open INBOX(deleted message should be skipped)
   */
  @Test
  fun testThreadDetailsWithMixedMessages() {
    openThreadBasedOnPosition(2)
    checkCorrectThreadDetails(
      messagesCount = 2,
      threadSubject = SUBJECT_MIXED_MESSAGES,
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
    checkCollapsedState(1, hasPgp = false, hasAttachments = false)
    checkCollapsedState(
      2,
      hasPgp = true,
      hasAttachments = true,
      needToCollapseVisibleExpandedItem = false
    )
  }

  @Test
  fun testThreadDetailsWithMessagesWithoutAttachments() {
    openThreadBasedOnPosition(4)
    checkCorrectThreadDetails(
      messagesCount = 2,
      threadSubject = SUBJECT_NO_ATTACHMENTS,
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
      PgpBadgeListAdapter.PgpBadge.Type.NOT_ENCRYPTED,
      PgpBadgeListAdapter.PgpBadge.Type.NOT_SIGNED
    )
    checkWebViewText(MESSAGE_EXISTING_STANDARD)
    checkAttachments(listOf())
    checkReplyButtons(isEncryptedMode = false)
    checkCollapsedState(1, hasPgp = false, hasAttachments = false)
    checkCollapsedState(
      2,
      hasPgp = false,
      hasAttachments = false,
      needToCollapseVisibleExpandedItem = false
    )
  }

  @Test
  fun testThreadDetailsFewMessagesAndSingleDraft() {
    openThreadBasedOnPosition(6)
    checkCorrectThreadDetails(
      messagesCount = 3,
      threadSubject = SUBJECT_FEW_MESSAGES_WITH_SINGLE_DRAFT,
      labels = listOf(
        GmailApiLabelsListAdapter.Label("Inbox")
      )
    )

    checkBaseMessageDetailsInTread(
      fromAddress = "me (Draft)",
      datetimeInMilliseconds = DATE_EXISTING_STANDARD
    ) {
      it.toMutableMap().apply {
        put(getResString(R.string.from), addAccountToDatabaseRule.account.email)
        put(getResString(R.string.reply_to), addAccountToDatabaseRule.account.email)
        put(getResString(R.string.to), addAccountToDatabaseRule.account.email)
      }
    }

    onView(allOf(withId(R.id.recyclerViewMessages), isDisplayed()))
      .perform(
        scrollTo<ViewHolder>(
          allOf(
            hasDescendant(
              allOf(
                withId(R.id.imageButtonEditDraft),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
              )
            ),
            hasDescendant(
              allOf(
                withId(R.id.imageButtonDeleteDraft),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
              )
            )
          )
        )
      )

    checkPgpBadges(
      2,
      PgpBadgeListAdapter.PgpBadge.Type.NOT_ENCRYPTED,
      PgpBadgeListAdapter.PgpBadge.Type.NOT_SIGNED
    )
    checkWebViewText(MESSAGE_EXISTING_STANDARD)
    checkAttachments(listOf())
    checkReplyButtons(isVisible = false)
  }

  @Test
  fun testDeleteDraftInThread() {
    openThreadBasedOnPosition(6)
    checkCorrectThreadDetails(
      messagesCount = 3,
      threadSubject = SUBJECT_FEW_MESSAGES_WITH_SINGLE_DRAFT,
      labels = listOf(
        GmailApiLabelsListAdapter.Label("Inbox")
      )
    )

    //check that a draft is displayed
    onView(allOf(withId(R.id.recyclerViewMessages), isDisplayed()))
      .perform(
        scrollTo<ViewHolder>(
          allOf(
            hasDescendant(
              allOf(
                withId(R.id.imageButtonEditDraft),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
              )
            ),
            hasDescendant(
              allOf(
                withId(R.id.imageButtonDeleteDraft),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
              )
            )
          )
        )
      )
    checkWebViewText(MESSAGE_EXISTING_STANDARD)

    //check that reply buttons are hidden
    checkReplyButtons(isVisible = false)

    //delete the draft
    onView(allOf(withId(R.id.recyclerViewMessages), isDisplayed()))
      .perform(
        RecyclerViewActions.actionOnItemAtPosition<ViewHolder>(
          3,
          ClickOnViewInRecyclerViewItem(R.id.imageButtonDeleteDraft)
        )
      )

    onView(withText(getResString(R.string.delete)))
      .inRoot(isDialog())
      .check(matches(isDisplayed()))
      .perform(click())

    //check that the draft delete from the messages list
    Thread.sleep(TimeUnit.SECONDS.toMillis(2))
    checkCorrectThreadDetails(
      messagesCount = 2,
      threadSubject = SUBJECT_FEW_MESSAGES_WITH_SINGLE_DRAFT,
      labels = listOf(
        GmailApiLabelsListAdapter.Label("Inbox")
      )
    )
  }

  @Test
  fun testThreadDetailsWithPgpMimeMessages() {
    openThreadBasedOnPosition(9)
    checkCorrectThreadDetails(
      messagesCount = 1,
      threadSubject = SUBJECT_EXISTING_PGP_MIME,
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
    checkWebViewText(MESSAGE_EXISTING_PGP_MIME)
    checkAttachments(
      listOf(
        Pair(ATTACHMENT_NAME_1, attachmentsDataCache[0].size.toLong()),
        Pair(ATTACHMENT_NAME_2, attachmentsDataCache[1].size.toLong()),
        Pair(ATTACHMENT_NAME_3, attachmentsDataCache[2].size.toLong())
      )
    )
    checkReplyButtons(isEncryptedMode = true)
    //hasAttachments = false because the app doesn't update UI when found attachments
    //in decrypted PGPMime message
    checkCollapsedState(1, hasPgp = true, hasAttachments = false)
  }
}