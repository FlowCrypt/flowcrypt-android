/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.base

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.flowcrypt.email.R
import com.flowcrypt.email.util.UIUtil
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.model.ListSendAsResponse
import com.google.api.services.gmail.model.SendAs
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.net.HttpURLConnection

/**
 * @author Denys Bondarenko
 */
abstract class BaseComposeGmailApiSignatureFlowTest :
  BaseComposeGmailFlow(
    accountEntity = BASE_ACCOUNT_ENTITY.copy(useAliasSignatures = true)
  ) {

  override fun handleCommonAPICalls(request: RecordedRequest): MockResponse {
    return when {
      request.path == "/gmail/v1/users/me/settings/sendAs" -> {
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
          ListSendAsResponse().apply {
            factory = GsonFactory.getDefaultInstance()
            sendAs = listOf(
              SendAs().apply {
                sendAsEmail = accountEntity.email
                displayName = accountEntity.displayName
                replyToAddress = ""
                signature = HTML_SIGNATURE_FOR_MAIN
                isPrimary = true
                isDefault = true
              },
              SendAs().apply {
                sendAsEmail = ALIAS_EMAIL
                displayName = accountEntity.displayName
                replyToAddress = ""
                signature = HTML_SIGNATURE_FOR_ALIAS
                treatAsAlias = true
                verificationStatus = "accepted"
                isDefault = false
              },
            )
          }.toString()
        )
      }

      else -> super.handleCommonAPICalls(request)
    }
  }

  protected fun doBaseChecking(){
    onView(withId(R.id.editTextEmailMessage))
      .perform(scrollTo())
      .check(matches(withText("\n\n$SIGNATURE_FOR_MAIN")))

    //switch to standard mode
    openActionBarOverflowOrOptionsMenu(getTargetContext())
    onView(withText(R.string.switch_to_standard_email))
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withId(R.id.editTextEmailMessage))
      .perform(scrollTo())
      .check(matches(withText("\n\n$SIGNATURE_FOR_MAIN")))

    Espresso.closeSoftKeyboard()

    onView(withId(R.id.imageButtonAliases))
      .perform(scrollTo(), click())

    onView(withText(ALIAS_EMAIL))
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withId(R.id.editTextEmailMessage))
      .perform(scrollTo())
      .check(matches(withText("\n\n$SIGNATURE_FOR_ALIS")))
  }

  companion object {
    const val ALIAS_EMAIL = "alias@flowcrypt.test"
    const val HTML_SIGNATURE_FOR_MAIN =
      "\u003cdiv dir=\"ltr\"\u003e\u003cdiv\u003eRegards,\u003c/div\u003e\u003cdiv\u003eDefault at FlowCrypt\u003c/div\u003e\u003c/div\u003e"
    const val HTML_SIGNATURE_FOR_ALIAS = "\u003cdiv dir=\"ltr\"\u003eSignature for alias\u003c/div\u003e"
    val SIGNATURE_FOR_MAIN =
      UIUtil.getHtmlSpannedFromText(HTML_SIGNATURE_FOR_MAIN)?.toString()?.trimEnd()
    val SIGNATURE_FOR_ALIS =
      UIUtil.getHtmlSpannedFromText(HTML_SIGNATURE_FOR_ALIAS)?.toString()?.trimEnd()
  }
}