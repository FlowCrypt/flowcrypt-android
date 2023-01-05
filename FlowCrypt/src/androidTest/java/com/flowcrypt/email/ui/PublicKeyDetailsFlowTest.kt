/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddRecipientsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.hamcrest.core.AllOf
import org.junit.AfterClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.File
import java.util.Date

/**
 * @author Denis Bondarenko
 *         Date: 10/2/19
 *         Time: 1:32 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class PublicKeyDetailsFlowTest : BaseTest() {
  private val keyDetails =
    PrivateKeysManager.getPgpKeyDetailsFromAssets("pgp/expired@flowcrypt.test_pub.asc")

  override val useIntents: Boolean = true
  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      destinationId = R.id.publicKeyDetailsFragment,
      extras = Bundle().apply {
        putParcelable(
          "recipientEntity", RecipientEntity(email = EMAIL_DENBOND7, name = USER_DENBOND7)
        )
        putParcelable(
          "publicKeyEntity", keyDetails.toPublicKeyEntity(EMAIL_DENBOND7).copy(id = 12)
        )
      }
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(AddAccountToDatabaseRule())
    .around(
      AddRecipientsToDatabaseRule(
        listOf(
          RecipientWithPubKeys(
            RecipientEntity(email = EMAIL_DENBOND7, name = USER_DENBOND7),
            listOf(keyDetails.toPublicKeyEntity(EMAIL_DENBOND7).copy(id = 12))
          )
        )
      )
    )
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testPubKeyDetails() {
    keyDetails.users.forEachIndexed { index, s ->
      onView(withText(getResString(R.string.template_user, index + 1, s)))
        .check(matches(isDisplayed()))
    }

    keyDetails.ids.forEachIndexed { index, s ->
      onView(withText(getResString(R.string.template_fingerprint_2, index + 1, s.fingerprint)))
        .check(matches(isDisplayed()))
    }

    onView(withId(R.id.textViewAlgorithm))
      .check(
        matches(
          withText(
            getResString(
              R.string.template_algorithm,
              keyDetails.algo.algorithm!!
            )
          )
        )
      )
    onView(withId(R.id.textViewCreated))
      .check(
        matches(
          withText(
            getResString(
              R.string.template_created,
              DateTimeUtil.getPgpDateFormat(getTargetContext()).format(Date(keyDetails.created))
            )
          )
        )
      )
  }

  @Test
  fun testActionCopy() {
    onView(withId(R.id.menuActionCopy))
      .check(matches(isDisplayed()))
      .perform(click())
    isToastDisplayed(getResString(R.string.public_key_copied_to_clipboard))
    UiThreadStatement.runOnUiThread {
      checkClipboardText(TestGeneralUtil.replaceVersionInKey(keyDetails.publicKey))
    }
  }

  @Test
  fun testActionSave() {
    val sanitizedEmail = EMAIL_DENBOND7.replace("[^a-z0-9]".toRegex(), "")
    val fileName = "0x" + keyDetails.fingerprint + "-" + sanitizedEmail + "-publickey" + ".asc"

    val file =
      File(getTargetContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

    if (file.exists()) {
      file.delete()
    }

    val resultData = Intent()
    resultData.data =
      FileProvider.getUriForFile(getTargetContext(), Constants.FILE_PROVIDER_AUTHORITY, file)

    Intents.intending(
      AllOf.allOf(
        IntentMatchers.hasAction(Intent.ACTION_CREATE_DOCUMENT),
        IntentMatchers.hasCategories(CoreMatchers.hasItem(Matchers.equalTo(Intent.CATEGORY_OPENABLE))),
        IntentMatchers.hasType(Constants.MIME_TYPE_PGP_KEY)
      )
    )
      .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultData))

    onView(withId(R.id.menuActionSave))
      .check(matches(isDisplayed()))
      .perform(click())

    isToastDisplayed(getResString(R.string.saved))
  }

  @Test
  fun testActionDeleteVisibility() {
    openActionBarOverflowOrOptionsMenu(getTargetContext())
    onView(withText(R.string.delete))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testActionHelp() {
    testHelpScreen()
  }

  companion object {
    private const val EMAIL_DENBOND7 = "denbond7@flowcrypt.test"
    private const val USER_DENBOND7 = "DenBond7"

    @AfterClass
    @JvmStatic
    fun removeContactFromDatabase() {
      val dao =
        FlowCryptRoomDatabase.getDatabase(ApplicationProvider.getApplicationContext())
          .recipientDao()
      dao.getRecipientByEmail(EMAIL_DENBOND7)?.let { dao.delete(it) }
    }
  }
}
