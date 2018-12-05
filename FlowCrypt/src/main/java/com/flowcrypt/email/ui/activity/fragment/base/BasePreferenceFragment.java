/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

/**
 * The base realization of {@link PreferenceFragmentCompat}.
 *
 * @author DenBond7
 * Date: 26.05.2017
 * Time: 10:16
 * E-mail: DenBond7@gmail.com
 */

public abstract class BasePreferenceFragment extends PreferenceFragmentCompat {
  /**
   * This method helps to generate a summary for {@link ListPreference}
   *
   * @param value   The preference current value;
   * @param values  The preferences entry values;
   * @param entries The preferences entries;
   * @return A generated summary.
   */
  protected CharSequence generateSummary(String value, CharSequence[] values, CharSequence[] entries) {
    for (int i = 0; i < values.length; i++) {
      if (values[i].equals(value)) {
        return entries[i];
      }
    }
    return "";
  }
}
