/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.flowcrypt.email.service.MessagesNotificationManager;
import com.flowcrypt.email.util.GeneralUtil;
import com.google.android.gms.common.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * This {@link BroadcastReceiver} will be used by {@link MessagesNotificationManager} to mark messages as old.
 *
 * @author Denis Bondarenko
 * Date: 03.07.2018
 * Time: 16:29
 * E-mail: DenBond7@gmail.com
 */
public class MarkMessagesAsOldBroadcastReceiver extends BroadcastReceiver {

  public static final String ACTION_MARK_MESSAGES_AS_OLD = GeneralUtil.generateUniqueExtraKey(
      "ACTION_MARK_MESSAGES_AS_OLD", MarkMessagesAsOldBroadcastReceiver.class);
  public static final String EXTRA_KEY_UID_LIST = GeneralUtil.generateUniqueExtraKey(
      "EXTRA_KEY_UID_LIST", MarkMessagesAsOldBroadcastReceiver.class);
  public static final String EXTRA_KEY_EMAIL = GeneralUtil.generateUniqueExtraKey(
      "EXTRA_KEY_EMAIL", MarkMessagesAsOldBroadcastReceiver.class);
  public static final String EXTRA_KEY_LABEL = GeneralUtil.generateUniqueExtraKey(
      "EXTRA_KEY_LABEL", MarkMessagesAsOldBroadcastReceiver.class);

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
    int step = 500;

    if (!CollectionUtils.isEmpty(uidList)) {
      MessageDaoSource daoSource = new MessageDaoSource();
      if (uidList.size() <= step) {
        daoSource.setOldStatus(context, email, label, uidList);
      } else {
        for (int i = 0; i < uidList.size(); i += step) {
          List<String> tempList;
          if (uidList.size() - i > step) {
            tempList = uidList.subList(i, i + step);
          } else {
            tempList = uidList.subList(i, uidList.size());
          }
          daoSource.setOldStatus(context, email, label, tempList);
        }
      }
    }
  }
}
