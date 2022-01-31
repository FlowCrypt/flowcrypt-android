/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.preferences

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import com.flowcrypt.email.R
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.ui.activity.fragment.base.BasePreferenceFragment
import com.flowcrypt.email.util.GeneralUtil

/**
 * @author DenBond7
 * Date: 29.09.2017.
 * Time: 22:46.
 * E-mail: DenBond7@gmail.com
 */
class ExperimentalSettingsFragment : BasePreferenceFragment() {

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    (activity as AppCompatActivity?)?.supportActionBar?.title =
      getString(R.string.experimental_settings)
  }

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.preferences_experimental_settings, rootKey)

    if (GeneralUtil.isDebugBuild()) {
      val preference = Preference(requireContext())
      preference.setTitle(R.string.action_dev_settings)
      preference.isIconSpaceReserved = false
      preference.setOnPreferenceClickListener {
        navController?.navigate(
          ExperimentalSettingsFragmentDirections
            .actionExperimentalSettingsFragmentToMainDevPreferencesFragment()
        )
        true
      }

      preferenceScreen.addPreference(preference)
    }
  }
}
