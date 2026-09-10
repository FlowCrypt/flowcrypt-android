/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.preferences

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.androidx.fragment.app.setFragmentResultListenerForTwoWayDialog
import com.flowcrypt.email.extensions.androidx.fragment.app.showTwoWayDialog
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.ui.activity.fragment.base.BasePreferenceFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.TwoWayDialogFragment
import com.flowcrypt.email.util.SharedPreferencesHelper

/**
 * This class describes notification settings.
 *
 * @author Denys Bondarenko
 */
open class NotificationsSettingsFragment : BasePreferenceFragment(),
  Preference.OnPreferenceClickListener,
  Preference.OnPreferenceChangeListener {

  private var levels: Array<CharSequence>? = null
  private var entries: Array<String>? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.POST_NOTIFICATIONS
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      showTwoWayDialog(
        requestCode = REQUEST_CODE_ASK_POST_NOTIFICATIONS_PERMISSION,
        dialogMsg = getString(R.string.need_post_notification_permission),
        positiveButtonTitle = getString(R.string.manage_notifications),
        negativeButtonTitle = getString(R.string.cancel),
      )
    }
  }

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.preferences_notifications_settings, rootKey)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.notifications)

    subscribeToTwoWayDialog()
  }

  override fun onPreferenceClick(preference: Preference): Boolean {
    return when (preference.key) {
      Constants.PREF_KEY_MANAGE_NOTIFICATIONS -> {
        openSettings()
        true
      }

      else -> false
    }
  }

  override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
    return when (preference.key) {
      Constants.PREF_KEY_MESSAGES_NOTIFICATION_FILTER -> {
        val pref = preference as ListPreference
        preference.setSummary(generateSummary(newValue.toString(), pref.entryValues, pref.entries))
        true
      }

      else -> false
    }
  }

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    if (accountEntity != null) {
      val isOnlyPgpModeEnabled = accountEntity.showOnlyEncrypted

      if (isOnlyPgpModeEnabled == true) {
        levels = arrayOf(
          NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY,
          NOTIFICATION_LEVEL_NEVER
        )
        entries = resources.getStringArray(R.array.notification_level_encrypted_entries)
      } else {
        levels = arrayOf(
          NOTIFICATION_LEVEL_ALL_MESSAGES,
          NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY,
          NOTIFICATION_LEVEL_NEVER
        )
        entries = resources.getStringArray(R.array.notification_level_entries)
      }

      initPreferences(isOnlyPgpModeEnabled == true)
    } else {
      val intent = Intent(context, MainActivity::class.java)
      intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      startActivity(intent)
      requireActivity().finish()
    }
  }

  private fun initPreferences(isEncryptedModeEnabled: Boolean) {
    val preferenceSettingsSecurity =
      findPreference<Preference>(Constants.PREF_KEY_MANAGE_NOTIFICATIONS)
    preferenceSettingsSecurity?.onPreferenceClickListener = this

    val filter =
      findPreference<Preference>(Constants.PREF_KEY_MESSAGES_NOTIFICATION_FILTER) as ListPreference
    filter.entryValues = levels
    filter.entries = entries
    filter.onPreferenceChangeListener = this

    var currentValue = SharedPreferencesHelper.getString(
      PreferenceManager.getDefaultSharedPreferences(
        requireContext()
      ), Constants.PREF_KEY_MESSAGES_NOTIFICATION_FILTER, ""
    )

    if (isEncryptedModeEnabled && NOTIFICATION_LEVEL_ALL_MESSAGES == currentValue) {
      filter.value = NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY
      currentValue = NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY
    }

    filter.summary = generateSummary(currentValue!!, filter.entryValues, filter.entries)
  }

  private fun subscribeToTwoWayDialog() {
    setFragmentResultListenerForTwoWayDialog { _, bundle ->
      val requestCode = bundle.getInt(TwoWayDialogFragment.KEY_REQUEST_CODE)
      val result = bundle.getInt(TwoWayDialogFragment.KEY_RESULT)

      when (requestCode) {
        REQUEST_CODE_ASK_POST_NOTIFICATIONS_PERMISSION -> {
          if (result == TwoWayDialogFragment.RESULT_OK) {
            openSettings()
          }
        }
      }
    }
  }

  private fun openSettings() {
    val intent = Intent()
    intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
    intent.putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
    startActivity(intent)
  }

  companion object {
    const val NOTIFICATION_LEVEL_ALL_MESSAGES = "all_messages"
    const val NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY = "encrypted_messages_only"
    const val NOTIFICATION_LEVEL_NEVER = "never"
    private const val REQUEST_CODE_ASK_POST_NOTIFICATIONS_PERMISSION = 10
  }
}
