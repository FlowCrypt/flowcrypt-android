/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.MailTo
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.rules.lazyActivityScenarioRule
import com.flowcrypt.email.ui.activity.CreateMessageActivity
import com.flowcrypt.email.util.TestGeneralUtil
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.emptyString
import org.hamcrest.Matchers.`is`
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
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class ComposeScreenExternalIntentsFlowTest : BaseTest() {
  override val activeActivityRule =
    lazyActivityScenarioRule<CreateMessageActivity>(launchActivity = false)
  override val activityScenario: ActivityScenario<*>?
    get() = activeActivityRule.scenario

  private val mockWebServerRule =
    FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        if (request.path?.startsWith("/attester/pub", ignoreCase = true) == true) {
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
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
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
        TestGeneralUtil.createFileWithTextContent("$i.txt", UUID.randomUUID().toString())
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
    checkViewsOnScreen()
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
    checkViewsOnScreen(
      to = arrayOf(recipients[0]),
      subject = ENCODED_SUBJECT,
      body = ENCODED_BODY
    )
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
    checkViewsOnScreen(
      to = arrayOf(recipients[0]),
      subject = ENCODED_SUBJECT,
      body = ENCODED_BODY
    )
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
    checkViewsOnScreen(
      to = recipients,
      subject = ENCODED_SUBJECT,
      body = ENCODED_BODY
    )
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
    checkViewsOnScreen(
      to = recipients,
      subject = ENCODED_SUBJECT,
      body = ENCODED_BODY
    )
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
    checkViewsOnScreen(
      to = recipients,
      subject = ENCODED_SUBJECT,
      body = ENCODED_BODY
    )
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
    checkViewsOnScreen(
      to = recipients,
      subject = ENCODED_SUBJECT,
      body = ENCODED_BODY
    )
  }

  @Test
  fun testEmptyMailToSchema() {
    activeActivityRule.launch(genIntentForUri(randomActionForRFC6068, MailTo.MAILTO_SCHEME))
    registerAllIdlingResources()
    checkViewsOnScreen()
  }

  @Test
  fun testSendEmptyExtras() {
    activeActivityRule.launch(generateIntentWithExtras(action = Intent.ACTION_SEND))
    registerAllIdlingResources()
    checkViewsOnScreen()
  }

  @Test
  fun testSendExtSubject() {
    activeActivityRule.launch(
      generateIntentWithExtras(action = Intent.ACTION_SEND, extraSubject = Intent.EXTRA_SUBJECT)
    )
    registerAllIdlingResources()
    checkViewsOnScreen(subject = Intent.EXTRA_SUBJECT)
  }

  @Test
  fun testSendExtBody() {
    activeActivityRule.launch(
      generateIntentWithExtras(action = Intent.ACTION_SEND, extraMsg = Intent.EXTRA_TEXT)
    )
    registerAllIdlingResources()
    checkViewsOnScreen(body = Intent.EXTRA_TEXT)
  }

  @Test
  fun testSendAtt() {
    activeActivityRule.launch(
      generateIntentWithExtras(action = Intent.ACTION_SEND, attachmentsCount = 1)
    )
    registerAllIdlingResources()
    checkViewsOnScreen(attachmentsCount = 1)
  }

  @Test
  fun testSendExtSubjectExtBody() {
    activeActivityRule.launch(
      generateIntentWithExtras(
        action = Intent.ACTION_SEND,
        extraSubject = Intent.EXTRA_SUBJECT,
        extraMsg = Intent.EXTRA_TEXT
      )
    )
    registerAllIdlingResources()
    checkViewsOnScreen(subject = Intent.EXTRA_SUBJECT, body = Intent.EXTRA_TEXT)
  }

  @Test
  fun testSendExtSubjectAtt() {
    activeActivityRule.launch(
      generateIntentWithExtras(
        action = Intent.ACTION_SEND,
        extraSubject = Intent.EXTRA_SUBJECT,
        attachmentsCount = 1
      )
    )
    registerAllIdlingResources()
    checkViewsOnScreen(subject = Intent.EXTRA_SUBJECT, attachmentsCount = 1)
  }

  @Test
  fun testSendExtBodyAtt() {
    activeActivityRule.launch(
      generateIntentWithExtras(
        action = Intent.ACTION_SEND,
        extraMsg = Intent.EXTRA_TEXT,
        attachmentsCount = 1
      )
    )
    registerAllIdlingResources()
    checkViewsOnScreen(body = Intent.EXTRA_TEXT, attachmentsCount = 1)
  }

  @Test
  fun testSendExtSubjectExtBodyAtt() {
    activeActivityRule.launch(
      generateIntentWithExtras(
        action = Intent.ACTION_SEND,
        extraSubject = Intent.EXTRA_SUBJECT,
        extraMsg = Intent.EXTRA_TEXT,
        attachmentsCount = 1
      )
    )
    registerAllIdlingResources()
    checkViewsOnScreen(
      subject = Intent.EXTRA_SUBJECT,
      body = Intent.EXTRA_TEXT,
      attachmentsCount = 1
    )
  }

  @Test
  fun testSendMultipleMultiAtt() {
    activeActivityRule.launch(
      generateIntentWithExtras(
        action = Intent.ACTION_SEND_MULTIPLE,
        attachmentsCount = atts.size
      )
    )
    registerAllIdlingResources()
    checkViewsOnScreen(attachmentsCount = atts.size)
  }

  @Test
  fun testSendMultipleExtSubjectExtBodyMultiAtt() {
    activeActivityRule.launch(
      generateIntentWithExtras(
        action = Intent.ACTION_SEND_MULTIPLE,
        extraSubject = Intent.EXTRA_SUBJECT,
        extraMsg = Intent.EXTRA_TEXT, attachmentsCount = atts.size
      )
    )
    registerAllIdlingResources()
    checkViewsOnScreen(
      subject = Intent.EXTRA_SUBJECT,
      body = Intent.EXTRA_TEXT,
      attachmentsCount = atts.size
    )
  }

  @Test
  fun testSendExtToCcBcc() {
    val to = "to@to.to"
    val cc = "cc@cc.cc"
    val bcc = "bcc@bcc.bcc"
    activeActivityRule.launch(
      generateIntentWithExtras(
        action = Intent.ACTION_SEND,
        to = arrayOf(to),
        cc = arrayOf(cc),
        bcc = arrayOf(bcc)
      )
    )
    registerAllIdlingResources()
    checkViewsOnScreen(
      to = arrayOf(to),
      cc = arrayOf(cc),
      bcc = arrayOf(bcc)
    )
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
    checkViewsOnScreen(subject = Intent.EXTRA_SUBJECT, body = Intent.EXTRA_TEXT)
  }

  private fun genIntentForUri(action: String?, stringUri: String?): Intent {
    return Intent(getTargetContext(), CreateMessageActivity::class.java).apply {
      this.action = action
      stringUri?.let { this.data = Uri.parse(it) }
    }
  }


  private fun generateIntentWithExtras(
    action: String? = null,
    extraSubject: String? = null,
    extraMsg: CharSequence? = null,
    attachmentsCount: Int = 0,
    to: Array<String>? = null,
    cc: Array<String>? = null,
    bcc: Array<String>? = null
  ): Intent {
    val intent = Intent(getTargetContext(), CreateMessageActivity::class.java)
    intent.action = action
    intent.putExtra(Intent.EXTRA_SUBJECT, extraSubject)
    intent.putExtra(Intent.EXTRA_TEXT, extraMsg)
    intent.putExtra(Intent.EXTRA_EMAIL, to)
    intent.putExtra(Intent.EXTRA_CC, cc)
    intent.putExtra(Intent.EXTRA_BCC, bcc)

    if (attachmentsCount > 0) {
      if (attachmentsCount == 1) {
        intent.putExtra(Intent.EXTRA_STREAM, genUriFromFile(atts.first()))
      } else {
        val attachmentUris = ArrayList<Uri>()
        for (att in atts) {
          attachmentUris.add(genUriFromFile(att))
        }
        intent.putExtra(Intent.EXTRA_STREAM, attachmentUris)
      }
    }
    return intent
  }

  private fun checkViewsOnScreen(
    subject: String? = null,
    body: CharSequence? = null,
    attachmentsCount: Int = 0,
    to: Array<String> = arrayOf(),
    cc: Array<String> = arrayOf(),
    bcc: Array<String> = arrayOf()
  ) {
    onView(withText(R.string.compose))
      .check(matches(isDisplayed()))
    onView(withId(R.id.editTextFrom))
      .check(matches(isDisplayed())).check(matches(withText(not(`is`(emptyString())))))
    closeSoftKeyboard()

    checkRecipients(R.id.recyclerViewChipsTo, to)
    if (cc.isNotEmpty()) {
      checkRecipients(R.id.recyclerViewChipsCc, cc)
    }
    if (bcc.isNotEmpty()) {
      checkRecipients(R.id.recyclerViewChipsBcc, bcc)
    }

    checkSubject(subject)
    checkBody(body)
    checkAtts(attachmentsCount)
  }

  private fun checkAtts(attachmentsCount: Int) {
    onView(withId(R.id.rVAttachments))
      .check(matches(withRecyclerViewItemCount(attachmentsCount)))

    atts.take(attachmentsCount).forEach {
      onView(withId(R.id.rVAttachments))
        .perform(
          scrollTo<ViewHolder>(
            allOf(
              hasDescendant(withText(it.name)),
              hasDescendant(withId(R.id.imageButtonPreviewAtt))
            )
          )
        )
    }
  }

  private fun checkBody(body: CharSequence?) {
    if (body != null) {
      onView(withId(R.id.editTextEmailMessage))
        .check(matches(isDisplayed()))
        .check(matches(withText(getRidOfCharacterSubstitutes(body.toString()))))
    } else {
      onView(withId(R.id.editTextEmailMessage))
        .check(matches(isDisplayed())).check(matches(withText(`is`(emptyString()))))
    }
  }

  private fun checkSubject(subject: String?) {
    if (subject != null) {
      onView(withId(R.id.editTextEmailSubject))
        .check(matches(isDisplayed()))
        .check(matches(withText(getRidOfCharacterSubstitutes(subject))))
    } else {
      onView(withId(R.id.editTextEmailSubject))
        .check(matches(isDisplayed())).check(matches(withText(`is`(emptyString()))))
    }
  }

  private fun checkRecipients(
    viewId: Int,
    recipients: Array<String> = arrayOf(),
  ) {
    val recipientsCount = recipients.size
    if (recipientsCount > 0) {
      onView(withId(viewId))
        .check(matches(isDisplayed()))
        .check(matches(withRecyclerViewItemCount(recipientsCount + 1)))

      for (recipient in recipients) {
        onView(withId(viewId))
          .perform(scrollTo<ViewHolder>(withText(recipient)))
      }
    } else {
      onView(withId(viewId))
        .check(matches(isDisplayed()))
        .check(matches(withRecyclerViewItemCount(1)))
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
