/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.preferences;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.ui.activity.fragment.base.BasePreferenceFragment;
import com.flowcrypt.email.util.SharedPreferencesHelper;

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

  private CharSequence[] notificationLevels;
  private CharSequence[] notificationEntries;

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.preferences_notifications_settings);

    AccountDaoSource accountDaoSource = new AccountDaoSource();
    AccountDao accountDao = accountDaoSource.getActiveAccountInformation(getContext());

    boolean isShowOnlyEncryptedMessages = new AccountDaoSource().isShowOnlyEncryptedMessages(getContext(),
        accountDao.getEmail());

    if (isShowOnlyEncryptedMessages) {
      notificationLevels = new CharSequence[]{NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY,
          NOTIFICATION_LEVEL_NEVER
      };
      notificationEntries = getResources().getStringArray(R.array.notification_level_encrypted_entries);
    } else {
      notificationLevels = new CharSequence[]{NOTIFICATION_LEVEL_ALL_MESSAGES,
          NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY,
          NOTIFICATION_LEVEL_NEVER
      };

      notificationEntries = getResources().getStringArray(R.array.notification_level_entries);
    }

    initPreferences(isShowOnlyEncryptedMessages);
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
        ListPreference listPreference = (ListPreference) preference;
        preference.setSummary(generateSummaryListPreferences(newValue.toString(), listPreference
            .getEntryValues(), listPreference.getEntries()));
        return true;

      default:
        return false;
    }
  }

  protected void initPreferences(boolean isShowOnlyEncryptedMessages) {
    Preference preferenceSettingsSecurity = findPreference(Constants.PREFERENCES_KEY_MANAGE_NOTIFICATIONS);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      preferenceSettingsSecurity.setOnPreferenceClickListener(this);
    } else {
      preferenceSettingsSecurity.setVisible(false);
    }

    ListPreference listPreferenceNotificationsFilter = (ListPreference) findPreference(Constants
        .PREFERENCES_KEY_MESSAGES_NOTIFICATION_FILTER);
    listPreferenceNotificationsFilter.setEntryValues(notificationLevels);
    listPreferenceNotificationsFilter.setEntries(notificationEntries);
    listPreferenceNotificationsFilter.setOnPreferenceChangeListener(this);

    String currentValue = SharedPreferencesHelper.getString(PreferenceManager.getDefaultSharedPreferences(
        getContext()), Constants.PREFERENCES_KEY_MESSAGES_NOTIFICATION_FILTER, "");

    if (isShowOnlyEncryptedMessages && NOTIFICATION_LEVEL_ALL_MESSAGES.equals(currentValue)) {
      listPreferenceNotificationsFilter.setValue(NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY);
      currentValue = NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY;
    }

    listPreferenceNotificationsFilter.setSummary(generateSummaryListPreferences(currentValue,
        listPreferenceNotificationsFilter.getEntryValues(),
        listPreferenceNotificationsFilter.getEntries()));
  }
}
