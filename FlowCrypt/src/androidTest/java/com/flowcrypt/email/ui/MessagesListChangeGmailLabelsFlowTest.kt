/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.rules.AddLabelsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.adapter.GmailApiLabelsListAdapter
import com.flowcrypt.email.ui.base.BaseGmailApiTest
import com.flowcrypt.email.viewaction.ClickOnViewInRecyclerViewItem
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.model.BatchModifyMessagesRequest
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okio.GzipSource
import okio.buffer
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@FlowCryptTestSettings(useCommonIdling = false, useIntents = true)
class MessagesListChangeGmailLabelsFlowTest : BaseGmailApiTest() {
  //the mock web server changes it on its own thread
  private val lastLabelIds = CopyOnWriteArrayList<String>()

  override val mockWebServerRule =
    FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        when {
          request.method == "POST" && request.path == "/gmail/v1/users/me/messages/batchModify" -> {
            val source = GzipSource(request.body)
            val batchModifyMessagesRequest = GsonFactory.getDefaultInstance().fromInputStream(
              source.buffer().inputStream(),
              BatchModifyMessagesRequest::class.java
            )

            batchModifyMessagesRequest.addLabelIds?.let {
              lastLabelIds.addAll(it)
            }
            batchModifyMessagesRequest.removeLabelIds?.let {
              lastLabelIds.removeAll(it)
            }

            return MockResponse()
              .setResponseCode(HttpURLConnection.HTTP_OK)
          }

          else -> return handleCommonAPICalls(request)
        }
      }
    })

  private val customLabelsRule = AddLabelsToDatabaseRule(
    account = accountEntity, folders = CUSTOM_LABELS.map {
      LocalFolder(
        account = addAccountToDatabaseRule.account.email,
        fullName = it.name,
        folderAlias = it.name,
        isCustom = true,
        labelColor = it.backgroundColor,
        textColor = it.textColor,
        attributes = listOf("\\HasNoChildren")
      )
    }
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(mockWebServerRule)
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(addLabelsToDatabaseRule)
    .around(customLabelsRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  @FlakyTest
  fun testLabelsManagement() {
    lastLabelIds.clear()
    //need to wait while the app loads the messages list
    waitForObjectWithText(SUBJECT_EXISTING_ENCRYPTED, TimeUnit.SECONDS.toMillis(10))

    //click on the first avatar to open ActionMenu
    onView(withId(R.id.recyclerViewMsgs))
      .perform(
        actionOnItemAtPosition<ViewHolder>(
          POSITION_EXISTING_ENCRYPTED,
          ClickOnViewInRecyclerViewItem(R.id.imageViewAvatar)
        )
      )
    //select the second message too
    onView(withId(R.id.recyclerViewMsgs))
      .perform(
        actionOnItemAtPosition<ViewHolder>(
          POSITION_EXISTING_STANDARD,
          ClickOnViewInRecyclerViewItem(R.id.imageViewAvatar)
        )
      )

    val existingMessageEntities = runBlocking {
      roomDatabase.msgDao().getMsgsSuspend(
        addAccountToDatabaseRule.account.email,
        JavaEmailConstants.FOLDER_INBOX
      )
    }

    //open dialog from the Action Bar menu
    openActionBarOverflowOrOptionsMenu(getTargetContext())
    onView(withText(R.string.change_labels))
      .check(matches(isDisplayed()))
      .perform(click())

    //the dialog shows the list after the labels are loaded asynchronously
    waitUntilViewMatches(
      viewId = R.id.recyclerViewLabels,
      matcher = allOf(isDisplayed(), hasMinimumChildCount(2))
    )

    //select the second and third labels
    onView(withId(R.id.recyclerViewLabels))
      .perform(actionOnItemAtPosition<ViewHolder>(0, click()))
    onView(withId(R.id.recyclerViewLabels))
      .perform(actionOnItemAtPosition<ViewHolder>(1, click()))

    onView(withText(R.string.change_labels))
      .inRoot(isDialog())
      .perform(click())

    waitForObjectWithText(CUSTOM_LABELS.first().name, TimeUnit.SECONDS.toMillis(10))

    /*
     The screen can show the new labels before the API call and the database update are done.
     Wait for both instead of checking them once.
     */
    val expectedApiLabelIds = CUSTOM_LABELS.take(2).map { it.name }
    waitUntil { lastLabelIds == expectedApiLabelIds }
    assertEquals(expectedApiLabelIds, lastLabelIds.toList())

    //check local changes for modified messages
    val expectedLabelIds = listOf(JavaEmailConstants.FOLDER_INBOX) + expectedApiLabelIds
    val encryptedMsgId = existingMessageEntities.getId(SUBJECT_EXISTING_ENCRYPTED)
    val standardMsgId = existingMessageEntities.getId(SUBJECT_EXISTING_STANDARD)
    waitUntil {
      getLabelIds(encryptedMsgId) == expectedLabelIds &&
          getLabelIds(standardMsgId) == expectedLabelIds
    }
    assertEquals(expectedLabelIds, getLabelIds(encryptedMsgId))
    assertEquals(expectedLabelIds, getLabelIds(standardMsgId))

    //as we didn't change the third message it should have only INBOX label
    assertEquals(
      listOf(JavaEmailConstants.FOLDER_INBOX),
      getLabelIds(existingMessageEntities.getId(SUBJECT_EXISTING_PGP_MIME))
    )
  }

  private fun List<MessageEntity>.getId(subject: String): Long =
    requireNotNull(first { it.subject == subject }.id)

  private fun getLabelIds(messageId: Long): List<String> = runBlocking {
    FlowCryptRoomDatabase.getDatabase(getTargetContext()).msgDao().getMsgById(messageId)
  }?.labelIds.orEmpty().split(MessageEntity.LABEL_IDS_SEPARATOR)

  companion object {
    private val CUSTOM_LABELS = listOf(
      GmailApiLabelsListAdapter.Label("LABEL_1", "#ff2323ff", "#FFFFFF"),
      GmailApiLabelsListAdapter.Label("LABEL_2", "#FFD14836", "#FFFFFF"),
      GmailApiLabelsListAdapter.Label("LABEL_3", "#FFFFEB3B", "#FFFFFF"),
      GmailApiLabelsListAdapter.Label("LABEL_4", "#FF00EB3B", "#FFFFFF"),
    )
  }
}
