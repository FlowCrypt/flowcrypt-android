/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import android.view.View
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withListViewItemCount
import com.flowcrypt.email.ui.activity.MessageDetailsActivity
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.isEmptyString
import org.hamcrest.Matchers.not

/**
 * @author Denis Bondarenko
 * Date: 04.05.2018
 * Time: 11:11
 * E-mail: DenBond7@gmail.com
 */
abstract class BaseEmailListActivityTest : BaseTest() {

  protected fun testDownloadAllMsgs(messageCount: Int) {
    onView(withId(R.id.emptyView))
        .check(matches(not<View>(isDisplayed())))
    // size of list = number of the letters in the mail + 1 footer.
    onView(withId(R.id.listViewMessages))
        .check(matches(withListViewItemCount(messageCount))).check(matches(isDisplayed()))
  }

  protected fun testRunMsgDetailsActivity(position: Int) {
    onData(anything())
        .inAdapterView(withId(R.id.listViewMessages))
        .atPosition(position)
        .perform(click())
    intended(hasComponent(MessageDetailsActivity::class.java.name))
    onView(withId(R.id.textViewSenderAddress))
        .check(matches(isDisplayed())).check(matches(withText(not(isEmptyString()))))
    onView(withId(R.id.textViewSubject))
        .check(matches(isDisplayed())).check(matches(withText(not(isEmptyString()))))
    onView(withId(R.id.textViewDate))
        .check(matches(isDisplayed())).check(matches(withText(not(isEmptyString()))))
  }
}
