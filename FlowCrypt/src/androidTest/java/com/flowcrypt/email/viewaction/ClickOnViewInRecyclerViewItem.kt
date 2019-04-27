/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.viewaction;

import android.view.View;

import org.hamcrest.Matcher;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

/**
 * Find a view by id in a recycler view item and click on it.
 *
 * @author Denis Bondarenko
 * Date: 2/20/19
 * Time: 6:07 PM
 * E-mail: DenBond7@gmail.com
 */
public class ClickOnViewInRecyclerViewItem implements ViewAction {
  private int viewId;

  public ClickOnViewInRecyclerViewItem(int viewId) {
    this.viewId = viewId;
  }

  @Override
  public Matcher<View> getConstraints() {
    return null;
  }

  @Override
  public String getDescription() {
    return "Click on a child view with id = " + viewId;
  }

  @Override
  public void perform(UiController uiController, View view) {
    View v = view.findViewById(viewId);
    v.performClick();
  }
}
