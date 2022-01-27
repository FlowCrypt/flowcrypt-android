/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
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
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.junit.annotations.DependsOnMailServer
import com.flowcrypt.email.junit.annotations.NotReadyForCI
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Ignore
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
      cls = ImportPrivateKeyActivity::class.java
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(addAccountToDatabaseRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  @NotReadyForCI
  fun testImportKeyFromBackup() {
    useIntentionFromRunCheckKeysActivity()

    onView(withId(R.id.buttonImportBackup))
      .check(matches(isDisplayed()))
      .perform(click())
    Assert.assertTrue(activityScenarioRule.scenario.result.resultCode == Activity.RESULT_OK)
  }

  @Test
  @DependsOnMailServer
  fun testImportKeyFromFile() {
    useIntentionToRunActivityToSelectFile(fileWithPrivateKey)
    useIntentionFromRunCheckKeysActivity()

    onView(withId(R.id.buttonLoadFromFile))
      .check(matches(isDisplayed()))
      .perform(click())
    Assert.assertTrue(activityScenarioRule.scenario.result.resultCode == Activity.RESULT_OK)
  }

  @Test
  @DependsOnMailServer
  fun testShowErrorWhenImportingKeyFromFile() {
    useIntentionToRunActivityToSelectFile(fileWithoutPrivateKey)

    onView(withId(R.id.buttonLoadFromFile))
      .check(matches(isDisplayed()))
      .perform(click())
    isDialogWithTextDisplayed(
      decorView,
      getResString(R.string.file_has_wrong_pgp_structure, getResString(R.string.private_))
    )
  }

  @Test
  @DependsOnMailServer
  @Ignore(
    "Temporary disabled due to https://github.com/FlowCrypt/flowcrypt-android/issues/1478"
  )
  fun testShowErrorWhenImportingKeyFromFileSHA1() {
    val prvKeyAsString = TestGeneralUtil.readFileFromAssetsAsString(
      "pgp/sha1@flowcrypt.test_prv_default.asc"
    )
    val file = TestGeneralUtil.createFileAndFillWithContent(
      fileName = "sha1@flowcrypt.test_prv_default.asc",
      fileText = prvKeyAsString
    )
    useIntentionToRunActivityToSelectFile(file)

    onView(withId(R.id.buttonLoadFromFile))
      .check(matches(isDisplayed()))
      .perform(click())
    isDialogWithTextDisplayed(
      decorView,
      getResString(R.string.key_sha1_warning_msg)
    )
  }

  @Test
  @DependsOnMailServer
  fun testImportKeyFromClipboard() {
    useIntentionFromRunCheckKeysActivity()

    addTextToClipboard("private key", privateKey)
    onView(withId(R.id.buttonLoadFromClipboard))
      .check(matches(isDisplayed()))
      .perform(click())
    Assert.assertTrue(activityScenarioRule.scenario.result.resultCode == Activity.RESULT_OK)
  }

  @Test
  @DependsOnMailServer
  fun testShowErrorWhenImportKeyFromClipboard() {
    addTextToClipboard("not private key", SOME_TEXT)
    onView(withId(R.id.buttonLoadFromClipboard))
      .check(matches(isDisplayed()))
      .perform(click())
    isDialogWithTextDisplayed(
      decorView,
      getResString(R.string.clipboard_has_wrong_structure, getResString(R.string.private_))
    )
  }

  private fun useIntentionToRunActivityToSelectFile(file: File) {
    val resultData = TestGeneralUtil.genIntentWithPersistedReadPermissionForFile(file)
    intending(
      allOf(
        hasAction(Intent.ACTION_CHOOSER), hasExtra(
          `is`(Intent.EXTRA_INTENT), allOf(
            hasAction(
              Intent
                .ACTION_OPEN_DOCUMENT
            ), hasCategories(hasItem(equalTo(Intent.CATEGORY_OPENABLE))), hasType("*/*")
          )
        )
      )
    )
      .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultData))
  }


  private fun useIntentionFromRunCheckKeysActivity() {
    val intent = Intent()
    val list: ArrayList<PgpKeyDetails> = ArrayList()
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
      PrivateKeysManager.getPgpKeyDetailsFromAssets("pgp/attested_user@flowcrypt.test_prv_default_strong.asc")

    @BeforeClass
    @JvmStatic
    fun createResources() {
      keyDetails.tempPassphrase = TestConstants.DEFAULT_STRONG_PASSWORD.toCharArray()
      keyDetails.passphraseType = KeyEntity.PassphraseType.DATABASE
      privateKey = keyDetails.privateKey!!
      fileWithPrivateKey = TestGeneralUtil.createFileAndFillWithContent(
        TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER + "_sec.asc", privateKey
      )
      fileWithoutPrivateKey = TestGeneralUtil.createFileAndFillWithContent(
        TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER + ".txt", SOME_TEXT
      )
    }

    @AfterClass
    @JvmStatic
    fun cleanResources() {
      TestGeneralUtil.deleteFiles(listOf(fileWithPrivateKey, fileWithoutPrivateKey))
    }
  }
}
