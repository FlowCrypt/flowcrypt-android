/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.MailTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.rules.lazyActivityScenarioRule
import com.flowcrypt.email.util.TestGeneralUtil
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.isEmptyString
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.File
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Random
import java.util.UUID

/**
 * @author Denis Bondarenko
 * Date: 22.03.2018
 * Time: 08:55
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class ShareIntentsTest : BaseTest() {
  override val activeActivityRule =
    lazyActivityScenarioRule<CreateMessageActivity>(launchActivity = false)
  override val activityScenario: ActivityScenario<*>?
    get() = activeActivityRule.scenario

  private val mockWebServerRule =
    FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        if (request.path?.startsWith("/pub", ignoreCase = true) == true) {
          val lastSegment = request.requestUrl?.pathSegments?.lastOrNull()

          when {
            TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER.equals(lastSegment, true) -> {
              return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
                .setBody(TestGeneralUtil.readResourceAsString("2.txt"))
            }

            TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER.equals(lastSegment, true) -> {
              return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(TestGeneralUtil.readResourceAsString("3.txt"))
            }
          }
        }

        return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
      }
    })

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(mockWebServerRule)
    .around(AddAccountToDatabaseRule())
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  private val randomActionForRFC6068: String
    get() = if (Random().nextBoolean()) Intent.ACTION_SENDTO else Intent.ACTION_VIEW

  private lateinit var atts: MutableList<File>

  @Before
  fun setUp() {
    atts = mutableListOf()

    for (i in 0 until ATTACHMENTS_COUNT) {
      atts.add(
        TestGeneralUtil.createFileAndFillWithContent(
          "$i.txt",
          UUID.randomUUID().toString()
        )
      )
    }
  }

  @After
  fun cleanResources() {
    TestGeneralUtil.deleteFiles(atts)
  }

  @Test
  fun testEmptyUri() {
    activeActivityRule.launch(genIntentForUri(randomActionForRFC6068, null))
    registerAllIdlingResources()
    checkViewsOnScreen(0, null, null, 0)
  }

  @Test
  fun testToSubjectBody() {
    activeActivityRule.launch(
      genIntentForUri(
        randomActionForRFC6068, MailTo.MAILTO_SCHEME + recipients[0]
            + "?subject=" + ENCODED_SUBJECT + "&body=" + ENCODED_BODY
      )
    )
    registerAllIdlingResources()
    checkViewsOnScreen(1, ENCODED_SUBJECT, ENCODED_BODY, 0)
  }

  @Test
  fun testToParamSubjectBody() {
    activeActivityRule.launch(
      genIntentForUri(
        randomActionForRFC6068, MailTo.MAILTO_SCHEME + "?to=" + recipients[0]
            + "&subject=" + ENCODED_SUBJECT + "&body=" + ENCODED_BODY
      )
    )
    registerAllIdlingResources()
    checkViewsOnScreen(1, ENCODED_SUBJECT, ENCODED_BODY, 0)
  }

  @Test
  fun testToToParamSubjectBody() {
    activeActivityRule.launch(
      genIntentForUri(
        randomActionForRFC6068, MailTo.MAILTO_SCHEME + recipients[0]
            + "?to=" + recipients[1] + "&subject=" + ENCODED_SUBJECT + "&body=" + ENCODED_BODY
      )
    )
    registerAllIdlingResources()
    checkViewsOnScreen(2, ENCODED_SUBJECT, ENCODED_BODY, 0)
  }

  @Test
  fun testToParamToSubjectBody() {
    activeActivityRule.launch(
      genIntentForUri(
        randomActionForRFC6068, MailTo.MAILTO_SCHEME + "?to=" + recipients[0]
            + "," + recipients[1] + "&subject=" + ENCODED_SUBJECT + "&body=" + ENCODED_BODY
      )
    )
    registerAllIdlingResources()
    checkViewsOnScreen(2, ENCODED_SUBJECT, ENCODED_BODY, 0)
  }

  @Test
  fun testMultiToSubjectBody() {
    activeActivityRule.launch(
      genIntentForUri(
        randomActionForRFC6068, MailTo.MAILTO_SCHEME + recipients[0]
            + "," + recipients[1] + "?subject=" + ENCODED_SUBJECT + "&body=" + ENCODED_BODY
      )
    )
    registerAllIdlingResources()
    checkViewsOnScreen(2, ENCODED_SUBJECT, ENCODED_BODY, 0)
  }

  @Test
  fun testMultiToParamSubjectBody() {
    activeActivityRule.launch(
      genIntentForUri(
        randomActionForRFC6068, MailTo.MAILTO_SCHEME + "?to=" + recipients[0]
            + "&to=" + recipients[1] + "&subject=" + ENCODED_SUBJECT + "&body=" + ENCODED_BODY
      )
    )
    registerAllIdlingResources()
    checkViewsOnScreen(2, ENCODED_SUBJECT, ENCODED_BODY, 0)
  }

  @Test
  fun testEmptyMailToSchema() {
    activeActivityRule.launch(genIntentForUri(randomActionForRFC6068, MailTo.MAILTO_SCHEME))
    registerAllIdlingResources()
    checkViewsOnScreen(0, null, null, 0)
  }

  @Test
  fun testSendEmptyExtras() {
    activeActivityRule.launch(generateIntentWithExtras(Intent.ACTION_SEND, null, null, 0))
    registerAllIdlingResources()
    checkViewsOnScreen(0, null, null, 0)
  }

  @Test
  fun testSendExtSubject() {
    activeActivityRule.launch(
      generateIntentWithExtras(
        Intent.ACTION_SEND,
        Intent.EXTRA_SUBJECT,
        null,
        0
      )
    )
    registerAllIdlingResources()
    checkViewsOnScreen(0, Intent.EXTRA_SUBJECT, null, 0)
  }

  @Test
  fun testSendExtBody() {
    activeActivityRule.launch(
      generateIntentWithExtras(
        Intent.ACTION_SEND,
        null,
        Intent.EXTRA_TEXT,
        0
      )
    )
    registerAllIdlingResources()
    checkViewsOnScreen(0, null, Intent.EXTRA_TEXT, 0)
  }

  @Test
  fun testSendAtt() {
    activeActivityRule.launch(generateIntentWithExtras(Intent.ACTION_SEND, null, null, 1))
    registerAllIdlingResources()
    checkViewsOnScreen(0, null, null, 1)
  }

  @Test
  fun testSendExtSubjectExtBody() {
    activeActivityRule.launch(
      generateIntentWithExtras(
        Intent.ACTION_SEND, Intent.EXTRA_SUBJECT,
        Intent.EXTRA_TEXT, 0
      )
    )
    registerAllIdlingResources()
    checkViewsOnScreen(0, Intent.EXTRA_SUBJECT, Intent.EXTRA_TEXT, 0)
  }

  @Test
  fun testSendExtSubjectAtt() {
    activeActivityRule.launch(
      generateIntentWithExtras(
        Intent.ACTION_SEND,
        Intent.EXTRA_SUBJECT,
        null,
        1
      )
    )
    registerAllIdlingResources()
    checkViewsOnScreen(0, Intent.EXTRA_SUBJECT, null, 1)
  }

  @Test
  fun testSendExtBodyAtt() {
    activeActivityRule.launch(
      generateIntentWithExtras(
        Intent.ACTION_SEND,
        null,
        Intent.EXTRA_TEXT,
        1
      )
    )
    registerAllIdlingResources()
    checkViewsOnScreen(0, null, Intent.EXTRA_TEXT, 1)
  }

  @Test
  fun testSendExtSubjectExtBodyAtt() {
    activeActivityRule.launch(
      generateIntentWithExtras(
        Intent.ACTION_SEND, Intent.EXTRA_SUBJECT,
        Intent.EXTRA_TEXT, 1
      )
    )
    registerAllIdlingResources()
    checkViewsOnScreen(0, Intent.EXTRA_SUBJECT, Intent.EXTRA_TEXT, 1)
  }

  @Test
  fun testSendMultipleMultiAtt() {
    activeActivityRule.launch(
      generateIntentWithExtras(
        Intent.ACTION_SEND_MULTIPLE,
        null,
        null,
        atts.size
      )
    )
    registerAllIdlingResources()
    checkViewsOnScreen(0, null, null, atts.size)
  }

  @Test
  fun testSendMultipleExtSubjectExtBodyMultiAtt() {
    activeActivityRule.launch(
      generateIntentWithExtras(
        Intent.ACTION_SEND_MULTIPLE, Intent.EXTRA_SUBJECT,
        Intent.EXTRA_TEXT, atts.size
      )
    )
    registerAllIdlingResources()
    checkViewsOnScreen(0, Intent.EXTRA_SUBJECT, Intent.EXTRA_TEXT, atts.size)
  }

  @Test
  fun testDoNotSupportAttWithFileSchema() {
    activeActivityRule.launch(
      generateIntentWithExtras(
        Intent.ACTION_SEND,
        Intent.EXTRA_SUBJECT,
        Intent.EXTRA_TEXT,
        0
      ).apply {
        putExtra(Intent.EXTRA_STREAM, atts.first())
      })
    registerAllIdlingResources()
    checkViewsOnScreen(0, Intent.EXTRA_SUBJECT, Intent.EXTRA_TEXT, 0)
  }

  private fun genIntentForUri(action: String?, stringUri: String?): Intent {
    val intent = Intent(getTargetContext(), CreateMessageActivity::class.java)
    intent.action = action
    if (stringUri != null) {
      intent.data = Uri.parse(stringUri)
    }
    return intent
  }


  private fun generateIntentWithExtras(
    action: String?, extraSubject: String?, extraMsg: CharSequence?,
    attachmentsCount: Int
  ): Intent {
    val intent = Intent(getTargetContext(), CreateMessageActivity::class.java)
    intent.action = action
    intent.putExtra(Intent.EXTRA_SUBJECT, extraSubject)
    intent.putExtra(Intent.EXTRA_TEXT, extraMsg)

    if (attachmentsCount > 0) {
      if (attachmentsCount == 1) {
        intent.putExtra(Intent.EXTRA_STREAM, genUriFromFile(atts.first()))
      } else {
        val urisFromAtts = ArrayList<Uri>()
        for (att in atts) {
          urisFromAtts.add(genUriFromFile(att))
        }
        intent.putExtra(Intent.EXTRA_STREAM, urisFromAtts)
      }
    }
    return intent
  }

  private fun checkViewsOnScreen(
    recipientsCount: Int,
    subject: String?,
    body: CharSequence?,
    attachmentsCount: Int
  ) {
    onView(withText(R.string.compose))
      .check(matches(isDisplayed()))
    onView(withId(R.id.editTextFrom))
      .check(matches(isDisplayed())).check(matches(withText(not(isEmptyString()))))
    closeSoftKeyboard()

    checkRecipients(recipientsCount)
    checkSubject(subject)
    checkBody(body)
    checkAtts(attachmentsCount)
  }

  private fun checkAtts(attachmentsCount: Int) {
    onView(withId(R.id.layoutAtts))
      .check(matches(hasChildCount(attachmentsCount)))

    when {
      attachmentsCount > 0 -> {
        if (attachmentsCount == 1) {
          onView(withText(atts.first().name))
            .check(matches(isDisplayed()))
        } else {
          for (att in atts) {
            onView(withText(att.name))
              .check(matches(isDisplayed()))
          }
        }
      }
    }
  }

  private fun checkBody(body: CharSequence?) {
    if (body != null) {
      onView(withId(R.id.editTextEmailMessage))
        .check(matches(isDisplayed()))
        .check(matches(withText(getRidOfCharacterSubstitutes(body.toString()))))
    } else {
      onView(withId(R.id.editTextEmailMessage))
        .check(matches(isDisplayed())).check(matches(withText(isEmptyString())))
    }
  }

  private fun checkSubject(subject: String?) {
    if (subject != null) {
      onView(withId(R.id.editTextEmailSubject))
        .check(matches(isDisplayed()))
        .check(matches(withText(getRidOfCharacterSubstitutes(subject))))
    } else {
      onView(withId(R.id.editTextEmailSubject))
        .check(matches(isDisplayed())).check(matches(withText(isEmptyString())))
    }
  }

  private fun checkRecipients(recipientsCount: Int) {
    if (recipientsCount > 0) {
      for (i in 0 until recipientsCount) {
        onView(withId(R.id.editTextRecipientTo))
          .check(matches(isDisplayed())).check(matches(withText(containsString(recipients[i]))))
      }
    } else {
      onView(withId(R.id.editTextRecipientTo))
        .check(matches(isDisplayed())).check(matches(withText(isEmptyString())))
    }
  }

  private fun getRidOfCharacterSubstitutes(message: String): String {
    return try {
      URLDecoder.decode(message, StandardCharsets.UTF_8.displayName())
    } catch (e: UnsupportedEncodingException) {
      e.printStackTrace()
      message
    }
  }

  private fun genUriFromFile(file: File): Uri {
    return FileProvider.getUriForFile(
      ApplicationProvider.getApplicationContext(), Constants
        .FILE_PROVIDER_AUTHORITY, file
    )
  }

  companion object {
    private const val ATTACHMENTS_COUNT = 3
    private const val ENCODED_SUBJECT = "some%20subject"
    private const val ENCODED_BODY = "some%20body"

    private val recipients: Array<String> = arrayOf(
      TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER,
      TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER
    )
  }
}
