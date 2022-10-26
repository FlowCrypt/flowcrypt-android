/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.retrofit.response.model.DecryptErrorMsgBlock
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.matchers.CustomMatchers
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import com.flowcrypt.email.ui.base.BaseMessageDetailsFlowTest
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.UUID

/**
 * @author Denis Bondarenko
 *         Date: 6/7/21
 *         Time: 1:16 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class MessageDetailsPassphraseInRamFlowTest : BaseMessageDetailsFlowTest() {

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(AddPrivateKeyToDatabaseRule(passphraseType = KeyEntity.PassphraseType.RAM))
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  @Test
  fun testShowNeedPassphraseError() {
    val incomingMsgInfo = getMsgInfo(
      "messages/info/encrypted_msg_need_passphrase_single_key.json",
      "messages/mime/encrypted_msg_info_plain_text.txt"
    )

    launchActivity(incomingMsgInfo!!.msgEntity)

    //close a dialog
    onView(withText(getResString(R.string.cancel)))
      .inRoot(isDialog())
      .check(matches(isDisplayed()))
      .perform(click())

    val decryptErrorMsgBlock = incomingMsgInfo.msgBlocks?.get(1) as DecryptErrorMsgBlock
    val decryptError = decryptErrorMsgBlock.decryptErr!!
    assertEquals(
      PgpDecryptAndOrVerify.DecryptionErrorType.NEED_PASSPHRASE,
      decryptError.details?.type
    )

    //check error message
    val errorMsg = getResString(
      R.string.could_not_decrypt_message_due_to_error,
      decryptError.details?.type.toString() + ": " + decryptError.details?.message
    )
    onView(withText(errorMsg))
      .check(matches(isDisplayed()))
    onView(withText(getResString(R.string.fix)))
      .check(matches(isDisplayed()))
      .perform(click())

    //check that "fix" button show a dialog
    val tVStatusMessageText = getQuantityString(
      R.plurals.please_provide_passphrase_for_following_keys,
      1
    )
    onView(withText(tVStatusMessageText))
      .inRoot(isDialog())
      .check(matches(isDisplayed()))
  }

  @Test
  fun testNeedPassphraseDialogTypingText() {
    val incomingMsgInfo = getMsgInfo(
      "messages/info/encrypted_msg_need_passphrase_single_key.json",
      "messages/mime/encrypted_msg_info_plain_text.txt"
    )

    launchActivity(incomingMsgInfo!!.msgEntity)

    //test show toast when we try to check an empty passphrase
    onView(withId(R.id.eTKeyPassword))
      .inRoot(isDialog())
      .perform(clearText())

    onView(withId(R.id.btnUpdatePassphrase))
      .inRoot(isDialog())
      .perform(click())

    isToastDisplayed(getResString(R.string.passphrase_must_be_non_empty), 3500)

    onView(withId(R.id.eTKeyPassword))
      .inRoot(isDialog())
      .perform(clearText(), pressImeActionButton())

    isToastDisplayed(getResString(R.string.passphrase_must_be_non_empty), 3500)

    //test to check a wrong passphrase
    onView(withId(R.id.eTKeyPassword))
      .inRoot(isDialog())
      .perform(
        clearText(),
        replaceText(UUID.randomUUID().toString()),
        pressImeActionButton()
      )

    isToastDisplayed(getResString(R.string.password_is_incorrect), 3500)

    onView(withId(R.id.eTKeyPassword))
      .inRoot(isDialog())
      .perform(clearText(), replaceText(UUID.randomUUID().toString()))

    onView(withId(R.id.btnUpdatePassphrase))
      .inRoot(isDialog())
      .perform(click())

    isToastDisplayed(getResString(R.string.password_is_incorrect))
  }

  @Test
  fun testNeedPassphraseSingleFingerprint() {
    val decryptedInfo = TestGeneralUtil.getObjectFromJson(
      jsonPathInAssets = "messages/info/encrypted_msg_info_text.json",
      classOfT = IncomingMessageInfo::class.java
    )

    val incomingMsgInfo = getMsgInfo(
      "messages/info/encrypted_msg_need_passphrase_single_key.json",
      "messages/mime/encrypted_msg_info_plain_text.txt"
    )

    launchActivity(incomingMsgInfo!!.msgEntity)

    val decryptErrorMsgBlock = incomingMsgInfo.msgBlocks?.get(1) as DecryptErrorMsgBlock
    val fingerprint = decryptErrorMsgBlock.decryptErr?.fingerprints?.first()
    val fingerprintFormatted = GeneralUtil.doSectionsInText(
      originalString = fingerprint, groupSize = 4
    )

    val tVStatusMessageText = getQuantityString(
      R.plurals.please_provide_passphrase_for_following_keys,
      1
    )
    onView(withId(R.id.tVStatusMessage))
      .check(matches(withText(tVStatusMessageText)))

    onView(withId(R.id.rVKeys))
      .check(matches(isDisplayed()))
      .check(matches(CustomMatchers.withRecyclerViewItemCount(1)))

    onView(withText(fingerprintFormatted))
      .check(matches(isDisplayed()))

    onView(withId(R.id.eTKeyPassword))
      .inRoot(isDialog())
      .perform(
        clearText(),
        replaceText(TestConstants.DEFAULT_STRONG_PASSWORD),
        pressImeActionButton()
      )

    checkWebView(decryptedInfo)
  }

  @Test
  fun testNeedPassphraseMultiplyFingerprintsFirstKey() {
    val decryptedInfo = TestGeneralUtil.getObjectFromJson(
      jsonPathInAssets = "messages/info/encrypted_msg_info_for_2_keys_text.json",
      classOfT = IncomingMessageInfo::class.java
    )

    prepareAndCheckBaseInfoForMultiplyKeys()

    onView(withId(R.id.eTKeyPassword))
      .inRoot(isDialog())
      .perform(
        clearText(),
        replaceText(TestConstants.DEFAULT_STRONG_PASSWORD),
        pressImeActionButton()
      )

    checkWebView(decryptedInfo)
  }

  @Test
  fun testNeedPassphraseMultiplyFingerprintsSecondKey() {
    val decryptedInfo = TestGeneralUtil.getObjectFromJson(
      jsonPathInAssets = "messages/info/encrypted_msg_info_for_2_keys_text.json",
      classOfT = IncomingMessageInfo::class.java
    )

    prepareAndCheckBaseInfoForMultiplyKeys()

    onView(withId(R.id.eTKeyPassword))
      .inRoot(isDialog())
      .perform(
        clearText(),
        replaceText(TestConstants.DEFAULT_SECOND_STRONG_PASSWORD),
        pressImeActionButton()
      )

    checkWebView(decryptedInfo)
  }

  @Test
  fun testShowNeedPassphraseDialogWhenTryingToDownloadAttachment() {
    val encryptedAttInfo = TestGeneralUtil.getObjectFromJson(
      "messages/attachments/encrypted_att.json",
      AttachmentInfo::class.java
    )

    val msgInfo = getMsgInfo(
      "messages/info/encrypted_msg_info_text_with_one_att.json",
      "messages/mime/encrypted_msg_info_plain_text_with_one_att.txt", encryptedAttInfo
    )

    launchActivity(msgInfo!!.msgEntity)

    //close a dialog during start up
    onView(withText(getResString(R.string.cancel)))
      .inRoot(isDialog())
      .check(matches(isDisplayed()))
      .perform(click())

    //Click on the download button
    onView(withId(R.id.imageButtonDownloadAtt))
      .check(matches(isDisplayed()))
      .perform(click())

    //check that a dialog is displayed
    val tVStatusMessageText = getQuantityString(
      R.plurals.please_provide_passphrase_for_following_keys,
      1
    )
    onView(withText(tVStatusMessageText))
      .inRoot(isDialog())
      .check(matches(isDisplayed()))
  }

  private fun checkWebView(decryptedInfo: IncomingMessageInfo?) {
    //todo-denbond7 improve that in the future
    //we need that to wait while webview rendered content
    Thread.sleep(2000)
    checkWebViewText(decryptedInfo?.text)
  }

  private fun prepareAndCheckBaseInfoForMultiplyKeys() {
    val secondKeyDetails = PrivateKeysManager.getPgpKeyDetailsFromAssets(
      "pgp/default@flowcrypt.test_secondKey_prv_strong_second.asc"
    )
    PrivateKeysManager.saveKeyToDatabase(
      accountEntity = addAccountToDatabaseRule.account,
      pgpKeyDetails = secondKeyDetails,
      passphrase = TestConstants.DEFAULT_STRONG_PASSWORD,
      sourceType = KeyImportDetails.SourceType.EMAIL,
      passphraseType = KeyEntity.PassphraseType.RAM
    )

    val incomingMsgInfo = getMsgInfo(
      "messages/info/encrypted_msg_need_passphrase_multiply_keys.json",
      "messages/mime/encrypted_msg_for_2_keys_of_the_same_user.txt"
    )

    launchActivity(incomingMsgInfo!!.msgEntity)

    val decryptErrorMsgBlock = incomingMsgInfo.msgBlocks?.get(1) as DecryptErrorMsgBlock
    val expectedKeysCount = 2
    assertEquals(expectedKeysCount, decryptErrorMsgBlock.decryptErr?.fingerprints?.size)

    val tVStatusMessageText = getQuantityString(
      resId = R.plurals.please_provide_passphrase_for_following_keys,
      quantity = expectedKeysCount
    )
    onView(withId(R.id.tVStatusMessage))
      .check(matches(withText(tVStatusMessageText)))

    onView(withId(R.id.rVKeys))
      .check(matches(isDisplayed()))
      .check(matches(CustomMatchers.withRecyclerViewItemCount(expectedKeysCount)))
  }
}
