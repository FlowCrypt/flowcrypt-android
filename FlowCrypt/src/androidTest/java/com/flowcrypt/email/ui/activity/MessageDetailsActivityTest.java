/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.text.format.DateFormat;
import android.text.format.Formatter;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.LocalFolder;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.rules.AddAccountToDatabaseRule;
import com.flowcrypt.email.rules.AddAttachmentToDatabaseRule;
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.ui.activity.base.BaseActivity;
import com.flowcrypt.email.util.TestGeneralUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * @author Denis Bondarenko
 * Date: 3/13/19
 * Time: 4:32 PM
 * E-mail: DenBond7@gmail.com
 */
public class MessageDetailsActivityTest extends BaseTest {
  private ActivityTestRule activityTestRule = new ActivityTestRule<>(MessageDetailsActivity.class, false, false);
  private AddAttachmentToDatabaseRule simpleAttachmentRule =
      new AddAttachmentToDatabaseRule(TestGeneralUtil.getObjectFromJson("messages/attachments/simple_att.json",
          AttachmentInfo.class));
  @Rule
  public TestRule ruleChain = RuleChain
      .outerRule(new ClearAppSettingsRule())
      .around(new AddAccountToDatabaseRule())
      .around(new AddPrivateKeyToDatabaseRule())
      .around(simpleAttachmentRule)
      .around(activityTestRule);

  private java.text.DateFormat dateFormat;
  private LocalFolder localFolder;

  @Override
  public ActivityTestRule getActivityTestRule() {
    return activityTestRule;
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
    launchActivity(details);
    matchHeader(details);
    onView(withText(incomingMsgInfo.getMsgParts().get(0).getValue())).check(matches(isDisplayed()));
  }

  @Test
  public void testStandardMsgPlaneTextWithOneAttachment() {
    GeneralMessageDetails details =
        TestGeneralUtil.getObjectFromJson("messages/general/standard_msg_plane_text_with_one_att.json",
            GeneralMessageDetails.class);
    IncomingMessageInfo incomingMsgInfo =
        TestGeneralUtil.getObjectFromJson("messages/info/standard_msg_info_plane_text_with_one_att.json",
            IncomingMessageInfo.class);

    launchActivity(details);
    matchHeader(details);
    onView(withText(incomingMsgInfo.getMsgParts().get(0).getValue())).check(matches(isDisplayed()));
    onView(withId(R.id.layoutAtt)).check(matches(isDisplayed()));
    matchAtt(simpleAttachmentRule.getAttInfo());
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

  private void launchActivity(GeneralMessageDetails details) {
    activityTestRule.launchActivity(MessageDetailsActivity.getIntent(getTargetContext(), localFolder, details));
    IdlingRegistry.getInstance().register(((BaseActivity) activityTestRule.getActivity()).getNodeIdlingResource());
    IdlingRegistry.getInstance().register(((MessageDetailsActivity) activityTestRule.getActivity()).getIdlingForDecryption());
  }
}