/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.viewaction

import androidx.drawerlayout.widget.DrawerLayout
import androidx.test.espresso.ViewAction
import com.google.android.material.navigation.NavigationView

/**
 * View actions for interacting with [NavigationView]
 *
 *
 * See [androidx.test.espresso.contrib.NavigationViewActions] for more details
 *
 * @author Denis Bondarenko
 * Date: 16.08.2018
 * Time: 12:11
 * E-mail: DenBond7@gmail.com
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
}
