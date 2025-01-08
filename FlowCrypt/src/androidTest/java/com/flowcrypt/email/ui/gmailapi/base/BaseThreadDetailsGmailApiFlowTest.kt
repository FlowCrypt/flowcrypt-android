/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.gmailapi.base

import android.text.format.Formatter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.scrollTo
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
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withDrawable
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withEmptyRecyclerView
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withPgpBadge
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withTextContentMatcher
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withViewBackgroundTint
import com.flowcrypt.email.rules.AddLabelsToDatabaseRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.ui.adapter.GmailApiLabelsListAdapter
import com.flowcrypt.email.ui.adapter.PgpBadgeListAdapter
import com.flowcrypt.email.ui.base.BaseGmailApiTest
import com.flowcrypt.email.ui.base.BaseGmailLabelsFlowTest.GmailApiLabelMatcher
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.`is`

/**
 * @author Denys Bondarenko
 */
abstract class BaseThreadDetailsGmailApiFlowTest(
  accountEntity: AccountEntity = BASE_ACCOUNT_ENTITY.copy(useConversationMode = true)
) : BaseGmailApiTest(accountEntity) {

  override val mockWebServerRule =
    FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        return when {
          else -> handleCommonAPICalls(request)
        }
      }
    })

  protected val customLabelsRule = AddLabelsToDatabaseRule(
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

  protected fun checkReplyButtons(isEncryptedMode: Boolean) {
    val tintColor = if (isEncryptedMode) {
      R.color.colorPrimary
    } else {
      R.color.red
    }
    onView(allOf(withId(R.id.recyclerViewMessages), isDisplayed()))
      .perform(
        scrollTo<ViewHolder>(
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
      .check(matches(withText(getResString(R.string.reply_encrypted.takeIf {
        isEncryptedMode
      } ?: R.string.reply))))
      .check(matches(withDrawable(resId = R.drawable.ic_reply, tintColor = tintColor)))

    onView(
      allOf(
        withId(R.id.replyAllButton),
        withParent(allOf(withId(R.id.layoutReplyButtons), isDisplayed()))
      )
    ).check(matches(isDisplayed()))
      .check(matches(withText(getResString(R.string.reply_all_encrypted.takeIf {
        isEncryptedMode
      } ?: R.string.reply_all))))
      .check(matches(withDrawable(resId = R.drawable.ic_reply_all, tintColor = tintColor)))

    onView(
      allOf(
        withId(R.id.forwardButton),
        withParent(allOf(withId(R.id.layoutReplyButtons), isDisplayed()))
      )
    ).check(matches(isDisplayed()))
      .check(matches(withText(getResString(R.string.forward_encrypted.takeIf {
        isEncryptedMode
      } ?: R.string.forward))))
      .check(matches(withDrawable(resId = R.drawable.ic_forward, tintColor = tintColor)))
  }

  protected fun checkCorrectThreadDetails(
    messagesCount: Int,
    threadSubject: String,
    labels: List<GmailApiLabelsListAdapter.Label>
  ) {
    //check correct messages count
    onView(allOf(withId(R.id.recyclerViewMessages), isDisplayed()))
      .check(matches(withRecyclerViewItemCount(messagesCount + 1)))//header + messages

    //check thread subject
    onView(allOf(withId(R.id.recyclerViewMessages), isDisplayed()))
      .perform(
        scrollTo<ViewHolder>(
          hasDescendant(
            allOf(
              withId(R.id.textViewSubject),
              withText(threadSubject)
            )
          )
        )
      )

    //check correct labels
    onView(allOf(withId(R.id.recyclerViewLabels), isDisplayed()))
      .check(matches(withRecyclerViewItemCount(labels.size)))

    for (label in labels) {
      onView(allOf(withId(R.id.recyclerViewLabels), isDisplayed()))
        .perform(RecyclerViewActions.scrollToHolder(GmailApiLabelMatcher(label)))
        .check(
          matches(
            hasDescendant(
              allOf(
                listOfNotNull(
                  isDisplayed(),
                  hasDescendant(withText(label.name)),
                  label.backgroundColor?.let { backgroundColor ->
                    withViewBackgroundTint(getTargetContext(), backgroundColor)
                  }
                )
              )
            )
          )
        )
    }
  }

  protected fun checkAttachments(pairs: List<Pair<String, Long>>) {
    if (pairs.isEmpty()) {
      onView(allOf(withId(R.id.rVAttachments), isDisplayed()))
        .check(matches(withEmptyRecyclerView()))
      return
    }

    onView(allOf(withId(R.id.rVAttachments), isDisplayed()))
      .check(matches(withRecyclerViewItemCount(pairs.size)))

    pairs.forEach { pair ->
      onView(allOf(withId(R.id.rVAttachments), isDisplayed()))
        .perform(
          scrollTo<ViewHolder>(
            allOf(
              hasDescendant(
                allOf(
                  withId(R.id.textViewAttachmentName),
                  withText(pair.first)
                )
              ),
              hasDescendant(
                allOf(
                  withId(R.id.textViewAttSize),
                  withText(Formatter.formatFileSize(getTargetContext(), pair.second))
                )
              ),
              hasDescendant(withId(R.id.imageButtonPreviewAtt)),
              hasDescendant(withId(R.id.imageButtonDownloadAtt)),
            )
          )
        )
    }
  }

  protected fun testPgpBadges(
    badgeCount: Int,
    vararg badgeTypes: PgpBadgeListAdapter.PgpBadge.Type
  ) {
    onView(allOf(withId(R.id.rVPgpBadges), isDisplayed()))
      .check(matches(withRecyclerViewItemCount(badgeCount)))

    for (badgeType in badgeTypes) {
      onView(allOf(withId(R.id.rVPgpBadges), isDisplayed()))
        .perform(
          RecyclerViewActions.scrollToHolder(
            withPgpBadge(PgpBadgeListAdapter.PgpBadge(badgeType))
          )
        )
    }
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
    val CUSTOM_LABELS = listOf(
      GmailApiLabelsListAdapter.Label("LABEL_1", "#ff2323ff", "#FFFFFF"),
      GmailApiLabelsListAdapter.Label("LABEL_2", "#FFD14836", "#FFFFFF"),
      GmailApiLabelsListAdapter.Label("LABEL_3", "#FFFFEB3B", "#FFFFFF"),
      GmailApiLabelsListAdapter.Label("LABEL_4", "#FF00EB3B", "#FFFFFF"),
    )
  }
}