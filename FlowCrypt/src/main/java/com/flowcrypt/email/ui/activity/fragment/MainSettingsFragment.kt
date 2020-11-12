/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.AccountEntity
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
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    (activity as AppCompatActivity?)?.supportActionBar?.title = getString(R.string.title_activity_settings)
  }

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    addPreferencesFromResource(R.xml.preferences_main_settings)

    findPreference<Preference>(getString(R.string.pref_key_server_settings))?.setOnPreferenceClickListener {
      val fragment = ServerSettingsFragment()
      activity?.supportFragmentManager?.beginTransaction()
          ?.replace(R.id.fragmentContainerView, fragment, ServerSettingsFragment::class.java.simpleName)
          ?.addToBackStack(null)
          ?.commit()
      true
    }
  }

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    super.onAccountInfoRefreshed(accountEntity)
    findPreference<Preference>(Constants.PREF_KEY_BACKUPS)?.isVisible =
        !(accountEntity?.isRuleExist(AccountEntity.DomainRule.NO_PRV_BACKUP) ?: false)
  }
}
