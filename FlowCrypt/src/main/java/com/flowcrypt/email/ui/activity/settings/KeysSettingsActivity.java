/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity;
import com.flowcrypt.email.ui.activity.fragment.KeysListFragment;

import androidx.annotation.Nullable;

/**
 * This {@link Activity} shows information about available keys in the database.
 * <p>
 * Here we can import new keys.
 *
 * @author DenBond7
 * Date: 29.05.2017
 * Time: 11:30
 * E-mail: DenBond7@gmail.com
 */

public class KeysSettingsActivity extends BaseBackStackActivity {

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (savedInstanceState == null) {
      KeysListFragment keysListFragment = KeysListFragment.newInstance();
      getSupportFragmentManager().beginTransaction().replace(R.id.layoutContent, keysListFragment).commitNow();
    }
  }

  @Override
  public int getContentViewResourceId() {
    return R.layout.activity_keys_settings;
  }

  @Override
  public View getRootView() {
    return null;
  }
}
