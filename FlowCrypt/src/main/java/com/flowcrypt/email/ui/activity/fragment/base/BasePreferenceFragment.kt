/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base

import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat

/**
 * The base realization of [PreferenceFragmentCompat].
 *
 * @author DenBond7
 * Date: 26.05.2017
 * Time: 10:16
 * E-mail: DenBond7@gmail.com
 */

abstract class BasePreferenceFragment : PreferenceFragmentCompat() {
  /**
   * This method helps to generate a summary for [ListPreference]
   *
   * @param value   The preference current value;
   * @param values  The preferences entry values;
   * @param entries The preferences entries;
   * @return A generated summary.
   */
  protected fun generateSummary(value: String, values: Array<CharSequence>, entries: Array<CharSequence>): CharSequence {
    for (i in values.indices) {
      if (values[i] == value) {
        return entries[i]
      }
    }
    return ""
  }
}
