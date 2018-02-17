/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
