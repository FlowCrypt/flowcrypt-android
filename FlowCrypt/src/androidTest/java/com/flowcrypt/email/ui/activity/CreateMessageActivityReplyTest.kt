/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.flowcrypt.email.DoesNotNeedMailserver
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.hootsuite.nachos.tokenizer.SpanChipTokenizer
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 *         Date: 10/1/19
 *         Time: 2:29 PM
 *         E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@DoesNotNeedMailserver
class CreateMessageActivityReplyTest : BaseTest() {
  override val activityTestRule: ActivityTestRule<*>? =
      IntentsTestRule(CreateMessageActivity::class.java, false, false)

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(AddAccountToDatabaseRule())
      .around(AddPrivateKeyToDatabaseRule())
      .around(activityTestRule)

  @Test
  fun testReplyToHeader() {
    val msgInfo = getMsgInfo("messages/info/standard_msg_reply_to_header.json",
        "messages/mime/standard_msg_reply_to_header.txt")
    activityTestRule?.launchActivity(CreateMessageActivity.generateIntent(
        getTargetContext(),
        msgInfo,
        MessageType.REPLY,
        MessageEncryptionType.STANDARD))
    registerNodeIdling()

    onView(withId(R.id.editTextRecipientTo))
        .check(matches(isDisplayed()))
        .check(matches(withText(prepareChipText(msgInfo?.getReplyTo()?.first()?.address))))
  }

  private fun prepareChipText(text: String?): String {
    val chipSeparator = SpanChipTokenizer.CHIP_SPAN_SEPARATOR.toString()
    val autoCorrectSeparator = SpanChipTokenizer.AUTOCORRECT_SEPARATOR.toString()
    return (autoCorrectSeparator
        + chipSeparator
        + text
        + chipSeparator
        + autoCorrectSeparator)
  }
}