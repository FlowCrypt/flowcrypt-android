/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.expiration
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.matchers.CustomMatchers.Companion.hasItem
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withAppBarLayoutBackgroundColor
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withChipsBackgroundColor
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.ui.adapter.RecipientChipRecyclerViewAdapter
import com.flowcrypt.email.ui.base.BaseComposeScreenTest
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import com.flowcrypt.email.util.UIUtil
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.apache.commons.io.FileUtils
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.emptyString
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.File
import java.net.HttpURLConnection
import java.time.Instant

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class ComposeScreenFlowTest : BaseComposeScreenTest() {
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule()
  private val temporaryFolderRule = TemporaryFolder()

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(temporaryFolderRule)
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  private val defaultMsgEncryptionType: MessageEncryptionType = MessageEncryptionType.ENCRYPTED

  private val pgpKeyRingDetails: PgpKeyRingDetails = PrivateKeysManager.getPgpKeyDetailsFromAssets(
    "pgp/not_attested_user@flowcrypt.test_prv_default.asc"
  )

  @Test
  fun testEmptyRecipient() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    onView(withId(R.id.recyclerViewChipsTo))
      .check(matches(isDisplayed()))
      .check(matches(withRecyclerViewItemCount(1)))

    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())
      .check(matches(isDisplayed()))

    onView(withText(getResString(R.string.add_recipient_to_send_message)))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testEmptyEmailSubject() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    onView(withId(R.id.editTextEmailAddress))
      .perform(
        typeText(TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER),
        pressImeActionButton()
      )
    onView(withId(R.id.editTextEmailSubject))
      .perform(scrollTo(), click(), typeText("subject"), clearText())
      .check(matches(withText(`is`(emptyString()))))
    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(
      withText(getResString(R.string.text_must_not_be_empty, getResString(R.string.prompt_subject)))
    ).check(matches(isDisplayed()))
  }

  @Test
  fun testEmptyEmailMsg() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    onView(withId(R.id.editTextEmailAddress))
      .perform(
        typeText(TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER),
        pressImeActionButton()
      )
    onView(withId(R.id.editTextEmailSubject))
      .check(matches(isDisplayed()))
      .perform(scrollTo(), click(), typeText(EMAIL_SUBJECT))
    onView(withId(R.id.editTextEmailMessage))
      .perform(scrollTo())
      .check(matches(withText(`is`(emptyString()))))
    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withText(getResString(R.string.your_message_must_be_non_empty)))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testUsingStandardMsgEncryptionType() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    if (defaultMsgEncryptionType != MessageEncryptionType.STANDARD) {
      openActionBarOverflowOrOptionsMenu(getTargetContext())
      onView(withText(R.string.switch_to_standard_email))
        .check(matches(isDisplayed()))
        .perform(click())
    }

    checkIsDisplayedStandardAttributes()
  }

  @Test
  fun testUsingSecureMsgEncryptionType() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    if (defaultMsgEncryptionType != MessageEncryptionType.ENCRYPTED) {
      openActionBarOverflowOrOptionsMenu(getTargetContext())
      onView(withText(R.string.switch_to_secure_email))
        .check(matches(isDisplayed()))
        .perform(click())
    }
    checkIsDisplayedEncryptedAttributes()
  }

  @Test
  fun testSwitchBetweenEncryptionTypes() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    val messageEncryptionType = defaultMsgEncryptionType

    if (messageEncryptionType == MessageEncryptionType.ENCRYPTED) {
      checkIsDisplayedEncryptedAttributes()
      openActionBarOverflowOrOptionsMenu(getTargetContext())
      onView(withText(R.string.switch_to_standard_email))
        .check(matches(isDisplayed()))
        .perform(click())
      checkIsDisplayedStandardAttributes()
    } else {
      checkIsDisplayedStandardAttributes()
      openActionBarOverflowOrOptionsMenu(getTargetContext())
      onView(withText(R.string.switch_to_secure_email))
        .check(matches(isDisplayed()))
        .perform(click())
      checkIsDisplayedEncryptedAttributes()
    }
  }

  @Test
  fun testIsScreenOfComposeNewMsg() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    onView(withText(R.string.compose))
      .check(matches(isDisplayed()))
    onView(withId(R.id.editTextFrom))
      .perform(scrollTo())
      .check(matches(withText(not(`is`(emptyString())))))
    onView(withId(R.id.recyclerViewChipsTo))
      .check(matches(isDisplayed()))
      .check(matches(withRecyclerViewItemCount(1)))
    onView(withId(R.id.editTextEmailSubject))
      .perform(scrollTo())
      .check(matches(withText(`is`(emptyString()))))
  }

  @Test
  fun testWrongFormatOfRecipientEmailAddress() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    val invalidEmailAddresses = arrayOf("test", "test@", "test@@flowcrypt.test", "@flowcrypt.test")

    for (invalidEmailAddress in invalidEmailAddresses) {
      onView(withId(R.id.editTextEmailAddress))
        .perform(
          clearText(),
          typeText(invalidEmailAddress),
          pressImeActionButton()
        )

      //after selecting typed text we check that new items were not added
      onView(withId(R.id.recyclerViewChipsTo))
        .check(matches(isDisplayed()))
        .check(matches(withRecyclerViewItemCount(1)))
    }
  }

  @Test
  fun testAddingAtts() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    onView(withId(R.id.editTextEmailAddress))
      .perform(
        clearText(),
        pressImeActionButton()
      )

    for (att in atts) {
      addAttAndCheck(att)
    }
  }

  @Test
  fun testMaxTotalAttachmentSize() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    onView(withId(R.id.editTextEmailAddress))
      .perform(
        clearText(),
        pressImeActionButton()
      )

    val fileWithBiggerSize = TestGeneralUtil.createFileWithGivenSize(
      Constants.MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES + 1024, temporaryFolderRule
    )
    addAttachment(fileWithBiggerSize)

    val sizeWarningMsg = getResString(
      R.string.template_warning_max_total_attachments_size,
      FileUtils.byteCountToDisplaySize(Constants.MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES)
    )

    onView(withText(sizeWarningMsg))
      .check(matches(isDisplayed()))

    val fileWithLowerSize = TestGeneralUtil.createFileWithGivenSize(
      Constants.MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES - 1024, temporaryFolderRule
    )
    addAttAndCheck(fileWithLowerSize)
  }

  @Test
  fun testDeletingAtts() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    onView(withId(R.id.editTextEmailAddress))
      .perform(
        clearText(),
        pressImeActionButton()
      )

    for (att in atts) {
      addAttAndCheck(att)
    }

    //Need to wait while the layout will be updated. Some emulators work fast and fail this place
    Thread.sleep(500)

    for (att in atts) {
      deleteAtt(att)
    }

    onView(withId(R.id.textViewAttachmentName))
      .check(doesNotExist())
  }

  @Test
  fun testSelectImportPublicKeyFromPopUp() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()
    intending(hasComponent(ComponentName(getTargetContext(), MainActivity::class.java)))
      .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    val email = requireNotNull(pgpKeyRingDetails.getPrimaryInternetAddress()?.address)
    fillInAllFields(email)

    onView(withId(R.id.recyclerViewChipsTo))
      .perform(
        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
          allOf(
            withText(email),
            withChipsBackgroundColor(
              getTargetContext(),
              RecipientChipRecyclerViewAdapter.CHIP_COLOR_RES_ID_NO_PUB_KEY
            )
          )
        )
      )

    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withText(R.string.import_their_public_key))
      .check(matches(isDisplayed()))
      .perform(click())

    addTextToClipboard("public key", pgpKeyRingDetails.publicKey)

    onView(withId(R.id.buttonLoadFromClipboard))
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withId(R.id.recyclerViewChipsTo))
      .perform(
        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
          allOf(
            withText(email),
            withChipsBackgroundColor(
              getTargetContext(),
              RecipientChipRecyclerViewAdapter.CHIP_COLOR_RES_ID_HAS_USABLE_PUB_KEY
            )
          )
        )
      )
  }

  @Test
  fun testSelectedStandardEncryptionTypeFromPopUp() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    fillInAllFields(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER)
    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withText(R.string.switch_to_standard_email))
      .check(matches(isDisplayed()))
      .perform(click())
    checkIsDisplayedStandardAttributes()
  }

  @Test
  fun testSelectedRemoveRecipientFromPopUp() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    onView(withId(R.id.editTextEmailAddress))
      .perform(
        typeText(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER),
        pressImeActionButton()
      )
    //move the focus to the next view
    onView(withId(R.id.editTextEmailMessage))
      .perform(
        scrollTo(), click(),
        typeText(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER), closeSoftKeyboard()
      )
    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(
      withText(
        getResString(
          R.string.template_remove_recipient,
          TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER
        )
      )
    )
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withId(R.id.recyclerViewChipsTo))
      .check(
        matches(
          not(hasItem(withText(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER)))
        )
      )
  }

  @Test
  @FlowCryptTestSettings(useCommonIdling = false)
  fun testSelectedCopyFromOtherContactFromPopUp() {
    activeActivityRule?.launch(intent)

    val pgpKeyRingDetails = PrivateKeysManager.getPgpKeyDetailsFromAssets(
      "pgp/attested_user@flowcrypt.test_prv_default_strong.asc"
    )

    pgpKeyRingDetails.toRecipientEntity()?.let {
      roomDatabase.recipientDao().insert(it)
      roomDatabase.pubKeyDao().insert(pgpKeyRingDetails.toPublicKeyEntity(it.email))
    }

    fillInAllFields(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER)

    Thread.sleep(2000)

    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withText(R.string.copy_from_other_contact))
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withId(R.id.recyclerViewContacts)).perform(
      RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
        hasDescendant(
          allOf(
            withId(R.id.tVOnlyEmail),
            withText(pgpKeyRingDetails.getPrimaryInternetAddress()?.address)
          )
        ),
        click()
      )
    )

    onView(withId(R.id.editTextEmailSubject))
      .perform(scrollTo(), click())

    Thread.sleep(2000)

    onView(withId(R.id.recyclerViewChipsTo))
      .perform(
        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
          allOf(
            withText(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER),
            withChipsBackgroundColor(
              getTargetContext(),
              RecipientChipRecyclerViewAdapter.CHIP_COLOR_RES_ID_HAS_USABLE_PUB_KEY
            )
          )
        )
      )
    isToastDisplayed(getResString(R.string.key_successfully_copied))
  }

  @Test
  fun testSharePubKeySingle() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    openActionBarOverflowOrOptionsMenu(getTargetContext())
    onView(withText(R.string.include_public_key))
      .check(matches(isDisplayed()))
      .perform(click())

    val att = EmailUtil.genAttInfoFromPubKey(
      addPrivateKeyToDatabaseRule.pgpKeyRingDetails,
      addPrivateKeyToDatabaseRule.accountEntity.email
    )

    onView(withText(att?.name))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testSharePubKeyMultiply() {
    val secondKeyDetails =
      PrivateKeysManager.getPgpKeyDetailsFromAssets(TestConstants.DEFAULT_SECOND_KEY_PRV_STRONG)
    PrivateKeysManager.saveKeyToDatabase(
      addAccountToDatabaseRule.account, secondKeyDetails,
      TestConstants.DEFAULT_STRONG_PASSWORD, KeyImportDetails.SourceType.EMAIL
    )
    val att =
      EmailUtil.genAttInfoFromPubKey(secondKeyDetails, addAccountToDatabaseRule.account.email)

    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    openActionBarOverflowOrOptionsMenu(getTargetContext())
    onView(withText(R.string.include_public_key))
      .check(matches(isDisplayed()))
      .perform(click())

    onData(withPubKeyName(att?.name))
      .inAdapterView(withId(R.id.listViewKeys))
      .perform(click())

    onView(withId(R.id.buttonOk))
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withText(att?.name))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testSharePubKeyNoOwnKeys() {
    PrivateKeysManager.deleteKey(
      addAccountToDatabaseRule.account,
      addPrivateKeyToDatabaseRule.keyPath
    )
    val keyDetails = PrivateKeysManager.getPgpKeyDetailsFromAssets(
      "pgp/key_testing@flowcrypt.test_keyB_default.asc"
    )
    PrivateKeysManager.saveKeyToDatabase(
      addAccountToDatabaseRule.account, keyDetails,
      TestConstants.DEFAULT_PASSWORD, KeyImportDetails.SourceType.EMAIL
    )

    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    openActionBarOverflowOrOptionsMenu(getTargetContext())
    onView(withText(R.string.include_public_key))
      .check(matches(isDisplayed()))
      .perform(click())

    val att = EmailUtil.genAttInfoFromPubKey(keyDetails, addAccountToDatabaseRule.account.email)

    onView(withText(att?.name))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testShowWarningIfFoundExpiredKey() {
    val keyDetails =
      PrivateKeysManager.getPgpKeyDetailsFromAssets("pgp/expired@flowcrypt.test_pub.asc")
    val email = requireNotNull(keyDetails.getPrimaryInternetAddress()).address
    val personal = requireNotNull(keyDetails.getPrimaryInternetAddress()).personal
    roomDatabase.recipientDao().insert(requireNotNull(keyDetails.toRecipientEntity()))
    roomDatabase.pubKeyDao().insert(keyDetails.toPublicKeyEntity(email))

    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    fillInAllFields(email)

    onView(withId(R.id.recyclerViewChipsTo))
      .perform(
        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
          allOf(
            withText(personal),
            withChipsBackgroundColor(
              getTargetContext(),
              RecipientChipRecyclerViewAdapter.CHIP_COLOR_RES_ID_HAS_PUB_KEY_BUT_EXPIRED
            )
          )
        )
      )

    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withText(R.string.warning_one_of_recipients_has_expired_pub_key))
      .check(matches(isDisplayed()))
      .perform(click())
  }

  @Test
  //fun testShowWarningIfFoundNotUsableKeySHA1() {
  fun testAcceptIfFoundKeySHA1() {
    val keyWithSHA1Algo =
      TestGeneralUtil.readFileFromAssetsAsByteArray("pgp/sha1@flowcrypt.test_pub.asc")
    val email = "sha1@flowcrypt.test"
    roomDatabase.recipientDao().insert(RecipientEntity(email = email))
    roomDatabase.pubKeyDao().insert(
      PublicKeyEntity(
        recipient = email,
        fingerprint = "5DE92AB364B3100D89FBF460241512660BDDC426",
        publicKey = keyWithSHA1Algo
      )
    )

    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    fillInAllFields(email)

    onView(withId(R.id.recyclerViewChipsTo))
      .perform(
        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
          allOf(
            withText(email),
            withChipsBackgroundColor(
              getTargetContext(),
              RecipientChipRecyclerViewAdapter.CHIP_COLOR_RES_ID_HAS_USABLE_PUB_KEY
            )
          )
        )
      )

    /*temporary disabled due too https://github.com/FlowCrypt/flowcrypt-android/issues/1478
    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withText(R.string.warning_one_of_pub_keys_is_not_usable))
      .check(matches(isDisplayed()))
      .perform(click())*/
  }

  @Test
  fun testKeepPublicKeysFresh() {
    val keyDetailsFromAssets =
      PrivateKeysManager.getPgpKeyDetailsFromAssets("pgp/expired_fixed@flowcrypt.test_expired_pub.asc")
    val internetAddress = requireNotNull(keyDetailsFromAssets.getPrimaryInternetAddress())
    val recipientEntity = keyDetailsFromAssets.toRecipientEntity()
    roomDatabase.recipientDao().insert(requireNotNull(recipientEntity))
    roomDatabase.pubKeyDao().insert(
      requireNotNull(keyDetailsFromAssets.toPublicKeyEntity(recipientEntity.email))
    )
    val existingRecipient =
      roomDatabase.recipientDao().getRecipientWithPubKeysByEmail(internetAddress.address)
        ?: throw IllegalArgumentException("Contact not found")

    val existingKeyExpiration =
      PgpKey.parseKeys(String(existingRecipient.publicKeys.first().publicKey))
        .pgpKeyRingCollection.pgpPublicKeyRingCollection.first().expiration
        ?: throw IllegalArgumentException("No expiration date")

    assertTrue(existingKeyExpiration.isBefore(Instant.now()))

    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    fillInAllFields(internetAddress.address)

    onView(withId(R.id.recyclerViewChipsTo))
      .perform(
        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
          allOf(
            withText(internetAddress.personal),
            withChipsBackgroundColor(
              getTargetContext(),
              R.color.colorPrimary
            )
          )
        )
      )
  }

  @Test
  fun testWebPortalPasswordButtonIsVisibleForUserWithoutCustomerFesUrl() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    onView(withId(R.id.editTextEmailAddress))
      .perform(
        typeText(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER),
        pressImeActionButton()
      )

    //need to leave focus from 'To' field. move the focus to the next view
    onView(withId(R.id.editTextEmailSubject))
      .perform(scrollTo(), click())

    //for users with account.useCustomerFesUrl == false btnSetWebPortalPassword should be visible too
    onView(withId(R.id.btnSetWebPortalPassword))
      .check(matches(isDisplayed()))
  }

  private fun checkIsDisplayedEncryptedAttributes() {
    onView(withId(R.id.underToolbarTextTextView))
      .check(matches(not(isDisplayed())))
    onView(withId(R.id.appBarLayout))
      .check(
        matches(
          withAppBarLayoutBackgroundColor(
            UIUtil.getColor(getTargetContext(), R.color.colorPrimary)
          )
        )
      )
  }

  private fun deleteAtt(att: File) {
    onView(
      allOf(
        withId(R.id.imageButtonDeleteAtt), withParent(
          allOf(withId(R.id.actionButtons), hasSibling(withText(att.name)))
        )
      )
    )
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withText(att.name))
      .check(doesNotExist())
  }

  private fun addAttAndCheck(att: File) {
    addAttachment(att)
    onView(withText(att.name))
      .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
  }

  private fun checkIsDisplayedStandardAttributes() {
    onView(withId(R.id.underToolbarTextTextView))
      .check(matches(isDisplayed()))
      .check(matches(withText(R.string.this_message_will_not_be_encrypted)))
    onView(withId(R.id.appBarLayout))
      .check(
        matches(
          withAppBarLayoutBackgroundColor(
            UIUtil.getColor(getTargetContext(), R.color.red)
          )
        )
      )
  }

  /**
   * Match an item in an adapter which has the given name
   */
  private fun withPubKeyName(attName: String?): Matcher<Any> {
    return object : BoundedMatcher<Any, AttachmentInfo>(AttachmentInfo::class.java) {
      public override fun matchesSafely(att: AttachmentInfo): Boolean {
        return att.name == attName
      }

      override fun describeTo(description: Description) {
        description.appendText("with item content: ")
      }
    }
  }

  companion object {

    private const val ATTACHMENTS_COUNT = 3
    private const val EMAIL_SUBJECT = "Test subject"

    private var atts: MutableList<File> = mutableListOf()

    @get:ClassRule
    @JvmStatic
    val temporaryFolderRule = TemporaryFolder()

    @get:ClassRule
    @JvmStatic
    val mockWebServerRule = FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT,
      object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
          if (request.path?.startsWith("/attester/pub", ignoreCase = true) == true) {
            val lastSegment = request.requestUrl?.pathSegments?.lastOrNull()

            when {
              TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER.equals(
                lastSegment, true
              ) -> {
                return MockResponse()
                  .setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
                  .setBody(TestGeneralUtil.readResourceAsString("2.txt"))
              }

              TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER.equals(
                lastSegment, true
              ) -> {
                return MockResponse()
                  .setResponseCode(HttpURLConnection.HTTP_OK)
                  .setBody(TestGeneralUtil.readResourceAsString("3.txt"))
              }

              "expired_fixed@flowcrypt.test".equals(lastSegment, true) -> {
                return MockResponse()
                  .setResponseCode(HttpURLConnection.HTTP_OK)
                  .setBody(
                    TestGeneralUtil.readFileFromAssetsAsString(
                      "pgp/expired_fixed@flowcrypt.test_not_expired_pub.asc"
                    )
                  )
              }
            }
          }

          return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
        }
      })

    @BeforeClass
    @JvmStatic
    fun setUp() {
      createFilesForCommonAtts()
    }

    private fun createFilesForCommonAtts() {
      for (i in 0 until ATTACHMENTS_COUNT) {
        atts.add(
          TestGeneralUtil.createFileWithTextContent(
            temporaryFolderRule,
            "$i.txt", "Text for filling the attached file"
          )
        )
      }
    }
  }
}
