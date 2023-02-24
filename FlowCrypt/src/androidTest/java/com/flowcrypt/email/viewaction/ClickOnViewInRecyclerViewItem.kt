/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.viewaction

import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import org.hamcrest.Matcher

/**
 * Find a view by id in a recycler view item and click on it.
 *
 * @author Denys Bondarenko
 */
class ClickOnViewInRecyclerViewItem(private val viewId: Int) : ViewAction {

  override fun getConstraints(): Matcher<View>? {
    return null
  }

  override fun getDescription(): String {
    return "Click on a child view with id = ${viewId}"
  }

  override fun perform(uiController: UiController, view: View) {
    view.findViewById<View>(viewId)?.performClick()
  }
}
