/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import android.util.Base64
import android.view.Gravity
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.DrawerMatchers.isClosed
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.response.api.EkmPrivateKeysResponse
import com.flowcrypt.email.api.retrofit.response.model.Key
import com.flowcrypt.email.api.retrofit.response.model.OrgRules
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.jetpack.viewmodel.DraftViewModel
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddLabelsToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.viewaction.CustomViewActions.clickOnFolderWithName
import com.google.api.client.json.JsonObjectParser
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.model.Draft
import com.google.api.services.gmail.model.Label
import com.google.api.services.gmail.model.ListDraftsResponse
import com.google.api.services.gmail.model.ListLabelsResponse
import com.google.api.services.gmail.model.ListMessagesResponse
import com.google.api.services.gmail.model.ListSendAsResponse
import com.google.api.services.gmail.model.Message
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import junit.framework.Assert.assertEquals
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.pgpainless.util.Passphrase
import java.io.InputStreamReader
import java.math.BigInteger
import java.net.HttpURLConnection
import java.util.Properties
import java.util.zip.GZIPInputStream
import kotlin.random.Random


@MediumTest
@RunWith(AndroidJUnit4::class)
class DraftsGmailAPITestCorrectCreatingAndUpdatingFlowTest : BaseTest() {
  override val activityScenarioRule = activityScenarioRule<MainActivity>()

  private val accountEntity = AccountDaoManager.getDefaultAccountDao().copy(
    accountType = AccountEntity.ACCOUNT_TYPE_GOOGLE, clientConfiguration = OrgRules(
      flags = listOf(
        OrgRules.DomainRule.NO_PRV_CREATE,
        OrgRules.DomainRule.NO_PRV_BACKUP,
        OrgRules.DomainRule.NO_ATTESTER_SUBMIT,
        OrgRules.DomainRule.PRV_AUTOIMPORT_OR_AUTOGEN,
        OrgRules.DomainRule.FORBID_STORING_PASS_PHRASE,
        OrgRules.DomainRule.RESTRICT_ANDROID_ATTACHMENT_HANDLING,
      ),
      keyManagerUrl = "https://localhost:1212/",
    ), useAPI = true, useFES = true
  )

  val addAccountToDatabaseRule: AddAccountToDatabaseRule = AddAccountToDatabaseRule(accountEntity)

  private val addPrivateKeyToDatabaseRule =
    AddPrivateKeyToDatabaseRule(addAccountToDatabaseRule.account)

  private val addLabelsToDatabaseRule = AddLabelsToDatabaseRule(
    account = addAccountToDatabaseRule.account, folders = listOf(
      LocalFolder(
        account = addAccountToDatabaseRule.account.email,
        fullName = JavaEmailConstants.FOLDER_DRAFT,
        folderAlias = JavaEmailConstants.FOLDER_DRAFT,
        attributes = listOf("\\HasNoChildren", "\\Draft")
      ), LocalFolder(
        account = addAccountToDatabaseRule.account.email,
        fullName = JavaEmailConstants.FOLDER_INBOX,
        folderAlias = JavaEmailConstants.FOLDER_INBOX,
        attributes = listOf("\\HasNoChildren")
      )
    )
  )

  private val decryptedPrivateKey = PgpKey.decryptKey(
    requireNotNull(addPrivateKeyToDatabaseRule.pgpKeyDetails.privateKey),
    Passphrase.fromPassword(TestConstants.DEFAULT_STRONG_PASSWORD)
  )

  private val draftsCache = mutableListOf<Draft>()

  private val mockWebServerRule =
    FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        println("request.method == + ${request.method} PATH:" + request.path)

        val gson = ApiHelper.getInstance(getTargetContext()).gson
        when {
          request.path == "/v1/keys/private" -> {
            return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
              gson.toJson(EkmPrivateKeysResponse(privateKeys = listOf(Key(decryptedPrivateKey))))
            )
          }

