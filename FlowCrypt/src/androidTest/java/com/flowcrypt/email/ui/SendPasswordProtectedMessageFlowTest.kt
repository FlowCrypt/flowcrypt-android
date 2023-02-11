/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddLabelsToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.base.BaseComposeScreenTest
import com.flowcrypt.email.util.AccountDaoManager
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.model.ListSendAsResponse
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.File
import java.net.HttpURLConnection

@MediumTest
@RunWith(AndroidJUnit4::class)
class SendPasswordProtectedMessageFlowTest : BaseComposeScreenTest() {
  private val accountEntity = AccountDaoManager.getDefaultAccountDao()
    .copy(
      accountType = AccountEntity.ACCOUNT_TYPE_GOOGLE,
      clientConfiguration = ClientConfiguration(
        flags = listOf(
          ClientConfiguration.ConfigurationProperty.NO_PRV_CREATE,
          ClientConfiguration.ConfigurationProperty.NO_PRV_BACKUP,
          ClientConfiguration.ConfigurationProperty.NO_ATTESTER_SUBMIT,
          ClientConfiguration.ConfigurationProperty.PRV_AUTOIMPORT_OR_AUTOGEN,
          ClientConfiguration.ConfigurationProperty.FORBID_STORING_PASS_PHRASE,
          ClientConfiguration.ConfigurationProperty.RESTRICT_ANDROID_ATTACHMENT_HANDLING,
        ),
        keyManagerUrl = "https://localhost:1212/",
      ),
      useAPI = true,
      useFES = true
    )

  override val addAccountToDatabaseRule =
    AddAccountToDatabaseRule(accountEntity)
  private val addPrivateKeyToDatabaseRule =
    AddPrivateKeyToDatabaseRule(addAccountToDatabaseRule.account)

  private val temporaryFolderRule = TemporaryFolder()
  private val mockWebServerRule = FlowCryptMockWebServerRule(
    TestConstants.MOCK_WEB_SERVER_PORT,
    object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        when {
          request.path == "/gmail/v1/users/me/settings/sendAs" -> {
            return MockResponse()
              .setResponseCode(HttpURLConnection.HTTP_OK)
              .setBody(
                ListSendAsResponse().apply {
                  factory = GsonFactory.getDefaultInstance()
                  sendAs = emptyList()
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
    .around(
      AddLabelsToDatabaseRule(
        account = addAccountToDatabaseRule.account,
        folders = listOf(
          LocalFolder(
            account = addAccountToDatabaseRule.account.email,
            fullName = "Draft",
            folderAlias = "Draft",
            attributes = listOf("\\HasNoChildren", "\\Draft")
          )
        )
      )
    )
    .around(temporaryFolderRule)
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  @Test
  fun testSendPasswordProtectedMessageWithFewAttachments() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    onView(withId(R.id.chipLayoutTo))
      .perform(scrollTo())
    onView(withId(R.id.editTextEmailAddress))
      .perform(
        typeText(RECIPIENT_WITHOUT_PUBLIC_KEY),
        pressImeActionButton(),
        closeSoftKeyboard()
      )
    onView(withId(R.id.editTextEmailSubject))
      .perform(
        scrollTo(),
        click(),
        typeText(MESSAGE_SUBJECT),
        closeSoftKeyboard()
      )
    onView(withId(R.id.editTextEmailMessage))
      .perform(
        scrollTo(),
        typeText(MESSAGE_TEXT),
        closeSoftKeyboard()
      )

    onView(withId(R.id.btnSetWebPortalPassword))
      .perform(
        scrollTo(),
        click()
      )

    onView(withId(R.id.eTPassphrase))
      .perform(
        typeText(WEB_PORTAL_PASSWORD),
        pressImeActionButton()
      )
  }

  companion object {
    private const val RECIPIENT_WITHOUT_PUBLIC_KEY = "no_key@flowcrypt.test"
    private const val WEB_PORTAL_PASSWORD = "Qwerty1234@"
    private const val MESSAGE_SUBJECT = "Subject"
    private const val MESSAGE_TEXT = "Some text"
    private const val ATTACHMENTS_COUNT = 3
    private var atts: MutableList<File> = mutableListOf()
    /*private fun createFilesForCommonAtts() {
      for (i in 0 until ATTACHMENTS_COUNT) {
        atts.add(
          TestGeneralUtil.createFileAndFillWithContent(
            temporaryFolderRule,
            "$i.txt", "Text for filling the attached file"
          )
        )
      }
    }*/
  }
}
