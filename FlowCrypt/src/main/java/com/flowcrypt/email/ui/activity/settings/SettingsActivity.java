/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings;

import android.view.View;

import com.flowcrypt.email.R;

/**
 * The settings activity which contains all application settings.
 *
 * @author DenBond7
 *         Date: 26.05.2017
 *         Time: 9:15
 *         E-mail: DenBond7@gmail.com
 */

public class SettingsActivity extends BaseSettingsActivity {

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_settings;
    }

    @Override
    public View getRootView() {
        return null;
    }
}
