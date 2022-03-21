/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import org.hamcrest.Matchers.isEmptyString
import org.hamcrest.Matchers.not

/**
 * @author Denis Bondarenko
 * Date: 04.05.2018
 * Time: 11:11
 * E-mail: DenBond7@gmail.com
 */
abstract class BaseEmailListActivityTest : BaseTest() {

  override val useIntents: Boolean = true

  protected fun testRunMsgDetailsActivity(position: Int) {
    onView(withId(R.id.rVMsgs))
      .perform(
        RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
          position,
          click()
        )
      )

    intended(hasComponent(MessageDetailsActivity::class.java.name))
    onView(withId(R.id.textViewSenderAddress))
      .check(matches(isDisplayed())).check(matches(withText(not(isEmptyString()))))
    onView(withId(R.id.textViewSubject))
      .check(matches(isDisplayed())).check(matches(withText(not(isEmptyString()))))
    onView(withId(R.id.textViewDate))
      .check(matches(isDisplayed())).check(matches(withText(not(isEmptyString()))))
  }
}
