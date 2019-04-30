/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.viewaction;

import com.google.android.material.navigation.NavigationView;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.test.espresso.ViewAction;

/**
 * View actions for interacting with {@link NavigationView}
 * <p>
 * See {@link androidx.test.espresso.contrib.NavigationViewActions} for more details
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
    return new NavigateToItemViewAction(menuItemName);
  }
}
