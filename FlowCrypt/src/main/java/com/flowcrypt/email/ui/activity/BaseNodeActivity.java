/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.content.Intent;
import android.os.Bundle;

import com.flowcrypt.email.ui.activity.base.BaseActivity;

import androidx.annotation.Nullable;

/**
 * It's a base activity where we will use Node.js.
 *
 * @author Denis Bondarenko
 * Date: 4/4/19
 * Time: 10:34 AM
 * E-mail: DenBond7@gmail.com
 */
public abstract class BaseNodeActivity extends BaseActivity {
  protected static final int REQUEST_CODE_NODE_RUNNER = 10000;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    if (!isNodeReady()) {
      startActivityForResult(new Intent(this, NodeRunnerActivity.class), REQUEST_CODE_NODE_RUNNER);
    }

    super.onCreate(savedInstanceState);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    switch (requestCode) {
      case REQUEST_CODE_NODE_RUNNER:

        break;

      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }
}
