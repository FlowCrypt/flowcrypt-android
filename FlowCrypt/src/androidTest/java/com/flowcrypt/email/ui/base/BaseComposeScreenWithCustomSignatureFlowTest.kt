/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.base

import android.content.Intent
import androidx.test.ext.junit.rules.activityScenarioRule
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.ui.ComposeScreenNewMessageWithCustomSignatureFlowTest
import com.flowcrypt.email.ui.activity.CreateMessageActivity
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.ui.activity.fragment.CreateMessageFragmentArgs
import com.flowcrypt.email.util.AccountDaoManager

/**
 * @author Denys Bondarenko
 */
abstract class BaseComposeScreenWithCustomSignatureFlowTest : BaseComposeScreenTest() {
  abstract val createMessageFragmentArgs: CreateMessageFragmentArgs

  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    Intent(getTargetContext(), CreateMessageActivity::class.java).apply {
      putExtras(createMessageFragmentArgs.toBundle())
    }
  )

  protected val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule()

  override val addAccountToDatabaseRule: AddAccountToDatabaseRule = BASE_ADD_ACCOUNT_RULE

  companion object {
    const val SIGNATURE = "My great signature"

    val BASE_ADD_ACCOUNT_RULE = AddAccountToDatabaseRule(
      AccountDaoManager.getDefaultAccountDao()
        .copy(signature = ComposeScreenNewMessageWithCustomSignatureFlowTest.SIGNATURE)
    )
  }
}