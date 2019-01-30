/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

/**
 * The base back stack activity. In this activity we add the back stack functionality. The
 * extended class must implement {@link BaseBackStackActivity#getContentViewResourceId()} method
 * to define the content view resources id. And the in {@link Activity#onCreate(Bundle)} method
 * we setup the toolbar if it exist in the contents and call
 * {@link androidx.appcompat.app.ActionBar#setDisplayHomeAsUpEnabled(boolean)} to implement the
 * back stack functionality.
 *
 * @author DenBond7
 * Date: 26.05.2017
 * Time: 10:03
 * E-mail: DenBond7@gmail.com
 */

public abstract class BaseBackStackActivity extends BaseActivity {

  @Override
  public boolean isDisplayHomeAsUpEnabled() {
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        return true;

    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onJsServiceConnected() {

  }
}
