/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.retrofit.response.model.MsgBlock
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.MessageDetailsFragment
import com.flowcrypt.email.ui.activity.fragment.MessageDetailsFragmentArgs
import com.flowcrypt.email.ui.base.BaseMessageDetailsFlowTest
import org.junit.Before
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
@FlowCryptTestSettings(useCommonIdling = false)
class MessageDetailsFragmentInIsolationTest : BaseMessageDetailsFlowTest() {

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(AddPrivateKeyToDatabaseRule())
    .around(ScreenshotTestRule())

  private lateinit var incomingMessageInfo: IncomingMessageInfo

  @Before
  fun launchFragmentInContainerWithPredefinedArgs() {
    incomingMessageInfo = requireNotNull(
      getMsgInfo(
        path = "messages/info/standard_msg_info_plaintext.json",
        mimeMsgPath = "messages/mime/standard_msg_info_plaintext.txt",
        accountEntity = addAccountToDatabaseRule.accountEntityWithDecryptedInfo
      )
    )

    launchFragmentInContainer<MessageDetailsFragment>(
      MessageDetailsFragmentArgs(
        localFolder = localFolder,
        messageEntity = incomingMessageInfo.msgEntity
      ).toBundle()
    )
  }

  @Test
  fun testBaseInfo() {
    Thread.sleep(2000)

    onView(withId(R.id.textViewSubject))
      .check(matches(withText(incomingMessageInfo.getSubject())))

    incomingMessageInfo.msgBlocks?.firstOrNull { it.type == MsgBlock.Type.PLAIN_HTML }?.let {
      checkWebViewText(it.content)
    }
  }
}