/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
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

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    addPreferencesFromResource(R.xml.preferences_main_settings)
  }

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    super.onAccountInfoRefreshed(accountEntity)
    findPreference<Preference>(Constants.PREF_KEY_BACKUPS)?.isVisible =
        !(accountEntity?.isRuleExist(AccountEntity.DomainRule.NO_PRV_BACKUP) ?: false)
  }
}
