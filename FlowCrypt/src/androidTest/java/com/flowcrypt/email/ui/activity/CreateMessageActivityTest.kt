/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.view.View
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
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withChild
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.flowcrypt.email.DoesNotNeedMailserver
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.dao.source.ContactsDaoSource
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withAppBarLayoutBackgroundColor
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import com.flowcrypt.email.util.UIUtil
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.isEmptyString
import org.hamcrest.Matchers.not
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.File

/**
 * A test for [CreateMessageActivity]. By default, this test describes running an activity with type
 * [MessageType.NEW] and empty [IncomingMessageInfo]
 *
 * @author Denis Bondarenko
 * Date: 06.02.2018
 * Time: 10:11
 * E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@DoesNotNeedMailserver
class CreateMessageActivityTest : BaseTest() {

  override val activityTestRule: ActivityTestRule<*>? =
      IntentsTestRule(CreateMessageActivity::class.java, false, false)

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(AddAccountToDatabaseRule())
      .around(AddPrivateKeyToDatabaseRule())
      .around(activityTestRule)

  private val intent: Intent = CreateMessageActivity.generateIntent(getTargetContext(), null,
      MessageEncryptionType.ENCRYPTED)
  private val defaultMsgEncryptionType: MessageEncryptionType = MessageEncryptionType.ENCRYPTED

  private val pgpContact: PgpContact
    get() {
      val details =
          PrivateKeysManager.getNodeKeyDetailsFromAssets("node/not_attester_user@denbond7.com_prv_default.json")
      return details.primaryPgpContact
    }

  @Test
  fun testEmptyRecipient() {
    activityTestRule?.launchActivity(intent)
    registerNodeIdling()
    onView(withId(R.id.editTextRecipientTo))
        .check(matches(isDisplayed())).check(matches(withText(isEmptyString())))
    onView(withId(R.id.menuActionSend))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(withText(getResString(R.string.text_must_not_be_empty, getResString(R.string.prompt_recipients_to))))
        .check(matches(isDisplayed()))
  }

  @Test
  fun testEmptyEmailSubject() {
    activityTestRule?.launchActivity(intent)
    registerNodeIdling()

    onView(withId(R.id.editTextRecipientTo))
        .check(matches(isDisplayed()))
        .perform(typeText(TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER))
    onView(withId(R.id.editTextEmailSubject))
        .check(matches(isDisplayed()))
        .perform(scrollTo(), typeText("subject"), clearText())
        .check(matches(withText(isEmptyString())))
    onView(withId(R.id.menuActionSend))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(withText(getResString(R.string.text_must_not_be_empty, getResString(R.string.prompt_subject))))
        .check(matches(isDisplayed()))
  }

  @Test
  fun testEmptyEmailMsg() {
    activityTestRule?.launchActivity(intent)
    registerNodeIdling()

    onView(withId(R.id.editTextRecipientTo))
        .check(matches(isDisplayed()))
        .perform(typeText(TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER))
    onView(withId(R.id.editTextEmailSubject))
        .check(matches(isDisplayed()))
        .perform(typeText(EMAIL_SUBJECT))
    onView(withId(R.id.editTextEmailMessage))
        .check(matches(isDisplayed())).check(matches(withText(isEmptyString())))
    onView(withId(R.id.menuActionSend))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(withText(getResString(R.string.your_message_must_be_non_empty)))
        .check(matches(isDisplayed()))
  }

  @Test
  fun testUsingStandardMsgEncryptionType() {
    activityTestRule?.launchActivity(intent)
    registerNodeIdling()

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
    activityTestRule?.launchActivity(intent)
    registerNodeIdling()

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
    activityTestRule?.launchActivity(intent)
    registerNodeIdling()

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
    activityTestRule?.launchActivity(intent)
    registerNodeIdling()

    openActionBarOverflowOrOptionsMenu(getTargetContext())
    onView(withText(R.string.help))
        .check(matches(isDisplayed()))
        .perform(click())

    onView(withId(R.id.textViewAuthorHint))
        .check(matches(isDisplayed()))
        .check(matches(withText(R.string.i_will_usually_reply_within_an_hour_except_when_i_sleep_tom)))
    onView(withText(R.string.help_feedback_or_question))
        .check(matches(isDisplayed()))
  }

  @Test
  fun testIsScreenOfComposeNewMsg() {
    activityTestRule?.launchActivity(intent)
    registerNodeIdling()

    onView(withText(R.string.compose))
        .check(matches(isDisplayed()))
    onView(withId(R.id.editTextFrom))
        .check(matches(isDisplayed())).check(matches(withText(not(isEmptyString()))))
    onView(withId(R.id.editTextRecipientTo))
        .check(matches(isDisplayed())).check(matches(withText(isEmptyString())))
    onView(withId(R.id.editTextEmailSubject))
        .check(matches(isDisplayed())).check(matches(withText(isEmptyString())))
  }

  @Test
  fun testWrongFormatOfRecipientEmailAddress() {
    activityTestRule?.launchActivity(intent)
    registerNodeIdling()

    val invalidEmailAddresses = arrayOf("test", "test@", "test@@denbond7.com", "@denbond7.com")

    for (invalidEmailAddress in invalidEmailAddresses) {
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
  fun testShowMsgAboutUpdateRecipientInformation() {
    activityTestRule?.launchActivity(intent)
    registerNodeIdling()

    onView(withId(R.id.editTextEmailSubject))
        .check(matches(isDisplayed()))
        .perform(typeText(EMAIL_SUBJECT), closeSoftKeyboard())
    onView(withId(R.id.editTextEmailMessage))
        .check(matches(isDisplayed()))
        .perform(typeText(EMAIL_MESSAGE), closeSoftKeyboard())
    onView(withId(R.id.editTextRecipientTo))
        .check(matches(isDisplayed()))
        .perform(typeText(TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER))
    onView(withId(R.id.menuActionSend))
        .check(matches(isDisplayed())).perform(click())

    onView(withText(getResString(R.string
        .please_update_information_about_contacts)))
        .check(matches(isDisplayed()))
    onView(withId(com.google.android.material.R.id.snackbar_action))
        .check(matches(isDisplayed()))
        .perform(click())
  }

  @Test
  fun testAddingAtts() {
    activityTestRule?.launchActivity(intent)
    registerNodeIdling()

    onView(withId(R.id.editTextRecipientTo))
        .check(matches(isDisplayed()))
        .perform(closeSoftKeyboard())

    for (att in atts) {
      addAtt(att)
    }
  }

  @Test
  fun testDeletingAtts() {
    activityTestRule?.launchActivity(intent)
    registerNodeIdling()

    onView(withId(R.id.editTextRecipientTo))
        .check(matches(isDisplayed()))
        .perform(closeSoftKeyboard())

    for (att in atts) {
      addAtt(att)
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
    activityTestRule?.launchActivity(intent)
    registerNodeIdling()

    fillInAllFields(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER)
    intending(hasComponent(ComponentName(getTargetContext(), ImportPublicKeyActivity::class.java)))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    onView(withId(R.id.menuActionSend))
        .check(matches(isDisplayed()))
        .perform(click())
    savePublicKeyInDatabase()
    onView(withText(R.string.import_their_public_key))
        .check(matches(isDisplayed()))
        .perform(click())
  }

  @Test
  fun testSelectedStandardEncryptionTypeFromPopUp() {
    activityTestRule?.launchActivity(intent)
    registerNodeIdling()

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
    activityTestRule?.launchActivity(intent)
    registerNodeIdling()

    onView(withId(R.id.editTextRecipientTo))
        .check(matches(isDisplayed()))
        .perform(typeText(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER), closeSoftKeyboard())
    //move the focus to the next view
    onView(withId(R.id.editTextEmailMessage))
        .check(matches(isDisplayed()))
        .perform(typeText(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER), closeSoftKeyboard())
    onView(withId(R.id.menuActionSend))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(withText(getResString(R.string.template_remove_recipient,
        TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER)))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(withId(R.id.editTextRecipientTo))
        .check(matches(isDisplayed()))
        .check(matches(withText(not(containsString(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER)))))
  }

  @Test
  fun testSelectedCopyFromOtherContactFromPopUp() {
    activityTestRule?.launchActivity(intent)
    registerNodeIdling()

    fillInAllFields(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER)
    val result = Intent()
    result.putExtra(SelectContactsActivity.KEY_EXTRA_PGP_CONTACT, pgpContact)
    intending(hasComponent(ComponentName(getTargetContext(), SelectContactsActivity::class.java)))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, result))
    onView(withId(R.id.menuActionSend))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(withText(R.string.copy_from_other_contact))
        .check(matches(isDisplayed()))
        .perform(click())
    isToastDisplayed(activityTestRule?.activity, getResString(R.string.key_successfully_copied))
  }

  private fun checkIsDisplayedEncryptedAttributes() {
    onView(withId(R.id.underToolbarTextTextView))
        .check(doesNotExist())
    onView(withId(R.id.appBarLayout))
        .check(matches(withAppBarLayoutBackgroundColor(UIUtil.getColor(getTargetContext(), R.color.colorPrimary))))
  }

  private fun savePublicKeyInDatabase() {
    val contactsDaoSource = ContactsDaoSource()
    val pgpContact = pgpContact
    contactsDaoSource.addRow(getTargetContext(), pgpContact)
  }

  private fun deleteAtt(att: File) {
    onView(allOf<View>(withId(R.id.imageButtonClearAtt), hasSibling(withChild(withText(att.name)))))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(withText(att.name))
        .check(doesNotExist())
  }

  private fun addAtt(att: File) {
    val resultData = Intent()
    resultData.data = Uri.fromFile(att)
    intending(allOf(hasAction(Intent.ACTION_CHOOSER), hasExtra(`is`(Intent.EXTRA_INTENT),
        allOf(hasAction(Intent.ACTION_OPEN_DOCUMENT), hasType("*/*"),
            hasCategories(hasItem(equalTo(Intent.CATEGORY_OPENABLE)))))))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultData))
    onView(withId(R.id.menuActionAttachFile))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(withText(att.name))
        .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
  }

  private fun checkIsDisplayedStandardAttributes() {
    onView(withId(R.id.underToolbarTextTextView))
        .check(matches(isDisplayed())).check(matches(withText(R.string.this_message_will_not_be_encrypted)))
    onView(withId(R.id.appBarLayout))
        .check(matches(withAppBarLayoutBackgroundColor(UIUtil.getColor(getTargetContext(), R.color.red))))
  }

  private fun fillInAllFields(recipient: String) {
    onView(withId(R.id.editTextRecipientTo))
        .check(matches(isDisplayed()))
        .perform(typeText(recipient), closeSoftKeyboard())
    onView(withId(R.id.editTextEmailSubject))
        .check(matches(isDisplayed()))
        .perform(typeText(EMAIL_SUBJECT), closeSoftKeyboard())
    onView(withId(R.id.editTextEmailMessage))
        .check(matches(isDisplayed()))
        .perform(typeText(EMAIL_SUBJECT), closeSoftKeyboard())
  }

  companion object {

    private const val ATTACHMENTS_COUNT = 3
    private const val EMAIL_SUBJECT = "Test subject"
    private const val EMAIL_MESSAGE = "Test message"

    private var atts: MutableList<File> = mutableListOf()

    @BeforeClass
    @JvmStatic
    fun setUp() {
      createFilesForAtts()
    }

    @AfterClass
    @JvmStatic
    fun cleanResources() {
      TestGeneralUtil.deleteFiles(atts)
    }

    private fun createFilesForAtts() {
      for (i in 0 until ATTACHMENTS_COUNT) {
        atts.add(TestGeneralUtil.createFile("$i.txt", "Text for filling the attached file"))
      }
    }
  }
}
