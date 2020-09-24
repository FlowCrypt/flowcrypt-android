/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.text.format.Formatter
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.retrofit.response.model.node.DecryptErrorMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.PublicKeyMsgBlock
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.matchers.CustomMatchers
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withDrawable
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.lazyActivityScenarioRule
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.notNullValue
import org.junit.After
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * @author Denis Bondarenko
 * Date: 3/13/19
 * Time: 4:32 PM
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@Ignore("fix me")
class MessageDetailsActivityTest : BaseTest() {
  override val useIntents: Boolean = true
  override val activeActivityRule = lazyActivityScenarioRule<MessageDetailsActivity>(launchActivity = false)
  override val activityScenario: ActivityScenario<*>?
    get() = activeActivityRule.scenario

  private val accountRule = AddAccountToDatabaseRule()
  private var idlingForWebView: IdlingResource? = null

  private val simpleAttInfo = TestGeneralUtil.getObjectFromJson("messages/attachments/simple_att.json", AttachmentInfo::class.java)
  private val encryptedAttInfo = TestGeneralUtil.getObjectFromJson("messages/attachments/encrypted_att.json", AttachmentInfo::class.java)
  private val pubKeyAttInfo = TestGeneralUtil.getObjectFromJson("messages/attachments/pub_key.json", AttachmentInfo::class.java)

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(accountRule)
      .around(AddPrivateKeyToDatabaseRule())
      .around(activeActivityRule)

  private val localFolder: LocalFolder = LocalFolder(
      accountRule.account.email,
      fullName = "INBOX",
      folderAlias = "INBOX",
      msgCount = 1,
      attributes = listOf("\\HasNoChildren"))

  @After
  fun unregisterDecryptionIdling() {
    idlingForWebView?.let { IdlingRegistry.getInstance().unregister(it) }
  }

  @Test
  fun testReplyButton() {
    testStandardMsgPlaneText()
    onView(withId(R.id.layoutReplyButton))
        .check(matches(isDisplayed()))
        .perform(scrollTo(), click())
    intended(hasComponent(CreateMessageActivity::class.java.name))
  }

  @Test
  fun testTopReplyButton() {
    testTopReplyAction(getResString(R.string.reply))
  }

  @Test
  fun testReplyAllButton() {
    testStandardMsgPlaneText()
    onView(withId(R.id.layoutReplyAllButton))
        .check(matches(isDisplayed()))
        .perform(scrollTo(), click())
    intended(hasComponent(CreateMessageActivity::class.java.name))
  }

  @Test
  fun testFwdButton() {
    testStandardMsgPlaneText()
    onView(withId(R.id.layoutFwdButton))
        .check(matches(isDisplayed()))
        .perform(scrollTo(), click())
    intended(hasComponent(CreateMessageActivity::class.java.name))
  }

  @Test
  fun testTopForwardButton() {
    testTopReplyAction(getResString(R.string.forward))
  }

  @Test
  fun testStandardMsgPlaneText() {
    baseCheck(getMsgInfo("messages/info/standard_msg_info_plain_text.json",
        "messages/mime/standard_msg_info_plain_text.txt"))
  }

  @Test
  fun testStandardMsgPlaneTextWithOneAttachment() {
    baseCheckWithAtt(getMsgInfo("messages/info/standard_msg_info_plain_text_with_one_att.json",
        "messages/mime/standard_msg_info_plain_text_with_one_att.txt", simpleAttInfo), simpleAttInfo)
  }

  @Test
  fun testEncryptedMsgPlaneText() {
    baseCheck(getMsgInfo("messages/info/encrypted_msg_info_plain_text.json",
        "messages/mime/encrypted_msg_info_plain_text.txt"))
  }

  @Test
  fun testEncryptedBigInlineAtt() {
    IdlingPolicies.setIdlingResourceTimeout(3, TimeUnit.MINUTES)
    baseCheck(getMsgInfo("messages/info/encrypted_msg_big_inline_att.json",
        "messages/mime/encrypted_msg_big_inline_att.txt"))
  }

