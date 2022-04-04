/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.preferences

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R

/**
 * The main developer options fragment.
 *
 * @author Denis Bondarenko
 * Date: 10.07.2017
 * Time: 11:19
 * E-mail: DenBond7@gmail.com
 */
class MainDevPreferencesFragment : BaseDevPreferencesFragment(),
  SharedPreferences.OnSharedPreferenceChangeListener {

  private lateinit var sharedPreferences: SharedPreferences

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    (activity as AppCompatActivity?)?.supportActionBar?.title =
      getString(R.string.action_dev_settings)
  }

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.dev_preferences, rootKey)
    findPreference<SwitchPreference>(Constants.PREF_KEY_IS_DETECT_MEMORY_LEAK_ENABLED)?.isVisible =
      BuildConfig.FLAVOR == "dev"
  }

  override fun onDisplayPreferenceDialog(preference: Preference) {
    if (preference is BuildConfInfoPreference) {
      val dialogFragment = BuildConfigInfoPreferencesFragment.newInstance(preference.getKey())
      dialogFragment.setTargetFragment(this, 0)
      dialogFragment.show(parentFragmentManager, null)
    } else {
      super.onDisplayPreferenceDialog(preference)
    }
  }

  override fun onResume() {
    super.onResume()
    sharedPreferences.registerOnSharedPreferenceChangeListener(this)
  }

  override fun onPause() {
    super.onPause()
    sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
  }

  override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
    when (key) {
      Constants.PREF_KEY_IS_WRITE_LOGS_TO_FILE_ENABLED,
      Constants.PREF_KEY_IS_ACRA_ENABLED,
      Constants.PREF_KEY_IS_MAIL_DEBUG_ENABLED,
      Constants.PREF_KEY_IS_DETECT_MEMORY_LEAK_ENABLED -> showApplicationDetailsMainActivity()
    }
  }

  private fun showApplicationDetailsMainActivity() {
    Toast.makeText(
      activity,
      R.string.toast_message_press_force_stop_to_apply_changes,
      Toast.LENGTH_SHORT
    ).show()
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", requireActivity().packageName, null)
    intent.data = uri
    startActivity(intent)
  }
}
