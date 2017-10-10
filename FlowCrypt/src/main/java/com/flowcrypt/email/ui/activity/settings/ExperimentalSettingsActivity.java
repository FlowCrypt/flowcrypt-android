/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings;

import android.view.View;

import com.flowcrypt.email.R;

/**
 * @author DenBond7
 *         Date: 29.09.2017.
 *         Time: 22:42.
 *         E-mail: DenBond7@gmail.com
 */
public class ExperimentalSettingsActivity extends BaseSettingsActivity {
    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_experimental_settings;
    }

    @Override
    public View getRootView() {
        return null;
    }
}
