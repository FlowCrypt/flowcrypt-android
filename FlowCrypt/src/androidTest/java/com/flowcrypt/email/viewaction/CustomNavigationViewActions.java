/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.viewaction;

import android.content.res.Resources;
import android.support.design.internal.NavigationMenu;
import android.support.design.widget.NavigationView;
import android.support.test.espresso.PerformException;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.espresso.util.HumanReadables;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import org.hamcrest.Matcher;

import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static org.hamcrest.Matchers.allOf;

/**
 * View actions for interacting with {@link NavigationView}
 * <p>
 * See {@link android.support.test.espresso.contrib.NavigationViewActions} for more details
 *
 * @author Denis Bondarenko
 * Date: 16.08.2018
 * Time: 12:11
 * E-mail: DenBond7@gmail.com
 */
public final class CustomNavigationViewActions {

  private CustomNavigationViewActions() {
    // no Instance
  }

  /**
   * Returns a {@link ViewAction} that navigates to a menu item in {@link NavigationView} using a
   * menu item title.
   * <p>
   * <p>View constraints:
   * <p>
   * <ul>
   * <li>View must be a child of a {@link DrawerLayout}
   * <li>View must be of type {@link NavigationView}
   * <li>View must be visible on screen
   * <li>View must be displayed on screen
   * <ul>
   *
   * @param menuItemName the name of the menu item title
   * @return a {@link ViewAction} that navigates on a menu item
   */
  public static ViewAction navigateTo(final String menuItemName) {

    return new ViewAction() {

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
              .withCause(new RuntimeException(getErrorMessage(navigationMenu, view)))
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

      private String getErrorMessage(Menu menu, View view) {
        String newLine = System.getProperty("line.separator");
        StringBuilder errorMessage =
            new StringBuilder("Menu item was not found, " + "available menu items:")
                .append(newLine);
        for (int position = 0; position < menu.size(); position++) {
          errorMessage.append("[MenuItem] position=").append(position);
          MenuItem menuItem = menu.getItem(position);
          if (menuItem != null) {
            CharSequence itemTitle = menuItem.getTitle();
            if (itemTitle != null) {
              errorMessage.append(", title=").append(itemTitle);
            }
            if (view.getResources() != null) {
              int itemId = menuItem.getItemId();
              try {
                errorMessage.append(", id=");
                String menuItemResourceName = view.getResources().getResourceName(itemId);
                errorMessage.append(menuItemResourceName);
              } catch (Resources.NotFoundException nfe) {
                errorMessage.append("not found");
              }
            }
            errorMessage.append(newLine);
          }
        }
        return errorMessage.toString();
      }
    };
  }
}
