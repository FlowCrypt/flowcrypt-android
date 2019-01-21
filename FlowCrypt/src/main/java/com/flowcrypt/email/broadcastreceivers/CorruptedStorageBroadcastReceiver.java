/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.flowcrypt.email.ui.activity.CorruptedStorageActivity;
import com.flowcrypt.email.util.GeneralUtil;

/**
 * This {@link BroadcastReceiver} handles situations when the app storage space was corrupted. This can
 * happen on certain rooted or unofficial systems, due to user intervention, etc.
 *
 * @author DenBond7
 * Date: 12/14/2018
 * Time: 12:00
 * E-mail: DenBond7@gmail.com
 */
public class CorruptedStorageBroadcastReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    if (GeneralUtil.isAppForegrounded()) {
      Intent newIntent = new Intent(context, CorruptedStorageActivity.class);
      newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
      context.startActivity(newIntent);
    }
  }
}
