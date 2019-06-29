/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.preferences

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.preference.Preference
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
class MainDevPreferencesFragment : BaseDevPreferencesFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

  private var sharedPreferences: SharedPreferences? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
  }

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    addPreferencesFromResource(R.xml.dev_preferences)
  }

  override fun onDisplayPreferenceDialog(preference: Preference?) {
    if (preference is BuildConfInfoPreference) {
      val dialogFragment = BuildConfigInfoPreferencesFragment.newInstance(preference.getKey())
      dialogFragment.setTargetFragment(this, 0)
      dialogFragment.show(fragmentManager!!, null)
    } else {
      super.onDisplayPreferenceDialog(preference)
    }
  }

  override fun onResume() {
    super.onResume()
    if (sharedPreferences != null) {
      sharedPreferences!!.registerOnSharedPreferenceChangeListener(this)
    }
  }

  override fun onPause() {
    super.onPause()
    if (sharedPreferences != null) {
      sharedPreferences!!.unregisterOnSharedPreferenceChangeListener(this)
    }
  }

  override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
    when (key) {
      Constants.PREFERENCES_KEY_IS_WRITE_LOGS_TO_FILE_ENABLED -> if (sharedPreferences?.getBoolean(key, false) ==
          true) {
        val isPermissionGranted = ContextCompat.checkSelfPermission(activity!!, Manifest.permission
            .WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        if (isPermissionGranted) {
          showApplicationDetailsSettingsActivity()
        } else {
          requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
              REQUEST_CODE_REQUEST_WRITE_EXTERNAL_PERMISSION_FOR_LOGS)
        }
      } else {
        showApplicationDetailsSettingsActivity()
      }

      Constants.PREFERENCES_KEY_IS_DETECT_MEMORY_LEAK_ENABLED,
      Constants.PREFERENCES_KEY_IS_ACRA_ENABLED,
      Constants.PREFERENCES_KEY_IS_MAIL_DEBUG_ENABLED -> showApplicationDetailsSettingsActivity()
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    when (requestCode) {
      REQUEST_CODE_REQUEST_WRITE_EXTERNAL_PERMISSION_FOR_LOGS -> if (grantResults.size == 1 && grantResults[0] ==
          PackageManager.PERMISSION_GRANTED) {
        showApplicationDetailsSettingsActivity()
      } else {
        Toast.makeText(activity, "Access not granted to write logs!!!", Toast.LENGTH_SHORT).show()
      }

      else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
  }

  private fun showApplicationDetailsSettingsActivity() {
    Toast.makeText(activity, R.string.toast_message_press_force_stop_to_apply_changes, Toast.LENGTH_SHORT).show()
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", activity!!.packageName, null)
    intent.data = uri
    startActivity(intent)
  }

  companion object {
    private const val REQUEST_CODE_REQUEST_WRITE_EXTERNAL_PERMISSION_FOR_LOGS = 100
  }
}
