/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.viewaction

import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import com.google.android.material.navigation.NavigationView
import org.hamcrest.Matcher

/**
 * View actions for interacting with [NavigationView]
 *
 *
 * See [androidx.test.espresso.contrib.NavigationViewActions] for more details
 *
 * @author Denys Bondarenko
 */

/**
 * Returns a [ViewAction] that navigates to a menu item in [NavigationView] using a
 * menu item title.
 *
 *
 *
 * View constraints:
 *
 *
 *
 *  * View must be a child of a [DrawerLayout]
 *  * View must be of type [NavigationView]
 *  * View must be visible on screen
 *  * View must be displayed on screen
 *
 *
 * @param menuItemName the name of the menu item title
 * @return a [ViewAction] that navigates on a menu item
 */

object CustomViewActions {
  fun doNothing(): ViewAction {
    return EmptyAction()
  }

  fun clickOnChipCloseIcon(): ViewAction {
    return ChipCloseIconClickViewAction()
  }

  fun clickOnFolderWithName(folderName: String): ViewAction {
    return ClickOnFolderViewAction(folderName)
  }

  fun swipeToRefresh(action: ViewAction, constraints: Matcher<View>): ViewAction {
    return object : ViewAction {
      override fun getConstraints(): Matcher<View> {
        return constraints
      }

      override fun getDescription(): String {
        return action.description
      }

      override fun perform(uiController: UiController?, view: View?) {
        action.perform(uiController, view)
      }
    }
  }
}
