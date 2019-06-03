/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings;

import android.view.View;

import com.flowcrypt.email.R;

/**
 * This activity contains actions which related to Security options.
 *
 * @author DenBond7
 * Date: 08.08.2018.
 * Time: 10:48.
 * E-mail: DenBond7@gmail.com
 */
public class SecuritySettingsActivity extends BaseSettingsActivity {
  @Override
  public int getContentViewResourceId() {
    return R.layout.activity_security_settings;
  }

  @Override
  public View getRootView() {
    return null;
  }
}
