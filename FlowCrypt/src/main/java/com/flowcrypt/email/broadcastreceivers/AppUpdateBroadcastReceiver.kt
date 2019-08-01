/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import com.flowcrypt.email.Constants
import com.flowcrypt.email.util.SharedPreferencesHelper

/**
 * This [BroadcastReceiver] can be used to run some actions when the app will be updated.
 *
 * @author Denis Bondarenko
 * Date: 3/15/19
 * Time: 10:34 AM
 * E-mail: DenBond7@gmail.com
 */
class AppUpdateBroadcastReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent?) {
    if (intent != null && Intent.ACTION_MY_PACKAGE_REPLACED == intent.action) {
      SharedPreferencesHelper.setBoolean(PreferenceManager
          .getDefaultSharedPreferences(context), Constants.PREF_KEY_IS_CHECK_KEYS_NEEDED, true)
    }
  }
}
