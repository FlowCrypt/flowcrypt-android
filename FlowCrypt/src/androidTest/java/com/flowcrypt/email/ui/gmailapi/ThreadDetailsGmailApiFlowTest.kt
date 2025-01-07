/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.gmailapi

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.web.assertion.TagSoupDocumentParser
import androidx.test.espresso.web.assertion.WebViewAssertions.webContent
import androidx.test.espresso.web.matcher.DomMatchers.elementByXPath
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withDrawable
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withTextContentMatcher
import com.flowcrypt.email.rules.AddLabelsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.adapter.GmailApiLabelsListAdapter
import com.flowcrypt.email.ui.base.BaseGmailApiTest
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.viewaction.ClickOnViewInRecyclerViewItem
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.`is`
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@FlowCryptTestSettings(useCommonIdling = false)
class ThreadDetailsGmailApiFlowTest : BaseGmailApiTest(
  BASE_ACCOUNT_ENTITY.copy(useConversationMode = true)
) {
  override val mockWebServerRule =
    FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        return when {
          else -> handleCommonAPICalls(request)
        }
      }
    })

  private val customLabelsRule = AddLabelsToDatabaseRule(
    account = accountEntity, folders = CUSTOM_LABELS.map {
      LocalFolder(
        account = addAccountToDatabaseRule.account.email,
        fullName = it.name,
        folderAlias = it.name,
        isCustom = true,
        labelColor = it.backgroundColor,
        textColor = it.textColor,
        attributes = listOf("\\HasNoChildren")
      )
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
    .around(customLabelsRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testThreadDetailsWithSingleMessage() {
    //need to wait while the app loads the thread list
    waitForObjectWithText(SUBJECT_EXISTING_STANDARD, TimeUnit.SECONDS.toMillis(20))

    //open a thread with a single message
    onView(withId(R.id.recyclerViewMsgs)).perform(
      RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
        hasDescendant(
          allOf(
            withId(R.id.textViewSubject),
            withText(SUBJECT_SINGLE)
          )
        ),
        click()
      )
    )

    //need to wait while the app loads the messages list and render the last one
    waitForObjectWithText(MESSAGE_EXISTING_STANDARD, TimeUnit.SECONDS.toMillis(10))

    onView(allOf(withId(R.id.recyclerViewMessages), isDisplayed()))
      .check(matches(withRecyclerViewItemCount(2)))//header + 1 message

    //check thread subject
    onView(allOf(withId(R.id.recyclerViewMessages), isDisplayed()))
      .perform(
        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
          hasDescendant(
            allOf(
              withId(R.id.textViewSubject),
              withText(SUBJECT_SINGLE)
            )
          )
        )
      )

    onView(allOf(withId(R.id.recyclerViewMessages), isDisplayed())).perform(
      RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
        1,
        ClickOnViewInRecyclerViewItem(R.id.iBShowDetails)
      )
    )

    onView(allOf(withId(R.id.recyclerViewMessages), isDisplayed()))
      .perform(
        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
          hasDescendant(
            allOf(
              withId(R.id.textViewSenderAddress),
              withText("From")
            )
          )
        )
      )

    onView(allOf(withId(R.id.recyclerViewMessages), isDisplayed()))
      .perform(
        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
          hasDescendant(
            allOf(
              withId(R.id.textViewDate),
              withText(DateTimeUtil.formatSameDayTime(getTargetContext(), DATE_EXISTING_STANDARD))
            )
          )
        )
      )

    //check reply buttons
    onView(allOf(withId(R.id.recyclerViewMessages), isDisplayed()))
      .perform(
        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
          hasDescendant(
            allOf(
              withId(R.id.imageButtonReplyAll),
              withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
            )
          )
        )
      )
    onView(
      allOf(
        withId(R.id.replyButton),
        withParent(allOf(withId(R.id.layoutReplyButtons), isDisplayed()))
      )
    ).check(matches(isDisplayed()))
      .check(matches(withText(getResString(R.string.reply))))
      .check(matches(withDrawable(resId = R.drawable.ic_reply, tintColor = R.color.red)))

    onView(
      allOf(
        withId(R.id.replyAllButton),
        withParent(allOf(withId(R.id.layoutReplyButtons), isDisplayed()))
      )
    ).check(matches(isDisplayed()))
      .check(matches(withText(getResString(R.string.reply_all))))
      .check(matches(withDrawable(resId = R.drawable.ic_reply_all, tintColor = R.color.red)))

    onView(
      allOf(
        withId(R.id.forwardButton),
        withParent(allOf(withId(R.id.layoutReplyButtons), isDisplayed()))
      )
    ).check(matches(isDisplayed()))
      .check(matches(withText(getResString(R.string.forward))))
      .check(matches(withDrawable(resId = R.drawable.ic_forward, tintColor = R.color.red)))
    //Thread.sleep(30000)
  }

  protected fun checkWebViewText(html: String?) {
    val document = TagSoupDocumentParser.newInstance().parse(html)
    val bodyElements = document.getElementsByTagName("body")

    val content = (0 until bodyElements.length)
      .asSequence()
      .map { bodyElements.item(it) }
      .filter { it.textContent.isNotEmpty() }
      .joinToString(separator = "\n") { it.textContent }
      .trim()

    onWebView(withId(R.id.emailWebView)).forceJavascriptEnabled()
    onWebView(withId(R.id.emailWebView))
      .check(webContent(elementByXPath("/html/body", withTextContentMatcher(`is`(content)))))
    //.check(webContent(withBody(withTextContentMatcher(`is`(content)))))
  }

  companion object {
    private val CUSTOM_LABELS = listOf(
      GmailApiLabelsListAdapter.Label("LABEL_1", "#ff2323ff", "#FFFFFF"),
      GmailApiLabelsListAdapter.Label("LABEL_2", "#FFD14836", "#FFFFFF"),
      GmailApiLabelsListAdapter.Label("LABEL_3", "#FFFFEB3B", "#FFFFFF"),
      GmailApiLabelsListAdapter.Label("LABEL_4", "#FF00EB3B", "#FFFFFF"),
    )
  }
}