/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings;

import android.view.View;

import com.flowcrypt.email.R;

/**
 * This activity describes notification settings.
 *
 * @author Denis Bondarenko
 * Date: 19.07.2018
 * Time: 11:59
 * E-mail: DenBond7@gmail.com
 */
public class NotificationsSettingsActivity extends BaseSettingsActivity {
  @Override
  public int getContentViewResourceId() {
    return R.layout.activity_notifications_settings;
  }

  @Override
  public View getRootView() {
    return findViewById(R.id.layoutContent);
  }
}
