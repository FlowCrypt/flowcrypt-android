package com.flowcrypt.email.ui.activity.settings;

import android.view.Menu;

import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity;

/**
 * The settings activity which contains all application settings.
 *
 * @author DenBond7
 *         Date: 26.05.2017
 *         Time: 9:15
 *         E-mail: DenBond7@gmail.com
 */

public class SettingsActivity extends BaseBackStackActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_settings, menu);
        return true;
    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_settings;
    }
}
