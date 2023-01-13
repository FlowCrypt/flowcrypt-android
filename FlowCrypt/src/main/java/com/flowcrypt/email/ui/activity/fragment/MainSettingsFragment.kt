/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import androidx.preference.Preference
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.ui.activity.fragment.base.BasePreferenceFragment

/**
 * The main settings fragment.
 *
 * @author DenBond7
 * Date: 26.05.2017
 * Time: 10:13
 * E-mail: DenBond7@gmail.com
 */
class MainSettingsFragment : BasePreferenceFragment() {
  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    addPreferencesFromResource(R.xml.preferences_main_settings)

    findPreference<Preference>(getString(R.string.pref_key_backups))
      ?.setOnPreferenceClickListener {
        navController?.navigate(
          MainSettingsFragmentDirections.actionMainSettingsFragmentToSearchBackupsInEmailFragment()
        )
        true
      }

    findPreference<Preference>(getString(R.string.pref_key_security))
      ?.setOnPreferenceClickListener {
        navController?.navigate(
          MainSettingsFragmentDirections.actionMainSettingsFragmentToSecuritySettingsFragment()
        )
        true
      }

    findPreference<Preference>(getString(R.string.pref_key_contacts))
      ?.setOnPreferenceClickListener {
        navController?.navigate(
          MainSettingsFragmentDirections.actionMainSettingsFragmentToRecipientsListFragment()
        )
        true
      }

    findPreference<Preference>(getString(R.string.pref_key_keys))
      ?.setOnPreferenceClickListener {
        navController?.navigate(
          MainSettingsFragmentDirections.actionMainSettingsFragmentToPrivateKeysListFragment()
        )
        true
      }

    findPreference<Preference>(getString(R.string.pref_key_attester))
      ?.setOnPreferenceClickListener {
        navController?.navigate(
          MainSettingsFragmentDirections.actionMainSettingsFragmentToAttesterSettingsFragment()
        )
        true
      }

    findPreference<Preference>(getString(R.string.pref_key_notification))
      ?.setOnPreferenceClickListener {
        navController?.navigate(
          MainSettingsFragmentDirections.actionMainSettingsFragmentToNotificationsSettingsFragment()
        )
        true
      }

    findPreference<Preference>(getString(R.string.pref_key_server_settings))
      ?.setOnPreferenceClickListener {
        navController?.navigate(
          MainSettingsFragmentDirections.actionMainSettingsFragmentToServerSettingsFragment()
        )
        true
      }

    findPreference<Preference>(getString(R.string.pref_key_legal))
      ?.setOnPreferenceClickListener {
        navController?.navigate(
          MainSettingsFragmentDirections.actionMainSettingsFragmentToLegalSettingsFragment()
        )
        true
      }

    findPreference<Preference>(getString(R.string.pref_key_experimental))
      ?.setOnPreferenceClickListener {
        navController?.navigate(
          MainSettingsFragmentDirections.actionMainSettingsFragmentToExperimentalSettingsFragment()
        )
        true
      }
  }

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    super.onAccountInfoRefreshed(accountEntity)
    findPreference<Preference>(Constants.PREF_KEY_BACKUPS)?.isVisible =
      !(accountEntity?.hasClientConfigurationProperty(ClientConfiguration.ConfigurationProperty.NO_PRV_BACKUP)
        ?: false)

    if (accountEntity?.useAPI == false) {
      findPreference<Preference>(Constants.PREF_KEY_SERVER_SETTINGS)?.isVisible = true
    }
  }
}
