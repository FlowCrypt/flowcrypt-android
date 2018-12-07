/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.flowcrypt.email.service.MessagesNotificationManager;
import com.google.android.gms.common.util.CollectionUtils;

import java.util.ArrayList;

/**
 * This {@link BroadcastReceiver} will be used by {@link MessagesNotificationManager} to mark messages as old.
 *
 * @author Denis Bondarenko
 * Date: 03.07.2018
 * Time: 16:29
 * E-mail: DenBond7@gmail.com
 */
public class MarkMessagesAsOldBroadcastReceiver extends BroadcastReceiver {

  public static final String ACTION_MARK_MESSAGES_AS_OLD =
      BuildConfig.APPLICATION_ID + ".ACTION_MARK_MESSAGES_AS_OLD";
  public static final String EXTRA_KEY_UID_LIST = BuildConfig.APPLICATION_ID + ".EXTRA_KEY_UID_LIST";
  public static final String EXTRA_KEY_EMAIL = BuildConfig.APPLICATION_ID + ".EXTRA_KEY_EMAIL";
  public static final String EXTRA_KEY_LABEL = BuildConfig.APPLICATION_ID + ".EXTRA_KEY_LABEL";

  private static final String TAG = MarkMessagesAsOldBroadcastReceiver.class.getSimpleName();

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d(TAG, "onReceive");
    if (intent == null || !ACTION_MARK_MESSAGES_AS_OLD.equals(intent.getAction())) {
      return;
    }

    String email = intent.getStringExtra(EXTRA_KEY_EMAIL);
    String label = intent.getStringExtra(EXTRA_KEY_LABEL);

    ArrayList<String> uidList = intent.getStringArrayListExtra(EXTRA_KEY_UID_LIST);

    if (!CollectionUtils.isEmpty(uidList)) {
      new MessageDaoSource().setOldStatusForLocalMsgs(context, email, label, uidList);
    }
  }
}
