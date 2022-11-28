/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.base

import android.util.Base64
import android.view.Gravity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions.open
import androidx.test.espresso.contrib.DrawerMatchers.isClosed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
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
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddLabelsToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
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
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.pgpainless.util.Passphrase
import java.io.InputStreamReader
import java.math.BigInteger
import java.net.HttpURLConnection
import java.util.Properties
import java.util.zip.GZIPInputStream
import kotlin.random.Random

/**
 * @author Denis Bondarenko
 *         Date: 11/28/22
 *         Time: 5:56 PM
 *         E-mail: DenBond7@gmail.com
 */
abstract class BaseDraftsGmailAPIFlowTest : BaseTest() {
  abstract val mockWebServerRule: FlowCryptMockWebServerRule
  override val activityScenarioRule = activityScenarioRule<MainActivity>()
  protected val draftsCache = mutableListOf<Draft>()

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

  protected val addAccountToDatabaseRule: AddAccountToDatabaseRule =
    AddAccountToDatabaseRule(accountEntity)

  protected val addPrivateKeyToDatabaseRule =
    AddPrivateKeyToDatabaseRule(addAccountToDatabaseRule.account)

  protected val addLabelsToDatabaseRule = AddLabelsToDatabaseRule(
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

  protected val decryptedPrivateKey = PgpKey.decryptKey(
    requireNotNull(addPrivateKeyToDatabaseRule.pgpKeyDetails.privateKey),
    Passphrase.fromPassword(TestConstants.DEFAULT_STRONG_PASSWORD)
  )

  protected fun getDraftAndMimeMessageFromRequest(request: RecordedRequest): Pair<Draft, MimeMessage> {
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

  protected fun genMsgDetailsMockResponse(
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

  protected fun prepareDraft(
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

  protected fun openComposeScreenAndTypeSubject(subject: String) {
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

  protected fun moveToDraftFolder() {
    onView(withId(R.id.drawer_layout))
      .check(matches(isClosed(Gravity.LEFT)))
      .perform(open())

    onView(withId(R.id.navigationView))
      .perform(clickOnFolderWithName(JavaEmailConstants.FOLDER_DRAFT))

    Thread.sleep(1000)
  }

  protected fun handleCommonAPICalls(request: RecordedRequest): MockResponse {
    println("request.method == + ${request.method} PATH:" + request.path)

    when {
      request.path == "/v1/keys/private" -> {
        return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
          ApiHelper.getInstance(getTargetContext()).gson
            .toJson(EkmPrivateKeysResponse(privateKeys = listOf(Key(decryptedPrivateKey))))
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

      else -> return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
    }
  }
}