  @Test
  fun testMissingKeyErrorImportKey() {
    testMissingKey(getMsgInfo("messages/info/encrypted_msg_info_plain_text_with_missing_key.json",
        "messages/mime/encrypted_msg_info_plain_text_with_missing_key.txt"))

    intending(hasComponent(ComponentName(getTargetContext(), ImportPrivateKeyActivity::class.java)))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

    PrivateKeysManager.saveKeyFromAssetsToDatabase("node/default@denbond7.com_secondKey_prv_strong.json",
        TestConstants.DEFAULT_STRONG_PASSWORD, KeyDetails.Type.EMAIL)

    onView(withId(R.id.buttonImportPrivateKey))
        .check(matches(isDisplayed()))
        .perform(scrollTo(), click())

    val incomingMsgInfoFixed =
        TestGeneralUtil.getObjectFromJson("messages/info/encrypted_msg_info_plain_text_with_missing_key_fixed.json",
            IncomingMessageInfo::class.java)

    onWebView(withId(R.id.emailWebView)).forceJavascriptEnabled()
    onWebView(withId(R.id.emailWebView))
        .withElement(findElement(Locator.XPATH, "/html/body"))
        .check(webMatches(getText(), equalTo(incomingMsgInfoFixed?.text)))

    PrivateKeysManager.deleteKey("node/default@denbond7.com_secondKey_prv_strong.json")
  }

  @Test
  fun testMissingPubKey() {
    testMissingKey(getMsgInfo("messages/info/encrypted_msg_info_plain_text_error_one_pub_key.json",
        "messages/mime/encrypted_msg_info_plain_text_error_one_pub_key.txt"))
  }

  @Test
  fun testBadlyFormattedMsg() {
    val msgInfo = getMsgInfo("messages/info/encrypted_msg_info_plain_text_error_badly_formatted.json",
        "messages/mime/encrypted_msg_info_plain_text_error_badly_formatted.txt")
        ?: throw NullPointerException()

    assertThat(msgInfo, notNullValue())

    val details = msgInfo.msgEntity

    launchActivity(details)
    matchHeader(details)

    val block = msgInfo.msgBlocks?.get(1) as DecryptErrorMsgBlock
    val decryptError = block.error
    val formatErrorMsg = (getResString(R.string.decrypt_error_message_badly_formatted,
        getResString(R.string.app_name)) + "\n\n" + decryptError?.details?.type + ": " + decryptError?.details?.message)

    onView(withId(R.id.textViewErrorMessage))
        .check(matches(withText(formatErrorMsg)))

    testSwitch(block.content ?: "")
    matchReplyButtons(details)
  }

  @Test
  fun testMissingKeyErrorChooseSinglePubKey() {
    val msgInfo = getMsgInfo("messages/info/encrypted_msg_info_plain_text_with_missing_key.json",
        "messages/mime/encrypted_msg_info_plain_text_with_missing_key.txt")

    testMissingKey(msgInfo)

    onView(withId(R.id.buttonSendOwnPublicKey))
        .check(matches(isDisplayed()))
        .perform(scrollTo(), click())
    onView(withId(R.id.textViewMessage)).check(
        matches(withText(getTargetContext().resources.getQuantityString(R.plurals
            .tell_sender_to_update_their_settings, 1))))
    onView(withId(R.id.buttonOk))
        .check(matches(isDisplayed()))
        .perform(click())
    intended(hasComponent(CreateMessageActivity::class.java.name))
  }

  @Test

