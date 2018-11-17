/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import com.flowcrypt.email.R;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.rules.AddAccountToDatabaseRule;
import com.flowcrypt.email.rules.ClearAppSettingsRule;
import com.flowcrypt.email.ui.activity.settings.ContactsSettingsActivity;

import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.io.IOException;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.not;

/**
 * @author Denis Bondarenko
 * Date: 20.02.2018
 * Time: 15:43
 * E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class ContactsSettingsActivityTest extends BaseTest {

  private static final String[] EMAILS = new String[]{
      "contact_0@denbond7.com",
      "contact_1@denbond7.com",
      "contact_2@denbond7.com",
      "contact_3@denbond7.com"};
  @Rule
  public TestRule ruleChain = RuleChain
      .outerRule(new ClearAppSettingsRule())
      .around(new AddAccountToDatabaseRule())
      .around(new ActivityTestRule<>(ContactsSettingsActivity.class));

  @AfterClass
  public static void clearContactsFromDatabase() {
    ContactsDaoSource contactsDaoSource = new ContactsDaoSource();
    for (String email : EMAILS) {
      contactsDaoSource.deletePgpContact(InstrumentationRegistry.getInstrumentation().getTargetContext(), email);
    }
  }

  @Test
  public void testShowHelpScreen() {
    testHelpScreen();
  }

  @Test
  public void testEmptyList() {
    onView(withId(R.id.listViewContacts)).check(matches(matchEmptyList())).check(matches(not(isDisplayed())));
    onView(withId(R.id.emptyView)).check(matches(isDisplayed())).check(matches(withText(R.string.no_results)));
  }

  @Test
  public void testDeleteContacts() throws IOException {
    addContactsToDatabase();
    for (String ignored : EMAILS) {
      onData(anything())
          .inAdapterView(withId(R.id.listViewContacts))
          .onChildView(withId(R.id.imageButtonDeleteContact))
          .atPosition(0)
          .perform(click());
    }
    testEmptyList();
    clearContactsFromDatabase();
  }

  private void addContactsToDatabase() throws IOException {
    ContactsDaoSource contactsDaoSource = new ContactsDaoSource();
    for (String email : EMAILS) {
      PgpContact pgpContact = new PgpContact(email, null, "", true, null,
          false, null, null, null, 0);
      contactsDaoSource.addRow(InstrumentationRegistry.getInstrumentation().getTargetContext(), pgpContact);
    }
  }
}