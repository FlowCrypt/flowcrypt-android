/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intending
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
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddRecipientsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.ui.activity.fragment.PublicKeyDetailsFragmentArgs
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.junit.AfterClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.File
import java.util.Date

/**
 * @author Denys Bondarenko
 */
@FlowCryptTestSettings(useIntents = true)
@MediumTest
@RunWith(AndroidJUnit4::class)
class PublicKeyDetailsFlowTest : BaseTest() {
  private val keyDetails =
    PrivateKeysManager.getPgpKeyDetailsFromAssets("pgp/expired@flowcrypt.test_pub.asc")

  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      destinationId = R.id.publicKeyDetailsFragment,
      extras = PublicKeyDetailsFragmentArgs(
        recipientEntity = RecipientEntity(
          email = EMAIL_DENBOND7,
          name = USER_DENBOND7
        ),
        publicKeyEntity = keyDetails.toPublicKeyEntity(EMAIL_DENBOND7).copy(id = 12)
      ).toBundle()
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

    keyDetails.ids.forEach { keyId ->
      val text = GeneralUtil.doSectionsInText(
        originalString = keyId.fingerprint, groupSize = 4
      )

      onView(withText(text))
        .check(matches(isDisplayed()))
    }

    onView(withId(R.id.textViewPrimaryKeyAlgorithm))
      .check(matches(withText(keyDetails.algo.algorithm!! + "/" + keyDetails.algo.bits)))
    onView(withId(R.id.textViewPrimaryKeyCreated))
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
      File(getTargetContext().getExternalFilesDir(Constants.EXTERNAL_FILES_PATH_SHARED), fileName)

    if (file.exists()) {
      file.delete()
    }

    val resultData = Intent()
    resultData.data =
      FileProvider.getUriForFile(getTargetContext(), Constants.FILE_PROVIDER_AUTHORITY, file)

    intending(
      allOf(
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
