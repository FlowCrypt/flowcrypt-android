/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.text.format.DateFormat;
import android.text.format.Formatter;

import com.flowcrypt.email.R;
import com.flowcrypt.email.TestConstants;
import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.LocalFolder;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.matchers.ToastMatcher;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.model.PgpContact;
import com.flowcrypt.email.model.messages.MessagePartPgpMessage;
import com.flowcrypt.email.rules.AddAccountToDatabaseRule;
import com.flowcrypt.email.rules.AddAttachmentToDatabaseRule;
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.ui.activity.base.BaseActivity;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.PrivateKeysManager;
import com.flowcrypt.email.util.TestGeneralUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import java.io.IOException;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.flowcrypt.email.matchers.CustomMatchers.withDrawable;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.not;

/**
 * @author Denis Bondarenko
 * Date: 3/13/19
 * Time: 4:32 PM
 * E-mail: DenBond7@gmail.com
 */
public class MessageDetailsActivityTest extends BaseTest {
  private IntentsTestRule intentsTestRule = new IntentsTestRule<>(MessageDetailsActivity.class, false, false);
  private AddAttachmentToDatabaseRule simpleAttachmentRule =
      new AddAttachmentToDatabaseRule(TestGeneralUtil.getObjectFromJson("messages/attachments/simple_att.json",
          AttachmentInfo.class));

  private AddAttachmentToDatabaseRule encryptedAttachmentRule =
      new AddAttachmentToDatabaseRule(TestGeneralUtil.getObjectFromJson("messages/attachments/encrypted_att.json",
          AttachmentInfo.class));

  private AddAttachmentToDatabaseRule pubKeyAttachmentRule =
      new AddAttachmentToDatabaseRule(TestGeneralUtil.getObjectFromJson("messages/attachments/pub_key.json",
          AttachmentInfo.class));
  @Rule
  public TestRule ruleChain = RuleChain
      .outerRule(new ClearAppSettingsRule())
      .around(new AddAccountToDatabaseRule())
      .around(new AddPrivateKeyToDatabaseRule())
      .around(simpleAttachmentRule)
      .around(encryptedAttachmentRule)
      .around(pubKeyAttachmentRule)
      .around(intentsTestRule);

  private java.text.DateFormat dateFormat;
  private LocalFolder localFolder;

  @Override
  public ActivityTestRule getActivityTestRule() {
    return intentsTestRule;
  }

  @After
  public void unregisterDecryptionIdling() {
    ActivityTestRule activityTestRule = getActivityTestRule();
    if (activityTestRule != null) {
      Activity activity = activityTestRule.getActivity();
      if (activity instanceof MessageDetailsActivity) {
        IdlingRegistry.getInstance().unregister(((MessageDetailsActivity) activity).getIdlingForDecryption());
      }
    }
  }

  @Before
  public void init() {
    dateFormat = DateFormat.getTimeFormat(getTargetContext());
    localFolder = new LocalFolder("INBOX", "INBOX", 1, new String[]{"\\HasNoChildren"}, false);
  }

  @Test
  public void testStandardMsgPlaneText() {
    GeneralMessageDetails details =
        TestGeneralUtil.getObjectFromJson("messages/general/standard_msg_plane_text.json", GeneralMessageDetails.class);
    IncomingMessageInfo incomingMsgInfo =
        TestGeneralUtil.getObjectFromJson("messages/info/standard_msg_info_plane_text.json", IncomingMessageInfo.class);
    baseCheck(details, incomingMsgInfo);
  }

  @Test
  public void testReplyButton() {
    testStandardMsgPlaneText();
    onView(withId(R.id.layoutReplyButton)).check(matches(isDisplayed())).perform(scrollTo(), click());
    intended(hasComponent(CreateMessageActivity.class.getName()));
  }

  @Test
  public void testReplyAllButton() {
    testStandardMsgPlaneText();
    onView(withId(R.id.layoutReplyAllButton)).check(matches(isDisplayed())).perform(scrollTo(), click());
    intended(hasComponent(CreateMessageActivity.class.getName()));
  }

  @Test
  public void testFwdButton() {
    testStandardMsgPlaneText();
    onView(withId(R.id.layoutFwdButton)).check(matches(isDisplayed())).perform(scrollTo(), click());
    intended(hasComponent(CreateMessageActivity.class.getName()));
  }

  @Test
  public void testStandardMsgPlaneTextWithOneAttachment() {
    GeneralMessageDetails details =
        TestGeneralUtil.getObjectFromJson("messages/general/standard_msg_plane_text_with_one_att.json",
            GeneralMessageDetails.class);
    IncomingMessageInfo incomingMsgInfo =
        TestGeneralUtil.getObjectFromJson("messages/info/standard_msg_info_plane_text_with_one_att.json",
            IncomingMessageInfo.class);

    baseCheckWithAtt(details, incomingMsgInfo, simpleAttachmentRule);
  }

