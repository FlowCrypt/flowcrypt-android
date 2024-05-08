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
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.MessageEntity
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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.net.HttpURLConnection

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class MessagesListChangeGmailLabelsFlowTest : BaseGmailApiTest() {
  private var lastLabelIds = mutableListOf<String>()

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
  fun testLabelsManagement() {
    lastLabelIds.clear()
    //need to wait while the app loads the messages list
    Thread.sleep(2000)

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

    //open dialog from the Action Bar menu
    openActionBarOverflowOrOptionsMenu(getTargetContext())
    onView(withText(R.string.change_labels))
      .check(matches(isDisplayed()))
      .perform(click())

    //select the second and third labels
    onView(withId(R.id.recyclerViewLabels))
      .perform(actionOnItemAtPosition<ViewHolder>(0, click()))
    onView(withId(R.id.recyclerViewLabels))
      .perform(actionOnItemAtPosition<ViewHolder>(1, click()))

    onView(withText(R.string.change_labels))
      .inRoot(isDialog())
      .perform(click())

    Thread.sleep(3000)

    //check API call results
    assertEquals(
      CUSTOM_LABELS.take(2).map { it.name },
      lastLabelIds
    )

    //check local changes for modified messages
    val expectedLabelIds =
      listOf(JavaEmailConstants.FOLDER_INBOX) + CUSTOM_LABELS.take(2).map { it.name }
    assertEquals(
      expectedLabelIds,
      runBlocking {
        FlowCryptRoomDatabase.getDatabase(getTargetContext()).msgDao().getMsgById(
          POSITION_EXISTING_ENCRYPTED + 1L
        )
      }?.labelIds.orEmpty().split(MessageEntity.LABEL_IDS_SEPARATOR)
    )
    assertEquals(
      expectedLabelIds,
      runBlocking {
        FlowCryptRoomDatabase.getDatabase(getTargetContext()).msgDao().getMsgById(
          POSITION_EXISTING_STANDARD + 1L
        )
      }?.labelIds.orEmpty().split(MessageEntity.LABEL_IDS_SEPARATOR)
    )
    //as we didn't change the third message it should have only INBOX label
    assertEquals(
      listOf(JavaEmailConstants.FOLDER_INBOX),
      runBlocking {
        FlowCryptRoomDatabase.getDatabase(getTargetContext()).msgDao().getMsgById(
          POSITION_EXISTING_PGP_MIME + 1L
        )
      }?.labelIds.orEmpty().split(MessageEntity.LABEL_IDS_SEPARATOR)
    )
  }

  companion object {
    private val CUSTOM_LABELS = listOf(
      GmailApiLabelsListAdapter.Label("LABEL_1", "#ff2323ff", "#FFFFFF"),
      GmailApiLabelsListAdapter.Label("LABEL_2", "#FFD14836", "#FFFFFF"),
      GmailApiLabelsListAdapter.Label("LABEL_3", "#FFFFEB3B", "#FFFFFF"),
      GmailApiLabelsListAdapter.Label("LABEL_4", "#FF00EB3B", "#FFFFFF"),
    )
  }
}