  fun testMissingKeyErrorChooseFromFewPubKeys() {
    val msgInfo = getMsgInfo("messages/info/encrypted_msg_info_plain_text_with_missing_key.json",
        "messages/mime/encrypted_msg_info_plain_text_with_missing_key.txt")

    testMissingKey(msgInfo)

    PrivateKeysManager.saveKeyFromAssetsToDatabase("node/default@denbond7.com_secondKey_prv_strong.json",
        TestConstants.DEFAULT_STRONG_PASSWORD, KeyDetails.Type.EMAIL)
    onView(withId(R.id.buttonSendOwnPublicKey))
        .check(matches(isDisplayed()))
        .perform(scrollTo(), click())

    val msg = getTargetContext().resources.getQuantityString(R.plurals
        .tell_sender_to_update_their_settings, 2)

    onView(withId(R.id.textViewMessage))
        .check(matches(withText(msg)))
    onData(anything())
        .inAdapterView(withId(R.id.listViewKeys))
        .atPosition(1)
        .perform(click())
    onView(withId(R.id.buttonOk))
        .check(matches(isDisplayed()))
        .perform(click())
    intended(hasComponent(CreateMessageActivity::class.java.name))
  }

  @Test
  fun testEncryptedMsgPlaneTextWithOneAttachment() {
    val msgInfo = getMsgInfo("messages/info/encrypted_msg_info_plain_text_with_one_att.json",
        "messages/mime/encrypted_msg_info_plain_text_with_one_att.txt", encryptedAttInfo)
    baseCheckWithAtt(msgInfo, encryptedAttInfo)
  }

  @Test
  fun testEncryptedMsgPlaneTextWithPubKey() {
    val msgInfo = getMsgInfo("messages/info/encrypted_msg_info_plain_text_with_pub_key.json",
        "messages/mime/encrypted_msg_info_plain_text_with_pub_key.txt", pubKeyAttInfo)
    baseCheckWithAtt(msgInfo, pubKeyAttInfo)

    val nodeKeyDetails = PrivateKeysManager.getNodeKeyDetailsFromAssets("node/denbond7@denbond7.com_pub.json")
    val pgpContact = nodeKeyDetails.primaryPgpContact

    onView(withId(R.id.textViewKeyOwnerTemplate)).check(matches(withText(
        getResString(R.string.template_message_part_public_key_owner, pgpContact.email))))

    onView(withId(R.id.textViewKeyWordsTemplate)).check(matches(withText(
        getHtmlString(getResString(R.string.template_message_part_public_key_key_words,
            nodeKeyDetails.keywords ?: "")))))

    onView(withId(R.id.textViewFingerprintTemplate)).check(matches(withText(
        getHtmlString(getResString(R.string.template_message_part_public_key_fingerprint,
            GeneralUtil.doSectionsInText(" ", nodeKeyDetails.fingerprint, 4)!!)))))

    val block = msgInfo?.msgBlocks?.get(1) as PublicKeyMsgBlock

    onView(withId(R.id.textViewPgpPublicKey))
        .check(matches(not(isDisplayed())))
    onView(withId(R.id.switchShowPublicKey))
        .check(matches(not(isChecked())))
        .perform(scrollTo(), click())
    onView(withId(R.id.textViewPgpPublicKey))
        .check(matches(isDisplayed()))
    onView(withId(R.id.textViewPgpPublicKey))
        .check(matches(withText(TestGeneralUtil.replaceVersionInKey(block.content))))
    onView(withId(R.id.switchShowPublicKey))
        .check(matches(isChecked()))
        .perform(scrollTo(), click())
    onView(withId(R.id.textViewPgpPublicKey))
        .check(matches(not(isDisplayed())))

    onView(withId(R.id.buttonKeyAction))
        .check(matches(isDisplayed()))
        .perform(scrollTo(), click())
    onView(withId(R.id.buttonKeyAction))
        .check(matches(not(isDisplayed())))
  }

  @Test
  fun test8bitEncodingUtf8() {
    baseCheck(getMsgInfo("messages/info/msg_info_8bit-utf8.json",
        "messages/mime/8bit-utf8.eml"))
  }

