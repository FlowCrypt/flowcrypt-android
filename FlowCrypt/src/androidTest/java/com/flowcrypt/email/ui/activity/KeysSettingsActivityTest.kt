/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import android.os.Environment
import android.text.TextUtils
import android.text.format.DateFormat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasCategories
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.matcher.ViewMatchers.hasTextColor
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.junit.annotations.NotReadyForCI
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withEmptyRecyclerView
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.settings.KeysSettingsActivity
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.PrivateKeysManager
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.hamcrest.core.AllOf.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author Denis Bondarenko
 * Date: 20.02.2018
 * Time: 15:42
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class KeysSettingsActivityTest : BaseTest() {

  override val useIntents: Boolean = true
  override val activityScenarioRule = activityScenarioRule<KeysSettingsActivity>()

  private val addAccountToDatabaseRule = AddAccountToDatabaseRule()
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule()

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(addAccountToDatabaseRule)
      .around(addPrivateKeyToDatabaseRule)
      .around(RetryRule.DEFAULT)
      .around(activityScenarioRule)
      .around(ScreenshotTestRule())

  @Before
  fun waitData() {
    //todo-denbond7 need to wait while activity lunches a fragment and loads data via ROOM.
    // Need to improve this code after espresso updates
    Thread.sleep(3000)
  }

  @Test
  fun testAddNewKeys() {
    intending(hasComponent(ComponentName(getTargetContext(), ImportPrivateKeyActivity::class.java)))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

    val details = PrivateKeysManager.getNodeKeyDetailsFromAssets("pgp/default@flowcrypt.test_secondKey_prv_default.asc")
    PrivateKeysManager.saveKeyToDatabase(
        accountEntity = addAccountToDatabaseRule.account,
        nodeKeyDetails = details,
        passphrase = TestConstants.DEFAULT_PASSWORD,
        type = KeyDetails.Type.EMAIL
    )

    onView(withId(R.id.floatActionButtonAddKey))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(withId(R.id.recyclerViewKeys))
        .check(matches(isDisplayed()))
        .check(matches(withRecyclerViewItemCount(2)))
  }

  @Test
  fun testKeyExists() {
    onView(withId(R.id.recyclerViewKeys))
        .check(matches(not(withEmptyRecyclerView()))).check(matches(isDisplayed()))
    onView(withId(R.id.emptyView))
        .check(matches(not(isDisplayed())))
  }

  @Test
  fun testShowKeyDetailsScreen() {
    selectFirstKey()
  }

  @Test
  fun testKeyDetailsShowPubKey() {
    selectFirstKey()
    val keyDetails = addPrivateKeyToDatabaseRule.nodeKeyDetails
    onView(withId(R.id.btnShowPubKey))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(withText(keyDetails.publicKey))
  }

  @Test
  fun testKeyDetailsCopyToClipBoard() {
    selectFirstKey()
    val details = addPrivateKeyToDatabaseRule.nodeKeyDetails
    onView(withId(R.id.btnCopyToClipboard))
        .check(matches(isDisplayed()))
        .perform(click())
    isToastDisplayed(decorView, getResString(R.string.copied))
    UiThreadStatement.runOnUiThread { checkClipboardText(details.publicKey ?: "") }
  }

  @Test
  @NotReadyForCI
  fun testKeyDetailsShowPrivateKey() {
    selectFirstKey()
    onView(withId(R.id.btnShowPrKey))
        .perform(scrollTo())
        .perform(click())
    isToastDisplayed(decorView, getResString(R.string.see_backups_to_save_your_private_keys))
  }

  @Test
  fun testKeyDetailsCheckDetails() {
    selectFirstKey()
    val details = addPrivateKeyToDatabaseRule.nodeKeyDetails

    onView(withId(R.id.textViewFingerprint))
        .check(matches(withText(getHtmlString(getResString(R.string.template_fingerprint,
            GeneralUtil.doSectionsInText(" ", details.fingerprint, 4)!!)))))

    onView(withId(R.id.textViewLongId)).check(
        matches(withText(getResString(R.string.template_longid, details.longId ?: ""))))

    onView(withId(R.id.textViewDate))
        .check(matches(withText(getHtmlString(getResString(R.string.template_date,
            DateFormat.getMediumDateFormat(getTargetContext()).format(
                Date(TimeUnit.MILLISECONDS.convert(details.created, TimeUnit.SECONDS))))))))

    onView(withId(R.id.tVPassPhraseVerification))
        .check(matches(withText(getResString(R.string.stored_pass_phrase_matched))))

    val emails = ArrayList<String>()

    for (pgpContact in details.pgpContacts) {
      emails.add(pgpContact.email)
    }

    onView(withId(R.id.textViewUsers))
        .check(matches(withText(getResString(R.string.template_users, TextUtils.join(", ", emails)))))
  }

  @Test
  fun testKeyDetailsTestPassPhraseMismatch() {
    val details = PrivateKeysManager.getNodeKeyDetailsFromAssets(
        "pgp/default@flowcrypt.test_secondKey_prv_default.asc")
    PrivateKeysManager.saveKeyToDatabase(
        accountEntity = addAccountToDatabaseRule.account,
        nodeKeyDetails = details,
        passphrase = "wrong passphrase",
        type = KeyDetails.Type.EMAIL
    )

    onView(withId(R.id.recyclerViewKeys))
        .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

    onView(withId(R.id.tVPassPhraseVerification))
        .check(matches(withText(getResString(R.string.stored_pass_phrase_mismatch))))
        .check(matches(hasTextColor(R.color.red)))
  }

  @Test
  fun testKeyDetailsSavePubKeyToFileWhenFileIsNotExist() {
    selectFirstKey()
    val details = addPrivateKeyToDatabaseRule.nodeKeyDetails

    val file =
        File(getTargetContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "0x" + details.longId + ".asc")

    if (file.exists()) {
      file.delete()
    }

    val resultData = Intent()
    resultData.data = FileProvider.getUriForFile(getTargetContext(), Constants.FILE_PROVIDER_AUTHORITY, file)

    intending(allOf(hasAction(Intent.ACTION_CREATE_DOCUMENT),
        hasCategories(hasItem(equalTo(Intent.CATEGORY_OPENABLE))),
        hasType(Constants.MIME_TYPE_PGP_KEY)))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultData))

    onView(withId(R.id.btnSaveToFile))
        .check(matches(isDisplayed()))
        .perform(click())
    isToastDisplayed(decorView, getResString(R.string.saved))
  }

  private fun selectFirstKey() {
    onView(withId(R.id.recyclerViewKeys))
        .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
  }
}
