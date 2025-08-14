/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.activity.fragment.preferences

import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.androidx.fragment.app.supportActionBar
import com.flowcrypt.email.ui.activity.fragment.base.BasePreferenceFragment

/**
 * This class describes general settings.
 *
 * @author Denys Bondarenko
 */
open class GeneralSettingsFragment : BasePreferenceFragment() {
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    supportActionBar?.title = getString(R.string.general)
  }

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.preferences_general_settings, rootKey)
  }

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    super.onAccountInfoRefreshed(accountEntity)
    findPreference<Preference>(Constants.PREF_KEY_ATTACHMENTS_CATEGORY)?.isVisible =
      accountEntity?.isHandlingAttachmentRestricted() == false
  }
}
