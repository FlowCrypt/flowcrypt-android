/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import android.text.TextUtils
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
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
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.junit.annotations.NotReadyForCI
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withChipCloseIconAvailability
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.CreateMessageActivity
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.TestGeneralUtil
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.emptyString
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * This class tests a case when we want to send a reply with [ServiceInfo]
 *
 * @author Denys Bondarenko
 */
@FlowCryptTestSettings(useIntents = true)
@MediumTest
@RunWith(AndroidJUnit4::class)
class StandardReplyWithServiceInfoAndOneFileFlowTest : BaseTest() {
  private val addAccountToDatabaseRule: AddAccountToDatabaseRule = AddAccountToDatabaseRule(
    AccountDaoManager.getDefaultAccountDao().copy(contactsLoaded = true)
  )
  private val attachmentInfo = AttachmentInfo(
    name = "test.txt",
    email = addAccountToDatabaseRule.account.email,
    encodedSize = STRING.length.toLong(),
    rawData = STRING.toByteArray(),
    type = JavaEmailConstants.MIME_TYPE_TEXT_PLAIN,
    folder = "SENT",
    id = EmailUtil.generateContentId(),
    isProtected = true
  )

  private val incomingMsgInfo = TestGeneralUtil.getObjectFromJson(
    "messages/info/encrypted_msg_info_text.json",
    IncomingMessageInfo::class.java
  )!!
  private val serviceInfo = ServiceInfo(
    isToFieldEditable = false,
    isFromFieldEditable = false,
    isMsgEditable = false,
    isSubjectEditable = false,
    isMsgTypeSwitchable = false,
    hasAbilityToAddNewAtt = false,
    systemMsg = getResString(R.string.message_was_encrypted_for_wrong_key),
    atts = listOf(attachmentInfo)
  )

  override val activityScenarioRule = activityScenarioRule<CreateMessageActivity>(
    intent = CreateMessageActivity.generateIntent(
      getTargetContext(),
      msgInfo = incomingMsgInfo,
      messageType = MessageType.REPLY,
      msgEncryptionType = MessageEncryptionType.STANDARD,
      serviceInfo = serviceInfo
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testFrom() {
    onView(withId(R.id.editTextFrom))
      .perform(scrollTo())
      .check(
        matches(
          allOf(
            isDisplayed(),
            if (serviceInfo.isFromFieldEditable) isFocusable() else not(isFocusable())
          )
        )
      )
  }

  @Test
  fun testToRecipients() {
    onView(withId(R.id.recyclerViewChipsTo))
      .perform(
        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
          allOf(
            withText(incomingMsgInfo.getFrom().first().address),
            withChipCloseIconAvailability(false)
          )
        )
      )

    onView(withId(R.id.recyclerViewChipsTo))
      .check(matches(isDisplayed()))
      .check(matches(withRecyclerViewItemCount(incomingMsgInfo.getFrom().size)))
  }

  @Test
  fun testSubject() {
    onView(withId(R.id.editTextEmailSubject))
      .perform(scrollTo())
      .check(
        matches(
          allOf(
            isDisplayed(),
            if (serviceInfo.isSubjectEditable) isFocusable() else not(isFocusable())
          )
        )
      )
  }

  @Test
  @NotReadyForCI
  fun testEmailMsg() {
    onView(withId(R.id.editTextEmailMessage))
      .perform(scrollTo())
      .check(
        matches(
          allOf(
            isDisplayed(),
            if (TextUtils.isEmpty(serviceInfo.systemMsg)) withText(`is`(emptyString()))
            else withText(serviceInfo.systemMsg),
            if (serviceInfo.isMsgEditable) isFocusable() else not(isFocusable())
          )
        )
      )

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

  companion object {
    private const val STRING = "Some short string"
  }
}
