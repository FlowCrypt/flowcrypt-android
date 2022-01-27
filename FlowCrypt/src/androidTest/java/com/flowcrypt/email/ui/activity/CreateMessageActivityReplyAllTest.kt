/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.AccountAliasesEntity
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.rules.lazyActivityScenarioRule
import com.flowcrypt.email.util.AccountDaoManager
import org.hamcrest.Matchers
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
class CreateMessageActivityReplyAllTest : BaseTest() {
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
      CreateMessageActivity.generateIntent(
        getTargetContext(),
        msgInfo,
        MessageType.REPLY_ALL,
        MessageEncryptionType.STANDARD
      )
    )

    registerAllIdlingResources()

    Espresso.onView(ViewMatchers.withId(R.id.editTextRecipientCc))
      .check(ViewAssertions.matches(Matchers.not(ViewMatchers.isDisplayed())))
  }
}
