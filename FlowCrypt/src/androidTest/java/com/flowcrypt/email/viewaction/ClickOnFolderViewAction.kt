/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.viewaction

import android.content.res.Resources.NotFoundException
import android.view.Menu
import android.view.View
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.util.HumanReadables
import com.flowcrypt.email.R
import com.google.android.material.internal.NavigationMenu
import com.google.android.material.navigation.NavigationView
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf

/**
 * Based on [NavigationViewActions]
 */
class ClickOnFolderViewAction(private val folderName: String) : ViewAction {
  override fun getDescription(): String = "click on folder with name $folderName"

  override fun getConstraints(): Matcher<View> {
    return allOf(
      isAssignableFrom(NavigationView::class.java),
      withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
      isDisplayingAtLeast(90)
    )
  }

  override fun perform(uiController: UiController?, view: View?) {
    val navigationView = view as NavigationView
    val menu = navigationView.menu as NavigationMenu
    if (null == menu.findItem(R.id.mailLabels)) {
      throw PerformException.Builder()
        .withActionDescription(this.description)
        .withViewDescription(HumanReadables.describe(view))
        .withCause(RuntimeException(getErrorMessage(menu, view)))
        .build()
    }

    val mailLabelsMenu = menu.findItem(R.id.mailLabels).subMenu
      ?: throw PerformException.Builder()
        .withActionDescription(this.description)
        .withViewDescription(HumanReadables.describe(view))
        .withCause(RuntimeException("Menu with id = R.id.mailLabels has no sub menus"))
        .build()

    for (i in 0 until mailLabelsMenu.size()) {
      val item = mailLabelsMenu.getItem(i)
      if (item.title == folderName) {
        menu.performItemAction(item, 0)
        return
      }
    }

    throw PerformException.Builder()
      .withActionDescription(this.description)
      .withViewDescription(HumanReadables.describe(view))
      .withCause(RuntimeException("Folder with name = $folderName not found"))
      .build()
  }

  private fun getErrorMessage(menu: Menu, view: View): String {
    val newLine = System.getProperty("line.separator")
    val errorMessage = StringBuilder("Menu item was not found, " + "available menu items:")
      .append(newLine)
    for (position in 0 until menu.size()) {
      errorMessage.append("[MenuItem] position=").append(position)
      val menuItem = menu.getItem(position)
      if (menuItem != null) {
        val itemTitle = menuItem.title
        if (itemTitle != null) {
          errorMessage.append(", title=").append(itemTitle)
        }
        if (view.resources != null) {
          val itemId = menuItem.itemId
          try {
            errorMessage.append(", id=")
            val menuItemResourceName = view.resources.getResourceName(itemId)
            errorMessage.append(menuItemResourceName)
          } catch (nfe: NotFoundException) {
            errorMessage.append("not found")
          }
        }
        errorMessage.append(newLine)
      }
    }
    return errorMessage.toString()
  }
}


