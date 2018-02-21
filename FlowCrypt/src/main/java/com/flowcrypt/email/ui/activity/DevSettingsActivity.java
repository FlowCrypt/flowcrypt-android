/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.flowcrypt.email.ui.activity.fragment.preferences.MainDevPreferencesFragment;

/**
 * The developer setting activity. This activity will be used to setup the debug process.
 *
 * @author Denis Bondarenko
 *         Date: 10.07.2017
 *         Time: 10:55
 *         E-mail: DenBond7@gmail.com
 */
public class DevSettingsActivity extends PreferenceActivity {
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new MainDevPreferencesFragment()).commit();
    }
}
