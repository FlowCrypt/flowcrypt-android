/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.ui.activity.settings.SettingsActivity
import com.flowcrypt.email.util.PrivateKeysManager
import org.hamcrest.Matchers
import org.junit.Before
import java.io.File

/**
 * @author Denis Bondarenko
 *         Date: 6/17/21
 *         Time: 5:26 PM
 *         E-mail: DenBond7@gmail.com
 */
abstract class BaseBackupKeysFragmentTest : BaseTest() {
  override val useIntents: Boolean = true
  override val activityScenarioRule = activityScenarioRule<SettingsActivity>()

  val addAccountToDatabaseRule = AddAccountToDatabaseRule()

  @Before
  fun goToBackupKeysFragment() {
    if (!useLazyInit) {
      goToBackupKeysFragmentInternal()
    }
  }

  protected fun goToBackupKeysFragmentInternal() {
    onView(withText(getResString(R.string.backups)))
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withId(R.id.btBackup))
      .check(matches(isDisplayed()))
      .perform(click())
  }

  protected fun intendingFileChoose(file: File) {
    val resultData = Intent()
    resultData.data = Uri.fromFile(file)
    Intents.intending(
      Matchers.allOf(
        IntentMatchers.hasAction(Intent.ACTION_CREATE_DOCUMENT),
        IntentMatchers.hasCategories(Matchers.hasItem(Matchers.equalTo(Intent.CATEGORY_OPENABLE))),
        IntentMatchers.hasType(Constants.MIME_TYPE_PGP_KEY)
      )
    )
      .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultData))
  }

  protected fun addFirstKeyWithDefaultPassword(
    passphraseType: KeyEntity.PassphraseType = KeyEntity.PassphraseType.DATABASE
  ) {
    PrivateKeysManager.saveKeyFromAssetsToDatabase(
      accountEntity = addAccountToDatabaseRule.account,
      keyPath = "pgp/default@flowcrypt.test_fisrtKey_prv_default.asc",
      passphrase = TestConstants.DEFAULT_PASSWORD,
      sourceType = KeyImportDetails.SourceType.EMAIL,
      passphraseType = passphraseType
    )
  }

  protected fun addSecondKeyWithStrongPassword(
    passphraseType: KeyEntity.PassphraseType = KeyEntity.PassphraseType.DATABASE
  ) {
    PrivateKeysManager.saveKeyFromAssetsToDatabase(
      accountEntity = addAccountToDatabaseRule.account,
      keyPath = TestConstants.DEFAULT_SECOND_KEY_PRV_STRONG,
      passphrase = TestConstants.DEFAULT_STRONG_PASSWORD,
      sourceType = KeyImportDetails.SourceType.EMAIL,
      passphraseType = passphraseType
    )
  }

  protected fun addSecondKeyWithStrongSecondPassword(
    passphraseType: KeyEntity.PassphraseType = KeyEntity.PassphraseType.DATABASE
  ) {
    PrivateKeysManager.saveKeyFromAssetsToDatabase(
      accountEntity = addAccountToDatabaseRule.account,
      keyPath = "pgp/default@flowcrypt.test_secondKey_prv_strong_second.asc",
      passphrase = TestConstants.DEFAULT_SECOND_STRONG_PASSWORD,
      sourceType = KeyImportDetails.SourceType.EMAIL,
      passphraseType = passphraseType
    )
  }
}