  @Test
  public void testEncryptedMsgPlaneText() {
    GeneralMessageDetails details =
        TestGeneralUtil.getObjectFromJson("messages/general/encrypted_msg_plane_text.json",
            GeneralMessageDetails.class);
    IncomingMessageInfo incomingMsgInfo =
        TestGeneralUtil.getObjectFromJson("messages/info/encrypted_msg_info_plane_text.json",
            IncomingMessageInfo.class);
    baseCheck(details, incomingMsgInfo);
  }

  @Test
  public void testMissingKeyErrorImportKey() throws Throwable {
    GeneralMessageDetails details =
        TestGeneralUtil.getObjectFromJson("messages/general/encrypted_msg_plane_text_with_missing_key.json",
            GeneralMessageDetails.class);
    IncomingMessageInfo incomingMsgInfo =
        TestGeneralUtil.getObjectFromJson("messages/info/encrypted_msg_info_plane_text_with_missing_key.json",
            IncomingMessageInfo.class);
    testDecryptError(details, incomingMsgInfo);

    intending(hasComponent(new ComponentName(getTargetContext(), ImportPrivateKeyActivity.class)))
        .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

    PrivateKeysManager.saveKeyFromAssetsToDatabase("node/default@denbond7.com_secondKey_prv_strong.json",
        TestConstants.DEFAULT_STRONG_PASSWORD, KeyDetails.Type.EMAIL, (BaseActivity) intentsTestRule.getActivity());

    onView(withId(R.id.buttonImportPrivateKey)).check(matches(isDisplayed())).perform(scrollTo(), click());

    IncomingMessageInfo incomingMsgInfoFixed =
        TestGeneralUtil.getObjectFromJson("messages/info/encrypted_msg_info_plane_text_with_missing_key_fixed.json",
            IncomingMessageInfo.class);
    onView(withText(incomingMsgInfoFixed.getMsgParts().get(0).getValue())).check(matches(isDisplayed()));
    matchReplyButtons(details);

    PrivateKeysManager.deleteKey("node/default@denbond7.com_secondKey_prv_strong.json",
        (BaseActivity) intentsTestRule.getActivity());
  }

  @Test
  public void testMissingPubKey() {
    GeneralMessageDetails details =
        TestGeneralUtil.getObjectFromJson("messages/general/encrypted_msg_plane_text_error_one_pub_key.json",
            GeneralMessageDetails.class);
    IncomingMessageInfo incomingMsgInfo =
        TestGeneralUtil.getObjectFromJson("messages/info/encrypted_msg_info_plane_text_error_one_pub_key.json",
            IncomingMessageInfo.class);
    testDecryptError(details, incomingMsgInfo);
  }

  @Test
  public void testBadlyFormattedMsg() {
    GeneralMessageDetails details =
        TestGeneralUtil.getObjectFromJson("messages/general/encrypted_msg_plane_text_error_badly_formatted.json",
            GeneralMessageDetails.class);
    IncomingMessageInfo incomingMsgInfo =
        TestGeneralUtil.getObjectFromJson("messages/info/encrypted_msg_info_plane_text_error_badly_formatted.json",
            IncomingMessageInfo.class);
    testDecryptError(details, incomingMsgInfo);
  }

  @Test
  public void testMissingKeyErrorChooseSinglePubKey() {
    GeneralMessageDetails details =
        TestGeneralUtil.getObjectFromJson("messages/general/encrypted_msg_plane_text_with_missing_key.json",
            GeneralMessageDetails.class);
    IncomingMessageInfo incomingMsgInfo =
        TestGeneralUtil.getObjectFromJson("messages/info/encrypted_msg_info_plane_text_with_missing_key.json",
            IncomingMessageInfo.class);
    testDecryptError(details, incomingMsgInfo);

    onView(withId(R.id.buttonSendOwnPublicKey)).check(matches(isDisplayed())).perform(scrollTo(), click());
    onView(withId(R.id.textViewMessage)).check(
        matches(withText(getResString(R.string.tell_sender_to_update_their_settings))));
    onView(withId(R.id.buttonOk)).check(matches(isDisplayed())).perform(click());
    intended(hasComponent(CreateMessageActivity.class.getName()));
  }

