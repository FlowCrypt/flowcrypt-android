/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasCategories
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withAppBarLayoutBackgroundColor
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withChipsBackgroundColor
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.ui.activity.base.BaseCreateMessageActivityTest
import com.flowcrypt.email.ui.widget.CustomChipSpanChipCreator
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import com.flowcrypt.email.util.UIUtil
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.apache.commons.io.FileUtils
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.isEmptyString
import org.hamcrest.Matchers.not
import org.junit.Assert
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
 * A test for [CreateMessageActivity]. By default, this test describes running an activity with type
 * [MessageType.NEW] and empty [IncomingMessageInfo]
 *
 * @author Denis Bondarenko
 * Date: 06.02.2018
 * Time: 10:11
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class CreateMessageActivityTest : BaseCreateMessageActivityTest() {
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule()
  private val temporaryFolderRule = TemporaryFolder()

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(ClearAppSettingsRule())
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(temporaryFolderRule)
    .around(RetryRule.DEFAULT)
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  private val defaultMsgEncryptionType: MessageEncryptionType = MessageEncryptionType.ENCRYPTED

  private val pgpContact: PgpContact
    get() {
      val details = PrivateKeysManager.getPgpKeyDetailsFromAssets(
        "pgp/not_attester_user@flowcrypt.test_prv_default.asc"
      )
      return details.primaryPgpContact
    }

  @Test
  fun testEmptyRecipient() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()
    onView(withId(R.id.editTextRecipientTo))
      .check(matches(withText(isEmptyString())))
    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(
      withText(
        getResString(R.string.text_must_not_be_empty, getResString(R.string.prompt_recipients_to))
      )
    )
      .check(matches(isDisplayed()))
  }

  @Test
  fun testEmptyEmailSubject() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    onView(withId(R.id.layoutTo))
      .perform(scrollTo())
    onView(withId(R.id.editTextRecipientTo))
      .perform(typeText(TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER))
    onView(withId(R.id.editTextEmailSubject))
      .perform(scrollTo(), click(), typeText("subject"), clearText())
      .check(matches(withText(isEmptyString())))
    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(
      withText(
        getResString(
          R.string.text_must_not_be_empty,
          getResString(R.string.prompt_subject)
        )
      )
    )
      .check(matches(isDisplayed()))
  }

  @Test
  fun testEmptyEmailMsg() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    onView(withId(R.id.layoutTo))
      .perform(scrollTo())
    onView(withId(R.id.editTextRecipientTo))
      .perform(typeText(TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER))
    onView(withId(R.id.editTextEmailSubject))
      .check(matches(isDisplayed()))
      .perform(scrollTo(), click(), typeText(EMAIL_SUBJECT))
    onView(withId(R.id.editTextEmailMessage))
      .perform(scrollTo())
      .check(matches(withText(isEmptyString())))
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
  fun testShowHelpScreen() {
    activeActivityRule?.launch(intent)
  }

  @Test
  fun testIsScreenOfComposeNewMsg() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    onView(withText(R.string.compose))
      .check(matches(isDisplayed()))
    onView(withId(R.id.editTextFrom))
      .perform(scrollTo())
      .check(matches(withText(not(isEmptyString()))))
    onView(withId(R.id.layoutTo))
      .perform(scrollTo())
    onView(withId(R.id.editTextRecipientTo))
      .check(matches(withText(isEmptyString())))
    onView(withId(R.id.editTextEmailSubject))
      .perform(scrollTo())
      .check(matches(withText(isEmptyString())))
  }

  @Test
  fun testWrongFormatOfRecipientEmailAddress() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    val invalidEmailAddresses = arrayOf("test", "test@", "test@@flowcrypt.test", "@flowcrypt.test")

    for (invalidEmailAddress in invalidEmailAddresses) {
      onView(withId(R.id.layoutTo))
        .perform(scrollTo())
      onView(withId(R.id.editTextRecipientTo))
        .perform(clearText(), typeText(invalidEmailAddress), closeSoftKeyboard())
      onView(withId(R.id.menuActionSend))
        .check(matches(isDisplayed()))
        .perform(click())
      onView(withText(getResString(R.string.error_some_email_is_not_valid, invalidEmailAddress)))
        .check(matches(isDisplayed()))
      onView(withId(com.google.android.material.R.id.snackbar_action))
        .check(matches(isDisplayed()))
        .perform(click())
    }
  }

  @Test
  fun testAddingAtts() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    onView(withId(R.id.layoutTo))
      .perform(scrollTo())
    onView(withId(R.id.editTextRecipientTo))
      .perform(closeSoftKeyboard())

    for (att in atts) {
      addAttAndCheck(att)
    }
  }

  @Test
  fun testMaxTotalAttachmentSize() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    onView(withId(R.id.layoutTo))
      .perform(scrollTo())
    onView(withId(R.id.editTextRecipientTo))
      .perform(closeSoftKeyboard())

    val fileWithBiggerSize = TestGeneralUtil.createFileWithGivenSize(
      Constants.MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES + 1024, temporaryFolderRule
    )
    addAtt(fileWithBiggerSize)

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

    onView(withId(R.id.layoutTo))
      .perform(scrollTo())
    onView(withId(R.id.editTextRecipientTo))
      .perform(closeSoftKeyboard())

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
    intending(hasComponent(ComponentName(getTargetContext(), ImportPublicKeyActivity::class.java)))
      .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

    fillInAllFields(pgpContact.email)

    //check that we show the right background for a chip
    onView(withId(R.id.editTextRecipientTo))
      .check(
        matches(
          withChipsBackgroundColor(
            chipText = pgpContact.email,
            backgroundColor = UIUtil.getColor(
              context = getTargetContext(),
              colorResourcesId = CustomChipSpanChipCreator.CHIP_COLOR_RES_ID_PGP_NOT_EXISTS
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

    val database = FlowCryptRoomDatabase.getDatabase(getTargetContext())
    val existedContact =
      requireNotNull(database.recipientDao().getRecipientByEmail(pgpContact.email))
    database.recipientDao().update(
      existedContact.copy(
        publicKey = pgpContact.pubkey?.toByteArray(),
        hasPgp = true
      )
    )

    //move focus to request the field updates
    onView(withId(R.id.editTextRecipientTo))
      .perform(scrollTo(), click())
    onView(withId(R.id.editTextEmailSubject))
      .perform(scrollTo(), click())
    onView(withId(R.id.editTextRecipientTo))
      .check(
        matches(
          withChipsBackgroundColor(
            chipText = pgpContact.email,
            backgroundColor = UIUtil.getColor(
              context = getTargetContext(),
              colorResourcesId = CustomChipSpanChipCreator.CHIP_COLOR_RES_ID_PGP_EXISTS
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

    onView(withId(R.id.layoutTo))
      .perform(scrollTo())
    onView(withId(R.id.editTextRecipientTo))
      .perform(
        typeText(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER),
        closeSoftKeyboard()
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
    onView(withId(R.id.layoutTo))
      .perform(scrollTo())
    onView(withId(R.id.editTextRecipientTo))
      .check(matches(isDisplayed()))
      .check(
        matches(
          withText(
            not(
              containsString(
                TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER
              )
            )
          )
        )
      )
  }

  @Test
  fun testSelectedCopyFromOtherContactFromPopUp() {
    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    fillInAllFields(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER)
    val result = Intent()
    result.putExtra(SelectContactsActivity.KEY_EXTRA_PGP_CONTACT, pgpContact.toRecipientEntity())
    intending(hasComponent(ComponentName(getTargetContext(), SelectContactsActivity::class.java)))
      .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, result))
    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withText(R.string.copy_from_other_contact))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withId(R.id.editTextRecipientTo))
      .check(
        matches(
          withChipsBackgroundColor(
            chipText = TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER,
            backgroundColor = UIUtil.getColor(
              context = getTargetContext(),
              colorResourcesId = CustomChipSpanChipCreator.CHIP_COLOR_RES_ID_PGP_EXISTS
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

    val att = EmailUtil.genAttInfoFromPubKey(addPrivateKeyToDatabaseRule.pgpKeyDetails)

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
    val att = EmailUtil.genAttInfoFromPubKey(secondKeyDetails)

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

    val att = EmailUtil.genAttInfoFromPubKey(keyDetails)

    onView(withText(att?.name))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testShowWarningIfFoundExpiredKey() {
    val keyDetails =
      PrivateKeysManager.getPgpKeyDetailsFromAssets("pgp/expired@flowcrypt.test_pub.asc")
    val contact = keyDetails.primaryPgpContact
    FlowCryptRoomDatabase.getDatabase(getTargetContext())
      .recipientDao().insert(contact.toRecipientEntity())

    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    fillInAllFields(contact.email)

    onView(withId(R.id.editTextRecipientTo))
      .check(
        matches(
          withChipsBackgroundColor(
            contact.email,
            UIUtil.getColor(
              getTargetContext(),
              CustomChipSpanChipCreator.CHIP_COLOR_RES_ID_PGP_EXISTS_KEY_EXPIRED
            )
          )
        )
      )

    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withText(R.string.warning_one_of_pub_keys_is_expired))
      .check(matches(isDisplayed()))
      .perform(click())
  }

  @Test
  //fun testShowWarningIfFoundNotUsableKeySHA1() {
  fun testAcceptIfFoundKeySHA1() {
    val keyWithSHA1Algo =
      TestGeneralUtil.readFileFromAssetsAsString("pgp/sha1@flowcrypt.test_pub.asc")
    val contact = PgpContact(
      email = "sha1@flowcrypt.test",
      hasPgp = true,
      fingerprint = "5DE92AB364B3100D89FBF460241512660BDDC426",
      pubkey = keyWithSHA1Algo
    )
    FlowCryptRoomDatabase.getDatabase(getTargetContext())
      .recipientDao().insert(contact.toRecipientEntity())

    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    fillInAllFields(contact.email)

    onView(withId(R.id.editTextRecipientTo))
      .check(
        matches(
          withChipsBackgroundColor(
            contact.email,
            UIUtil.getColor(
              getTargetContext(),
              CustomChipSpanChipCreator.CHIP_COLOR_RES_ID_PGP_EXISTS
            )
          )
        )
      )

    /*
    temporary disabled due too https://github.com/FlowCrypt/flowcrypt-android/issues/1478
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
    val contact = keyDetailsFromAssets.primaryPgpContact
    val recipientDao = FlowCryptRoomDatabase.getDatabase(getTargetContext()).recipientDao()
    recipientDao.insert(contact.toRecipientEntity())
    val existedContact = recipientDao.getRecipientByEmail(contact.email)
      ?: throw IllegalArgumentException("Contact not found")

    val existedKeyExpiration = PgpKey.parseKeys(
      existedContact.publicKey ?: throw IllegalArgumentException("Empty pub key")
    )
      .pgpKeyRingCollection.pgpPublicKeyRingCollection.first().expiration
      ?: throw IllegalArgumentException("No expiration date")

    Assert.assertTrue(existedKeyExpiration.isBefore(Instant.now()))

    activeActivityRule?.launch(intent)
    registerAllIdlingResources()

    fillInAllFields(contact.email)

    onView(withId(R.id.editTextRecipientTo))
      .check(
        matches(
          withChipsBackgroundColor(
            contact.email,
            UIUtil.getColor(getTargetContext(), R.color.colorPrimary)
          )
        )
      )
  }

  private fun checkIsDisplayedEncryptedAttributes() {
    onView(withId(R.id.underToolbarTextTextView))
      .check(doesNotExist())
    onView(withId(R.id.appBarLayout))
      .check(
        matches(
          withAppBarLayoutBackgroundColor(
            UIUtil.getColor(getTargetContext(), R.color.colorPrimary)
          )
        )
      )
  }

  private fun savePublicKeyInDatabase() {
    FlowCryptRoomDatabase.getDatabase(getTargetContext()).recipientDao()
      .insert(pgpContact.toRecipientEntity())
  }

  private fun deleteAtt(att: File) {
    onView(
      allOf(
        withId(R.id.imageButtonClearAtt), ViewMatchers.withParent(
          allOf(withId(R.id.actionButtons), hasSibling(withText(att.name)))
        )
      )
    )
      .check(matches(isDisplayed()))
      .perform(click())
    onView(withText(att.name))
      .check(doesNotExist())
  }

  private fun addAtt(att: File) {
    val intent = TestGeneralUtil.genIntentWithPersistedReadPermissionForFile(att)
    intending(
      allOf(
        hasAction(Intent.ACTION_CHOOSER),
        hasExtra(
          `is`(Intent.EXTRA_INTENT),
          allOf(
            hasAction(Intent.ACTION_OPEN_DOCUMENT),
            hasType("*/*"),
            hasCategories(hasItem(equalTo(Intent.CATEGORY_OPENABLE)))
          )
        )
      )
    ).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, intent))
    onView(withId(R.id.menuActionAttachFile))
      .check(matches(isDisplayed()))
      .perform(click())
  }

  private fun addAttAndCheck(att: File) {
    addAtt(att)
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
          if (request.path?.startsWith("/pub", ignoreCase = true) == true) {
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

              "95FC072E853C9C333C68EDD34B9CA2FBCA5B5FE7".equals(lastSegment, true) -> {
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
          TestGeneralUtil.createFileAndFillWithContent(
            temporaryFolderRule,
            "$i.txt", "Text for filling the attached file"
          )
        )
      }
    }
  }
}
