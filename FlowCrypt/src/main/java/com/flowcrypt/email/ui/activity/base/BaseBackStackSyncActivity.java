/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base;

import android.view.MenuItem;

/**
 * The base back stack sync {@link android.app.Activity}
 *
 * @author DenBond7
 * Date: 27.06.2017
 * Time: 13:26
 * E-mail: DenBond7@gmail.com
 */

public abstract class BaseBackStackSyncActivity extends BaseSyncActivity {
  @Override
  public boolean isDisplayHomeAsUpEnabled() {
    return true;
  }

  @Override
  public boolean isSyncEnabled() {
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
  public void onProgressReplyReceived(int requestCode, int resultCode, Object obj) {

  }

  @Override
  public void onSyncServiceConnected() {

  }

  @Override
  public void onJsServiceConnected() {

  }
}
