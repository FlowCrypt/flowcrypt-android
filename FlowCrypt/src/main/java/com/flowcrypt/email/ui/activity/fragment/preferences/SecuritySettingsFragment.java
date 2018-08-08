/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.preferences;

import android.os.Bundle;
import android.support.v7.preference.Preference;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.ui.activity.ChangePassPhraseActivity;
import com.flowcrypt.email.ui.activity.fragment.base.BasePreferenceFragment;

/**
 * This fragment contains actions which related to Security options.
 *
 * @author DenBond7
 * Date: 08.08.2018.
 * Time: 10:47.
 * E-mail: DenBond7@gmail.com
 */
public class SecuritySettingsFragment extends BasePreferenceFragment implements Preference.OnPreferenceClickListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_security_settings);

        Preference preferenceChangePassPhrase = findPreference(Constants.PREFERENCES_KEY_SECURITY_CHANGE_PASS_PHRASE);
        if (preferenceChangePassPhrase != null) {
            preferenceChangePassPhrase.setOnPreferenceClickListener(this);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case Constants.PREFERENCES_KEY_SECURITY_CHANGE_PASS_PHRASE:
                startActivity(ChangePassPhraseActivity.newIntent(getContext(), new AccountDaoSource()
                        .getActiveAccountInformation(getContext())));
                return true;

            default:
                return false;
        }
    }
}
