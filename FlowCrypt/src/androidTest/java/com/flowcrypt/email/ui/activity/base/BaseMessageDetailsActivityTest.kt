/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import android.text.format.Formatter
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.espresso.matcher.ViewMatchers.isChecked
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
import com.flowcrypt.email.api.retrofit.response.model.DecryptErrorMsgBlock
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.matchers.CustomMatchers
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withDrawable
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.lazyActivityScenarioRule
import com.flowcrypt.email.ui.activity.CreateMessageActivity
import com.flowcrypt.email.ui.adapter.MsgDetailsRecyclerViewAdapter
import com.flowcrypt.email.ui.adapter.PgpBadgeListAdapter
import com.flowcrypt.email.util.DateTimeUtil
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.notNullValue
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

  protected fun testStandardMsgPlaintextInternal() {
    baseCheck(
      getMsgInfo(
        "messages/info/standard_msg_info_plaintext.json",
        "messages/mime/standard_msg_info_plaintext.txt"
      )
    )
    onView(withId(R.id.tVTo))
      .check(matches(withText(getResString(R.string.to_receiver, getResString(R.string.me)))))
  }

  protected fun matchReplyButtons(msgEntity: MessageEntity) {
    onView(withId(R.id.imageButtonReplyAll))
      .check(matches(isDisplayed()))
    onView(withId(R.id.layoutReplyButton))
      .perform(scrollTo())
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
    assertThat(incomingMsgInfo, notNullValue())

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

  protected fun testMissingKey(incomingMsgInfo: IncomingMessageInfo?) {
    assertThat(incomingMsgInfo, notNullValue())

    val details = incomingMsgInfo!!.msgEntity

    launchActivity(details)
    matchHeader(incomingMsgInfo)

    val block = incomingMsgInfo.msgBlocks?.get(1) as DecryptErrorMsgBlock
    val errorMsg = getResString(R.string.decrypt_error_current_key_cannot_open_message)

    onView(withId(R.id.textViewErrorMessage))
      .check(matches(withText(errorMsg)))

    testSwitch(block.content ?: "")
    matchReplyButtons(details)
  }

  protected fun testSwitch(content: String) {
    onView(withId(R.id.textViewOrigPgpMsg))
      .check(matches(not(isDisplayed())))
    onView(withId(R.id.switchShowOrigMsg))
      .check(matches(not(isChecked())))
      .perform(scrollTo(), click())
    onView(withId(R.id.textViewOrigPgpMsg))
      .check(matches(isDisplayed()))
    onView(withId(R.id.textViewOrigPgpMsg))
      .check(matches(withText(content)))
    onView(withId(R.id.switchShowOrigMsg))
      .check(matches(isChecked()))
      .perform(scrollTo(), click())
    onView(withId(R.id.textViewOrigPgpMsg))
      .check(matches(not(isDisplayed())))
  }

  protected fun baseCheck(incomingMsgInfo: IncomingMessageInfo?) {
    assertThat(incomingMsgInfo, notNullValue())

    val details = incomingMsgInfo!!.msgEntity
    launchActivity(details)
    matchHeader(incomingMsgInfo)

    checkWebViewText(incomingMsgInfo.text)
    matchReplyButtons(details)
  }

  protected fun testTopReplyAction(title: String) {
    testStandardMsgPlaintextInternal()

    onView(withId(R.id.imageButtonMoreOptions))
      .check(matches(isDisplayed()))
      .perform(scrollTo(), click())

    onView(withText(title))
      .inRoot(RootMatchers.isPlatformPopup())
      .perform(click())

    Intents.intended(IntentMatchers.hasComponent(CreateMessageActivity::class.java.name))

    onView(withId(R.id.toolbar))
      .check(matches(CustomMatchers.withToolBarText(title)))
  }

  protected fun withHeaderInfo(header: MsgDetailsRecyclerViewAdapter.Header):
      Matcher<RecyclerView.ViewHolder> {
    return object : BoundedMatcher<RecyclerView.ViewHolder,
        MsgDetailsRecyclerViewAdapter.ViewHolder>(
      MsgDetailsRecyclerViewAdapter.ViewHolder::class.java
    ) {
      override fun matchesSafely(holder: MsgDetailsRecyclerViewAdapter.ViewHolder): Boolean {
        return holder.tVHeaderName.text.toString() == header.name
            && holder.tVHeaderValue.text.toString() == header.value
      }

      override fun describeTo(description: Description) {
        description.appendText("with: $header")
      }
    }
  }

  protected fun testPgpBadges(
    badgeCount: Int,
    vararg badgeTypes: PgpBadgeListAdapter.PgpBadge.Type
  ) {
    onView(withId(R.id.rVPgpBadges))
      .check(matches(CustomMatchers.withRecyclerViewItemCount(badgeCount)))


    for (badgeType in badgeTypes) {
      onView(withId(R.id.rVPgpBadges))
        .perform(
          RecyclerViewActions.scrollToHolder(
            CustomMatchers.withPgpBadge(
              PgpBadgeListAdapter.PgpBadge(
                badgeType
              )
            )
          )
        )
    }
  }
}
