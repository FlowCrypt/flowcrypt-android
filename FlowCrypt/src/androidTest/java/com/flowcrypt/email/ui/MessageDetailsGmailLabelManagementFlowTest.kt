/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.scrollTo
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.extensions.kotlin.capitalize
import com.flowcrypt.email.extensions.kotlin.toHex
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withViewBackgroundTint
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddLabelsToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.adapter.GmailApiLabelsListAdapter
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
import org.hamcrest.Description
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.math.BigInteger
import java.net.HttpURLConnection
import java.util.UUID
import kotlin.random.Random

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class MessageDetailsGmailLabelManagementFlowTest : BaseMessageDetailsFlowTest() {
  private val simpleAttInfo = TestGeneralUtil.getObjectFromJson(
    "messages/attachments/simple_att.json",
    AttachmentInfo::class.java
  )
  private val userWithClientConfiguration = AccountDaoManager.getDefaultAccountDao().copy(
    clientConfiguration = ClientConfiguration(
      flags = listOf(
        ClientConfiguration.ConfigurationProperty.NO_PRV_CREATE,
        ClientConfiguration.ConfigurationProperty.NO_PRV_BACKUP,
        ClientConfiguration.ConfigurationProperty.RESTRICT_ANDROID_ATTACHMENT_HANDLING
      ),
      customKeyserverUrl = null,
      keyManagerUrl = "https://keymanagerurl.test",
      disallowAttesterSearchForDomains = null,
      enforceKeygenAlgo = null,
      enforceKeygenExpireMonths = null
    ),
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

  private val addLabelsToDatabaseRule = AddLabelsToDatabaseRule(
    account = addAccountToDatabaseRule.account, folders = mutableListOf(
      LocalFolder(
        account = addAccountToDatabaseRule.account.email,
        fullName = JavaEmailConstants.FOLDER_INBOX,
        folderAlias = JavaEmailConstants.FOLDER_INBOX,
        attributes = listOf("\\HasNoChildren")
      )
    ).apply {
      addAll(LABELS.map {
        LocalFolder(
          account = addAccountToDatabaseRule.account.email,
          fullName = it.name,
          folderAlias = it.name,
          isCustom = true,
          labelColor = it.backgroundColor,
          textColor = it.textColor,
          attributes = listOf("\\HasNoChildren")
        )
      })
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
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  @Test
  fun testDisplayLabels() {
    val details = getMsgInfo(
      "messages/info/standard_msg_info_plaintext.json",
      "messages/mime/standard_msg_info_plaintext.txt"
    ) { incomingMsgInfo ->
      val originalMessageEntity = incomingMsgInfo?.msgEntity ?: return@getMsgInfo null
      return@getMsgInfo incomingMsgInfo.copy(
        msgEntity = originalMessageEntity.copy(
          labelIds = "INBOX Test1 Test2 Test3"
        )
      )
    }?.msgEntity
    requireNotNull(details)
    launchActivity(details)
    Thread.sleep(1000)

    onView(withId(R.id.recyclerViewLabels))
      .check(matches(withRecyclerViewItemCount(LABELS.size + 1)))

    onView(withId(R.id.recyclerViewLabels))
      .perform(
        scrollTo<ViewHolder>(hasDescendant(withText(GmailApiHelper.LABEL_INBOX.capitalize())))
      )

    for (label in LABELS) {
      onView(withId(R.id.recyclerViewLabels))
        .perform(RecyclerViewActions.scrollToHolder(GmailApiLabelMatcher(label)))
        .check(
          matches(
            hasDescendant(
              allOf(
                isDisplayed(),
                hasDescendant(withText(label.name)),
                withViewBackgroundTint(
                  getTargetContext(),
                  requireNotNull(label.backgroundColor)
                )
              )
            )
          )
        )
    }
  }

  private class GmailApiLabelMatcher(private val label: GmailApiLabelsListAdapter.Label) :
    BoundedMatcher<ViewHolder, GmailApiLabelsListAdapter.ViewHolder>(
      GmailApiLabelsListAdapter.ViewHolder::class.java
    ) {
    override fun matchesSafely(holder: GmailApiLabelsListAdapter.ViewHolder): Boolean {
      return holder.binding.textViewLabel.text == label.name
    }

    override fun describeTo(description: Description) {
      description.appendText("with: $label")
    }
  }

  companion object {
    val ATTACHMENT_ID = UUID.randomUUID().toString()
    val LABELS = listOf(
      GmailApiLabelsListAdapter.Label("Test1", "#ff2323ff", "#FFFFFF"),
      GmailApiLabelsListAdapter.Label("Test2", "#FFD14836", "#FFFFFF"),
      GmailApiLabelsListAdapter.Label("Test3", "#FFFFEB3B", "#FFFFFF"),
    )
  }
}
