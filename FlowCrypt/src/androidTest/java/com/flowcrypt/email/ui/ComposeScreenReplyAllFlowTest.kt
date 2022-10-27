/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.AccountAliasesEntity
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.rules.lazyActivityScenarioRule
import com.flowcrypt.email.ui.activity.CreateMessageActivity
import com.flowcrypt.email.ui.activity.fragment.CreateMessageFragmentArgs
import com.flowcrypt.email.util.AccountDaoManager
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 *         Date: 2/13/20
 *         Time: 12:30 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class ComposeScreenReplyAllFlowTest : BaseTest() {
  override val activeActivityRule =
    lazyActivityScenarioRule<CreateMessageActivity>(launchActivity = false)
  override val activityScenario: ActivityScenario<*>?
    get() = activeActivityRule.scenario

  private val account = AccountDaoManager.getDefaultAccountDao()
  private val accountAliasesEntity = AccountAliasesEntity(
    email = account.email,
    accountType = account.accountType ?: "",
    sendAsEmail = "alias@flowcrypt.test",
    displayName = "Alias",
    isDefault = true,
    verificationStatus = "accepted"
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(AddAccountToDatabaseRule(account))
    .around(AddPrivateKeyToDatabaseRule())
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  @Test
  fun testReplyAllUsingGmailAlias() {
    val msgInfo = getMsgInfo(
      "messages/info/standard_msg_reply_all_via_gmail_alias.json",
      "messages/mime/standard_msg_reply_to_header.txt"
    )

    roomDatabase.accountAliasesDao().insert(accountAliasesEntity)

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

    onView(withId(R.id.chipLayoutCc))
      .check(matches(not(isDisplayed())))
  }
}
