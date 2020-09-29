/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
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
import androidx.test.platform.app.InstrumentationRegistry
import com.flowcrypt.email.DoesNotNeedMailserver
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.request.model.PostLookUpEmailModel
import com.flowcrypt.email.api.retrofit.response.attester.LookUpEmailResponse
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withAppBarLayoutBackgroundColor
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.rules.lazyActivityScenarioRule
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import com.flowcrypt.email.util.UIUtil
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.isEmptyString
import org.hamcrest.Matchers.not
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStreamReader

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
class CreateMessageActivityTest : BaseTest() {
  override val useIntents: Boolean = true
  override val activeActivityRule = lazyActivityScenarioRule<CreateMessageActivity>(launchActivity = false)
  override val activityScenario: ActivityScenario<*>?
    get() = activeActivityRule.scenario

  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule()

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(AddAccountToDatabaseRule())
      .around(addPrivateKeyToDatabaseRule)
      .around(RetryRule())
      .around(activeActivityRule)
      .around(ScreenshotTestRule())

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
  @DoesNotNeedMailserver
  fun testEmptyRecipient() {
    activeActivityRule.launch(intent)
    registerAllIdlingResources()
    onView(withId(R.id.editTextRecipientTo))
        .check(matches(withText(isEmptyString())))
    onView(withId(R.id.menuActionSend))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(withText(getResString(R.string.text_must_not_be_empty, getResString(R.string.prompt_recipients_to))))
        .check(matches(isDisplayed()))
  }

  @Test
  @DoesNotNeedMailserver
  fun testEmptyEmailSubject() {
    activeActivityRule.launch(intent)
    registerAllIdlingResources()

    onView(withId(R.id.layoutTo))
        .perform(scrollTo())
    onView(withId(R.id.editTextRecipientTo))
        .perform(typeText(TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER))
    onView(withId(R.id.editTextEmailSubject))
        .perform(scrollTo(), typeText("subject"), clearText())
        .check(matches(withText(isEmptyString())))
    onView(withId(R.id.menuActionSend))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(withText(getResString(R.string.text_must_not_be_empty, getResString(R.string.prompt_subject))))
        .check(matches(isDisplayed()))
  }

