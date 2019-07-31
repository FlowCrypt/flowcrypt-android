/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.text.format.Formatter
import android.view.View
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsTestRule
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
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.flowcrypt.email.DoesNotNeedMailserver
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.GeneralMessageDetails
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.retrofit.response.model.node.DecryptErrorMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.PublicKeyMsgBlock
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource
import com.flowcrypt.email.matchers.CustomMatchers.Companion.isToastDisplayed
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withDrawable
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddAttachmentToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.ui.activity.base.BaseActivity
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.notNullValue
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 3/13/19
 * Time: 4:32 PM
 * E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MessageDetailsActivityTest : BaseTest() {
  override val activityTestRule: ActivityTestRule<*>? = IntentsTestRule(MessageDetailsActivity::class.java,
      false, false)
  private val simpleAttachmentRule =
      AddAttachmentToDatabaseRule(TestGeneralUtil.getObjectFromJson("messages/attachments/simple_att.json",
          AttachmentInfo::class.java)!!)

  private val encryptedAttachmentRule =
      AddAttachmentToDatabaseRule(TestGeneralUtil.getObjectFromJson("messages/attachments/encrypted_att.json",
          AttachmentInfo::class.java)!!)

  private val pubKeyAttachmentRule =
      AddAttachmentToDatabaseRule(TestGeneralUtil.getObjectFromJson("messages/attachments/pub_key.json",
          AttachmentInfo::class.java)!!)

  private val msgDaoSource = MessageDaoSource()

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(AddAccountToDatabaseRule())
      .around(AddPrivateKeyToDatabaseRule())
      .around(simpleAttachmentRule)
      .around(encryptedAttachmentRule)
      .around(pubKeyAttachmentRule)
      .around(activityTestRule)

  private val localFolder: LocalFolder = LocalFolder(
      fullName = "INBOX",
      folderAlias = "INBOX",
      msgCount = 1,
      attributes = listOf("\\HasNoChildren"))

  @After
  fun unregisterDecryptionIdling() {
    val activity = activityTestRule?.activity ?: return
    if (activity is MessageDetailsActivity) {
      IdlingRegistry.getInstance().unregister(activity.idlingForDecryption)
    }
  }

  @Test
  @DoesNotNeedMailserver
  fun testReplyButton() {
    testStandardMsgPlaneText()
    onView(withId(R.id.layoutReplyButton))
        .check(matches(isDisplayed()))
        .perform(scrollTo(), click())
    intended(hasComponent(CreateMessageActivity::class.java.name))
  }

  @Test
  @DoesNotNeedMailserver
  fun testReplyAllButton() {
    testStandardMsgPlaneText()
    onView(withId(R.id.layoutReplyAllButton))
        .check(matches(isDisplayed()))
        .perform(scrollTo(), click())
    intended(hasComponent(CreateMessageActivity::class.java.name))
  }

  @Test
  @DoesNotNeedMailserver
  fun testFwdButton() {
    testStandardMsgPlaneText()
    onView(withId(R.id.layoutFwdButton))
        .check(matches(isDisplayed()))
        .perform(scrollTo(), click())
    intended(hasComponent(CreateMessageActivity::class.java.name))
  }

  @Test
  @DoesNotNeedMailserver
  fun testStandardMsgPlaneText() {
    baseCheck(getMsgInfo("messages/info/standard_msg_info_plain_text.json",
        "messages/mime/standard_msg_info_plain_text.txt"))
  }

  @Test
  @DoesNotNeedMailserver
  fun testStandardMsgPlaneTextWithOneAttachment() {
    baseCheckWithAtt(getMsgInfo("messages/info/standard_msg_info_plain_text_with_one_att.json",
        "messages/mime/standard_msg_info_plain_text_with_one_att.txt"), simpleAttachmentRule)
  }

  @Test
  @DoesNotNeedMailserver
  fun testEncryptedMsgPlaneText() {
    baseCheck(getMsgInfo("messages/info/encrypted_msg_info_plain_text.json",
        "messages/mime/encrypted_msg_info_plain_text.txt"))
  }

  @Test
  @DoesNotNeedMailserver
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
  @DoesNotNeedMailserver
  fun testMissingPubKey() {
    testMissingKey(getMsgInfo("messages/info/encrypted_msg_info_plain_text_error_one_pub_key.json",
        "messages/mime/encrypted_msg_info_plain_text_error_one_pub_key.txt"))
  }

  @Test
  @DoesNotNeedMailserver
  fun testBadlyFormattedMsg() {
    val msgInfo = getMsgInfo("messages/info/encrypted_msg_info_plain_text_error_badly_formatted.json",
        "messages/mime/encrypted_msg_info_plain_text_error_badly_formatted.txt")

    assertThat(msgInfo, notNullValue())

    val details = msgInfo!!.generalMsgDetails

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
  @DoesNotNeedMailserver
  fun testMissingKeyErrorChooseSinglePubKey() {
    val msgInfo = getMsgInfo("messages/info/encrypted_msg_info_plain_text_with_missing_key.json",
        "messages/mime/encrypted_msg_info_plain_text_with_missing_key.txt")

    testMissingKey(msgInfo)

    onView(withId(R.id.buttonSendOwnPublicKey))
        .check(matches(isDisplayed()))
        .perform(scrollTo(), click())
    onView(withId(R.id.textViewMessage)).check(
        matches(withText(getResString(R.string.tell_sender_to_update_their_settings))))
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

    val msg = (getResString(R.string.tell_sender_to_update_their_settings) + "\n\n" + getResString(R.string.select_key))

    onView(withId(R.id.textViewMessage))
        .check(matches(withText(msg)))
    onView(withId(R.id.buttonOk))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(withText(getResString(R.string.please_select_key)))
        .inRoot(isToastDisplayed())
        .check(matches(isDisplayed()))
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
  @DoesNotNeedMailserver
  fun testEncryptedMsgPlaneTextWithOneAttachment() {
    val msgInfo = getMsgInfo("messages/info/encrypted_msg_info_plain_text_with_one_att.json",
        "messages/mime/encrypted_msg_info_plain_text_with_one_att.txt")
    baseCheckWithAtt(msgInfo, encryptedAttachmentRule)
  }

  @Test
  @DoesNotNeedMailserver
  fun testEncryptedMsgPlaneTextWithPubKey() {
    val msgInfo = getMsgInfo("messages/info/encrypted_msg_info_plain_text_with_pub_key.json",
        "messages/mime/encrypted_msg_info_plain_text_with_pub_key.txt")
    baseCheckWithAtt(msgInfo, pubKeyAttachmentRule)

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
        .check(matches(not<View>(isDisplayed())))
    onView(withId(R.id.switchShowPublicKey))
        .check(matches(not<View>(isChecked())))
        .perform(scrollTo(), click())
    onView(withId(R.id.textViewPgpPublicKey))
        .check(matches(isDisplayed()))
    onView(withId(R.id.textViewPgpPublicKey))
        .check(matches(withText(TestGeneralUtil.replaceVersionInKey(block.content))))
    onView(withId(R.id.switchShowPublicKey))
        .check(matches(isChecked()))
        .perform(scrollTo(), click())
    onView(withId(R.id.textViewPgpPublicKey))
        .check(matches(not<View>(isDisplayed())))

    onView(withId(R.id.buttonKeyAction))
        .check(matches(isDisplayed()))
        .perform(scrollTo(), click())
    onView(withId(R.id.buttonKeyAction))
        .check(matches(not<View>(isDisplayed())))
  }

  private fun testMissingKey(incomingMsgInfo: IncomingMessageInfo?) {
    assertThat(incomingMsgInfo, notNullValue())

    val details = incomingMsgInfo!!.generalMsgDetails

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
        .check(matches(not<View>(isDisplayed())))
    onView(withId(R.id.switchShowOrigMsg))
        .check(matches(not<View>(isChecked())))
        .perform(scrollTo(), click())
    onView(withId(R.id.textViewOrigPgpMsg))
        .check(matches(isDisplayed()))
    onView(withId(R.id.textViewOrigPgpMsg))
        .check(matches(withText(content)))
    onView(withId(R.id.switchShowOrigMsg))
        .check(matches(isChecked()))
        .perform(scrollTo(), click())
    onView(withId(R.id.textViewOrigPgpMsg))
        .check(matches(not<View>(isDisplayed())))
  }

  private fun baseCheck(incomingMsgInfo: IncomingMessageInfo?) {
    assertThat(incomingMsgInfo, notNullValue())

    val details = incomingMsgInfo!!.generalMsgDetails
    launchActivity(details)
    matchHeader(details)

    onWebView(withId(R.id.emailWebView)).forceJavascriptEnabled()
    onWebView(withId(R.id.emailWebView))
        .withElement(findElement(Locator.XPATH, "/html/body"))
        .check(webMatches(getText(), equalTo(incomingMsgInfo.text)))
    matchReplyButtons(details)
  }

  private fun baseCheckWithAtt(incomingMsgInfo: IncomingMessageInfo?, rule: AddAttachmentToDatabaseRule) {
    assertThat(incomingMsgInfo, notNullValue())

    val details = incomingMsgInfo!!.generalMsgDetails
    launchActivity(details)
    matchHeader(details)

    onWebView(withId(R.id.emailWebView)).forceJavascriptEnabled()
    onWebView(withId(R.id.emailWebView))
        .withElement(findElement(Locator.XPATH, "/html/body"))
        .check(webMatches(getText(), equalTo(incomingMsgInfo.text)))
    onView(withId(R.id.layoutAtt))
        .check(matches(isDisplayed()))
    matchAtt(rule.attInfo)
    matchReplyButtons(details)
  }

  private fun matchHeader(details: GeneralMessageDetails) {
    onView(withId(R.id.textViewSenderAddress))
        .check(matches(withText(EmailUtil.getFirstAddressString(details.from))))
    onView(withId(R.id.textViewDate))
        .check(matches(withText(DateTimeUtil.formatSameDayTime(getTargetContext(), details.receivedDate))))
    onView(withId(R.id.textViewSubject))
        .check(matches(withText(details.subject)))
  }

  private fun matchAtt(att: AttachmentInfo) {
    onView(withId(R.id.textViewAttchmentName))
        .check(matches(withText(att.name)))
    onView(withId(R.id.textViewAttSize))
        .check(matches(withText(Formatter.formatFileSize(getContext(), att.encodedSize))))
  }

  private fun matchReplyButtons(details: GeneralMessageDetails) {
    onView(withId(R.id.imageButtonReplyAll))
        .check(matches(isDisplayed()))
    onView(withId(R.id.layoutReplyButton))
        .check(matches(isDisplayed()))
    onView(withId(R.id.layoutReplyAllButton))
        .check(matches(isDisplayed()))
    onView(withId(R.id.layoutFwdButton))
        .check(matches(isDisplayed()))

    if (details.isEncrypted) {
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

  private fun launchActivity(details: GeneralMessageDetails) {
    activityTestRule?.launchActivity(MessageDetailsActivity.getIntent(getTargetContext(), localFolder, details))
    IdlingRegistry.getInstance().register((activityTestRule?.activity as BaseActivity).nodeIdlingResource)
    IdlingRegistry.getInstance().register((activityTestRule.activity as MessageDetailsActivity).idlingForDecryption)
  }

  private fun getMsgInfo(path: String, mimeMsgPath: String): IncomingMessageInfo? {
    val incomingMsgInfo = TestGeneralUtil.getObjectFromJson(path, IncomingMessageInfo::class.java)
    incomingMsgInfo?.generalMsgDetails?.let {
      msgDaoSource.addRow(getTargetContext(), it)
      msgDaoSource.updateRawMime(getTargetContext(), it.email, localFolder.folderAlias, it.uid.toLong(),
          TestGeneralUtil.readFileFromAssetsAsString(getContext(), mimeMsgPath).toByteArray())
    }
    return incomingMsgInfo
  }
}
