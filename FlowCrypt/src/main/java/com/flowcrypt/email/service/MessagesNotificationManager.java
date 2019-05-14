/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.text.Spannable;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.model.LocalFolder;
import com.flowcrypt.email.broadcastreceivers.MarkMessagesAsOldBroadcastReceiver;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.flowcrypt.email.ui.NotificationChannelManager;
import com.flowcrypt.email.ui.activity.EmailManagerActivity;
import com.flowcrypt.email.ui.activity.MessageDetailsActivity;
import com.flowcrypt.email.ui.activity.fragment.preferences.NotificationsSettingsFragment;
import com.flowcrypt.email.ui.notifications.CustomNotificationManager;
import com.flowcrypt.email.util.SharedPreferencesHelper;
import com.google.android.gms.common.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

/**
 * This manager is responsible for displaying messages notifications.
 *
 * @author Denis Bondarenko
 * Date: 23.06.2018
 * Time: 12:10
 * E-mail: DenBond7@gmail.com
 */
public class MessagesNotificationManager extends CustomNotificationManager {
  public static final String GROUP_NAME_FLOWCRYPT_MESSAGES = BuildConfig.APPLICATION_ID + ".MESSAGES";
  public static final int NOTIFICATIONS_GROUP_MESSAGES = -1;

  public MessagesNotificationManager(Context context) {
    super(context);
  }

  @Override
  public String getGroupName() {
    return GROUP_NAME_FLOWCRYPT_MESSAGES;
  }

  @Override
  public int getGroupId() {
    return NOTIFICATIONS_GROUP_MESSAGES;
  }

  /**
   * Show a {@link Notification} of an incoming message.
   *
   * @param context               Interface to global information about an application environment.
   * @param account               An {@link AccountDao} object which contains information about an email account.
   * @param localFolder           A local implementation of a remote folder.
   * @param generalMsgDetailsList A list of models which consists information about some messages.
   * @param uidListOfUnseenMsgs   A list of UID of unseen messages.
   * @param isSilent              true if we don't need sound and vibration for Android 7.0 and below.
   */
  public void notify(Context context, AccountDao account, LocalFolder localFolder, List<GeneralMessageDetails>
      generalMsgDetailsList, List<Integer> uidListOfUnseenMsgs, boolean isSilent) {

    if (account == null || generalMsgDetailsList == null || generalMsgDetailsList.isEmpty()) {
      notificationManagerCompat.cancel(NOTIFICATIONS_GROUP_MESSAGES);
      return;
    }

    boolean isNotificationDisabled = NotificationsSettingsFragment.NOTIFICATION_LEVEL_NEVER.equals
        (SharedPreferencesHelper.getString(PreferenceManager.getDefaultSharedPreferences(context),
            Constants.PREFERENCES_KEY_MESSAGES_NOTIFICATION_FILTER, ""));

    if (isNotificationDisabled) {
      return;
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      notifyWithGroupSupport(context, account, localFolder, generalMsgDetailsList);
    } else {
      notifyWithSingleNotification(context, account, localFolder, generalMsgDetailsList, uidListOfUnseenMsgs, isSilent);
    }
  }

  public void cancelAll(Context context, AccountDao account) {
    notificationManagerCompat.cancel(NOTIFICATIONS_GROUP_MESSAGES);

    FoldersManager foldersManager = FoldersManager.fromDatabase(context, account.getEmail());
    LocalFolder localFolder = foldersManager.findInboxFolder();

    if (localFolder != null) {
      new MessageDaoSource().setOldStatus(context,
          account.getEmail(), localFolder.getFolderAlias());
    }
  }

