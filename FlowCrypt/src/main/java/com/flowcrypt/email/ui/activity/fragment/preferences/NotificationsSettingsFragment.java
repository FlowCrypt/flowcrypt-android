/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.preferences;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.ui.activity.fragment.base.BasePreferenceFragment;
import com.flowcrypt.email.util.SharedPreferencesHelper;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

/**
 * This class describes notification settings.
 *
 * @author Denis Bondarenko
 * Date: 19.07.2018
 * Time: 12:04
 * E-mail: DenBond7@gmail.com
 */
public class NotificationsSettingsFragment extends BasePreferenceFragment
    implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
  public static final String NOTIFICATION_LEVEL_ALL_MESSAGES = "all_messages";
  public static final String NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY = "encrypted_messages_only";
  public static final String NOTIFICATION_LEVEL_NEVER = "never";

  private CharSequence[] levels;
  private CharSequence[] entries;

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.preferences_notifications_settings);

    AccountDaoSource accountDaoSource = new AccountDaoSource();
    AccountDao account = accountDaoSource.getActiveAccountInformation(getContext());

    boolean isEncryptedModeEnabled = new AccountDaoSource().isEncryptedModeEnabled(getContext(), account.getEmail());

    if (isEncryptedModeEnabled) {
      levels = new CharSequence[]{NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY,
          NOTIFICATION_LEVEL_NEVER
      };
      entries = getResources().getStringArray(R.array.notification_level_encrypted_entries);
    } else {
      levels = new CharSequence[]{NOTIFICATION_LEVEL_ALL_MESSAGES,
          NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY,
          NOTIFICATION_LEVEL_NEVER
      };

      entries = getResources().getStringArray(R.array.notification_level_entries);
    }

    initPreferences(isEncryptedModeEnabled);
  }

  @Override
  public boolean onPreferenceClick(Preference preference) {
    switch (preference.getKey()) {
      case Constants.PREFERENCES_KEY_MANAGE_NOTIFICATIONS:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          Intent intent = new Intent();
          intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
          intent.putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID);
          startActivity(intent);
        }

        return true;

      default:
        return false;
    }
  }

  @Override
  public boolean onPreferenceChange(Preference preference, Object newValue) {
    switch (preference.getKey()) {
      case Constants.PREFERENCES_KEY_MESSAGES_NOTIFICATION_FILTER:
        ListPreference pref = (ListPreference) preference;
        preference.setSummary(generateSummary(newValue.toString(), pref.getEntryValues(), pref.getEntries()));
        return true;

      default:
        return false;
    }
  }

  protected void initPreferences(boolean isEncryptedModeEnabled) {
    Preference preferenceSettingsSecurity = findPreference(Constants.PREFERENCES_KEY_MANAGE_NOTIFICATIONS);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      preferenceSettingsSecurity.setOnPreferenceClickListener(this);
    } else {
      preferenceSettingsSecurity.setVisible(false);
    }

    ListPreference filter = (ListPreference) findPreference(Constants.PREFERENCES_KEY_MESSAGES_NOTIFICATION_FILTER);
    filter.setEntryValues(levels);
    filter.setEntries(entries);
    filter.setOnPreferenceChangeListener(this);

    String currentValue = SharedPreferencesHelper.getString(PreferenceManager.getDefaultSharedPreferences(
        getContext()), Constants.PREFERENCES_KEY_MESSAGES_NOTIFICATION_FILTER, "");

    if (isEncryptedModeEnabled && NOTIFICATION_LEVEL_ALL_MESSAGES.equals(currentValue)) {
      filter.setValue(NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY);
      currentValue = NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY;
    }

    filter.setSummary(generateSummary(currentValue, filter.getEntryValues(), filter.getEntries()));
  }
}
