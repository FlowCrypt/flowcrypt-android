/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.gmailapi.base

import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.web.assertion.TagSoupDocumentParser
import androidx.test.espresso.web.assertion.WebViewAssertions.webContent
import androidx.test.espresso.web.matcher.DomMatchers.elementByXPath
import androidx.test.espresso.web.sugar.Web.onWebView
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withTextContentMatcher
import com.flowcrypt.email.rules.AddLabelsToDatabaseRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.ui.adapter.GmailApiLabelsListAdapter
import com.flowcrypt.email.ui.base.BaseGmailApiTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
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