  private void notifyWithSingleNotification(Context context, AccountDao account,
                                            LocalFolder folder, List<GeneralMessageDetails> details,
                                            List<Integer> uidOfUnseenMsgs, boolean isSilent) {
    boolean onlyEncrypted = NotificationsSettingsFragment.NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY
        .equals(SharedPreferencesHelper.getString(PreferenceManager.getDefaultSharedPreferences(context),
            Constants.PREFERENCES_KEY_MESSAGES_NOTIFICATION_FILTER, ""));

    NotificationCompat.Builder builder =
        new NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_ID_MESSAGES)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_EMAIL)
            .setSmallIcon(R.drawable.ic_email_encrypted)
            .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setAutoCancel(true)
            .setGroup(GROUP_NAME_FLOWCRYPT_MESSAGES)
            .setSubText(account.getEmail());

    if (!isSilent) {
      builder.setDefaults(Notification.DEFAULT_ALL);
    }

    if (uidOfUnseenMsgs.size() > 1) {
      builder.setNumber(uidOfUnseenMsgs.size());
    }

    if (details.size() > 1) {
      boolean hasAllowedNotifications = false;

      NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
      for (GeneralMessageDetails generalMsgDetails : details) {
        if (onlyEncrypted && !generalMsgDetails.isEncrypted()) {
          continue;
        }

        hasAllowedNotifications = true;
        inboxStyle.addLine(formatInboxStyleLine(context, EmailUtil.getFirstAddressString(generalMsgDetails
            .getFrom()), generalMsgDetails.getSubject()));
      }

      if (!hasAllowedNotifications) {
        return;
      }

      builder.setStyle(inboxStyle)
          .setSmallIcon(R.drawable.ic_email_multiply_encrypted)
          .setContentIntent(getInboxPendingIntent(context, account))
          .setDeleteIntent(genDeletePendingIntent(context, NOTIFICATIONS_GROUP_MESSAGES, account,
              folder, details))
          .setContentTitle(context.getString(R.string.incoming_message,
              details.size()));
    } else {
      GeneralMessageDetails msgDetails = details.get(0);

      if (onlyEncrypted && !msgDetails.isEncrypted()) {
        return;
      }

      NotificationCompat.Style style = new NotificationCompat.BigTextStyle().bigText(formatText(msgDetails.getSubject(),
          ContextCompat.getColor(context, android.R.color.black)));

      Spannable contentText = formatText(msgDetails.getSubject(),
          ContextCompat.getColor(context, android.R.color.black));

      builder.setContentText(contentText)
          .setContentIntent(getMsgDetailsPendingIntent(context, NOTIFICATIONS_GROUP_MESSAGES, folder, msgDetails))
          .setContentTitle(EmailUtil.getFirstAddressString(msgDetails.getFrom()))
          .setStyle(style)
          .setDeleteIntent(genDeletePendingIntent(context, NOTIFICATIONS_GROUP_MESSAGES, account, folder, details))
          .setColor(ContextCompat.getColor(context, msgDetails.isEncrypted() ? R.color.colorPrimary : R.color.red))
          .setSmallIcon(R.drawable.ic_email_encrypted);
    }

    notificationManagerCompat.notify(NOTIFICATIONS_GROUP_MESSAGES, builder.build());
  }

  private void notifyWithGroupSupport(Context context, AccountDao account,
                                      LocalFolder localFolder, List<GeneralMessageDetails> detailsList) {

    boolean isEncryptedModeEnabled = NotificationsSettingsFragment.NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY
        .equals(SharedPreferencesHelper.getString(PreferenceManager.getDefaultSharedPreferences(context),
            Constants.PREFERENCES_KEY_MESSAGES_NOTIFICATION_FILTER, ""));

    NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

    if (manager != null) {
      prepareAndShowMsgGroup(context, account, localFolder, manager, detailsList);
    }

    for (GeneralMessageDetails generalMsgDetails : detailsList) {
      if (isEncryptedModeEnabled && !generalMsgDetails.isEncrypted()) {
        continue;
      }

      NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationChannelManager
          .CHANNEL_ID_MESSAGES)
          .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
          .setPriority(NotificationCompat.PRIORITY_DEFAULT)
          .setCategory(NotificationCompat.CATEGORY_EMAIL)
          .setSmallIcon(R.drawable.ic_email_encrypted)
          .setLargeIcon(generateLargeIcon(context, generalMsgDetails))
          .setColor(ContextCompat.getColor(context, generalMsgDetails.isEncrypted()
              ? R.color.colorPrimary : R.color.red))
          .setDeleteIntent(genDeletePendingIntent(context,
              generalMsgDetails.getUid(), account, localFolder, generalMsgDetails))
          .setAutoCancel(true)
          .setContentTitle(EmailUtil.getFirstAddressString(generalMsgDetails.getFrom()))
          .setStyle(new NotificationCompat.BigTextStyle().bigText(generalMsgDetails.getSubject()))
          .setGroup(GROUP_NAME_FLOWCRYPT_MESSAGES)
          .setContentText(generalMsgDetails.getSubject())
          .setContentIntent(getMsgDetailsPendingIntent(context, generalMsgDetails.getUid(),
              localFolder, generalMsgDetails))
          .setDefaults(Notification.DEFAULT_ALL)
          .setSubText(account.getEmail());

      notificationManagerCompat.notify(generalMsgDetails.getUid(), builder.build());
    }
  }

  private void prepareAndShowMsgGroup(Context context, AccountDao account, LocalFolder localFolder,
                                      NotificationManager notificationManager,
                                      List<GeneralMessageDetails> generalMsgDetailsList) {
    boolean isEncryptedModeEnabled = NotificationsSettingsFragment.NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY
        .equals(SharedPreferencesHelper.getString(PreferenceManager.getDefaultSharedPreferences(context),
            Constants.PREFERENCES_KEY_MESSAGES_NOTIFICATION_FILTER, ""));

    if (isEncryptedModeEnabled) {
      boolean isEncryptedMsgFound = false;
      for (GeneralMessageDetails generalMsgDetails : generalMsgDetailsList) {
        if (generalMsgDetails.isEncrypted()) {
          isEncryptedMsgFound = true;
          break;
        }
      }

      if (!isEncryptedMsgFound) {
        return;
      }
    }

    int groupResourceId = R.drawable.ic_email_encrypted;

    if (generalMsgDetailsList.size() > 1) {
      groupResourceId = R.drawable.ic_email_multiply_encrypted;
    } else {
      for (StatusBarNotification statusBarNotification : notificationManager.getActiveNotifications()) {
        if (GROUP_NAME_FLOWCRYPT_MESSAGES.equals(statusBarNotification.getNotification().getGroup())) {
          groupResourceId = R.drawable.ic_email_multiply_encrypted;
          break;
        }
      }
    }

    NotificationCompat.Builder groupBuilder =
        new NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_ID_MESSAGES)
            .setSmallIcon(groupResourceId)
            .setContentInfo(account.getEmail())
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setSubText(account.getEmail())
            .setGroup(GROUP_NAME_FLOWCRYPT_MESSAGES)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setContentIntent(getInboxPendingIntent(context, account))
            .setDeleteIntent(genDeletePendingIntent(context, NOTIFICATIONS_GROUP_MESSAGES,
                account, localFolder, generalMsgDetailsList))
            .setDefaults(Notification.DEFAULT_ALL)
            .setAutoCancel(true)
            .setGroupSummary(true);
    notificationManager.notify(NOTIFICATIONS_GROUP_MESSAGES, groupBuilder.build());
  }

  private PendingIntent getInboxPendingIntent(Context context, AccountDao account) {
    Intent inboxIntent = new Intent(context, EmailManagerActivity.class);
    inboxIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    return PendingIntent.getActivity(context, 0, inboxIntent, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  private PendingIntent genDeletePendingIntent(Context context, int requestCode,
                                               AccountDao account, LocalFolder localFolder,
                                               GeneralMessageDetails generalMsgDetails) {
    List<GeneralMessageDetails> generalMsgDetailsList = new ArrayList<>();
    generalMsgDetailsList.add(generalMsgDetails);
    return genDeletePendingIntent(context, requestCode, account, localFolder, generalMsgDetailsList);
  }

  private PendingIntent genDeletePendingIntent(Context context, int requestCode,
                                               AccountDao account, LocalFolder localFolder,
                                               List<GeneralMessageDetails> generalMsgDetailsList) {
    Intent intent = new Intent(context, MarkMessagesAsOldBroadcastReceiver.class);
    intent.setAction(MarkMessagesAsOldBroadcastReceiver.ACTION_MARK_MESSAGES_AS_OLD);
    intent.putExtra(MarkMessagesAsOldBroadcastReceiver.EXTRA_KEY_EMAIL, account.getEmail());
    intent.putExtra(MarkMessagesAsOldBroadcastReceiver.EXTRA_KEY_LABEL, localFolder.getFolderAlias());

    if (!CollectionUtils.isEmpty(generalMsgDetailsList)) {
      ArrayList<String> uidList = new ArrayList<>();
      for (GeneralMessageDetails generalMsgDetails : generalMsgDetailsList) {
        uidList.add(String.valueOf(generalMsgDetails.getUid()));
      }
      intent.putStringArrayListExtra(MarkMessagesAsOldBroadcastReceiver.EXTRA_KEY_UID_LIST, uidList);
    }

    return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  private Bitmap generateLargeIcon(Context context, GeneralMessageDetails generalMsgDetails) {
    return BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
  }

  private PendingIntent getMsgDetailsPendingIntent(Context context, int requestCode, LocalFolder localFolder,
                                                   GeneralMessageDetails generalMsgDetails) {
    Intent intent = MessageDetailsActivity.getIntent(context, localFolder, generalMsgDetails);

    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
    stackBuilder.addParentStack(MessageDetailsActivity.class);
    stackBuilder.addNextIntent(intent);

    return stackBuilder.getPendingIntent(requestCode, PendingIntent.FLAG_UPDATE_CURRENT);
  }
}
