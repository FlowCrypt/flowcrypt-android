/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.base.BaseGmailLabelsFlowTest
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
class MessageDetailsGmailAPIArchiveFlowTest : BaseGmailLabelsFlowTest() {
  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(addLabelsToDatabaseRule)
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  /**
   * Test that the 'archive' action is visible on the message details screen
   * when 'INBOX' label is exist.
   */
  @Test
  fun testDisplayArchiveAction() {
    val details = genIncomingMessageInfo()?.msgEntity
    requireNotNull(details)
    launchActivity(details)
    Thread.sleep(1000)

    onView(withId(R.id.menuActionArchiveMessage))
      .check(matches(isDisplayed()))
  }

  /**
   * Test that the 'move to Inbox' action is visible on the message details screen
   * when 'INBOX' label is not exist.
   */
  @Test
  fun testDisplayMoveToInboxAction() {
    val details =
      genIncomingMessageInfo(initLabelIds().apply { remove(GmailApiHelper.LABEL_INBOX) })?.msgEntity
    requireNotNull(details)
    launchActivity(details)
    Thread.sleep(1000)

    onView(withId(R.id.menuActionMoveToInbox))
      .check(matches(isDisplayed()))
  }
}
