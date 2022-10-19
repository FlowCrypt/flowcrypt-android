/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.preferences

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResultListener
import androidx.preference.Preference
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.extensions.getNavigationResult
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.showInfoDialog
import com.flowcrypt.email.extensions.showNeedPassphraseDialog
import com.flowcrypt.email.extensions.supportActionBar
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.ui.activity.fragment.RecheckProvidedPassphraseFragment
import com.flowcrypt.email.ui.activity.fragment.base.BasePreferenceFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.FixNeedPassphraseIssueDialogFragment

/**
 * This fragment contains actions which related to Security options.
 *
 * @author DenBond7
 * Date: 08.08.2018.
 * Time: 10:47.
 * E-mail: DenBond7@gmail.com
 */
class SecuritySettingsFragment : BasePreferenceFragment(), Preference.OnPreferenceClickListener {
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    supportActionBar?.title = getString(R.string.security_and_privacy)

    observeOnResultLiveData()
    subscribeFixNeedPassphraseIssueDialogFragment()
  }

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.preferences_security_settings, rootKey)
    findPreference<Preference>(Constants.PREF_KEY_SECURITY_CHANGE_PASS_PHRASE)?.onPreferenceClickListener =
      this
  }

  override fun onPreferenceClick(preference: Preference): Boolean {
    return when (preference.key) {
      Constants.PREF_KEY_SECURITY_CHANGE_PASS_PHRASE -> {
        val keysStorage = KeysStorageImpl.getInstance(requireContext())
        if (keysStorage.getRawKeys().isEmpty()) {
          showInfoDialog(
            dialogMsg = getString(
              R.string.account_has_no_associated_keys,
              getString(R.string.support_email)
            )
          )
        } else {
          val fingerprints = keysStorage.getFingerprintsWithEmptyPassphrase()
          if (fingerprints.isNotEmpty()) {
            showNeedPassphraseDialog(
              fingerprints = fingerprints,
              logicType = FixNeedPassphraseIssueDialogFragment.LogicType.ALL
            )
          } else {
            navigateToCheckPassphraseStrengthFragment()
          }
        }
        true
      }

      else -> false
    }
  }

  private fun observeOnResultLiveData() {
    getNavigationResult<Result<*>>(RecheckProvidedPassphraseFragment.KEY_ACCEPTED_PASSPHRASE_RESULT) {
      if (it.isSuccess) {
        val passphrase = it.getOrNull() as? CharArray ?: return@getNavigationResult
        account?.let { accountEntity ->
          navController?.navigate(
            SecuritySettingsFragmentDirections
              .actionSecuritySettingsFragmentToChangePassphraseOfImportedKeysFragment(
                popBackStackIdIfSuccess = R.id.securitySettingsFragment,
                title = getString(R.string.pass_phrase_changed),
                subTitle = getString(R.string.passphrase_was_changed),
                passphrase = String(passphrase),
                accountEntity = accountEntity
              )
          )
        }
      }
    }
  }

  private fun subscribeFixNeedPassphraseIssueDialogFragment() {
    setFragmentResultListener(FixNeedPassphraseIssueDialogFragment.REQUEST_KEY_RESULT) { _, _ ->
      navigateToCheckPassphraseStrengthFragment()
    }
  }

  private fun navigateToCheckPassphraseStrengthFragment() {
    navController?.navigate(
      SecuritySettingsFragmentDirections
        .actionSecuritySettingsFragmentToCheckPassphraseStrengthFragment(
          popBackStackIdIfSuccess = R.id.securitySettingsFragment,
          title = getString(R.string.change_pass_phrase),
          lostPassphraseTitle = getString(R.string.loss_of_this_pass_phrase_cannot_be_recovered)
        )
    )
  }
}
