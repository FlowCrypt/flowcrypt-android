/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity;

/**
 * The base settings activity which describes a general logic..
 *
 * @author DenBond7
 * Date: 30.05.2017
 * Time: 9:05
 * E-mail: DenBond7@gmail.com
 */

public abstract class BaseSettingsActivity extends BaseBackStackActivity {
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_settings, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menuActionHelp:
        startActivity(new Intent(this, FeedbackActivity.class));
        return true;

      default:
        return super.onOptionsItemSelected(item);
    }
  }
}
