/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.util.GeneralUtil

/**
 * This [BroadcastReceiver] updates [KeysStorageImpl] instances.
 *
 * @author Denis Bondarenko
 * Date: 2/25/19
 * Time: 4:28 PM
 * E-mail: DenBond7@gmail.com
 */
class UpdateStorageConnectorBroadcastReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent?) {
    if (intent != null && ACTION_UPDATE_STORAGE_CONNECTOR == intent.action) {
      KeysStorageImpl.getInstance(context).refresh(context)
    }
  }

  companion object {
    @JvmField
    val ACTION_UPDATE_STORAGE_CONNECTOR = GeneralUtil.generateUniqueExtraKey(
        "ACTION_UPDATE_STORAGE_CONNECTOR", UpdateStorageConnectorBroadcastReceiver::class.java)

    @JvmStatic
    fun newIntent(context: Context): Intent {
      val intent = Intent(context, UpdateStorageConnectorBroadcastReceiver::class.java)
      intent.action = ACTION_UPDATE_STORAGE_CONNECTOR

      return intent
    }
  }
}
