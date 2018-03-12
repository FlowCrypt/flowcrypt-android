/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.flowcrypt.email.R;
import com.flowcrypt.email.TestConstants;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.rules.AddAccountToDatabaseRule;
import com.flowcrypt.email.rules.ClearAppSettingsRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.closeSoftKeyboard;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withChild;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.not;


/**
 * @author Denis Bondarenko
 *         Date: 23.02.2018
 *         Time: 10:34
 *         E-mail: DenBond7@gmail.com
 */

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SelectContactsActivityTest extends BaseTest {
    private static final String[] EMAILS = new String[]{
            "contact_0@denbond7.com",
            "contact_1@denbond7.com",
            "contact_2@denbond7.com",
            "contact_3@denbond7.com"};

    @Rule
    public TestRule ruleChain = RuleChain
            .outerRule(new ClearAppSettingsRule())
            .around(new AddAccountToDatabaseRule())
            .around(new ActivityTestRule<>(SelectContactsActivity.class));

    @Before
    public void saveContactsToDatabase() {
        ContactsDaoSource contactsDaoSource = new ContactsDaoSource();
        for (int i = 0; i < EMAILS.length; i++) {
            String email = EMAILS[i];
            PgpContact pgpContact;
            if (i % 2 == 0) {
                pgpContact = new PgpContact(email, getUserName(email), "publicKey", true,
                        null, false, null, null, null, 0);
            } else {
                pgpContact = new PgpContact(email, null, "publicKey", true, null,
                        false, null, null, null, 0);
            }
            contactsDaoSource.addRow(InstrumentationRegistry.getTargetContext(), pgpContact);
        }
    }

    @Test
    public void testShowEmptyView() {
        clearContactsFromDatabase();
        onView(withId(R.id.listViewContacts)).check(matches(matchEmptyList())).check(matches(not(isDisplayed())));
        onView(withId(R.id.emptyView)).check(matches(isDisplayed())).check(matches(withText(R.string.no_results)));
    }

    @Test
    public void testShowListContacts() {
        onView(withId(R.id.listViewContacts)).check(matches((isDisplayed()))).check(matches(not(matchEmptyList())));
        for (int i = 0; i < EMAILS.length; i++) {
            if (i % 2 == 0) {
                checkIsDataItemDisplayed(i, R.id.textViewName, getUserName(EMAILS[i]));
            } else {
                checkIsDataItemDisplayed(i, R.id.textViewOnlyEmail, EMAILS[i]);
            }
        }
    }

    @Test
    public void testCheckSearchExistingContact() {
        onView(withId(R.id.menuSearch)).check(matches(isDisplayed())).perform(click());
        for (int i = 0; i < EMAILS.length; i++) {
            if (i % 2 == 0) {
                checkIsTypedUserFound(R.id.textViewName, getUserName(EMAILS[i]));
            } else {
                checkIsTypedUserFound(R.id.textViewOnlyEmail, EMAILS[i]);
            }
        }
    }

    @Test
    public void testNoResults() {
        onView(withId(R.id.menuSearch)).check(matches(isDisplayed())).perform(click());
        onView(withId(android.support.design.R.id.search_src_text)).perform(clearText(),
                typeText("some email"));
        closeSoftKeyboard();
        onView(withId(R.id.listViewContacts)).check(matches(matchEmptyList()));
        onView(withId(R.id.emptyView)).check(matches(isDisplayed())).check(matches(withText(R.string.no_results)));
    }

    private String getUserName(String email) {
        return email.substring(0, email.indexOf(TestConstants.COMMERCIAL_AT_SYMBOL));
    }

    private void clearContactsFromDatabase() {
        ContactsDaoSource contactsDaoSource = new ContactsDaoSource();
        for (String email : EMAILS) {
            contactsDaoSource.deletePgpContact(InstrumentationRegistry.getTargetContext(), email);
        }
    }

    private void checkIsTypedUserFound(int viewId, String viewText) {
        onView(withId(android.support.design.R.id.search_src_text)).perform(clearText(), typeText(viewText));
        closeSoftKeyboard();
        onView(withId(viewId)).check(matches(isDisplayed())).check(matches(withText(viewText)));
    }

    private void checkIsDataItemDisplayed(int index, int viewId, String viewText) {
        onData(anything())
                .inAdapterView(withId(R.id.listViewContacts))
                .onChildView(withChild(withId(viewId)))
                .atPosition(index)
                .check(matches(withChild(withText(viewText))));
    }
}
