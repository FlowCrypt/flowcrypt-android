/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.content.ContentValues
import android.content.Intent
import android.text.TextUtils
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isFocusable
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.email.model.ServiceInfo
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.UpdateAccountRule
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.TestGeneralUtil
import com.hootsuite.nachos.tokenizer.SpanChipTokenizer
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.isEmptyString
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.*

/**
 * This class tests a case when we want to send a reply with [ServiceInfo]
 *
 * @author Denis Bondarenko
 * Date: 14.05.2018
 * Time: 16:34
 * E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class StandardReplyWithServiceInfoAndOneFileTest : BaseTest() {
  private lateinit var serviceInfo: ServiceInfo
  private lateinit var incomingMsgInfo: IncomingMessageInfo

  val addAccountToDatabaseRule: AddAccountToDatabaseRule = AddAccountToDatabaseRule()

  override val activityTestRule: ActivityTestRule<*>? =
      object : IntentsTestRule<CreateMessageActivity>(CreateMessageActivity::class.java) {
        override fun getActivityIntent(): Intent {
          incomingMsgInfo = TestGeneralUtil.getObjectFromJson("{\n  \"encryptionType\": \"ENCRYPTED\",\n  \"generalMsgDetails\": {\n    \"email\": \"default@denbond7.com\",\n    \"from\": [\n      {\n        \"address\": \"denbond7@denbond7.com\"\n      }\n    ],\n    \"hasAtts\": false,\n    \"isEncrypted\": true,\n    \"isRawMsgAvailable\": true,\n    \"label\": \"INBOX\",\n    \"msgFlags\": [\n      \"\\\\SEEN\"\n    ],\n    \"msgState\": \"NONE\",\n    \"receivedDate\": 1552661530000,\n    \"sentDate\": 1552661519000,\n    \"subject\": \"Simple encrypted message\",\n    \"to\": [\n      {\n        \"address\": \"default@denbond7.com\"\n      }\n    ],\n    \"uid\": 239\n  },\n  \"msgBlocks\": [\n    {\n      \"complete\": true,\n      \"content\": \"\\n  \\u003c!DOCTYPE html\\u003e\\u003chtml\\u003e\\n    \\u003chead\\u003e\\n      \\u003cmeta name\\u003d\\\"viewport\\\" content\\u003d\\\"width\\u003ddevice-width\\\" /\\u003e\\n      \\u003cstyle\\u003e\\n        body { word-wrap: break-word; word-break: break-word; hyphens: auto; margin-left: 0px; padding-left: 0px; }\\n        body img { display: inline !important; height: auto !important; max-width: 95% !important; }\\n        body pre { white-space: pre-wrap !important; }\\n        body \\u003e div.MsgBlock \\u003e table { zoom: 75% } /* table layouts tend to overflow - eg emails from fb */\\n      \\u003c/style\\u003e\\n    \\u003c/head\\u003e\\n    \\u003cbody\\u003e\\u003cdiv class\\u003d\\\"MsgBlock green\\\" style\\u003d\\\"background: white;padding-left: 8px;min-height: 50px;padding-top: 4px;padding-bottom: 4px;width: 100%;border: 1px solid #f0f0f0;border-left: 8px solid #31A217;border-right: none;background-image: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFoAAABaCAMAAAAPdrEwAAAAh1BMVEXw8PD////w8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PDw8PD7MuHIAAAALXRSTlMAAAECBAcICw4QEhUZIyYqMTtGTV5kdn2Ii5mfoKOqrbG0uL6/xcnM0NTX2t1l7cN4AAAB0UlEQVR4Ae3Y3Y4SQRCG4bdHweFHRBTBH1FRFLXv//qsA8kmvbMdXhh2Q0KfknpSCQc130c67s22+e9+v/+d84fxkSPH0m/+5P9vN7vRV0vPfx7or1NB23e99KAHuoXOOc6moQsBwNN1Q9g4Wdh1uq3MA7Qn0+2ylAt7WbWpyT+Wo8roKH6v2QhZ2ghZ2ghZ2ghZ2ghZ2ghZ2ghZ2ghZ2ghZ2ghZ2ghZ2ghZ2ghZ2gjZ2AUNOLmwgQdogEJ2dnF3UJdU3WjqO/u96aYtVd/7jqvIyu76G5se6GaY7tNNcy5d7se7eWVnDz87fMkuVuS8epF6f9NPObPY5re9y4N1/vya9Gr3se2bfvl9M0mkyZdv077p+a/3z4Meby5Br4NWiV51BaiUqfLro9I3WiR61RVcffwfXI7u5zZ20EOA82Uu8x3SlrSwXQuBSvSqK0AletUVoBK96gpIwlZy0MJWctDCVnLQwlZy0MJWctDCVnLQwlZy0MJWctDCVnLQwlZy0MJWctDCVnLQwlZy0MJWckIletUVIJJxITN6wtZd2EI+0NquyIJOnUpFVvRpcwmV6FVXgEr0qitAJXrVFaASveoKUIledQWoRK+6AlSiV13BP+/VVbky7Xq1AAAAAElFTkSuQmCC);\\\"\\u003eSimple encrypted text\\u003c/div\\u003e\\u003c!-- next MsgBlock --\\u003e\\n\\u003c/body\\u003e\\n  \\u003c/html\\u003e\",\n      \"type\": \"plainHtml\"\n    }\n  ],\n  \"origMsgHeaders\": \"Return-Path: \\u003cdenbond7@denbond7.com\\u003e\\r\\nDelivered-To: default@denbond7.com\\r\\nReceived: from mail.denbond7.com (localhost [127.0.0.1])\\r\\n\\tby mail.denbond7.com (Postfix) with ESMTP id 35CBC202F1\\r\\n\\tfor \\u003cdefault@denbond7.com\\u003e; Fri, 15 Mar 2019 14:52:10 +0000 (UTC)\\r\\nX-Virus-Scanned: Debian amavisd-new at mail.denbond7.com\\r\\nReceived: from mail.denbond7.com ([127.0.0.1])\\r\\n\\tby mail.denbond7.com (mail.denbond7.com [127.0.0.1]) (amavisd-new, port 10024)\\r\\n\\twith ESMTP id EstQPjUmZ4Hn for \\u003cdefault@denbond7.com\\u003e;\\r\\n\\tFri, 15 Mar 2019 14:52:01 +0000 (UTC)\\r\\nReceived: from localhost (MiA1 [192.168.3.6])\\r\\n\\tby mail.denbond7.com (Postfix) with ESMTP id E17EF202E9\\r\\n\\tfor \\u003cdefault@denbond7.com\\u003e; Fri, 15 Mar 2019 14:52:00 +0000 (UTC)\\r\\nContent-Type: multipart/mixed;\\r\\n boundary\\u003d\\\"----sinikael-?\\u003d_1-15526615192100.5959024994440685\\\"\\r\\nIn-Reply-To: \\u003c\\u003e\\r\\nReferences: \\u003c\\u003e\\r\\nTo: default@denbond7.com\\r\\nFrom: denbond7@denbond7.com\\r\\nSubject: Simple encrypted message\\r\\nDate: Fri, 15 Mar 2019 14:51:59 +0000\\r\\nMessage-Id: \\u003c1552661519275-40db11d3-834101fe-9096ab5d@denbond7.com\\u003e\\r\\nMIME-Version: 1.0\",\n  \"text\": \"Simple encrypted text\"\n}",
              IncomingMessageInfo::class.java)!!

          val attachmentInfo = AttachmentInfo(name = "test.txt",
              email = addAccountToDatabaseRule.account.email,
              encodedSize = STRING.length.toLong(),
              rawData = STRING,
              type = JavaEmailConstants.MIME_TYPE_TEXT_PLAIN,
              folder = "SENT",
              id = EmailUtil.generateContentId(),
              isProtected = true)

          val attachmentInfoList = ArrayList<AttachmentInfo>()
          attachmentInfoList.add(attachmentInfo)

          serviceInfo = ServiceInfo(false,
              false,
              false,
              false,
              false,
              false,
              getResString(R.string.message_was_encrypted_for_wrong_key),
              attachmentInfoList)

          return CreateMessageActivity.generateIntent(getTargetContext(), incomingMsgInfo, MessageType.REPLY,
              MessageEncryptionType.STANDARD, serviceInfo)
        }
      }

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(AddAccountToDatabaseRule())
      .around(UpdateAccountRule(AccountDaoManager.getDefaultAccountDao(), generateContentValues()))
      .around(activityTestRule)

  @Test
  fun testFrom() {
    onView(withId(R.id.editTextFrom))
        .perform(scrollTo())
        .check(matches(allOf<View>(isDisplayed(),
            if (serviceInfo.isFromFieldEditable) isFocusable() else not<View>(isFocusable()))))
  }

  @Test
  fun testToRecipients() {
    val chipSeparator = Character.toString(SpanChipTokenizer.CHIP_SPAN_SEPARATOR)
    val autoCorrectSeparator = Character.toString(SpanChipTokenizer.AUTOCORRECT_SEPARATOR)
    val textWithSeparator = (autoCorrectSeparator
        + chipSeparator
        + incomingMsgInfo.getFrom()?.first()?.address
        + chipSeparator
        + autoCorrectSeparator)

    onView(withId(R.id.editTextRecipientTo))
        .perform(scrollTo())
        .check(matches(allOf<View>(isDisplayed(), withText(textWithSeparator),
            if (serviceInfo.isToFieldEditable) isFocusable() else not<View>(isFocusable()))))
  }

  @Test
  fun testSubject() {
    onView(withId(R.id.editTextEmailSubject))
        .check(matches(allOf<View>(isDisplayed(),
            if (serviceInfo.isSubjectEditable) isFocusable() else not<View>(isFocusable()))))
  }

  @Test
  fun testEmailMsg() {
    onView(withId(R.id.editTextEmailMessage))
        .check(matches(allOf<View>(isDisplayed(),
            if (TextUtils.isEmpty(serviceInfo.systemMsg)) withText(isEmptyString())
            else withText(serviceInfo.systemMsg),
            if (serviceInfo.isMsgEditable) isFocusable() else not<View>(isFocusable()))))

    if (serviceInfo.isMsgEditable) {
      onView(withId(R.id.editTextEmailMessage))
          .perform(replaceText(STRING))
    }
  }

  @Test
  fun testAvailabilityAddingAtts() {
    if (!serviceInfo.hasAbilityToAddNewAtt()) {
      onView(withId(R.id.menuActionAttachFile))
          .check(doesNotExist())
    }
  }

  @Test
  fun testDisabledSwitchingBetweenEncryptionTypes() {
    if (!serviceInfo.isMsgTypeSwitchable) {
      onView(withText(R.string.switch_to_standard_email))
          .check(doesNotExist())
      onView(withText(R.string.switch_to_secure_email))
          .check(doesNotExist())
    }
  }

  @Test
  fun testShowHelpScreen() {
    testHelpScreen()
  }

  private fun generateContentValues(): ContentValues {
    val contentValues = ContentValues()
    contentValues.put(AccountDaoSource.COL_IS_CONTACTS_LOADED, true)
    return contentValues
  }

  companion object {
    private const val STRING = "Some short string"
  }
}
