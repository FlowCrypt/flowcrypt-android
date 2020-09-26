/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.GrantPermissionRule
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.ui.activity.base.BaseImportKeyActivity
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.PrivateKeysManager
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 *         Date: 7/10/20
 *         Time: 4:57 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class ImportPrivateKeyActivityNoPubOrgRulesTest : BaseTest() {
  private val account = AccountDaoManager.getAccountDao("no.pub@org-rules-test.flowcrypt.com.json")

  override val useIntents: Boolean = true
  override val activityScenarioRule = activityScenarioRule<ImportPrivateKeyActivity>(
      intent = BaseImportKeyActivity.newIntent(
          context = getTargetContext(),
          accountEntity = account,
          isSyncEnabled = false,
          title = getTargetContext().getString(R.string.import_private_key),
          throwErrorIfDuplicateFoundEnabled = true,
          cls = ImportPrivateKeyActivity::class.java))

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(GrantPermissionRule.grant(android.Manifest.permission.READ_EXTERNAL_STORAGE))
      .around(RetryRule())
      .around(activityScenarioRule)

  @Test
  @Ignore("fix me")
  fun testErrorWhenImportingKeyFromFile() {
    useIntentionFromRunCheckKeysActivity()
    addTextToClipboard("private key", privateKey)

    Espresso.onView(ViewMatchers.withId(R.id.buttonLoadFromClipboard))
        .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        .perform(ViewActions.click())

    //isDialogWithTextDisplayed(activityTestRule?.activity, ERROR_MESSAGE_FROM_ATTESTER)
  }

  private fun useIntentionFromRunCheckKeysActivity() {
    val intent = Intent()
    val list: ArrayList<NodeKeyDetails> = ArrayList()
    list.add(keyDetails)
    intent.putExtra(CheckKeysActivity.KEY_EXTRA_UNLOCKED_PRIVATE_KEYS, list)

    Intents.intending(IntentMatchers.hasComponent(ComponentName(getTargetContext(), CheckKeysActivity::class.java)))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, intent))
  }

  companion object {
    private const val ERROR_MESSAGE_FROM_ATTESTER = "Could not find LDAP pubkey on a LDAP-only domain for email no.pub@org-rules-test.flowcrypt.com on server keys.flowcrypt.com"

    private lateinit var privateKey: String
    private var keyDetails =
        PrivateKeysManager.getNodeKeyDetailsFromAssets("node/no.pub@org-rules-test.flowcrypt.com_orv_default.json")

    @BeforeClass
    @JvmStatic
    fun createResources() {
      keyDetails.passphrase = TestConstants.DEFAULT_PASSWORD
      privateKey = keyDetails.privateKey!!
    }
  }
}
