/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.viewaction

import android.content.res.Resources
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.util.HumanReadables
import com.google.android.material.internal.NavigationMenu
import com.google.android.material.navigation.NavigationView
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf

/**
 * Create a [ViewAction] that navigates to a menu item in [NavigationView] using a menu item title.
 *
 * @author Denis Bondarenko
 * Date: 11/28/18
 * Time: 11:16 AM
 * E-mail: DenBond7@gmail.com
 */
class NavigateToItemViewAction(private val menuItemName: String) : ViewAction {

  override fun perform(uiController: UiController, view: View) {
    val navigationView = view as NavigationView
    val navigationMenu = navigationView.menu as NavigationMenu

    var matchedMenuItem: MenuItem? = null

    for (i in 0 until navigationMenu.size()) {
      val menuItem = navigationMenu.getItem(i)
      if (menuItem.hasSubMenu()) {
        val subMenu = menuItem.subMenu
        for (j in 0 until subMenu.size()) {
          val subMenuItem = subMenu.getItem(j)
          if (subMenuItem.title == menuItemName) {
            matchedMenuItem = subMenuItem
          }
        }
      } else {
        if (menuItem.title == menuItemName) {
          matchedMenuItem = menuItem
        }
      }
    }

    if (matchedMenuItem == null) {
      throw PerformException.Builder()
        .withActionDescription(this.description)
        .withViewDescription(HumanReadables.describe(view))
        .withCause(RuntimeException(getErrorMsg(navigationMenu, view)))
        .build()
    }
    navigationMenu.performItemAction(matchedMenuItem, 0)
  }

  override fun getDescription(): String {
    return "click on menu item with id"
  }

  override fun getConstraints(): Matcher<View> {
    return allOf(
      isAssignableFrom(NavigationView::class.java),
      withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
      isDisplayingAtLeast(90)
    )
  }

  private fun getErrorMsg(menu: Menu, view: View): String {
    val newLine = System.getProperty("line.separator")
    val errorMsg = StringBuilder("Menu item was not found, available menu items:").append(newLine)
    for (position in 0 until menu.size()) {
      errorMsg.append("[MenuItem] position=").append(position)
      val menuItem = menu.getItem(position)
      if (menuItem != null) {
        val itemTitle = menuItem.title
        if (itemTitle != null) {
          errorMsg.append(", title=").append(itemTitle)
        }
        if (view.resources != null) {
          val itemId = menuItem.itemId
          try {
            errorMsg.append(", id=")
            val menuItemResourceName = view.resources.getResourceName(itemId)
            errorMsg.append(menuItemResourceName)
          } catch (nfe: Resources.NotFoundException) {
            errorMsg.append("not found")
          }

        }
        errorMsg.append(newLine)
      }
    }
    return errorMsg.toString()
  }
}
