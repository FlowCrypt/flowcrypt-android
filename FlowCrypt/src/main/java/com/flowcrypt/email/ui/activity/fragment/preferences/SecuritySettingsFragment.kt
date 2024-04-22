/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.preferences

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.NavDirections
import androidx.preference.Preference
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.extensions.androidx.fragment.app.getNavigationResult
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.extensions.androidx.fragment.app.showInfoDialog
import com.flowcrypt.email.extensions.androidx.fragment.app.showNeedPassphraseDialog
import com.flowcrypt.email.extensions.androidx.fragment.app.supportActionBar
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.ui.activity.fragment.CheckPassphraseStrengthFragmentArgs
import com.flowcrypt.email.ui.activity.fragment.RecheckProvidedPassphraseFragment
import com.flowcrypt.email.ui.activity.fragment.base.BasePreferenceFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.FixNeedPassphraseIssueDialogFragment
import com.flowcrypt.email.util.GeneralUtil

/**
 * This fragment contains actions which related to Security options.
 *
 * @author Denys Bondarenko
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
              requestKey = REQUEST_KEY_FIX_MISSING_PASSPHRASE,
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
    setFragmentResultListener(REQUEST_KEY_FIX_MISSING_PASSPHRASE) { _, _ ->
      navigateToCheckPassphraseStrengthFragment()
    }
  }

  private fun navigateToCheckPassphraseStrengthFragment() {
    navController?.navigate(
      object : NavDirections {
        override val actionId = R.id.pass_phrase_strength_graph
        override val arguments = CheckPassphraseStrengthFragmentArgs(
          popBackStackIdIfSuccess = R.id.securitySettingsFragment,
          title = getString(R.string.change_pass_phrase),
          lostPassphraseTitle = getString(R.string.loss_of_this_pass_phrase_cannot_be_recovered)
        ).toBundle()
      }
    )
  }

  companion object {
    private val REQUEST_KEY_FIX_MISSING_PASSPHRASE = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_FIX_MISSING_PASSPHRASE",
      SecuritySettingsFragment::class.java
    )
  }
}
