/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.base.BaseActivity;

import androidx.annotation.Nullable;

/**
 * This activity will be used by other activities to wait while Node.js is starting. If Node.js starts we will close
 * this activity. Usually Node.js starts in a few seconds. We have disabled the "Back" action here to prevent close
 * the app.
 *
 * @author Denis Bondarenko
 * Date: 4/4/19
 * Time: 8:48 AM
 * E-mail: DenBond7@gmail.com
 */
public class NodeRunnerActivity extends BaseActivity {
  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (isNodeReady()) {
      finish();
    }
  }

  @Override
  public boolean isDisplayHomeAsUpEnabled() {
    return false;
  }

  @Override
  public int getContentViewResourceId() {
    return 0;
  }

  @Override
  public View getRootView() {
    return null;
  }

  @Override
  public void onBackPressed() {
    //disabled
  }

  @Override
  protected void onNodeStateChanged(boolean isReady) {
    super.onNodeStateChanged(isReady);
    if (isReady) {
      finish();
    } else {
      Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_LONG).show();
    }
  }
}
