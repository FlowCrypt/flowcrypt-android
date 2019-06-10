/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.preferences

import android.os.Bundle
import androidx.preference.Preference
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDaoSource
import com.flowcrypt.email.ui.activity.ChangePassPhraseActivity
import com.flowcrypt.email.ui.activity.fragment.base.BasePreferenceFragment
import com.flowcrypt.email.util.UIUtil

/**
 * This fragment contains actions which related to Security options.
 *
 * @author DenBond7
 * Date: 08.08.2018.
 * Time: 10:47.
 * E-mail: DenBond7@gmail.com
 */
class SecuritySettingsFragment : BasePreferenceFragment(), Preference.OnPreferenceClickListener {
  private var account: AccountDao? = null

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    addPreferencesFromResource(R.xml.preferences_security_settings)

    account = AccountDaoSource().getActiveAccountInformation(context!!)

    val preferenceChangePassPhrase = findPreference(Constants.PREFERENCES_KEY_SECURITY_CHANGE_PASS_PHRASE)
    if (preferenceChangePassPhrase != null) {
      preferenceChangePassPhrase.onPreferenceClickListener = this
    }
  }

  override fun onPreferenceClick(preference: Preference): Boolean {
    return when (preference.key) {
      Constants.PREFERENCES_KEY_SECURITY_CHANGE_PASS_PHRASE -> {
        if (UserIdEmailsKeysDaoSource().getLongIdsByEmail(context!!, account!!.email).isEmpty()) {
          UIUtil.showInfoSnackbar(view!!, getString(R.string.account_has_no_associated_keys, getString(R.string.support_email)))
        } else {
          startActivity(ChangePassPhraseActivity.newIntent(context, account))
        }
        true
      }

      else -> false
    }
  }
}