  private fun testMissingKey(incomingMsgInfo: IncomingMessageInfo?) {
    assertThat(incomingMsgInfo, notNullValue())

    val details = incomingMsgInfo!!.msgEntity

    launchActivity(details)
    matchHeader(details)

    val block = incomingMsgInfo.msgBlocks?.get(1) as DecryptErrorMsgBlock
    val errorMsg = getResString(R.string.decrypt_error_current_key_cannot_open_message)

    onView(withId(R.id.textViewErrorMessage))
        .check(matches(withText(errorMsg)))

    testSwitch(block.content ?: "")
    matchReplyButtons(details)
  }

  private fun testSwitch(content: String) {
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

  private fun baseCheck(incomingMsgInfo: IncomingMessageInfo?) {
    assertThat(incomingMsgInfo, notNullValue())

    val details = incomingMsgInfo!!.msgEntity
    launchActivity(details)
    matchHeader(details)

    onWebView(withId(R.id.emailWebView)).forceJavascriptEnabled()
    onWebView(withId(R.id.emailWebView))
        .withElement(findElement(Locator.XPATH, "/html/body"))
        .check(webMatches(getText(), equalTo(incomingMsgInfo.text)))
    matchReplyButtons(details)
  }

  private fun baseCheckWithAtt(incomingMsgInfo: IncomingMessageInfo?, att: AttachmentInfo?) {
    assertThat(incomingMsgInfo, notNullValue())

    val msgEntity = incomingMsgInfo!!.msgEntity
    launchActivity(msgEntity)
    matchHeader(msgEntity)

    onWebView(withId(R.id.emailWebView)).forceJavascriptEnabled()
    onWebView(withId(R.id.emailWebView))
        .withElement(findElement(Locator.XPATH, "/html/body"))
        .check(webMatches(getText(), equalTo(incomingMsgInfo.text)))
    onView(withId(R.id.layoutAtt))
        .check(matches(isDisplayed()))
    matchAtt(att)
    matchReplyButtons(msgEntity)
  }

  private fun matchHeader(msgEntity: MessageEntity) {
    onView(withId(R.id.textViewSenderAddress))
        .check(matches(withText(EmailUtil.getFirstAddressString(msgEntity.from))))
    onView(withId(R.id.textViewDate))
        .check(matches(withText(DateTimeUtil.formatSameDayTime(getTargetContext(), msgEntity.receivedDate))))
    onView(withId(R.id.textViewSubject))
        .check(matches(withText(msgEntity.subject)))
  }

  private fun matchAtt(att: AttachmentInfo?) {
    requireNotNull(att)
    onView(withId(R.id.textViewAttachmentName))
        .check(matches(withText(att.name)))
    onView(withId(R.id.textViewAttSize))
        .check(matches(withText(Formatter.formatFileSize(getContext(), att.encodedSize))))
  }

  private fun matchReplyButtons(msgEntity: MessageEntity) {
    onView(withId(R.id.imageButtonReplyAll))
        .check(matches(isDisplayed()))
    onView(withId(R.id.layoutReplyButton))
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

  private fun launchActivity(msgEntity: MessageEntity) {
    activeActivityRule.launch(MessageDetailsActivity.getIntent(getTargetContext(), localFolder, msgEntity))
    registerAllIdlingResources()

    activityScenario?.onActivity { activity ->
      val messageDetailsActivity = (activity as? MessageDetailsActivity) ?: return@onActivity
      idlingForWebView = messageDetailsActivity.idlingForWebView
      idlingForWebView?.let { IdlingRegistry.getInstance().register(it) }
    }
  }

  private fun testTopReplyAction(title: String) {
    testStandardMsgPlaneText()

    onView(withId(R.id.imageButtonMoreOptions))
        .check(matches(isDisplayed()))
        .perform(scrollTo(), click())

    onView(withText(title))
        .inRoot(RootMatchers.isPlatformPopup())
        .perform(click())

    intended(hasComponent(CreateMessageActivity::class.java.name))

    onView(withId(R.id.toolbar))
        .check(matches(CustomMatchers.withToolBarText(title)))
  }
}
