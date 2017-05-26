package com.flowcrypt.email.ui.activity.fragment;

import android.os.Bundle;

import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.fragment.base.BasePreferenceFragment;

/**
 * The main settings fragment.
 *
 * @author DenBond7
 *         Date: 26.05.2017
 *         Time: 10:13
 *         E-mail: DenBond7@gmail.com
 */

public class MainSettingsFragment extends BasePreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_main_settings);
    }
}
