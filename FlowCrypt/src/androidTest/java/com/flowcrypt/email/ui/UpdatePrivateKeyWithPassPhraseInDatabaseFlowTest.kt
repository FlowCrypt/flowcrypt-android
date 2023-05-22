/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.AddRecipientsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.ui.activity.fragment.PrivateKeyDetailsFragmentArgs
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.Date

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class UpdatePrivateKeyWithPassPhraseInDatabaseFlowTest : BaseTest() {
  private val addAccountToDatabaseRule = AddAccountToDatabaseRule()
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule()

  override val useIntents: Boolean = true
  override val activityScenarioRule = activityScenarioRule<MainActivity>(
    TestGeneralUtil.genIntentForNavigationComponent(
      destinationId = R.id.privateKeyDetailsFragment,
      extras = PrivateKeyDetailsFragmentArgs(
        fingerprint = addPrivateKeyToDatabaseRule.pgpKeyDetails.fingerprint
      ).toBundle()
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(
      AddRecipientsToDatabaseRule(
        listOf(
          RecipientWithPubKeys(
            RecipientEntity(
              email = addAccountToDatabaseRule.account.email,
              name = "Default"
            ),
            listOf(
              addPrivateKeyToDatabaseRule.pgpKeyDetails
                .toPublicKeyEntity(addAccountToDatabaseRule.account.email)
                .copy(id = 1)
            )
          )
        )
      )
    )
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testUpdateSuccess() {
    val dateFormat = DateTimeUtil.getPgpDateFormat(getTargetContext())
    val originalKeyDetails = addPrivateKeyToDatabaseRule.pgpKeyDetails
    val updatedKeyDetails = PrivateKeysManager.getPgpKeyDetailsFromAssets(
      "pgp/default@flowcrypt.test_fisrtKey_prv_default_mod_06_17_2022.asc"
    )

    //check a key before update
    onView(
      withText(
        getResString(
          R.string.template_modification_date,
          dateFormat.format(Date(requireNotNull(originalKeyDetails.lastModified)))
        )
      )
    ).check(matches(isDisplayed()))

    val roomDatabase = FlowCryptRoomDatabase.getDatabase(getTargetContext())
    val recipientDao = roomDatabase.recipientDao()

    val existingRecipientWithPubKeysBeforeUpdate = runBlocking {
      recipientDao.getRecipientWithPubKeysByEmailSuspend(addAccountToDatabaseRule.account.email)
    }

    assertEquals(
      originalKeyDetails.lastModified,
      PgpKey.parseKeys(
        existingRecipientWithPubKeysBeforeUpdate?.publicKeys?.first()?.publicKey ?: byteArrayOf()
      ).pgpKeyDetailsList.first().lastModified
    )

    //open update screen
    onView(withId(R.id.btnUpdatePrivateKey))
      .check(matches(isDisplayed()))
      .perform(click())

    //type new private key
    onView(withId(R.id.editTextNewPrivateKey))
      .check(matches(isDisplayed()))
      .perform(replaceText(updatedKeyDetails.privateKey), closeSoftKeyboard())

    onView(withId(R.id.buttonCheck))
      .check(matches(isDisplayed()))
      .perform(click())

    //click on 'use this key'
    onView(withId(android.R.id.button1))
      .perform(click())

    //type pass phrase
    onView(withId(R.id.editTextKeyPassword))
      .perform(scrollTo(), replaceText(TestConstants.DEFAULT_PASSWORD), closeSoftKeyboard())
    onView(withId(R.id.buttonPositiveAction))
      .perform(scrollTo(), click())

    //do checks after update
    onView(
      withText(
        getResString(
          R.string.template_modification_date,
          dateFormat.format(Date(requireNotNull(updatedKeyDetails.lastModified)))
        )
      )
    ).check(matches(isDisplayed()))

    val existingRecipientWithPubKeysAfterUpdate = runBlocking {
      recipientDao.getRecipientWithPubKeysByEmailSuspend(addAccountToDatabaseRule.account.email)
    }
    assertEquals(
      updatedKeyDetails.lastModified,
      PgpKey.parseKeys(
        existingRecipientWithPubKeysAfterUpdate?.publicKeys?.first()?.publicKey ?: byteArrayOf()
      ).pgpKeyDetailsList.first().lastModified
    )
  }
}
