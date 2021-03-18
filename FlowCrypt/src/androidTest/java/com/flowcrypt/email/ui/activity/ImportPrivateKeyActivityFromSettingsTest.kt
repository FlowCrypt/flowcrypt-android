/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasCategories
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.ReadyForCIAnnotation
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.File

/**
 * @author Denis Bondarenko
 * Date: 17.03.2018
 * Time: 13:33
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class ImportPrivateKeyActivityFromSettingsTest : BaseTest() {
  private val addAccountToDatabaseRule = AddAccountToDatabaseRule()

  override val useIntents: Boolean = true
  override val activityScenarioRule = activityScenarioRule<ImportPrivateKeyActivity>(
      intent = ImportPrivateKeyActivity.getIntent(
          context = getTargetContext(),
          accountEntity = addAccountToDatabaseRule.account,
          isSyncEnabled = true,
          title = getTargetContext().getString(R.string.import_private_key),
          throwErrorIfDuplicateFoundEnabled = true,
          isSubmittingPubKeysEnabled = false,
          cls = ImportPrivateKeyActivity::class.java))

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(addAccountToDatabaseRule)
      .around(RetryRule.DEFAULT)
      .around(activityScenarioRule)
      .around(ScreenshotTestRule())

  @Test
  @ReadyForCIAnnotation
  fun testImportKeyFromBackup() {
    useIntentionFromRunCheckKeysActivity()

    onView(withId(R.id.buttonImportBackup))
        .check(matches(isDisplayed()))
        .perform(click())
    Assert.assertTrue(activityScenarioRule.scenario.result.resultCode == Activity.RESULT_OK)
  }

  @Test
  @ReadyForCIAnnotation
  fun testImportKeyFromFile() {
    useIntentionToRunActivityToSelectFile(fileWithPrivateKey)
    useIntentionFromRunCheckKeysActivity()

    onView(withId(R.id.buttonLoadFromFile))
        .check(matches(isDisplayed()))
        .perform(click())
    Assert.assertTrue(activityScenarioRule.scenario.result.resultCode == Activity.RESULT_OK)
  }

  @Test
  @ReadyForCIAnnotation
  fun testShowErrorWhenImportingKeyFromFile() {
    useIntentionToRunActivityToSelectFile(fileWithoutPrivateKey)

    onView(withId(R.id.buttonLoadFromFile))
        .check(matches(isDisplayed()))
        .perform(click())
    isDialogWithTextDisplayed(decorView, getResString(R.string.file_has_wrong_pgp_structure, getResString(R.string.private_)))
  }

  @Test
  @ReadyForCIAnnotation
  fun testImportKeyFromClipboard() {
    useIntentionFromRunCheckKeysActivity()

    addTextToClipboard("private key", privateKey)
    onView(withId(R.id.buttonLoadFromClipboard))
        .check(matches(isDisplayed()))
        .perform(click())
    Assert.assertTrue(activityScenarioRule.scenario.result.resultCode == Activity.RESULT_OK)
  }

  @Test
  @ReadyForCIAnnotation
  fun testShowErrorWhenImportKeyFromClipboard() {
    addTextToClipboard("not private key", SOME_TEXT)
    onView(withId(R.id.buttonLoadFromClipboard))
        .check(matches(isDisplayed()))
        .perform(click())
    isDialogWithTextDisplayed(decorView, getResString(R.string.clipboard_has_wrong_structure, getResString(R.string.private_)))
  }

  private fun useIntentionToRunActivityToSelectFile(file: File?) {
    val resultData = Intent()
    resultData.data = Uri.fromFile(file)
    intending(allOf(hasAction(Intent.ACTION_CHOOSER), hasExtra(`is`(Intent.EXTRA_INTENT), allOf(hasAction(Intent
        .ACTION_OPEN_DOCUMENT), hasCategories(hasItem(equalTo(Intent.CATEGORY_OPENABLE))), hasType("*/*")))))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultData))
  }


  private fun useIntentionFromRunCheckKeysActivity() {
    val intent = Intent()
    val list: ArrayList<NodeKeyDetails> = ArrayList()
    list.add(keyDetails)
    intent.putExtra(CheckKeysActivity.KEY_EXTRA_UNLOCKED_PRIVATE_KEYS, list)

    intending(hasComponent(ComponentName(getTargetContext(), CheckKeysActivity::class.java)))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, intent))
  }

  companion object {
    private const val SOME_TEXT = "Some text"
    private lateinit var fileWithPrivateKey: File
    private lateinit var fileWithoutPrivateKey: File
    private lateinit var privateKey: String
    private var keyDetails =
        PrivateKeysManager.getNodeKeyDetailsFromAssets("node/attested_user@denbond7.com_prv_default_strong.json")

    @BeforeClass
    @JvmStatic
    fun createResources() {
      keyDetails.passphrase = TestConstants.DEFAULT_STRONG_PASSWORD
      privateKey = keyDetails.privateKey!!
      fileWithPrivateKey = TestGeneralUtil.createFile(
          TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER + "_sec.asc", privateKey)
      fileWithoutPrivateKey = TestGeneralUtil.createFile(
          TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER + ".txt", SOME_TEXT)
    }

    @AfterClass
    @JvmStatic
    fun cleanResources() {
      TestGeneralUtil.deleteFiles(listOf(fileWithPrivateKey, fileWithoutPrivateKey))
    }
  }
}
