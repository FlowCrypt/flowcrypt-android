/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.doBaseUISetup
import com.flowcrypt.email.jetpack.viewmodel.AccountViewModel

/**
 * The base realization of [PreferenceFragmentCompat].
 *
 * @author Denys Bondarenko
 */
abstract class BasePreferenceFragment : PreferenceFragmentCompat(), UiUxSettings {
  protected val accountViewModel: AccountViewModel by viewModels()
  protected val account: AccountEntity?
    get() = accountViewModel.activeAccountLiveData.value

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    doBaseUISetup(this)
    initAccountViewModel()
  }

  /**
   * This method helps to generate a summary for [ListPreference]
   *
   * @param value   The preference current value;
   * @param values  The preferences entry values;
   * @param entries The preferences entries;
   * @return A generated summary.
   */
  protected fun generateSummary(
    value: String, values: Array<CharSequence>, entries: Array<CharSequence>
  ): CharSequence {
    for (i in values.indices) {
      if (values[i] == value) {
        return entries[i]
      }
    }
    return ""
  }

  open fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {}

  private fun initAccountViewModel() {
    accountViewModel.activeAccountLiveData.observe(viewLifecycleOwner) {
      onAccountInfoRefreshed(account)
    }
  }
}
