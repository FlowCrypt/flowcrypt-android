/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.gmailapi.base

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.scrollTo
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToHolder
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.web.assertion.WebViewAssertions.webContent
import androidx.test.espresso.web.matcher.DomMatchers.elementByXPath
import androidx.test.espresso.web.sugar.Web.onWebView
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.matchers.CustomMatchers.Companion.hasItemAtPosition
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withDrawable
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withEmptyRecyclerView
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withMessageHeaderInfo
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withPgpBadge
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withTextContentMatcher
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withViewBackgroundTint
import com.flowcrypt.email.rules.AddLabelsToDatabaseRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.ui.adapter.GmailApiLabelsListAdapter
import com.flowcrypt.email.ui.adapter.MessageHeadersListAdapter
import com.flowcrypt.email.ui.adapter.PgpBadgeListAdapter
import com.flowcrypt.email.ui.base.BaseGmailApiTest
import com.flowcrypt.email.ui.base.BaseGmailLabelsFlowTest.GmailApiLabelMatcher
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.viewaction.ClickOnViewInRecyclerViewItem
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.Before
import java.util.concurrent.TimeUnit

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

  @Before
  fun waitForThreadsList() {
    //need to wait while the app loads the thread list
    waitForObjectWithText(SUBJECT_EXISTING_STANDARD, TimeUnit.SECONDS.toMillis(20))
  }

  protected fun checkReplyButtons(isVisible: Boolean = true, isEncryptedMode: Boolean = true) {
    if (!isVisible) {
      onView(
        allOf(
          withId(R.id.replyButton),
          withParent(
            allOf(
              withId(R.id.layoutReplyButtons),
              withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
              hasSibling(allOf(withId(R.id.recyclerViewMessages), isDisplayed()))
            )
          )
        )
      ).check(matches(not(isDisplayed())))
      onView(
        allOf(
          withId(R.id.replyAllButton),
          withParent(
            allOf(
              withId(R.id.layoutReplyButtons),
              withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
              hasSibling(allOf(withId(R.id.recyclerViewMessages), isDisplayed()))
            )
          )
        )
      ).check(matches(not(isDisplayed())))
      onView(
        allOf(
          withId(R.id.forwardButton),
          withParent(
            allOf(
              withId(R.id.layoutReplyButtons),
              withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
              hasSibling(allOf(withId(R.id.recyclerViewMessages), isDisplayed()))
            )
          )
        )
      ).check(matches(not(isDisplayed())))

      return
    }

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
        .perform(scrollToHolder(GmailApiLabelMatcher(label)))
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
      onView(allOf(withId(R.id.rVAttachments)))
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
              listOfNotNull(
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
                ).takeIf { pair.second > 0 },
                hasDescendant(withId(R.id.imageButtonPreviewAtt)),
                hasDescendant(withId(R.id.imageButtonDownloadAtt)),
              )
            )
          )
        )
    }
  }

  protected fun checkPgpBadges(
    badgeCount: Int,
    vararg badgeTypes: PgpBadgeListAdapter.PgpBadge.Type
  ) {
    onView(allOf(withId(R.id.rVPgpBadges), isDisplayed()))
      .check(matches(withRecyclerViewItemCount(badgeCount)))

    for (badgeType in badgeTypes) {
      onView(allOf(withId(R.id.rVPgpBadges), isDisplayed()))
        .perform(
          scrollToHolder(
            withPgpBadge(PgpBadgeListAdapter.PgpBadge(badgeType))
          )
        )
    }
  }

  protected fun checkWebViewText(content: String?) {
    onWebView(allOf(withId(R.id.emailWebView), isDisplayed())).forceJavascriptEnabled()
    onWebView(allOf(withId(R.id.emailWebView), isDisplayed()))
      .check(webContent(elementByXPath("/html/body", withTextContentMatcher(`is`(content)))))
  }

  protected fun openThreadBasedOnPosition(position: Int) {
    onView(allOf(withId(R.id.recyclerViewMsgs), isDisplayed())).perform(
      RecyclerViewActions.actionOnItemAtPosition<ViewHolder>(position, click())
    )

    //need to wait while the app loads the messages list and render the last one
    waitForObjectWithResourceName("imageButtonReplyAll", TimeUnit.SECONDS.toMillis(10))
  }

  protected fun checkBaseMessageDetailsInTread(
    fromAddress: String,
    datetimeInMilliseconds: Long,
    headersMapTransformation: (Map<String, String>) -> Map<String, String> = { it }
  ) {
    onView(allOf(withId(R.id.recyclerViewMessages), isDisplayed()))
      .perform(
        scrollTo<ViewHolder>(
          hasDescendant(
            allOf(
              withId(R.id.textViewSenderAddress),
              withText(fromAddress)
            )
          )
        )
      )

    onView(allOf(withId(R.id.recyclerViewMessages), isDisplayed()))
      .perform(
        scrollTo<ViewHolder>(
          allOf(
            hasDescendant(
              withId(R.id.imageButtonMoreOptions)
            ),
            hasDescendant(
              allOf(
                withId(R.id.textViewDate),
                withText(DateTimeUtil.formatSameDayTime(getTargetContext(), datetimeInMilliseconds))
              )
            )
          )
        )
      )

    //open headers details and check them
    onView(allOf(withId(R.id.recyclerViewMessages), isDisplayed()))
      .perform(
        RecyclerViewActions.actionOnItem<ViewHolder>(
          hasDescendant(withId(R.id.iBShowDetails)),
          ClickOnViewInRecyclerViewItem(R.id.iBShowDetails)
        )
      )

    val messageHeadersList = headersMapTransformation.invoke(
      mapOf(
        getResString(R.string.from) to DEFAULT_FROM_RECIPIENT,
        getResString(R.string.reply_to) to DEFAULT_FROM_RECIPIENT,
        getResString(R.string.to) to EXISTING_MESSAGE_TO_RECIPIENT,
        getResString(R.string.cc) to EXISTING_MESSAGE_CC_RECIPIENT,
        getResString(R.string.date) to DateUtils.formatDateTime(
          getTargetContext(),
          datetimeInMilliseconds,
          DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_YEAR
        )
      )
    ).map { MessageHeadersListAdapter.Header(name = it.key, value = it.value) }

    messageHeadersList.forEach {
      onView(allOf(withId(R.id.rVMsgDetails), isDisplayed()))
        .perform(scrollToHolder(withMessageHeaderInfo(it)))
    }
  }

  protected fun checkCollapsedState(
    position: Int,
    hasPgp: Boolean,
    hasAttachments: Boolean,
    needToCollapseVisibleExpandedItem: Boolean = true
  ) {
    if (needToCollapseVisibleExpandedItem) {
      collapseVisibleExpandedItem()
    }

    onView(allOf(withId(R.id.recyclerViewMessages), isDisplayed()))
      .check(
        matches(
          hasItemAtPosition(
            position = position,
            matcher = hasDescendant(
              allOf(
                withId(R.id.viewHasAttachments),
                withEffectiveVisibility(
                  if (hasAttachments) {
                    ViewMatchers.Visibility.VISIBLE
                  } else {
                    ViewMatchers.Visibility.GONE
                  }
                )
              )
            )
          )
        )
      )

    onView(allOf(withId(R.id.recyclerViewMessages), isDisplayed()))
      .check(
        matches(
          hasItemAtPosition(
            position = position,
            matcher = hasDescendant(
              allOf(
                withId(R.id.viewHasPgp),
                withEffectiveVisibility(
                  if (hasPgp) {
                    ViewMatchers.Visibility.VISIBLE
                  } else {
                    ViewMatchers.Visibility.GONE
                  }
                )
              )
            )
          )
        )
      )
  }

  protected fun collapseVisibleExpandedItem() {
    onView(allOf(withId(R.id.recyclerViewMessages), isDisplayed()))
      .perform(
        RecyclerViewActions.actionOnItem<ViewHolder>(
          hasDescendant(withId(R.id.layoutHeader)),
          ClickOnViewInRecyclerViewItem(R.id.layoutHeader)
        )
      )
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