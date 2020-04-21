/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
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
          incomingMsgInfo = TestGeneralUtil.getObjectFromJson("messages/info/encrypted_msg_info_plain_text.json",
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

  private fun generateContentValues(): ContentValues {
    val contentValues = ContentValues()
    contentValues.put(AccountDaoSource.COL_IS_CONTACTS_LOADED, true)
    return contentValues
  }

  companion object {
    private const val STRING = "Some short string"
  }
}
