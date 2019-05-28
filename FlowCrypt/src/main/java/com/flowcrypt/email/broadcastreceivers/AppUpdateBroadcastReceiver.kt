/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.util.SharedPreferencesHelper;

import androidx.preference.PreferenceManager;

/**
 * This {@link BroadcastReceiver} can be used to run some actions when the app will be updated.
 *
 * @author Denis Bondarenko
 * Date: 3/15/19
 * Time: 10:34 AM
 * E-mail: DenBond7@gmail.com
 */
public class AppUpdateBroadcastReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent != null && Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
      SharedPreferencesHelper.setBoolean(PreferenceManager
          .getDefaultSharedPreferences(context), Constants.PREFERENCES_KEY_IS_CHECK_KEYS_NEEDED, true);
    }
  }
}
