/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.junit.Assert.assertTrue
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
@MediumTest
@RunWith(AndroidJUnit4::class)
@FlowCryptTestSettings(useIntents = true, useCommonIdling = false)
class AttachmentDownloadingWithRestrictionFlowTest : BaseMessageDetailsFlowTest() {
  private val simpleAttInfo = TestGeneralUtil.getObjectFromJson(
    "messages/attachments/simple_att.json",
    AttachmentInfo::class.java
  )
  private val userWithClientConfiguration = AccountDaoManager.getDefaultAccountDao().copy(
    clientConfiguration = ClientConfiguration(
      flags = listOf(
        ClientConfiguration.ConfigurationProperty.NO_PRV_CREATE,
        ClientConfiguration.ConfigurationProperty.NO_PRV_BACKUP,
        ClientConfiguration.ConfigurationProperty.NO_ATTESTER_SUBMIT,
        ClientConfiguration.ConfigurationProperty.PRV_AUTOIMPORT_OR_AUTOGEN,
        ClientConfiguration.ConfigurationProperty.FORBID_STORING_PASS_PHRASE,
        ClientConfiguration.ConfigurationProperty.RESTRICT_ANDROID_ATTACHMENT_HANDLING,
      )
    ),
    accountType = AccountEntity.ACCOUNT_TYPE_GOOGLE,
    useCustomerFesUrl = true,
    useAPI = true
  )

  private val notificationManager: NotificationManager
    get() = requireNotNull(getTargetContext().getSystemService<NotificationManager>())

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
                        filename = simpleAttInfo?.name
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
              .setBodyDelay(DELAY_FOR_DOWNLOADING_IN_SECONDS, TimeUnit.SECONDS)
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
  fun testDownloadingAttachment() {
    val incomingMessageInfo = getMsgInfo(
      path = "messages/info/standard_msg_info_plaintext_with_one_att.json",
      mimeMsgPath = "messages/mime/standard_msg_info_plaintext_with_one_att.txt",
      atts = arrayOf(simpleAttInfo),
      accountEntity = addAccountToDatabaseRule.accountEntityWithDecryptedInfo
    ) {
      it?.copy(msgEntity = it.msgEntity.copy(accountType = AccountEntity.ACCOUNT_TYPE_GOOGLE))
    }

    baseCheckWithAtt(incomingMsgInfo = incomingMessageInfo, att = simpleAttInfo) {
      waitForObjectWithText(
        requireNotNull(incomingMessageInfo?.text),
        TimeUnit.SECONDS.toMillis(10)
      )

      waitForObjectWithText(
        requireNotNull(simpleAttInfo?.name),
        TimeUnit.SECONDS.toMillis(10)
      )
    }

    onView(withId(R.id.imageViewAttIcon))
      .check(matches(withDrawable(R.drawable.ic_attachment)))

    //RetryRule reruns the test in the same process, so remove a notification of a previous run
    notificationManager.cancelAll()

    onView(withId(R.id.imageButtonDownloadAtt))
      .check(matches(isDisplayed()))
      .perform(click())

    isDialogWithTextDisplayed(decorView, getResString(R.string.downloading))
    isDialogWithTextDisplayed(decorView, requireNotNull(simpleAttInfo?.name))

    /*
     The download takes at least DELAY_FOR_DOWNLOADING_IN_SECONDS (the mock server delay),
     and much longer on a slow CI emulator. Wait for the result instead of a fixed sleep.
     */
    waitUntil(TimeUnit.SECONDS.toMillis(DELAY_FOR_DOWNLOADING_IN_SECONDS + 30)) {
      isDownloadCompletedNotificationShown()
    }
    assertTrue(
      "The 'download complete' notification is missing",
      isDownloadCompletedNotificationShown()
    )

    /*
     At this stage the icon should be as by default. The screen updates the attachment item
     after the download, and ListAdapter calculates the diff on a background thread that
     Espresso doesn't track. Until it's done, the list can contain the old and the new item views.
     */
    val defaultAttachmentIcon = matches(withDrawable(R.drawable.ic_attachment))
    waitUntil(TimeUnit.SECONDS.toMillis(10)) {
      runCatching {
        //the default failure handler takes a screenshot on every failed check
        onView(withId(R.id.imageViewAttIcon))
          .withFailureHandler { error, _ -> throw error }
          .check(defaultAttachmentIcon)
      }.isSuccess
    }
    onView(withId(R.id.imageViewAttIcon)).check(defaultAttachmentIcon)
  }

  private fun isDownloadCompletedNotificationShown(): Boolean {
    val expectedText = getResString(R.string.download_complete)
    return notificationManager.activeNotifications.any { statusBarNotification ->
      statusBarNotification.tag == simpleAttInfo?.id &&
          statusBarNotification.notification.extras
            .getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString() == expectedText
    }
  }

  companion object {
    val ATTACHMENT_ID = UUID.randomUUID().toString()
    const val DELAY_FOR_DOWNLOADING_IN_SECONDS = 4L
  }
}
