/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.preferences

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.ui.activity.SignInActivity
import com.flowcrypt.email.ui.activity.fragment.base.BasePreferenceFragment
import com.flowcrypt.email.util.SharedPreferencesHelper

/**
 * This class describes notification settings.
 *
 * @author Denis Bondarenko
 * Date: 19.07.2018
 * Time: 12:04
 * E-mail: DenBond7@gmail.com
 */
open class NotificationsSettingsFragment : BasePreferenceFragment(), Preference.OnPreferenceClickListener,
    Preference.OnPreferenceChangeListener {

  private var levels: Array<CharSequence>? = null
  private var entries: Array<String>? = null

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    addPreferencesFromResource(R.xml.preferences_notifications_settings)

    val accountDaoSource = AccountDaoSource()
    val account = accountDaoSource.getActiveAccountInformation(context!!)

    if (account != null) {
      val isEncryptedModeEnabled = AccountDaoSource().isEncryptedModeEnabled(context!!, account.email)

      if (isEncryptedModeEnabled) {
        levels = arrayOf(
            NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY,
            NOTIFICATION_LEVEL_NEVER)
        entries = resources.getStringArray(R.array.notification_level_encrypted_entries)
      } else {
        levels = arrayOf(
            NOTIFICATION_LEVEL_ALL_MESSAGES,
            NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY,
            NOTIFICATION_LEVEL_NEVER)
        entries = resources.getStringArray(R.array.notification_level_entries)
      }

      initPreferences(isEncryptedModeEnabled)
    } else {
      val intent = Intent(context, SignInActivity::class.java)
      intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      startActivity(intent)
      activity!!.finish()
    }
  }

  override fun onPreferenceClick(preference: Preference?): Boolean {
    return when (preference?.key) {
      Constants.PREF_KEY_MANAGE_NOTIFICATIONS -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          val intent = Intent()
          intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
          intent.putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
          startActivity(intent)
        }

        true
      }

      else -> false
    }
  }

  override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
    return when (preference?.key) {
      Constants.PREF_KEY_MESSAGES_NOTIFICATION_FILTER -> {
        val pref = preference as ListPreference
        preference.setSummary(generateSummary(newValue.toString(), pref.entryValues, pref.entries))
        true
      }

      else -> false
    }
  }

  private fun initPreferences(isEncryptedModeEnabled: Boolean) {
    val preferenceSettingsSecurity = findPreference(Constants.PREF_KEY_MANAGE_NOTIFICATIONS)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      preferenceSettingsSecurity.onPreferenceClickListener = this
    } else {
      preferenceSettingsSecurity.isVisible = false
    }

    val filter = findPreference(Constants.PREF_KEY_MESSAGES_NOTIFICATION_FILTER) as ListPreference
    filter.entryValues = levels
    filter.entries = entries
    filter.onPreferenceChangeListener = this

    var currentValue = SharedPreferencesHelper.getString(PreferenceManager.getDefaultSharedPreferences(
        context!!), Constants.PREF_KEY_MESSAGES_NOTIFICATION_FILTER, "")

    if (isEncryptedModeEnabled && NOTIFICATION_LEVEL_ALL_MESSAGES == currentValue) {
      filter.value = NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY
      currentValue = NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY
    }

    filter.summary = generateSummary(currentValue!!, filter.entryValues, filter.entries)
  }

  companion object {
    const val NOTIFICATION_LEVEL_ALL_MESSAGES = "all_messages"
    const val NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY = "encrypted_messages_only"
    const val NOTIFICATION_LEVEL_NEVER = "never"
  }
}
