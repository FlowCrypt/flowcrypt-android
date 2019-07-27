/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
import android.view.View
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasCategories
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.rule.ActivityTestRule
import com.flowcrypt.email.Constants
import com.flowcrypt.email.DebugTestAnnotation
import com.flowcrypt.email.DoesNotNeedMailserver
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.matchers.CustomMatchers.Companion.isToastDisplayed
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withEmptyRecyclerView
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.ui.activity.settings.KeysSettingsActivity
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.hamcrest.core.AllOf.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.File
import java.util.*

/**
 * @author Denis Bondarenko
 * Date: 20.02.2018
 * Time: 15:42
 * E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@DoesNotNeedMailserver
class KeysSettingsActivityTest : BaseTest() {

  override val activityTestRule: ActivityTestRule<*>? = IntentsTestRule(KeysSettingsActivity::class.java)
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule()

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(AddAccountToDatabaseRule())
      .around(addPrivateKeyToDatabaseRule)
      .around(activityTestRule)

  @Test
  fun testAddNewKeys() {
    intending(hasComponent(ComponentName(getTargetContext(), ImportPrivateKeyActivity::class.java)))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

    val details = PrivateKeysManager.getNodeKeyDetailsFromAssets("node/default@denbond7.com_secondKey_prv_default.json")
    PrivateKeysManager.saveKeyToDatabase(details, TestConstants.DEFAULT_PASSWORD, KeyDetails.Type.EMAIL)

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
        .check(matches(not<View>(withEmptyRecyclerView()))).check(matches(isDisplayed()))
    onView(withId(R.id.emptyView))
        .check(matches(not<View>(isDisplayed())))
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
    onView(withText(TestGeneralUtil.replaceVersionInKey(keyDetails.publicKey)))
  }

  @Test
  fun testKeyDetailsCopyToClipBoard() {
    selectFirstKey()
    val details = addPrivateKeyToDatabaseRule.nodeKeyDetails
    onView(withId(R.id.btnCopyToClipboard))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(withText(getResString(R.string.copied)))
        .inRoot(isToastDisplayed())
        .check(matches(isDisplayed()))
    UiThreadStatement.runOnUiThread { checkClipboardText(TestGeneralUtil.replaceVersionInKey(details.publicKey)) }
  }

  @Test
  @DebugTestAnnotation
  fun testKeyDetailsShowPrivateKey() {
    selectFirstKey()
    onView(withId(R.id.btnShowPrKey))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(withText(getResString(R.string.see_backups_to_save_your_private_keys)))
        .inRoot(isToastDisplayed())
        .check(matches(isDisplayed()))
  }

  @Test
  fun testKeyDetailsCheckDetails() {
    selectFirstKey()
    val details = addPrivateKeyToDatabaseRule.nodeKeyDetails
    onView(withId(R.id.textViewKeyWords))
        .check(matches(withText(getHtmlString(getResString(R.string.template_key_words, details.keywords ?: "")))))

    onView(withId(R.id.textViewFingerprint))
        .check(matches(withText(getHtmlString(getResString(R.string.template_fingerprint,
            GeneralUtil.doSectionsInText(" ", details.fingerprint, 4)!!)))))

    onView(withId(R.id.textViewLongId)).check(
        matches(withText(getResString(R.string.template_longid, details.longId ?: ""))))

    onView(withId(R.id.textViewDate))
        .check(matches(withText(getHtmlString(getResString(R.string.template_date,
            DateFormat.getMediumDateFormat(getTargetContext()).format(Date(details.created)))))))

    val emails = ArrayList<String>()

    for (pgpContact in details.pgpContacts) {
      emails.add(pgpContact.email)
    }

    onView(withId(R.id.textViewUsers))
        .check(matches(withText(getResString(R.string.template_users, TextUtils.join(", ", emails)))))
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
    onView(withText(getResString(R.string.saved)))
        .inRoot(isToastDisplayed())
        .check(matches(isDisplayed()))
  }

  private fun selectFirstKey() {
    onView(withId(R.id.recyclerViewKeys))
        .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
  }
}
