/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.model.OrgRules
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddRecipientsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.ui.activity.fragment.PublicKeyDetailsFragmentArgs
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import org.bouncycastle.bcpg.ArmoredInputStream
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.hamcrest.core.AllOf
import org.junit.Assert.assertArrayEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.File

/**
 * https://github.com/FlowCrypt/flowcrypt-android/issues/1532
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class PublicKeyDetailsHideArmorMetaFlowTest : BaseTest() {
  private val userWithOrgRules = AccountDaoManager.getUserWithOrgRules(
    OrgRules(
      flags = listOf(
        OrgRules.DomainRule.HIDE_ARMOR_META
      )
    )
  )
  private val addAccountToDatabaseRule = AddAccountToDatabaseRule(userWithOrgRules)
  private val recipientEntity = RecipientEntity(
    email = addAccountToDatabaseRule.account.email,
    name = addAccountToDatabaseRule.account.displayName
  )
  private val keyDetails =
    PrivateKeysManager.getPgpKeyDetailsFromAssets("pgp/default@flowcrypt.test_fisrtKey_pub.asc")
  private val publicKeyEntity =
    keyDetails.toPublicKeyEntity(addAccountToDatabaseRule.account.email).copy(id = 12)

  override val useIntents: Boolean = true
  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      destinationId = R.id.publicKeyDetailsFragment,
      extras = PublicKeyDetailsFragmentArgs(
        recipientEntity,
        publicKeyEntity
      ).toBundle()
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(
      AddRecipientsToDatabaseRule(
        listOf(
          RecipientWithPubKeys(
            recipientEntity,
            listOf(publicKeyEntity)
          )
        )
      )
    )
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testActionSave() {
    val sanitizedEmail = addAccountToDatabaseRule.account.email.replace("[^a-z0-9]".toRegex(), "")
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

    val bytes = file.readBytes()
    val armoredInputStream = ArmoredInputStream(bytes.inputStream())
    assertArrayEquals(emptyArray(), armoredInputStream.armorHeaders ?: emptyArray())
  }
}
