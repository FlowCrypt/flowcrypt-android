/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import com.flowcrypt.email.ui.activity.CorruptedStorageActivity
import com.flowcrypt.email.util.GeneralUtil

/**
 * This [BroadcastReceiver] handles situations when the app storage space was corrupted. This can
 * happen on certain rooted or unofficial systems, due to user intervention, etc.
 *
 * @author DenBond7
 * Date: 12/14/2018
 * Time: 12:00
 * E-mail: DenBond7@gmail.com
 */
class CorruptedStorageBroadcastReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    if (GeneralUtil.isAppForegrounded()) {
      val newIntent = Intent(context, CorruptedStorageActivity::class.java)
      newIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      context.startActivity(newIntent)
    }
  }
}
