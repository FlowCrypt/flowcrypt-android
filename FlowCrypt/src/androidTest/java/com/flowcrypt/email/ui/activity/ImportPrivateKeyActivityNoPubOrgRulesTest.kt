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
import androidx.test.platform.app.InstrumentationRegistry
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.request.model.InitialLegacySubmitModel
import com.flowcrypt.email.api.retrofit.response.attester.InitialLegacySubmitResponse
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.InputStreamReader

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
      intent = ImportPrivateKeyActivity.getIntent(
          context = getTargetContext(),
          accountEntity = account,
          isSyncEnabled = false,
          title = getTargetContext().getString(R.string.import_private_key),
          throwErrorIfDuplicateFoundEnabled = true,
          cls = ImportPrivateKeyActivity::class.java))

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(RetryRule.DEFAULT)
      .around(activityScenarioRule)
      .around(ScreenshotTestRule())

  @Test
  fun testErrorWhenImportingKeyFromFile() {
    useIntentionFromRunCheckKeysActivity()
    addTextToClipboard("private key", privateKey)

    Espresso.onView(ViewMatchers.withId(R.id.buttonLoadFromClipboard))
        .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        .perform(ViewActions.click())

    isDialogWithTextDisplayed(decorView, ERROR_MESSAGE_FROM_ATTESTER)
  }

  private fun useIntentionFromRunCheckKeysActivity() {
    val intent = Intent()
    val list: ArrayList<PgpKeyDetails> = ArrayList()
    list.add(keyDetails)
    intent.putExtra(CheckKeysActivity.KEY_EXTRA_UNLOCKED_PRIVATE_KEYS, list)

    Intents.intending(IntentMatchers.hasComponent(ComponentName(getTargetContext(), CheckKeysActivity::class.java)))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, intent))
  }

  companion object {
    private const val ERROR_MESSAGE_FROM_ATTESTER = "Could not find LDAP pubkey on a LDAP-only domain for email no.pub@org-rules-test.flowcrypt.com on server keys.flowcrypt.com"

    private lateinit var privateKey: String
    private var keyDetails =
        PrivateKeysManager.getNodeKeyDetailsFromAssets("pgp/no.pub@org-rules-test.flowcrypt.test_orv_default.asc")

    @BeforeClass
    @JvmStatic
    fun createResources() {
      keyDetails.tempPassphrase = TestConstants.DEFAULT_PASSWORD.toCharArray()
      privateKey = keyDetails.privateKey!!
    }

    @get:ClassRule
    @JvmStatic
    val mockWebServerRule = FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        val gson = ApiHelper.getInstance(InstrumentationRegistry.getInstrumentation().targetContext).gson
        if (request.path.equals("/initial/legacy_submit")) {
          val requestModel = gson.fromJson(InputStreamReader(request.body.inputStream()), InitialLegacySubmitModel::class.java)

          when {
            requestModel.email.equals("no.pub@org-rules-test.flowcrypt.com", true) -> {
              val model = gson.fromJson(
                  InputStreamReader(ByteArrayInputStream(TestGeneralUtil.readObjectFromResourcesAsByteArray("4.json"))),
                  InitialLegacySubmitResponse::class.java)
              return MockResponse().setResponseCode(200).setBody(gson.toJson(model))
            }
          }
        }

        return MockResponse().setResponseCode(404)
      }
    })
  }
}