  @Test
  @DoesNotNeedMailserver
  fun testEmptyEmailMsg() {
    activeActivityRule.launch(intent)
    registerAllIdlingResources()

    onView(withId(R.id.layoutTo))
        .perform(scrollTo())
    onView(withId(R.id.editTextRecipientTo))
        .perform(typeText(TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER))
    onView(withId(R.id.editTextEmailSubject))
        .check(matches(isDisplayed()))
        .perform(scrollTo(), typeText(EMAIL_SUBJECT))
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
  @DoesNotNeedMailserver
  fun testUsingStandardMsgEncryptionType() {
    activeActivityRule.launch(intent)
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
  @DoesNotNeedMailserver
  fun testUsingSecureMsgEncryptionType() {
    activeActivityRule.launch(intent)
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
  @DoesNotNeedMailserver
  fun testSwitchBetweenEncryptionTypes() {
    activeActivityRule.launch(intent)
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
  @DoesNotNeedMailserver
  fun testShowHelpScreen() {
    activeActivityRule.launch(intent)
  }

  @Test
  @DoesNotNeedMailserver
  fun testIsScreenOfComposeNewMsg() {
    activeActivityRule.launch(intent)
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
  @DoesNotNeedMailserver
  fun testWrongFormatOfRecipientEmailAddress() {
    activeActivityRule.launch(intent)
    registerAllIdlingResources()

    val invalidEmailAddresses = arrayOf("test", "test@", "test@@denbond7.com", "@denbond7.com")

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
  @DoesNotNeedMailserver
  fun testAddingAtts() {
    activeActivityRule.launch(intent)
    registerAllIdlingResources()

    onView(withId(R.id.layoutTo))
        .perform(scrollTo())
    onView(withId(R.id.editTextRecipientTo))
        .perform(closeSoftKeyboard())

    for (att in atts) {
      addAtt(att)
    }
  }

  @Test
  @DoesNotNeedMailserver
  fun testDeletingAtts() {
    activeActivityRule.launch(intent)
    registerAllIdlingResources()

    onView(withId(R.id.layoutTo))
        .perform(scrollTo())
    onView(withId(R.id.editTextRecipientTo))
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
  @DoesNotNeedMailserver
  fun testSelectImportPublicKeyFromPopUp() {
    activeActivityRule.launch(intent)
    registerAllIdlingResources()

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
  @DoesNotNeedMailserver
  fun testSelectedStandardEncryptionTypeFromPopUp() {
    activeActivityRule.launch(intent)
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
  @DoesNotNeedMailserver
  fun testSelectedRemoveRecipientFromPopUp() {
    activeActivityRule.launch(intent)
    registerAllIdlingResources()

    onView(withId(R.id.layoutTo))
        .perform(scrollTo())
    onView(withId(R.id.editTextRecipientTo))
        .perform(typeText(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER), closeSoftKeyboard())
    //move the focus to the next view
    onView(withId(R.id.editTextEmailMessage))
        .perform(scrollTo(), typeText(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER), closeSoftKeyboard())
    onView(withId(R.id.menuActionSend))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(withText(getResString(R.string.template_remove_recipient,
        TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER)))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(withId(R.id.layoutTo))
        .perform(scrollTo())
    onView(withId(R.id.editTextRecipientTo))
        .check(matches(isDisplayed()))
        .check(matches(withText(not(containsString(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER)))))
  }

  @Test
  @DoesNotNeedMailserver
  fun testSelectedCopyFromOtherContactFromPopUp() {
    activeActivityRule.launch(intent)
    registerAllIdlingResources()

    fillInAllFields(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER)
    val result = Intent()
    result.putExtra(SelectContactsActivity.KEY_EXTRA_PGP_CONTACT, pgpContact.toContactEntity())
    intending(hasComponent(ComponentName(getTargetContext(), SelectContactsActivity::class.java)))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, result))
    onView(withId(R.id.menuActionSend))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(withText(R.string.copy_from_other_contact))
        .check(matches(isDisplayed()))
        .perform(click())
    isToastDisplayed(decorView, getResString(R.string.key_successfully_copied))
  }

  @Test
  @DoesNotNeedMailserver
  fun testSharePubKeySingle() {
    activeActivityRule.launch(intent)
    registerAllIdlingResources()

    openActionBarOverflowOrOptionsMenu(getTargetContext())
    onView(withText(R.string.include_public_key))
        .check(matches(isDisplayed()))
        .perform(click())

    val att = EmailUtil.genAttInfoFromPubKey(addPrivateKeyToDatabaseRule.nodeKeyDetails)

    onView(withText(att?.name))
        .check(matches(isDisplayed()))
  }

  @Test
  @DoesNotNeedMailserver
  fun testSharePubKeyMultiply() {
    val secondKeyDetails =
        PrivateKeysManager.getNodeKeyDetailsFromAssets("node/default@denbond7.com_secondKey_prv_strong.json")
    PrivateKeysManager.saveKeyToDatabase(secondKeyDetails, TestConstants.DEFAULT_STRONG_PASSWORD, KeyDetails.Type.EMAIL)
    val att = EmailUtil.genAttInfoFromPubKey(secondKeyDetails)

    activeActivityRule.launch(intent)
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
  @DoesNotNeedMailserver
  fun testSharePubKeyNoOwnKeys() {
    PrivateKeysManager.deleteKey(addPrivateKeyToDatabaseRule.keyPath)
    val keyDetails =
        PrivateKeysManager.getNodeKeyDetailsFromAssets("node/key_testing@denbond7.com_keyB_default.json")
    PrivateKeysManager.saveKeyToDatabase(keyDetails, TestConstants.DEFAULT_PASSWORD, KeyDetails.Type.EMAIL)

    activeActivityRule.launch(intent)
    registerAllIdlingResources()

    openActionBarOverflowOrOptionsMenu(getTargetContext())
    onView(withText(R.string.include_public_key))
        .check(matches(isDisplayed()))
        .perform(click())

    val att = EmailUtil.genAttInfoFromPubKey(keyDetails)

    onView(withText(att?.name))
        .check(matches(isDisplayed()))
  }

  private fun checkIsDisplayedEncryptedAttributes() {
    onView(withId(R.id.underToolbarTextTextView))
        .check(doesNotExist())
    onView(withId(R.id.appBarLayout))
        .check(matches(withAppBarLayoutBackgroundColor(UIUtil.getColor(getTargetContext(), R.color.colorPrimary))))
  }

  private fun savePublicKeyInDatabase() {
    FlowCryptRoomDatabase.getDatabase(getTargetContext()).contactsDao().insert(pgpContact.toContactEntity())
  }

  private fun deleteAtt(att: File) {
    onView(allOf(withId(R.id.imageButtonClearAtt), ViewMatchers.withParent(
        allOf(withId(R.id.actionButtons), hasSibling(withText(att.name))))))
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
    onView(withId(R.id.layoutTo))
        .perform(scrollTo())
    onView(withId(R.id.editTextRecipientTo))
        .perform(typeText(recipient), closeSoftKeyboard())
    onView(withId(R.id.editTextEmailSubject))
        .perform(scrollTo(), typeText(EMAIL_SUBJECT), closeSoftKeyboard())
    onView(withId(R.id.editTextEmailMessage))
        .perform(scrollTo(), typeText(EMAIL_SUBJECT), closeSoftKeyboard())
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
    val mockWebServerRule = FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        val gson = ApiHelper.getInstance(InstrumentationRegistry.getInstrumentation().targetContext).gson
        if (request.path.equals("/lookup/email")) {
          val requestModel = gson.fromJson(InputStreamReader(request.body.inputStream()), PostLookUpEmailModel::class.java)

          when {
            requestModel.email.equals("not_attested_user@denbond7.com", true) -> {
              val model = gson.fromJson(
                  InputStreamReader(ByteArrayInputStream(TestGeneralUtil.readObjectFromResourcesAsByteArray("2.json"))),
                  LookUpEmailResponse::class.java)
              return MockResponse().setResponseCode(200).setBody(gson.toJson(model))
            }

            requestModel.email.equals("attested_user@denbond7.com", true) -> {
              val model = gson.fromJson(
                  InputStreamReader(ByteArrayInputStream(TestGeneralUtil.readObjectFromResourcesAsByteArray("3.json"))),
                  LookUpEmailResponse::class.java)
              return MockResponse().setResponseCode(200).setBody(gson.toJson(model))
            }
          }
        }

        return MockResponse().setResponseCode(404)
      }
    })

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
