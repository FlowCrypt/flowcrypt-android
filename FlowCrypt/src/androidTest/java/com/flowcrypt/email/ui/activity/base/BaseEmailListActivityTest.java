/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base;

import com.flowcrypt.email.R;
import com.flowcrypt.email.base.BaseTest;
import com.flowcrypt.email.ui.activity.MessageDetailsActivity;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;

/**
 * @author Denis Bondarenko
 * Date: 04.05.2018
 * Time: 11:11
 * E-mail: DenBond7@gmail.com
 */
public class BaseEmailListActivityTest extends BaseTest {

    protected void testDownloadAllMessages(int messageCount) {
        onView(withId(R.id.emptyView)).check(matches(not(isDisplayed())));
        // size of list = number of the letters in the mail + 1 footer.
        onView(withId(R.id.listViewMessages)).check(matches(matchListSize(messageCount))).check(matches(isDisplayed()));
    }

    protected void testRunMessageDetailsActivity(int position) {
        onData(anything())
                .inAdapterView(withId(R.id.listViewMessages))
                .atPosition(position)
                .perform(click());
        intended(hasComponent(MessageDetailsActivity.class.getName()));
        onView(withId(R.id.textViewSenderAddress)).check(matches(isDisplayed()))
                .check(matches(withText(not(isEmptyString()))));
        onView(withId(R.id.textViewSubject)).check(matches(isDisplayed()))
                .check(matches(withText(not(isEmptyString()))));
        onView(withId(R.id.textViewDate)).check(matches(isDisplayed())).check(matches(withText(not(isEmptyString()))));
    }
}