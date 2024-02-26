/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.extensions.kotlin.toHex
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withDrawable
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.base.BaseMessageDetailsFlowTest
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.TestGeneralUtil
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.model.Message
import com.google.api.services.gmail.model.MessagePart
import com.google.api.services.gmail.model.MessagePartBody
import com.google.api.services.gmail.model.MessagePartHeader
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.math.BigInteger
import java.net.HttpURLConnection
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * @author Denys Bondarenko
 */
@FlowCryptTestSettings(useIntents = true, useCommonIdling = false)
@MediumTest
@RunWith(AndroidJUnit4::class)
class AttachmentDownloadingProgressFlowTest : BaseMessageDetailsFlowTest() {
  private val simpleAttInfo = TestGeneralUtil.getObjectFromJson(
    "messages/attachments/simple_att.json",
    AttachmentInfo::class.java
  )
  private val userWithClientConfiguration = AccountDaoManager.getDefaultAccountDao().copy(
    clientConfiguration = ClientConfiguration(flags = listOf()),
    accountType = AccountEntity.ACCOUNT_TYPE_GOOGLE,
    useCustomerFesUrl = true,
    useAPI = true
  )

  override val addAccountToDatabaseRule = AddAccountToDatabaseRule(userWithClientConfiguration)
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule(
    accountEntity = addAccountToDatabaseRule.account,
    keyPath = "pgp/default@flowcrypt.test_fisrtKey_prv_strong.asc",
    passphrase = TestConstants.DEFAULT_STRONG_PASSWORD,
    sourceType = KeyImportDetails.SourceType.EMAIL,
    passphraseType = KeyEntity.PassphraseType.DATABASE
  )

  private val mockWebServerRule = FlowCryptMockWebServerRule(
    TestConstants.MOCK_WEB_SERVER_PORT,
    object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        when (request.path) {
          "/gmail/v1/users/me/messages/${simpleAttInfo?.uid?.toHex()}?fields=" +
              "id,threadId,labelIds,snippet,sizeEstimate,historyId,internalDate,payload/partId," +
              "payload/mimeType,payload/filename,payload/headers,payload/body,payload/" +
              "parts(partId,mimeType,filename,headers,body/size,body/attachmentId)&format=full" -> {
            return MockResponse()
              .setResponseCode(HttpURLConnection.HTTP_OK)
              .setBody(
                Message().apply {
                  factory = GsonFactory.getDefaultInstance()
                  id = "5555555555555555"
                  threadId = "1111111111111111"
                  payload = MessagePart().apply {
                    partId = ""
                    mimeType = "multipart/mixed"
                    filename = ""
                    parts = listOf(
                      MessagePart().apply {
                        partId = "0"
                        mimeType = "image/png"
                        filename = "android.png"
                        headers = listOf(
                          MessagePartHeader().apply {
                            name = "Content-Type"
                            value = "image/png; name=\"android.png\""
                          }, MessagePartHeader().apply {
                            name = "Content-Disposition"
                            value = "attachment; filename=\"android.png\""
                          })
                        body = MessagePartBody().apply {
                          attachmentId = ATTACHMENT_ID
                        }
                      }
                    )
                  }
                  historyId = BigInteger.valueOf(Random.nextLong())
                }.toString()
              )
          }

          "/gmail/v1/users/me/messages/${simpleAttInfo?.uid?.toHex()}/attachments/$ATTACHMENT_ID?fields=data&prettyPrint=false" -> {
            return MockResponse()
              .setResponseCode(HttpURLConnection.HTTP_OK)
              .setBodyDelay(4, TimeUnit.SECONDS)
              .setBody(
                MessagePartBody().apply {
                  factory = GsonFactory.getDefaultInstance()
                  data = "we don't care about this content"
                }.toString()
              )
          }

          else -> return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
        }
      }
    })

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(mockWebServerRule)
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  @Test
  @FlakyTest
  fun testVisibilityOfDownloadingProgressIcon() {
    baseCheckWithAtt(
      incomingMsgInfo = getMsgInfo(
        path = "messages/info/standard_msg_info_plaintext_with_one_att.json",
        mimeMsgPath = "messages/mime/standard_msg_info_plaintext_with_one_att.txt",
        simpleAttInfo,
        accountEntity = addAccountToDatabaseRule.accountEntityWithDecryptedInfo
      ), att = simpleAttInfo
    )

    waitForObjectWithText("It's a standard message with plaintext and one attachment", 2000)

    onView(withId(R.id.imageViewAttIcon))
      .check(matches(withDrawable(R.drawable.ic_attachment)))

    onView(withId(R.id.imageButtonDownloadAtt))
      .check(matches(isDisplayed()))
      .perform(click())

    Thread.sleep(2000)
    //at this stage icon should be different
    onView(withId(R.id.imageViewAttIcon))
      .check(matches(not(withDrawable(R.drawable.ic_attachment))))

    Thread.sleep(5000)
    //at this stage icon should be as by default
    onView(withId(R.id.imageViewAttIcon))
      .check(matches(withDrawable(R.drawable.ic_attachment)))
  }

  companion object {
    val ATTACHMENT_ID = UUID.randomUUID().toString()
  }
}
