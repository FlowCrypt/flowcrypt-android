/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.content.Intent
import android.os.Parcelable
import android.text.TextUtils
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isFocusable
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.email.model.ServiceInfo
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
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

/**
 * This class tests a case when we want to send a reply with [ServiceInfo]
 *
 * @author Denis Bondarenko
 * Date: 14.05.2018
 * Time: 16:34
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class StandardReplyWithServiceInfoAndOneFileTest : BaseTest() {
  private val addAccountToDatabaseRule: AddAccountToDatabaseRule = AddAccountToDatabaseRule(AccountDaoManager.getDefaultAccountDao().copy(areContactsLoaded = true))
  private val attachmentInfo = AttachmentInfo(name = "test.txt",
      email = addAccountToDatabaseRule.account.email,
      encodedSize = STRING.length.toLong(),
      rawData = STRING,
      type = JavaEmailConstants.MIME_TYPE_TEXT_PLAIN,
      folder = "SENT",
      id = EmailUtil.generateContentId(),
      isProtected = true)

  private val incomingMsgInfo = TestGeneralUtil.getObjectFromJson("messages/info/encrypted_msg_info_plain_text.json", IncomingMessageInfo::class.java)!!
  private val serviceInfo = ServiceInfo(isToFieldEditable = false,
      isFromFieldEditable = false,
      isMsgEditable = false,
      isSubjectEditable = false,
      isMsgTypeSwitchable = false,
      hasAbilityToAddNewAtt = false,
      systemMsg = getResString(R.string.message_was_encrypted_for_wrong_key),
      atts = listOf(attachmentInfo))

  override val useIntents: Boolean = true
  override val activityScenarioRule = activityScenarioRule<SignInActivity>(
      intent = Intent(getTargetContext(), CreateMessageActivity::class.java).apply {
        putExtra(CreateMessageActivity.EXTRA_KEY_INCOMING_MESSAGE_INFO, incomingMsgInfo)
        putExtra(CreateMessageActivity.EXTRA_KEY_MESSAGE_TYPE, MessageType.REPLY as Parcelable)
        putExtra(CreateMessageActivity.EXTRA_KEY_MESSAGE_ENCRYPTION_TYPE, MessageEncryptionType.STANDARD as Parcelable)
        putExtra(CreateMessageActivity.EXTRA_KEY_SERVICE_INFO, serviceInfo)
      })

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(addAccountToDatabaseRule)
      .around(RetryRule.DEFAULT)
      .around(activityScenarioRule)
      .around(ScreenshotTestRule())

  @Test
  fun testFrom() {
    onView(withId(R.id.editTextFrom))
        .perform(scrollTo())
        .check(matches(allOf(isDisplayed(),
            if (serviceInfo.isFromFieldEditable) isFocusable() else not(isFocusable()))))
  }

  @Test
  fun testToRecipients() {
    val chipSeparator = SpanChipTokenizer.CHIP_SPAN_SEPARATOR.toString()
    val autoCorrectSeparator = SpanChipTokenizer.AUTOCORRECT_SEPARATOR.toString()
    val textWithSeparator = (autoCorrectSeparator
        + chipSeparator
        + incomingMsgInfo.getFrom().first().address
        + chipSeparator
        + autoCorrectSeparator)

    onView(withId(R.id.editTextRecipientTo))
        .perform(scrollTo())
        .check(matches(allOf(isDisplayed(), withText(textWithSeparator),
            if (serviceInfo.isToFieldEditable) isFocusable() else not(isFocusable()))))
  }

  @Test
  fun testSubject() {
    onView(withId(R.id.editTextEmailSubject))
        .perform(scrollTo())
        .check(matches(allOf(isDisplayed(),
            if (serviceInfo.isSubjectEditable) isFocusable() else not(isFocusable()))))
  }

  @Test
  fun testEmailMsg() {
    onView(withId(R.id.editTextEmailMessage))
        .perform(scrollTo())
        .check(matches(allOf(isDisplayed(),
            if (TextUtils.isEmpty(serviceInfo.systemMsg)) withText(isEmptyString())
            else withText(serviceInfo.systemMsg),
            if (serviceInfo.isMsgEditable) isFocusable() else not(isFocusable()))))

    if (serviceInfo.isMsgEditable) {
      onView(withId(R.id.editTextEmailMessage))
          .perform(scrollTo(), replaceText(STRING))
    }
  }

  @Test
  fun testAvailabilityAddingAtts() {
    if (!serviceInfo.hasAbilityToAddNewAtt) {
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

  companion object {
    private const val STRING = "Some short string"
  }
}
