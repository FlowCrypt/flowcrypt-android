/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import com.flowcrypt.email.R;
import com.flowcrypt.email.assertions.RecyclerViewItemCountAssertion;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.model.PgpContact;
import com.flowcrypt.email.rules.AddAccountToDatabaseRule;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.util.PrivateKeysManagerKt;
import com.flowcrypt.email.util.TestGeneralUtilKt;
import com.flowcrypt.email.viewaction.ClickOnViewInRecyclerViewItem;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * @author Denis Bondarenko
 * Date: 21.05.2018
 * Time: 14:50
 * E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class PreviewImportPgpContactActivityTest extends BaseTest {
  private ActivityTestRule activityTestRule =
      new ActivityTestRule<PreviewImportPgpContactActivity>(PreviewImportPgpContactActivity.class, false, false) {
      };

  @Rule
  public TestRule ruleChain = RuleChain
      .outerRule(new ClearAppSettingsRule())
      .around(new AddAccountToDatabaseRule())
      .around(activityTestRule);

  @Override
  public ActivityTestRule getActivityTestRule() {
    return activityTestRule;
  }

  @Test
  public void testShowHelpScreen() {
    activityTestRule.launchActivity(PreviewImportPgpContactActivity.newIntent(
        InstrumentationRegistry.getInstrumentation().getTargetContext(), getSinglePublicKeyForUnsavedContact()));
    testHelpScreen();
  }

  @Test
  public void testIsDisplayedSingleItem() {
    PgpContact pgpContact = new PgpContact("default@denbond7.com", null,
        getSinglePublicKeyForUnsavedContact(), true, null, false, null, null, null, 0);
    new ContactsDaoSource().addRow(InstrumentationRegistry.getInstrumentation().getTargetContext(), pgpContact);
    activityTestRule.launchActivity(PreviewImportPgpContactActivity.newIntent(
        InstrumentationRegistry.getInstrumentation().getTargetContext(), getSinglePublicKeyForUnsavedContact()));
    onView(withId(R.id.recyclerViewContacts)).check(new RecyclerViewItemCountAssertion(1));
    onView(withText(InstrumentationRegistry.getInstrumentation().getTargetContext().getString(
        R.string.template_message_part_public_key_owner, "default@denbond7.com")))
        .check(matches(isDisplayed()));
  }

  @Test
  public void testIsDisplayedLabelAlreadyImported() {
    activityTestRule.launchActivity(PreviewImportPgpContactActivity.newIntent(
        InstrumentationRegistry.getInstrumentation().getTargetContext(), getSinglePublicKeyForUnsavedContact()));
    onView(withId(R.id.recyclerViewContacts)).check(new RecyclerViewItemCountAssertion(1));
  }

  @Test
  public void testSaveButtonForSingleContact() {
    activityTestRule.launchActivity(PreviewImportPgpContactActivity.newIntent(
        InstrumentationRegistry.getInstrumentation().getTargetContext(), getSinglePublicKeyForUnsavedContact()));
    onView(withId(R.id.recyclerViewContacts)).check(new RecyclerViewItemCountAssertion(1));
    onView(withId(R.id.recyclerViewContacts)).perform(RecyclerViewActions.actionOnItemAtPosition(0,
        new ClickOnViewInRecyclerViewItem(R.id.buttonSaveContact)));
    checkIsToastDisplayed(activityTestRule.getActivity(), getResString(R.string.contact_successfully_saved));
  }

  @Test
  public void testIsImportAllButtonDisplayed() {
    activityTestRule.launchActivity(PreviewImportPgpContactActivity.newIntent(
        InstrumentationRegistry.getInstrumentation().getTargetContext(), get10PublicKeysForUnsavedContacts()));
    onView(withId(R.id.buttonImportAll)).check(matches(isDisplayed()));
  }

  @Test
  public void testLoadLotOfContacts() {
    int countOfKeys = 10;

    activityTestRule.launchActivity(PreviewImportPgpContactActivity.newIntent(
        InstrumentationRegistry.getInstrumentation().getTargetContext(), get10PublicKeysForUnsavedContacts()));
    onView(withId(R.id.recyclerViewContacts)).check(new RecyclerViewItemCountAssertion(countOfKeys))
        .perform(RecyclerViewActions.scrollToPosition(countOfKeys - 1));
  }

  private String getSinglePublicKeyForUnsavedContact() {
    return PrivateKeysManagerKt.getNodeKeyDetailsFromAssets("node/default@denbond7.com_fisrtKey_pub.json")
        .getPublicKey();
  }

  private String get10PublicKeysForUnsavedContacts() {
    return TestGeneralUtilKt.readFileFromAssetsAsString(InstrumentationRegistry.getInstrumentation().getContext(),
        "pgp/pub_keys_2048_bits_10.asc");
  }
}
