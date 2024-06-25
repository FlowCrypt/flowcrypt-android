/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui


import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.CreateMessageFragmentArgs
import com.flowcrypt.email.ui.base.BaseComposeScreenWithCustomSignatureFlowTest
import org.hamcrest.core.StringStartsWith
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class ComposeScreenForwardWithCustomSignatureFlowTest :
  BaseComposeScreenWithCustomSignatureFlowTest() {
  override val createMessageFragmentArgs: CreateMessageFragmentArgs
    get() = CreateMessageFragmentArgs(
      incomingMessageInfo = getMsgInfo(
        path = "messages/info/standard_msg_reply_to_header.json",
        mimeMsgPath = "messages/mime/standard_msg_reply_to_header.txt",
        accountEntity = BASE_ADD_ACCOUNT_RULE.apply { execute() }.accountEntityWithDecryptedInfo
      ),
      encryptedByDefault = false,
      messageType = MessageType.FORWARD
    )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testAddingSignatureAfterStart() {
    Thread.sleep(1000)
    onView(withId(R.id.editTextEmailMessage))
      .perform(scrollTo())
      .check(matches(withText(StringStartsWith.startsWith("\n\n$SIGNATURE"))))
  }
}