          request.path == "/gmail/v1/users/me/settings/sendAs" -> {
            return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
              ListSendAsResponse().apply {
                factory = GsonFactory.getDefaultInstance()
                sendAs = emptyList()
              }.toString()
            )
          }

          request.path == "/gmail/v1/users/me/labels" -> {
            return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
              ListLabelsResponse().apply {
                factory = GsonFactory.getDefaultInstance()
                labels = addLabelsToDatabaseRule.folders.map {
                  Label().apply {
                    id = it.fullName
                    name = it.folderAlias
                  }
                }
              }.toString()
            )
          }

          request.path == "/gmail/v1/users/me/messages?labelIds=${JavaEmailConstants.FOLDER_INBOX}&maxResults=45" -> {
            return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
              ListMessagesResponse().apply {
                factory = GsonFactory.getDefaultInstance()
                messages = emptyList()
              }.toString()
            )
          }

          request.method == "GET" && request.path == "/gmail/v1/users/me/drafts?maxResults=45" -> {
            return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
              ListDraftsResponse().apply {
                factory = GsonFactory.getDefaultInstance()
                drafts = draftsCache.map { draft ->
                  Draft().apply {
                    id = draft.id
                    message = Message().apply {
                      id = draft.message.id
                      threadId = draft.message.threadId
                    }
                  }
                }
              }.toString()
            )
          }

          request.method == "GET" && request.path == "/gmail/v1/users/me/drafts?fields=drafts/id,drafts/message/id&maxResults=500" -> {
            return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
              ListDraftsResponse().apply {
                factory = GsonFactory.getDefaultInstance()
                drafts = draftsCache.map { draft ->
                  Draft().apply {
                    id = draft.id
                    message = Message().apply {
                      id = draft.message.id
                    }
                  }
                }
              }.toString()
            )
          }

          request.method == "PUT" && request.path == "/gmail/v1/users/me/drafts/$DRAFT_ID_FIRST" -> {
            val (draft, mimeMessage) = getDraftAndMimeMessageFromRequest(request)
            val existingDraftInCache = draftsCache.firstOrNull { it.id == DRAFT_ID_FIRST }

            return if (existingDraftInCache != null && mimeMessage.subject == MESSAGE_SUBJECT_FIRST_EDITED) {
              val existingMessage = existingDraftInCache.message
              existingDraftInCache.message = Message().apply {
                id = existingMessage.id
                threadId = existingMessage.threadId
                labelIds = existingMessage.labelIds
                raw = draft.message.raw
              }

              MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(existingDraftInCache.toString())
            } else {
              MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
            }
          }

          request.method == "POST" && request.path == "/gmail/v1/users/me/drafts" -> {
            val (draft, mimeMessage) = getDraftAndMimeMessageFromRequest(request)

            return when (mimeMessage.subject) {
              MESSAGE_SUBJECT_FIRST -> {
                val newDraft = prepareDraft(
                  draftId = DRAFT_ID_FIRST,
                  messageId = MESSAGE_ID_FIRST,
                  messageThreadId = THREAD_ID_FIRST,
                  rawMsg = draft.message.raw
                )
                draftsCache.add(newDraft)

                MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                  .setBody(newDraft.toString())
              }

              MESSAGE_SUBJECT_SECOND -> {
                val newDraft = prepareDraft(
                  draftId = DRAFT_ID_SECOND,
                  messageId = MESSAGE_ID_SECOND,
                  messageThreadId = THREAD_ID_SECOND,
                  rawMsg = draft.message.raw
                )
                draftsCache.add(newDraft)

                MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                  .setBody(newDraft.toString())
              }

              else -> {
                MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
              }
            }
          }

          request.path == "/gmail/v1/users/me/messages/${MESSAGE_ID_FIRST}?fields=id,threadId,historyId&format=full" -> {
            return genMsgDetailsMockResponse(MESSAGE_ID_FIRST, THREAD_ID_FIRST)
          }

          request.path == "/gmail/v1/users/me/messages/${MESSAGE_ID_SECOND}?fields=id,threadId,historyId&format=full" -> {
            return genMsgDetailsMockResponse(MESSAGE_ID_SECOND, THREAD_ID_SECOND)
          }

          else -> return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
        }
      }
    })

  private fun getDraftAndMimeMessageFromRequest(request: RecordedRequest): Pair<Draft, MimeMessage> {
    val gzipInputStream = GZIPInputStream(request.body.inputStream())
    val draft = JsonObjectParser(GsonFactory.getDefaultInstance()).parseAndClose(
      InputStreamReader(gzipInputStream), Draft::class.java
    )
    val rawMimeMessageAsByteArray = Base64.decode(
      draft.message.raw, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
    )
    val mimeMessage = MimeMessage(
      Session.getInstance(Properties()), rawMimeMessageAsByteArray.inputStream()
    )
    return Pair(draft, mimeMessage)
  }

  private fun genMsgDetailsMockResponse(
    messageId: String,
    messageThreadId: String,
  ) =
    MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
      .setBody(Message().apply {
        factory = GsonFactory.getDefaultInstance()
        id = messageId
        threadId = messageThreadId
        labelIds = listOf(JavaEmailConstants.FOLDER_DRAFT)
        historyId = BigInteger.valueOf(Random.nextLong())
      }.toString())

  private fun prepareDraft(
    draftId: String,
    messageId: String,
    messageThreadId: String,
    rawMsg: String
  ): Draft {
    return Draft().apply {
      factory = GsonFactory.getDefaultInstance()
      id = draftId
      message = Message().apply {
        id = messageId
        threadId = messageThreadId
        labelIds = listOf(JavaEmailConstants.FOLDER_DRAFT)
        raw = rawMsg
      }
    }
  }

  @get:Rule
  var ruleChain: TestRule =
    RuleChain.outerRule(RetryRule.DEFAULT)
      .around(ClearAppSettingsRule())
      .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
      .around(mockWebServerRule).around(addAccountToDatabaseRule)
      .around(addPrivateKeyToDatabaseRule)
      .around(addLabelsToDatabaseRule)
      .around(activityScenarioRule)
      .around(ScreenshotTestRule())

  @Test
  fun testCorrectCreatingAndUpdating() {
    draftsCache.clear()
    moveToDraftFolder()

    onView(withId(R.id.recyclerViewMsgs))
      .check(matches(withRecyclerViewItemCount(0)))

    openComposeScreenAndTypeSubject(MESSAGE_SUBJECT_FIRST)
    Thread.sleep(DraftViewModel.DELAY_TIMEOUT * 2)
    Espresso.pressBack()

    //check that the first draft was created
    assertEquals(1, draftsCache.size)
    onView(withId(R.id.recyclerViewMsgs))
      .check(matches(withRecyclerViewItemCount(1)))
    val mimeMessageFirst = getMimeMessageFromCache(0)
    assertEquals(MESSAGE_SUBJECT_FIRST, mimeMessageFirst.subject)

    openComposeScreenAndTypeSubject(MESSAGE_SUBJECT_SECOND)
    Thread.sleep(DraftViewModel.DELAY_TIMEOUT * 2)
    Espresso.pressBack()

    //check that the second draft was created
    assertEquals(2, draftsCache.size)
    onView(withId(R.id.recyclerViewMsgs))
      .check(matches(withRecyclerViewItemCount(2)))
    val mimeMessageSecond = getMimeMessageFromCache(1)
    assertEquals(MESSAGE_SUBJECT_SECOND, mimeMessageSecond.subject)

    //open the first draft and modify it
    onView(withId(R.id.recyclerViewMsgs))
      .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))
    onView(withId(R.id.imageButtonEditDraft))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withId(R.id.editTextEmailSubject))
      .check(matches(isDisplayed()))
      .perform(
        scrollTo(),
        click(),
        replaceText(MESSAGE_SUBJECT_FIRST_EDITED),
        closeSoftKeyboard()
      )

    Thread.sleep(DraftViewModel.DELAY_TIMEOUT * 2)
    Espresso.pressBack()//back to the message details screen
    Espresso.pressBack()//back to the messages list screen

    //check if 1st draft is updated correctly and 2nd draft remains same
    assertEquals(2, draftsCache.size)
    onView(withId(R.id.recyclerViewMsgs))
      .check(matches(withRecyclerViewItemCount(2)))
    val mimeMessageFirstEdited = getMimeMessageFromCache(0)
    assertEquals(MESSAGE_SUBJECT_FIRST_EDITED, mimeMessageFirstEdited.subject)
    onView(withText(MESSAGE_SUBJECT_FIRST_EDITED))
      .check(matches(isDisplayed()))
    val mimeMessageSecondAfterEditingFirst = getMimeMessageFromCache(1)
    assertEquals(MESSAGE_SUBJECT_SECOND, mimeMessageSecondAfterEditingFirst.subject)
  }

  private fun openComposeScreenAndTypeSubject(subject: String) {
    //open the compose screen
    onView(withId(R.id.floatActionButtonCompose))
      .check(matches(isDisplayed()))
      .perform(click())

    //type some text in the subject
    onView(withId(R.id.editTextEmailSubject))
      .check(matches(isDisplayed()))
      .perform(
        scrollTo(),
        click(),
        typeText(subject),
        closeSoftKeyboard()
      )
  }

  private fun getMimeMessageFromCache(msgPosition: Int): MimeMessage {
    val rawMimeMessageAsByteArrayOfSecondMsg = Base64.decode(
      draftsCache[msgPosition].message.raw, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
    )
    return MimeMessage(
      Session.getInstance(Properties()), rawMimeMessageAsByteArrayOfSecondMsg.inputStream()
    )
  }

  private fun moveToDraftFolder() {
    onView(withId(R.id.drawer_layout))
      .check(matches(isClosed(Gravity.LEFT)))
      .perform(DrawerActions.open())

    onView(withId(R.id.navigationView))
      .perform(clickOnFolderWithName(JavaEmailConstants.FOLDER_DRAFT))

    Thread.sleep(1000)
  }

  companion object {
    const val DRAFT_ID_FIRST = "r5555555555555555551"
    const val MESSAGE_ID_FIRST = "5555555555555551"
    const val THREAD_ID_FIRST = "1111111111111111"
    const val MESSAGE_SUBJECT_FIRST = "first"
    const val MESSAGE_SUBJECT_FIRST_EDITED = "first edited"

    const val DRAFT_ID_SECOND = "r5555555555555555552"
    const val MESSAGE_ID_SECOND = "5555555555555552"
    const val THREAD_ID_SECOND = "11111111111111112"
    const val MESSAGE_SUBJECT_SECOND = "second"
  }
}
