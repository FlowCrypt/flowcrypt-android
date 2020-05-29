/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.viewaction

import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher

/**
 * This view action does nothing
 *
 * @author Denis Bondarenko
 *         Date: 5/21/20
 *         Time: 2:58 PM
 *         E-mail: DenBond7@gmail.com
 */
class EmptyAction : ViewAction {
  override fun perform(uiController: UiController, view: View) {
    //nothing
  }

  override fun getDescription(): String {
    return "do nothing"
  }

  override fun getConstraints(): Matcher<View> {
    return ViewMatchers.isDisplayingAtLeast(90)
  }
}
