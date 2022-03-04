/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import android.text.format.Formatter
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.webdriver.Locator
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withDrawable
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.lazyActivityScenarioRule
import com.flowcrypt.email.ui.activity.MessageDetailsActivity
import com.flowcrypt.email.util.DateTimeUtil
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matchers
import org.junit.After

/**
 * @author Denis Bondarenko
 *         Date: 6/7/21
 *         Time: 1:25 PM
 *         E-mail: DenBond7@gmail.com
 */
abstract class BaseMessageDetailsActivityTest : BaseTest() {
  override val useIntents: Boolean = true
  override val activeActivityRule =
    lazyActivityScenarioRule<MessageDetailsActivity>(launchActivity = false)
  override val activityScenario: ActivityScenario<*>?
    get() = activeActivityRule.scenario
  protected open val addAccountToDatabaseRule = AddAccountToDatabaseRule()

  private val localFolder: LocalFolder
    get() = LocalFolder(
      addAccountToDatabaseRule.account.email,
      fullName = "INBOX",
      folderAlias = "INBOX",
      msgCount = 1,
      attributes = listOf("\\HasNoChildren")
    )

  private var idlingForWebView: IdlingResource? = null

  @After
  fun unregisterDecryptionIdling() {
    idlingForWebView?.let { IdlingRegistry.getInstance().unregister(it) }
  }

  protected fun launchActivity(msgEntity: MessageEntity) {
    activeActivityRule.launch(
      MessageDetailsActivity.getIntent(
        getTargetContext(),
        localFolder,
        msgEntity
      )
    )
    registerAllIdlingResources()

    activityScenario?.onActivity { activity ->
      val messageDetailsActivity = (activity as? MessageDetailsActivity) ?: return@onActivity
      idlingForWebView = messageDetailsActivity.idlingForWebView
      idlingForWebView?.let { IdlingRegistry.getInstance().register(it) }
    }
  }

  protected fun matchReplyButtons(msgEntity: MessageEntity) {
    onView(withId(R.id.imageButtonReplyAll))
      .check(matches(isDisplayed()))
    onView(withId(R.id.layoutReplyButton))
      .perform(ViewActions.scrollTo())
      .check(matches(isDisplayed()))
    onView(withId(R.id.layoutReplyAllButton))
      .check(matches(isDisplayed()))
    onView(withId(R.id.layoutFwdButton))
      .check(matches(isDisplayed()))

    if (msgEntity.isEncrypted == true) {
      onView(withId(R.id.textViewReply))
        .check(matches(withText(getResString(R.string.reply_encrypted))))
      onView(withId(R.id.textViewReplyAll))
        .check(matches(withText(getResString(R.string.reply_all_encrypted))))
      onView(withId(R.id.textViewFwd))
        .check(matches(withText(getResString(R.string.forward_encrypted))))

      onView(withId(R.id.imageViewReply))
        .check(matches(withDrawable(R.mipmap.ic_reply_green)))
      onView(withId(R.id.imageViewReplyAll))
        .check(matches(withDrawable(R.mipmap.ic_reply_all_green)))
      onView(withId(R.id.imageViewFwd))
        .check(matches(withDrawable(R.mipmap.ic_forward_green)))
    } else {
      onView(withId(R.id.textViewReply))
        .check(matches(withText(getResString(R.string.reply))))
      onView(withId(R.id.textViewReplyAll))
        .check(matches(withText(getResString(R.string.reply_all))))
      onView(withId(R.id.textViewFwd))
        .check(matches(withText(getResString(R.string.forward))))

      onView(withId(R.id.imageViewReply))
        .check(matches(withDrawable(R.mipmap.ic_reply_red)))
      onView(withId(R.id.imageViewReplyAll))
        .check(matches(withDrawable(R.mipmap.ic_reply_all_red)))
      onView(withId(R.id.imageViewFwd))
        .check(matches(withDrawable(R.mipmap.ic_forward_red)))
    }
  }

  protected fun baseCheckWithAtt(incomingMsgInfo: IncomingMessageInfo?, att: AttachmentInfo?) {
    ViewMatchers.assertThat(incomingMsgInfo, Matchers.notNullValue())

    val msgEntity = incomingMsgInfo!!.msgEntity
    launchActivity(msgEntity)
    matchHeader(incomingMsgInfo)

    checkWebViewText(incomingMsgInfo.text)
    onView(withId(R.id.layoutAtt))
      .check(matches(isDisplayed()))
    matchAtt(att)
    matchReplyButtons(msgEntity)
  }

  protected fun matchHeader(incomingMsgInfo: IncomingMessageInfo?) {
    val msgEntity = incomingMsgInfo?.msgEntity
    requireNotNull(msgEntity)

    onView(withId(R.id.textViewSenderAddress))
      .check(matches(withText(EmailUtil.getFirstAddressString(msgEntity.from))))
    onView(withId(R.id.textViewDate))
      .check(
        matches(
          withText(DateTimeUtil.formatSameDayTime(getTargetContext(), msgEntity.receivedDate))
        )
      )
    onView(withId(R.id.textViewSubject))
      .check(
        matches(
          Matchers.anyOf(
            withText(msgEntity.subject),
            withText(incomingMsgInfo.inlineSubject)
          )
        )
      )
  }

  private fun matchAtt(att: AttachmentInfo?) {
    requireNotNull(att)
    onView(withId(R.id.textViewAttachmentName))
      .check(matches(withText(att.name)))
    onView(withId(R.id.textViewAttSize))
      .check(matches(withText(Formatter.formatFileSize(getContext(), att.encodedSize))))
  }

  protected fun checkWebViewText(text: String?) {
    onWebView(withId(R.id.emailWebView)).forceJavascriptEnabled()
    onWebView(withId(R.id.emailWebView))
      .withElement(findElement(Locator.XPATH, "/html/body"))
      .check(webMatches(getText(), equalTo(text)))
  }
}
