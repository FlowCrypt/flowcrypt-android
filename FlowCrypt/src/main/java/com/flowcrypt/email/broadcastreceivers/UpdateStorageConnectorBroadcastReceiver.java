/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.flowcrypt.email.security.KeysStorageImpl;
import com.flowcrypt.email.util.GeneralUtil;

/**
 * This {@link BroadcastReceiver} updates {@link KeysStorageImpl} instances.
 *
 * @author Denis Bondarenko
 * Date: 2/25/19
 * Time: 4:28 PM
 * E-mail: DenBond7@gmail.com
 */
public class UpdateStorageConnectorBroadcastReceiver extends BroadcastReceiver {
  public static final String ACTION_UPDATE_STORAGE_CONNECTOR = GeneralUtil.generateUniqueExtraKey(
      "ACTION_UPDATE_STORAGE_CONNECTOR", UpdateStorageConnectorBroadcastReceiver.class);

  public static Intent newIntent(Context context) {
    Intent intent = new Intent(context, UpdateStorageConnectorBroadcastReceiver.class);
    intent.setAction(ACTION_UPDATE_STORAGE_CONNECTOR);

    return intent;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent != null && ACTION_UPDATE_STORAGE_CONNECTOR.equals(intent.getAction())) {
      KeysStorageImpl.getInstance(context).refresh(context);
    }
  }
}
