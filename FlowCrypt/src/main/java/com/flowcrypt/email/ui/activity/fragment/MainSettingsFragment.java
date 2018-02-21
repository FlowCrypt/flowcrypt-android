/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment;

import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.widget.Toast;

import com.flowcrypt.email.Constants;
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


public class MainSettingsFragment extends BasePreferenceFragment implements Preference.OnPreferenceClickListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_main_settings);

        Preference preferenceSettingsSecurity = findPreference(Constants.PREFERENCES_KEY_SETTINGS_SECURITY);
        if (preferenceSettingsSecurity != null) {
            preferenceSettingsSecurity.setOnPreferenceClickListener(this);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case Constants.PREFERENCES_KEY_SETTINGS_SECURITY:
                Toast.makeText(getContext(), R.string.security_settings_were_not_implemented_yet,
                        Toast.LENGTH_SHORT).show();
                return true;

            default:
                return false;
        }
    }
}
