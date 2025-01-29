/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasCategories
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.MessageEntity
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
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.OutgoingMessagesManager
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.runBlocking
import org.bouncycastle.bcpg.ArmoredInputStream
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertArrayEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.File
import java.util.Properties
import kotlin.random.Random

/**
 * https://github.com/FlowCrypt/flowcrypt-android/issues/1532
 */
@FlowCryptTestSettings(useIntents = true)
@MediumTest
@RunWith(AndroidJUnit4::class)
class PublicKeyDetailsHideArmorMetaFlowTest : BaseTest() {
  private val userWithClientConfiguration = AccountDaoManager.getUserWithClientConfiguration(
    ClientConfiguration(
      flags = listOf(
        ClientConfiguration.ConfigurationProperty.HIDE_ARMOR_META
      )
    )
  )
  private val addAccountToDatabaseRule = AddAccountToDatabaseRule(userWithClientConfiguration)
  private val recipientEntity = RecipientEntity(
    email = addAccountToDatabaseRule.account.email,
    name = addAccountToDatabaseRule.account.displayName
  )
  private val keyDetails =
    PrivateKeysManager.getPgpKeyDetailsFromAssets("pgp/default@flowcrypt.test_fisrtKey_pub.asc")
  private val publicKeyEntity =
    keyDetails.toPublicKeyEntity(addAccountToDatabaseRule.account.email).copy(id = 12)

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
      File(getTargetContext().getExternalFilesDir(Constants.EXTERNAL_FILES_PATH_SHARED), fileName)

    if (file.exists()) {
      file.delete()
    }

    val resultData = Intent()
    resultData.data =
      FileProvider.getUriForFile(getTargetContext(), Constants.FILE_PROVIDER_AUTHORITY, file)

    intending(
      allOf(
        hasAction(Intent.ACTION_CREATE_DOCUMENT),
        hasCategories(hasItem(equalTo(Intent.CATEGORY_OPENABLE))),
        hasType(Constants.MIME_TYPE_PGP_KEY)
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

  @Test
  fun testCreateMimeMessageWithHiddenUserAgentMimeField() {
    val generatedMimeMsg = runBlocking {
      val mimeMessage = MimeMessage(Session.getDefaultInstance(Properties())).apply {
        subject = "Test"
        setText("Some text")
        setFrom(InternetAddress(addAccountToDatabaseRule.account.email))
        setRecipients(Message.RecipientType.TO, arrayOf(InternetAddress("user@flowcrypt.test")))
      }
      val messageEntity = MessageEntity.genMsgEntity(
        account = addAccountToDatabaseRule.account.email,
        accountType = addAccountToDatabaseRule.account.accountType,
        label = JavaEmailConstants.FOLDER_OUTBOX,
        msg = mimeMessage,
        uid = 1,
        isNew = true,
        isEncrypted = false,
        hasAttachments = false
      ).copy(
        id = Random.nextLong()
      )

      OutgoingMessagesManager.enqueueOutgoingMessage(
        getTargetContext(),
        requireNotNull(messageEntity.id),
        mimeMessage
      )

      EmailUtil.createMimeMsg(
        context = getTargetContext(),
        sess = Session.getDefaultInstance(Properties()),
        msgEntity = messageEntity,
        atts = emptyList()
      )
    }

    assertArrayEquals(arrayOf(), generatedMimeMsg.getHeader("User-Agent") ?: arrayOf())
  }
}
