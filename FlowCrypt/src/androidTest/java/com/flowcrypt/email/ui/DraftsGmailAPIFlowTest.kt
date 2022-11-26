/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import android.util.Base64
import android.view.Gravity
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.DrawerMatchers.isClosed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
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
class DraftsGmailAPIFlowTest : BaseTest() {
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

  private val drafts = mutableListOf<Draft>()

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

          request.method == "GET" && request.path == "/gmail/v1/users/me/drafts?fields=drafts/id,drafts/message/id&maxResults=500" -> {
            return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
              ListDraftsResponse().apply {
                factory = GsonFactory.getDefaultInstance()
                drafts = drafts
              }.toString()
            )
          }

          request.method == "POST" && request.path == "/gmail/v1/users/me/drafts" -> {
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

            return if (MESSAGE_SUBJECT_FIRST == mimeMessage.subject) {
              val newDraft = Draft().apply {
                factory = GsonFactory.getDefaultInstance()
                id = DRAFT_ID_FIRST
                message = Message().apply {
                  id = MESSAGE_ID_FIRST
                  threadId = THREAD_ID_FIRST
                  labelIds = listOf(LABEL_DRAFT)
                  raw = draft.message.raw
                }
              }
              drafts.add(newDraft)

              MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(draft.toString())
            } else {
              MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
            }
          }

          request.path == "/gmail/v1/users/me/messages/${MESSAGE_ID_FIRST}?fields=id,threadId,historyId&format=full" -> {
            return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
              .setBody(Message().apply {
                factory = GsonFactory.getDefaultInstance()
                id = MESSAGE_ID_FIRST
                threadId = THREAD_ID_FIRST
                labelIds = listOf(LABEL_DRAFT)
                historyId = BigInteger.valueOf(Random.nextLong())
              }.toString())
          }

          else -> return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
        }
      }
    })

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
    moveToDraftFolder()

    Thread.sleep(10000)
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
        typeText(MESSAGE_SUBJECT_FIRST),
        closeSoftKeyboard()
      )

    Thread.sleep(DraftViewModel.DELAY_TIMEOUT * 2)

    Espresso.pressBack()

    //check that the first draft was created
    assertEquals(1, drafts.size)
    val rawMimeMessageAsByteArray = Base64.decode(
      drafts[0].message.raw, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
    )
    val mimeMessage = MimeMessage(
      Session.getInstance(Properties()), rawMimeMessageAsByteArray.inputStream()
    )
    assertEquals(MESSAGE_SUBJECT_FIRST, mimeMessage.subject)
    Thread.sleep(1000)
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
    const val DRAFT_ID_FIRST = "r5555555555555555555"
    const val MESSAGE_ID_FIRST = "5555555555555555"
    const val THREAD_ID_FIRST = "1111111111111111"
    const val LABEL_DRAFT = "Draft"
    const val MESSAGE_SUBJECT_FIRST = "Some subject"
  }
}