  @Test
  public void testMissingKeyErrorChooseFromFewPubKeys() throws Throwable {
    GeneralMessageDetails details =
        TestGeneralUtil.getObjectFromJson("messages/general/encrypted_msg_plane_text_with_missing_key.json",
            GeneralMessageDetails.class);
    IncomingMessageInfo incomingMsgInfo =
        TestGeneralUtil.getObjectFromJson("messages/info/encrypted_msg_info_plane_text_with_missing_key.json",
            IncomingMessageInfo.class);
    testDecryptError(details, incomingMsgInfo);

    PrivateKeysManager.saveKeyFromAssetsToDatabase("node/default@denbond7.com_secondKey_prv_strong.json",
        TestConstants.DEFAULT_STRONG_PASSWORD, KeyDetails.Type.EMAIL, (BaseActivity) intentsTestRule.getActivity());
    onView(withId(R.id.buttonSendOwnPublicKey)).check(matches(isDisplayed())).perform(scrollTo(), click());

    String msg = getResString(R.string.tell_sender_to_update_their_settings)
        + "\n\n" + getResString(R.string.select_key);

    onView(withId(R.id.textViewMessage)).check(matches(withText(msg)));
    onView(withId(R.id.buttonOk)).check(matches(isDisplayed())).perform(click());
    onView(withText(getResString(R.string.please_select_key))).inRoot(new ToastMatcher()).check(matches(isDisplayed()));
    onData(anything()).inAdapterView(withId(R.id.listViewKeys)).atPosition(1).perform(click());
    onView(withId(R.id.buttonOk)).check(matches(isDisplayed())).perform(click());
    intended(hasComponent(CreateMessageActivity.class.getName()));
  }

  @Test
  public void testEncryptedMsgPlaneTextWithOneAttachment() {
    GeneralMessageDetails details =
        TestGeneralUtil.getObjectFromJson("messages/general/encrypted_msg_plane_text_with_one_att.json",
            GeneralMessageDetails.class);
    IncomingMessageInfo incomingMsgInfo =
        TestGeneralUtil.getObjectFromJson("messages/info/encrypted_msg_info_plane_text_with_one_att.json",
            IncomingMessageInfo.class);

    baseCheckWithAtt(details, incomingMsgInfo, encryptedAttachmentRule);
  }

  @Test
  public void testEncryptedMsgPlaneTextWithPubKey() throws IOException {
    GeneralMessageDetails details =
        TestGeneralUtil.getObjectFromJson("messages/general/encrypted_msg_plane_text_with_pub_key.json",
            GeneralMessageDetails.class);
    IncomingMessageInfo incomingMsgInfo =
        TestGeneralUtil.getObjectFromJson("messages/info/encrypted_msg_info_plane_text_with_pub_key.json",
            IncomingMessageInfo.class);

    baseCheckWithAtt(details, incomingMsgInfo, pubKeyAttachmentRule);

    NodeKeyDetails nodeKeyDetails =
        PrivateKeysManager.getNodeKeyDetailsFromAssets("node/denbond7@denbond7.com_pub.json");
    PgpContact pgpContact = nodeKeyDetails.getPrimaryPgpContact();

    onView(withId(R.id.textViewKeyOwnerTemplate)).check(matches(withText(
        getResString(R.string.template_message_part_public_key_owner, pgpContact.getEmail()))));

    onView(withId(R.id.textViewKeyWordsTemplate)).check(matches(withText(
        getHtmlString(getResString(R.string.template_message_part_public_key_key_words,
            nodeKeyDetails.getKeywords())))));

    onView(withId(R.id.textViewFingerprintTemplate)).check(matches(withText(
        getHtmlString(getResString(R.string.template_message_part_public_key_fingerprint,
            GeneralUtil.doSectionsInText(" ", nodeKeyDetails.getFingerprint(), 4))))));

    onView(withId(R.id.textViewPgpPublicKey)).check(matches(not(isDisplayed())));
    onView(withId(R.id.switchShowPublicKey)).check(matches(not(isChecked()))).perform(scrollTo(), click());
    onView(withId(R.id.textViewPgpPublicKey)).check(matches(isDisplayed()));
    onView(withId(R.id.textViewPgpPublicKey)).check(matches(withText(pgpContact.getPubkey())));
    onView(withId(R.id.switchShowPublicKey)).check(matches(isChecked())).perform(scrollTo(), click());
    onView(withId(R.id.textViewPgpPublicKey)).check(matches(not(isDisplayed())));

    onView(withId(R.id.buttonKeyAction)).check(matches(isDisplayed())).perform(scrollTo(), click());
    onView(withId(R.id.buttonKeyAction)).check(matches(not(isDisplayed())));
  }

