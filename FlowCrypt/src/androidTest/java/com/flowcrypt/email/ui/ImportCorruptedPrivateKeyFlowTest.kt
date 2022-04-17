/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.rules.lazyActivityScenarioRule
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import org.apache.commons.io.FilenameUtils
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 *         Date: 4/9/22
 *         Time: 3:46 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class ImportCorruptedPrivateKeyFlowTest : BaseTest() {
  override val activeActivityRule = lazyActivityScenarioRule<MainActivity>(launchActivity = false)
  override val activityScenario: ActivityScenario<*>?
    get() = activeActivityRule.scenario

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  /**
   * It refers to https://github.com/FlowCrypt/flowcrypt-android/issues/1669
   */
  @Test
  fun testImportCorruptedKey() {
    val keysPaths = arrayOf("pgp/keys/issue_1669_corrupted_prv.asc")
    launchActivity(keysPaths)
    typePassword()
    onView(
      withText(
        getResString(
          R.string.warning_when_import_invalid_prv_key,
          getResString(R.string.support_email)
        )
      )
    )
      .check(matches(isDisplayed()))
  }

  private fun launchActivity(keysPaths: Array<String>) {
    activeActivityRule.launch(genIntent(keysPaths, KeyImportDetails.SourceType.EMAIL))
    registerAllIdlingResources()
  }


  /**
   * Type a password and click on the "CONTINUE" button.
   *
   * @param password The input password.
   */
  private fun typePassword() {
    onView(withId(R.id.editTextKeyPassword))
      .perform(scrollTo(), typeText("123"), closeSoftKeyboard())
    onView(withId(R.id.buttonPositiveAction))
      .perform(scrollTo(), click())
  }

  private fun genIntent(
    keysPaths: Array<String>,
    sourceType: KeyImportDetails.SourceType = KeyImportDetails.SourceType.EMAIL
  ): Intent {
    val keyDetailsList = PrivateKeysManager.getKeysFromAssets(keysPaths, true)

    val bottomTitle: String
    when (sourceType) {
      KeyImportDetails.SourceType.FILE -> {
        assert(keysPaths.size == 1)
        val fileName = FilenameUtils.getName(keysPaths.first())
        bottomTitle = getQuantityString(
          R.plurals.file_contains_some_amount_of_keys,
          keyDetailsList.size, fileName, keyDetailsList.size
        )
      }
      else -> {
        bottomTitle = getQuantityString(
          R.plurals.found_backup_of_your_account_key,
          keyDetailsList.size, keyDetailsList.size
        )
      }
    }

    return TestGeneralUtil.genIntentForNavigationComponent(
      uri = "flowcrypt://email.flowcrypt.com/check_private_keys",
      extras = Bundle().apply {
        putParcelableArray("privateKeys", keyDetailsList.toTypedArray())
        putParcelable("sourceType", sourceType)
        putString("subTitle", bottomTitle)
        putString("positiveBtnTitle", getTargetContext().getString(R.string.continue_))
        putString("negativeBtnTitle", getTargetContext().getString(R.string.choose_another_key))
        putBoolean("isExtraImportOpt", sourceType != KeyImportDetails.SourceType.EMAIL)
        putInt(
          "initSubTitlePlurals",
          if (sourceType == KeyImportDetails.SourceType.FILE) {
            0
          } else {
            R.plurals.found_backup_of_your_account_key
          }
        )
      }
    )
  }
}
