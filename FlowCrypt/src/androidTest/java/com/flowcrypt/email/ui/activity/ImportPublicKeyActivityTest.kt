/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasCategories
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.flowcrypt.email.DoesNotNeedMailserver
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.ui.activity.base.BaseImportKeyActivity
import com.flowcrypt.email.util.TestGeneralUtil
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.junit.AfterClass
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
 * Date: 23.02.2018
 * Time: 16:53
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@Ignore("Failed on CI")
class ImportPublicKeyActivityTest : BaseTest() {
  override val useIntents: Boolean = true
  override val activityScenarioRule = activityScenarioRule<ImportPublicKeyActivity>(
      intent = Intent(getTargetContext(), ImportPublicKeyActivity::class.java).apply {
        putExtra(BaseImportKeyActivity.KEY_EXTRA_IS_SYNC_ENABLE, true)
        putExtra(BaseImportKeyActivity.KEY_EXTRA_TITLE, getResString(R.string.import_public_key))
        putExtra(BaseImportKeyActivity.KEY_EXTRA_IS_THROW_ERROR_IF_DUPLICATE_FOUND, false)
        putExtra(ImportPublicKeyActivity.KEY_EXTRA_PGP_CONTACT, PgpContact(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER))
      })

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(AddAccountToDatabaseRule())
      .around(GrantPermissionRule.grant(android.Manifest.permission.READ_EXTERNAL_STORAGE))
      .around(RetryRule())
      .around(activityScenarioRule)

  @Test
  @DoesNotNeedMailserver
  @Ignore("fix me")
  fun testImportKeyFromFile() {
    val resultData = Intent()
    resultData.data = Uri.fromFile(fileWithPublicKey)
    intending(allOf(hasAction(Intent.ACTION_CHOOSER), hasExtra(`is`(Intent.EXTRA_INTENT), allOf(hasAction(Intent
        .ACTION_OPEN_DOCUMENT), hasCategories(hasItem(equalTo(Intent.CATEGORY_OPENABLE))), hasType("*/*")))))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultData))
    onView(withId(R.id.buttonLoadFromFile))
        .check(matches(isDisplayed()))
        .perform(click())
    //assertThat(activityTestRule?.activityResult, hasResultCode(Activity.RESULT_OK))
  }

  @Test
  @DoesNotNeedMailserver
  @Ignore("fix me")
  fun testShowErrorWhenImportingKeyFromFile() {
    val resultData = Intent()
    resultData.data = Uri.fromFile(fileWithoutPublicKey)
    intending(allOf(hasAction(Intent.ACTION_CHOOSER), hasExtra(`is`(Intent.EXTRA_INTENT), allOf(hasAction(Intent
        .ACTION_OPEN_DOCUMENT), hasCategories(hasItem(equalTo(Intent.CATEGORY_OPENABLE))), hasType("*/*")))))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultData))
    onView(withId(R.id.buttonLoadFromFile))
        .check(matches(isDisplayed()))
        .perform(click())
    //isDialogWithTextDisplayed(activityTestRule?.activity, getResString(R.string.file_has_wrong_pgp_structure, getResString(R.string.public_)))
  }

  @Test
  @DoesNotNeedMailserver
  @Ignore("fix me. Failed on CI")
  fun testImportKeyFromClipboard() {
    addTextToClipboard("public key", publicKey)
    onView(withId(R.id.buttonLoadFromClipboard))
        .check(matches(isDisplayed()))
        .perform(click())
    //assertThat(activityTestRule?.activityResult, hasResultCode(Activity.RESULT_OK))
  }

  @Test
  @Ignore("fix me")
  fun testShowErrorWhenImportKeyFromClipboard() {
    addTextToClipboard("not public key", SOME_TEXT)
    onView(withId(R.id.buttonLoadFromClipboard))
        .check(matches(isDisplayed()))
        .perform(click())
    //isDialogWithTextDisplayed(activityTestRule?.activity, getResString(R.string        .clipboard_has_wrong_structure,        getResString(R.string.public_)))
  }

  companion object {
    private const val SOME_TEXT = "Some text"
    private lateinit var fileWithPublicKey: File
    private lateinit var fileWithoutPublicKey: File
    private lateinit var publicKey: String

    @BeforeClass
    @JvmStatic
    fun createResources() {
      publicKey = TestGeneralUtil.readFileFromAssetsAsString(InstrumentationRegistry.getInstrumentation().context,
          "pgp/" + TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER + "-pub.asc")
      fileWithPublicKey = TestGeneralUtil.createFile(
          TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER + "_pub.asc", publicKey)
      fileWithoutPublicKey = TestGeneralUtil.createFile(
          TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER + ".txt", SOME_TEXT)
    }

    @AfterClass
    @JvmStatic
    fun cleanResources() {
      TestGeneralUtil.deleteFiles(listOf(fileWithPublicKey, fileWithoutPublicKey))
    }
  }
}
