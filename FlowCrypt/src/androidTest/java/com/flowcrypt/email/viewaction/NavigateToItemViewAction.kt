/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.viewaction;

import android.content.res.Resources;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import com.google.android.material.internal.NavigationMenu;
import com.google.android.material.navigation.NavigationView;

import org.hamcrest.Matcher;

import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.espresso.util.HumanReadables;

import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static org.hamcrest.Matchers.allOf;

/**
 * Create a {@link ViewAction} that navigates to a menu item in {@link NavigationView} using a menu item title.
 *
 * @author Denis Bondarenko
 * Date: 11/28/18
 * Time: 11:16 AM
 * E-mail: DenBond7@gmail.com
 */
public class NavigateToItemViewAction implements ViewAction {
  private final String menuItemName;

  public NavigateToItemViewAction(String menuItemName) {
    this.menuItemName = menuItemName;
  }

  @Override
  public void perform(UiController uiController, View view) {
    NavigationView navigationView = (NavigationView) view;
    NavigationMenu navigationMenu = (NavigationMenu) navigationView.getMenu();

    MenuItem matchedMenuItem = null;

    for (int i = 0; i < navigationMenu.size(); i++) {
      MenuItem menuItem = navigationMenu.getItem(i);
      if (menuItem.hasSubMenu()) {
        SubMenu subMenu = menuItem.getSubMenu();
        for (int j = 0; j < subMenu.size(); j++) {
          MenuItem subMenuItem = subMenu.getItem(j);
          if (subMenuItem.getTitle().equals(menuItemName)) {
            matchedMenuItem = subMenuItem;
          }
        }
      } else {
        if (menuItem.getTitle().equals(menuItemName)) {
          matchedMenuItem = menuItem;
        }
      }
    }

    if (matchedMenuItem == null) {
      throw new PerformException.Builder()
          .withActionDescription(this.getDescription())
          .withViewDescription(HumanReadables.describe(view))
          .withCause(new RuntimeException(getErrorMsg(navigationMenu, view)))
          .build();
    }
    navigationMenu.performItemAction(matchedMenuItem, 0);
  }

  @Override
  public String getDescription() {
    return "click on menu item with id";
  }

  @Override
  public Matcher<View> getConstraints() {
    return allOf(
        isAssignableFrom(NavigationView.class),
        withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
        isDisplayingAtLeast(90));
  }

  private String getErrorMsg(Menu menu, View view) {
    String newLine = System.getProperty("line.separator");
    StringBuilder errorMsg =
        new StringBuilder("Menu item was not found, " + "available menu items:")
            .append(newLine);
    for (int position = 0; position < menu.size(); position++) {
      errorMsg.append("[MenuItem] position=").append(position);
      MenuItem menuItem = menu.getItem(position);
      if (menuItem != null) {
        CharSequence itemTitle = menuItem.getTitle();
        if (itemTitle != null) {
          errorMsg.append(", title=").append(itemTitle);
        }
        if (view.getResources() != null) {
          int itemId = menuItem.getItemId();
          try {
            errorMsg.append(", id=");
            String menuItemResourceName = view.getResources().getResourceName(itemId);
            errorMsg.append(menuItemResourceName);
          } catch (Resources.NotFoundException nfe) {
            errorMsg.append("not found");
          }
        }
        errorMsg.append(newLine);
      }
    }
    return errorMsg.toString();
  }
}
