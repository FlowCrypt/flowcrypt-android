/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import android.os.Environment
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.response.api.MessageReplyTokenResponse
import com.flowcrypt.email.extensions.kotlin.toInputStream
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.base.BaseDraftsGmailAPIFlowTest
import com.flowcrypt.email.util.TestGeneralUtil
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.pgpainless.PGPainless
import org.pgpainless.key.util.UserId
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection

@MediumTest
@RunWith(AndroidJUnit4::class)
@Ignore("not completed")
class SendPasswordProtectedMessageFlowTest : BaseDraftsGmailAPIFlowTest() {

  override val mockWebServerRule =
    FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        val gson = ApiHelper.getInstance(getTargetContext()).gson
        return when {
          request.path.equals("/api/v1/message/new-reply-token") -> {
            MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
              .setBody(gson.toJson(MessageReplyTokenResponse(replyToken = REPLY_TOKEN)))
          }

          else -> handleCommonAPICalls(request)
        }
      }
    })

  @get:Rule
  var ruleChain: TestRule =
    RuleChain.outerRule(RetryRule.DEFAULT)
      .around(ClearAppSettingsRule())
      .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
      .around(mockWebServerRule)
      .around(addAccountToDatabaseRule)
      .around(addPrivateKeyToDatabaseRule)
      .around(addLabelsToDatabaseRule)
      .around(activityScenarioRule)
      .around(ScreenshotTestRule())

  @Test
  fun testSendPasswordProtectedMessageWithFewAttachments() {
    onView(withId(R.id.floatActionButtonCompose))
      .check(matches(isDisplayed()))
      .perform(click())

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

    for (att in atts) {
      addAttachment(att)
    }

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

    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())

    Thread.sleep(10000)
  }

  companion object {
    private const val RECIPIENT_WITHOUT_PUBLIC_KEY = "no_key@flowcrypt.test"
    private const val WEB_PORTAL_PASSWORD = "Qwerty1234@"
    private const val MESSAGE_SUBJECT = "Subject"
    private const val MESSAGE_TEXT = "Some text"
    private const val ATTACHMENT_NAME_1 = "text.txt"
    private const val ATTACHMENT_NAME_2 = "text1.txt"
    private const val ATTACHMENT_NAME_3 = "binary_key.key"
    private const val REPLY_TOKEN = "some_reply_token"
    private var atts: MutableList<File> = mutableListOf()
    private val pgpSecretKeyRing = PGPainless.generateKeyRing().simpleEcKeyRing(
      UserId.nameAndEmail(RECIPIENT_WITHOUT_PUBLIC_KEY, RECIPIENT_WITHOUT_PUBLIC_KEY),
      TestConstants.DEFAULT_PASSWORD
    )

    @BeforeClass
    @JvmStatic
    fun setUp() {
      val directory = InstrumentationRegistry.getInstrumentation().targetContext
        .getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        ?: File(Environment.DIRECTORY_DOCUMENTS)

      val buffer = ByteArrayOutputStream()
      pgpSecretKeyRing.encode(buffer)
      atts.addAll(
        listOf(
          TestGeneralUtil.createFileWithContent(
            directory = directory,
            fileName = ATTACHMENT_NAME_1,
            inputStream = "Text for filling the attached file".toInputStream()
          ),
          TestGeneralUtil.createFileWithContent(
            directory = directory,
            fileName = ATTACHMENT_NAME_2,
            inputStream = "Text for filling the attached file".toInputStream()
          ),
          TestGeneralUtil.createFileWithContent(
            directory = directory,
            fileName = ATTACHMENT_NAME_3,
            inputStream = ByteArrayInputStream(buffer.toByteArray())
          )
        )
      )
    }
  }
}
