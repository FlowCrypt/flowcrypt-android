/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.rules.lazyActivityScenarioRule
import com.flowcrypt.email.ui.activity.fragment.CreateMessageFragmentArgs
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
@MediumTest
@RunWith(AndroidJUnit4::class)
class CreateMessageActivityReplyTest : BaseTest() {
  override val activeActivityRule =
    lazyActivityScenarioRule<CreateMessageActivity>(launchActivity = false)
  override val activityScenario: ActivityScenario<*>?
    get() = activeActivityRule.scenario

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(AddAccountToDatabaseRule())
    .around(AddPrivateKeyToDatabaseRule())
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  @Test
  fun testReplyToHeader() {
    val msgInfo = getMsgInfo(
      "messages/info/standard_msg_reply_to_header.json",
      "messages/mime/standard_msg_reply_to_header.txt"
    )
    activeActivityRule.launch(
      Intent(getTargetContext(), CreateMessageActivity::class.java).apply {
        putExtras(
          CreateMessageFragmentArgs(
            incomingMessageInfo = msgInfo,
            encryptedByDefault = false,
            messageType = MessageType.REPLY
          ).toBundle()
        )
      }
    )

    registerAllIdlingResources()

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
