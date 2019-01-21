/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.preferences;

import android.os.Bundle;

import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.fragment.base.BasePreferenceFragment;

/**
 * @author DenBond7
 * Date: 29.09.2017.
 * Time: 22:46.
 * E-mail: DenBond7@gmail.com
 */
public class ExperimentalSettingsFragment extends BasePreferenceFragment {
  @Override
  public void onCreatePreferences(Bundle bundle, String s) {
    addPreferencesFromResource(R.xml.preferences_experimental_settings);
  }
}