  private void testDecryptError(GeneralMessageDetails details, IncomingMessageInfo incomingMsgInfo) {
    launchActivity(details);
    matchHeader(details);

    MessagePartPgpMessage messagePartPgpMessage = (MessagePartPgpMessage) incomingMsgInfo.getMsgParts().get(0);
    String errorMsg = messagePartPgpMessage.getErrorMsg();

    onView(withId(R.id.textViewErrorMessage)).check(matches(withText(errorMsg)));

    onView(withId(R.id.textViewOrigPgpMsg)).check(matches(not(isDisplayed())));
    onView(withId(R.id.switchShowOrigMsg)).check(matches(not(isChecked()))).perform(scrollTo(), click());
    onView(withId(R.id.textViewOrigPgpMsg)).check(matches(isDisplayed()));
    onView(withId(R.id.textViewOrigPgpMsg)).check(matches(withText(messagePartPgpMessage.getValue())));
    onView(withId(R.id.switchShowOrigMsg)).check(matches(isChecked())).perform(scrollTo(), click());
    onView(withId(R.id.textViewOrigPgpMsg)).check(matches(not(isDisplayed())));

    matchReplyButtons(details);
  }

  private void baseCheck(GeneralMessageDetails details, IncomingMessageInfo incomingMsgInfo) {
    launchActivity(details);
    matchHeader(details);
    onView(withText(incomingMsgInfo.getMsgParts().get(0).getValue())).check(matches(isDisplayed()));
    matchReplyButtons(details);
  }

  private void baseCheckWithAtt(GeneralMessageDetails details, IncomingMessageInfo incomingMsgInfo,
                                AddAttachmentToDatabaseRule encryptedAttachmentRule) {
    launchActivity(details);
    matchHeader(details);
    onView(withText(incomingMsgInfo.getMsgParts().get(0).getValue())).check(matches(isDisplayed()));
    onView(withId(R.id.layoutAtt)).check(matches(isDisplayed()));
    matchAtt(encryptedAttachmentRule.getAttInfo());
    matchReplyButtons(details);
  }

  private void matchHeader(GeneralMessageDetails details) {
    onView(withId(R.id.textViewSenderAddress)).check(matches(withText(EmailUtil.getFirstAddressString(details.getFrom()))));
    onView(withId(R.id.textViewDate)).check(matches(withText(dateFormat.format(details.getReceivedDate()))));
    onView(withId(R.id.textViewSubject)).check(matches(withText(details.getSubject())));
  }

  private void matchAtt(AttachmentInfo att) {
    onView(withId(R.id.textViewAttchmentName)).check(matches(withText(att.getName())));
    onView(withId(R.id.textViewAttSize)).check(matches(withText(
        Formatter.formatFileSize(getContext(), att.getEncodedSize()))));
  }

  private void matchReplyButtons(GeneralMessageDetails details) {
    onView(withId(R.id.imageButtonReplyAll)).check(matches(isDisplayed()));
    onView(withId(R.id.layoutReplyButton)).check(matches(isDisplayed()));
    onView(withId(R.id.layoutReplyAllButton)).check(matches(isDisplayed()));
    onView(withId(R.id.layoutFwdButton)).check(matches(isDisplayed()));

    if (details.isEncrypted()) {
      onView(withId(R.id.textViewReply)).check(matches(withText(getResString(R.string.reply_encrypted))));
      onView(withId(R.id.textViewReplyAll)).check(matches(withText(getResString(R.string.reply_all_encrypted))));
      onView(withId(R.id.textViewFwd)).check(matches(withText(getResString(R.string.forward_encrypted))));

      onView(withId(R.id.imageViewReply)).check(matches(withDrawable(R.mipmap.ic_reply_green)));
      onView(withId(R.id.imageViewReplyAll)).check(matches(withDrawable(R.mipmap.ic_reply_all_green)));
      onView(withId(R.id.imageViewFwd)).check(matches(withDrawable(R.mipmap.ic_forward_green)));
    } else {
      onView(withId(R.id.textViewReply)).check(matches(withText(getResString(R.string.reply))));
      onView(withId(R.id.textViewReplyAll)).check(matches(withText(getResString(R.string.reply_all))));
      onView(withId(R.id.textViewFwd)).check(matches(withText(getResString(R.string.forward))));

      onView(withId(R.id.imageViewReply)).check(matches(withDrawable(R.mipmap.ic_reply_red)));
      onView(withId(R.id.imageViewReplyAll)).check(matches(withDrawable(R.mipmap.ic_reply_all_red)));
      onView(withId(R.id.imageViewFwd)).check(matches(withDrawable(R.mipmap.ic_forward_red)));
    }
  }

  private void launchActivity(GeneralMessageDetails details) {
    intentsTestRule.launchActivity(MessageDetailsActivity.getIntent(getTargetContext(), localFolder, details));
    IdlingRegistry.getInstance().register(((BaseActivity) intentsTestRule.getActivity()).getNodeIdlingResource());
    IdlingRegistry.getInstance().register(((MessageDetailsActivity) intentsTestRule.getActivity()).getIdlingForDecryption());
  }
}