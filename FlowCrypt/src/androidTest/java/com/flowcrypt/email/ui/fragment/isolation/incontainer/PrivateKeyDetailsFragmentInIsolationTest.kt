/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
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
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.junit.annotations.NotReadyForCI
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.PrivateKeyDetailsFragment
import com.flowcrypt.email.ui.activity.fragment.PrivateKeyDetailsFragmentArgs
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.PrivateKeysManager
import org.hamcrest.Matchers.allOf
import org.junit.Before
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
class PrivateKeyDetailsFragmentInIsolationTest : BaseTest() {
  private val addAccountToDatabaseRule = AddAccountToDatabaseRule()
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule()
  private val dateFormat = DateTimeUtil.getPgpDateFormat(getTargetContext())

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(ScreenshotTestRule())

  @Before
  fun launchFragmentInContainerWithPredefinedArgs() {
    launchFragmentInContainer<PrivateKeyDetailsFragment>(
      fragmentArgs = PrivateKeyDetailsFragmentArgs(
        fingerprint = PrivateKeysManager
          .getPgpKeyDetailsFromAssets(addPrivateKeyToDatabaseRule.keyPath).fingerprint
      ).toBundle()
    )
  }

  @Test
  fun testKeyDetailsCheckDetails() {
    val details = addPrivateKeyToDatabaseRule.pgpKeyRingDetails

    onView(withId(R.id.textViewFingerprint))
      .check(
        matches(
          withText(
            getHtmlString(
              getResString(
                R.string.template_fingerprint,
                GeneralUtil.doSectionsInText(" ", details.fingerprint, 4)!!
              )
            )
          )
        )
      )

    onView(withId(R.id.textViewCreationDate))
      .check(
        matches(
          withText(
            getHtmlString(
              getResString(
                R.string.template_created,
                dateFormat.format(Date(details.created))
              )
            )
          )
        )
      )

    onView(withId(R.id.textViewExpirationDate))
      .check(
        matches(
          withText(
            getHtmlString(
              getResString(R.string.expires, getResString(R.string.never))
            )
          )
        )
      )

    onView(withId(R.id.tVPassPhraseVerification))
      .check(matches(withText(getResString(R.string.stored_pass_phrase_matched))))

    details.users.forEachIndexed { index, userId ->
      onView(withId(R.id.recyclerViewUserIds))
        .perform(
          RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
            withText(getResString(R.string.template_user, index + 1, userId))
          )
        )
    }
  }

  @Test
  fun testKeyDetailsCopyToClipBoard() {
    val details = addPrivateKeyToDatabaseRule.pgpKeyRingDetails
    onView(withId(R.id.btnCopyToClipboard))
      .check(matches(isDisplayed()))
      .perform(click())
    isToastDisplayed(getResString(R.string.copied))
    UiThreadStatement.runOnUiThread { checkClipboardText(details.publicKey) }
  }

  @Test
  @NotReadyForCI
  fun testKeyDetailsShowPrivateKey() {
    onView(withId(R.id.btnShowPrKey))
      .perform(scrollTo())
      .perform(click())
    isToastDisplayed(getResString(R.string.see_backups_to_save_your_private_keys))
  }

  @Test
  fun testKeyDetailsSavePubKeyToFileWhenFileIsNotExist() {
    val details = addPrivateKeyToDatabaseRule.pgpKeyRingDetails

    val file = File(
      getTargetContext().getExternalFilesDir(Constants.EXTERNAL_FILES_PATH_SHARED),
      "0x" + details.fingerprint + ".asc"
    )

    if (file.exists()) {
      file.delete()
    }

    val resultData = Intent()
    resultData.data =
      FileProvider.getUriForFile(getTargetContext(), Constants.FILE_PROVIDER_AUTHORITY, file)

    intending(
      allOf(
        hasAction(Intent.ACTION_CREATE_DOCUMENT),
        hasCategories(setOf(Intent.CATEGORY_OPENABLE)),
        hasType(Constants.MIME_TYPE_PGP_KEY)
      )
    ).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultData))

    onView(withId(R.id.btnSaveToFile))
      .check(matches(isDisplayed()))
      .perform(click())
    isToastDisplayed(getResString(R.string.saved))
  }
}
