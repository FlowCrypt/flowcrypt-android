/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service;

import android.annotation.TargetApi;
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

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
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

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
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
  private NotificationManagerCompat notificationManagerCompat;

  public MessagesNotificationManager(Context context) {
    this.notificationManagerCompat = NotificationManagerCompat.from(context);
  }

  /**
   * Show a {@link Notification} of an incoming message.
   *
   * @param context                   Interface to global information about an application environment.
   * @param accountDao                An {@link AccountDao} object which contains information about an email account.
   * @param localFolder               A local implementation of a remote folder.
   * @param generalMessageDetailsList A list of models which consists information about some messages.
   * @param uidListOfUnseenMessages   A list of UID of unseen messages.
   * @param isSilent                  true if we don't need sound and vibration for Android 7.0 and below.
   */
  public void notify(Context context, AccountDao accountDao, Folder localFolder, List<GeneralMessageDetails>
      generalMessageDetailsList, List<Integer> uidListOfUnseenMessages, boolean isSilent) {

    if (accountDao == null || generalMessageDetailsList == null || generalMessageDetailsList.isEmpty()) {
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
      notifyWithGroupSupport(context, accountDao, localFolder, generalMessageDetailsList);
    } else {
      notifyWithSingleNotification(context, accountDao, localFolder, generalMessageDetailsList,
          uidListOfUnseenMessages, isSilent);
    }
  }

  public void cancel(Context context, int messageUID) {
    notificationManagerCompat.cancel(messageUID);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      NotificationManager notificationManager =
          (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

      if (notificationManager != null) {
        int messageCount = 0;
        for (StatusBarNotification statusBarNotification : notificationManager.getActiveNotifications()) {
          if (GROUP_NAME_FLOWCRYPT_MESSAGES.equals(statusBarNotification.getNotification().getGroup())) {
            messageCount++;
          }
        }

        if (messageCount == 1) {
          notificationManager.cancel(NOTIFICATIONS_GROUP_MESSAGES);
        }
      }
    }
  }

  public void cancelAll(Context context, AccountDao accountDao) {
    notificationManagerCompat.cancel(NOTIFICATIONS_GROUP_MESSAGES);

    FoldersManager foldersManager = FoldersManager.fromDatabase(context, accountDao.getEmail());
    Folder folder = foldersManager.findInboxFolder();

    if (folder != null) {
      new MessageDaoSource().setOldStatusForLocalMessages(context,
          accountDao.getEmail(), folder.getFolderAlias());
    }
  }

  private void notifyWithSingleNotification(Context context, AccountDao accountDao,
                                            Folder localFolder, List<GeneralMessageDetails> generalMessageDetailsList,
                                            List<Integer> uidOfUnseenMessages, boolean isSilent) {
    boolean isEncryptedMessagesOnly = NotificationsSettingsFragment.NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY
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
            .setSubText(accountDao.getEmail());

    if (!isSilent) {
      builder.setDefaults(Notification.DEFAULT_ALL);
    }

    if (uidOfUnseenMessages.size() > 1) {
      builder.setNumber(uidOfUnseenMessages.size());
    }

    if (generalMessageDetailsList.size() > 1) {
      boolean isAllowedNotificationsExist = false;

      NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
      for (GeneralMessageDetails generalMessageDetails : generalMessageDetailsList) {
        if (isEncryptedMessagesOnly && !generalMessageDetails.isEncrypted()) {
          continue;
        }

        isAllowedNotificationsExist = true;
        inboxStyle.addLine(formatInboxStyleLine(context,
            EmailUtil.getFirstAddressString(generalMessageDetails.getFrom()),
            generalMessageDetails.getSubject()));
      }

      if (!isAllowedNotificationsExist) {
        return;
      }

      builder.setStyle(inboxStyle)
          .setSmallIcon(R.drawable.ic_email_multiply_encrypted)
          .setContentIntent(getInboxPendingIntent(context, accountDao))
          .setDeleteIntent(generateDeletePendingIntent(context, NOTIFICATIONS_GROUP_MESSAGES, accountDao,
              localFolder, generalMessageDetailsList))
          .setContentTitle(context.getString(R.string.incoming_message,
              generalMessageDetailsList.size()));
    } else {
      GeneralMessageDetails generalMessageDetails = generalMessageDetailsList.get(0);

      if (isEncryptedMessagesOnly && !generalMessageDetails.isEncrypted()) {
        return;
      }

      builder.setContentText(formatText(generalMessageDetails.getSubject(),
          ContextCompat.getColor(context, android.R.color.black)))
          .setContentIntent(getMessageDetailsPendingIntent(context, NOTIFICATIONS_GROUP_MESSAGES,
              localFolder, generalMessageDetails))
          .setContentTitle(EmailUtil.getFirstAddressString(generalMessageDetails.getFrom()))
          .setStyle(new NotificationCompat.BigTextStyle().bigText(
              formatText(generalMessageDetails.getSubject(),
                  ContextCompat.getColor(context, android.R.color.black))))
          .setDeleteIntent(generateDeletePendingIntent(context, NOTIFICATIONS_GROUP_MESSAGES, accountDao,
              localFolder, generalMessageDetailsList))
          .setColor(ContextCompat.getColor(context, generalMessageDetails.isEncrypted()
              ? R.color.colorPrimary : R.color.red))
          .setSmallIcon(R.drawable.ic_email_encrypted);
    }

    notificationManagerCompat.notify(NOTIFICATIONS_GROUP_MESSAGES, builder.build());
  }

  @TargetApi(Build.VERSION_CODES.M)
  private void notifyWithGroupSupport(Context context, AccountDao accountDao,
                                      Folder localFolder, List<GeneralMessageDetails> generalMessageDetailsList) {

    boolean isEncryptedMessagesOnly = NotificationsSettingsFragment.NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY
        .equals(SharedPreferencesHelper.getString(PreferenceManager.getDefaultSharedPreferences(context),
            Constants.PREFERENCES_KEY_MESSAGES_NOTIFICATION_FILTER, ""));

    NotificationManager notificationManager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

    if (notificationManager != null) {
      prepareAndShowMessageGroup(context, accountDao, localFolder, notificationManager,
          generalMessageDetailsList);
    }

    for (GeneralMessageDetails generalMessageDetails : generalMessageDetailsList) {
      if (isEncryptedMessagesOnly && !generalMessageDetails.isEncrypted()) {
        continue;
      }

      NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationChannelManager
          .CHANNEL_ID_MESSAGES)
          .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
          .setPriority(NotificationCompat.PRIORITY_DEFAULT)
          .setCategory(NotificationCompat.CATEGORY_EMAIL)
          .setSmallIcon(R.drawable.ic_email_encrypted)
          .setLargeIcon(generateLargeIcon(context, generalMessageDetails))
          .setColor(ContextCompat.getColor(context, generalMessageDetails.isEncrypted()
              ? R.color.colorPrimary : R.color.red))
          .setDeleteIntent(generateDeletePendingIntent(context,
              generalMessageDetails.getUid(), accountDao, localFolder, generalMessageDetails))
          .setAutoCancel(true)
          .setContentTitle(EmailUtil.getFirstAddressString(generalMessageDetails.getFrom()))
          .setStyle(new NotificationCompat.BigTextStyle().bigText(generalMessageDetails.getSubject()))
          .setGroup(GROUP_NAME_FLOWCRYPT_MESSAGES)
          .setContentText(generalMessageDetails.getSubject())
          .setContentIntent(getMessageDetailsPendingIntent(context, generalMessageDetails.getUid(),
              localFolder, generalMessageDetails))
          .setDefaults(Notification.DEFAULT_ALL)
          .setSubText(accountDao.getEmail());

      notificationManagerCompat.notify(generalMessageDetails.getUid(), builder.build());
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  private void prepareAndShowMessageGroup(Context context, AccountDao accountDao, Folder localFolder,
                                          NotificationManager notificationManager,
                                          List<GeneralMessageDetails> generalMessageDetailsList) {
    boolean isEncryptedMessagesOnly = NotificationsSettingsFragment.NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY
        .equals(SharedPreferencesHelper.getString(PreferenceManager.getDefaultSharedPreferences(context),
            Constants.PREFERENCES_KEY_MESSAGES_NOTIFICATION_FILTER, ""));

    if (isEncryptedMessagesOnly) {
      boolean isEncryptedMessageFound = false;
      for (GeneralMessageDetails generalMessageDetails : generalMessageDetailsList) {
        if (generalMessageDetails.isEncrypted()) {
          isEncryptedMessageFound = true;
          break;
        }
      }

      if (!isEncryptedMessageFound) {
        return;
      }
    }

    int groupResourceId = R.drawable.ic_email_encrypted;

    if (generalMessageDetailsList.size() > 1) {
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
            .setContentInfo(accountDao.getEmail())
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setSubText(accountDao.getEmail())
            .setGroup(GROUP_NAME_FLOWCRYPT_MESSAGES)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setContentIntent(getInboxPendingIntent(context, accountDao))
            .setDeleteIntent(generateDeletePendingIntent(context, NOTIFICATIONS_GROUP_MESSAGES,
                accountDao, localFolder, generalMessageDetailsList))
            .setDefaults(Notification.DEFAULT_ALL)
            .setAutoCancel(true)
            .setGroupSummary(true);
    notificationManager.notify(NOTIFICATIONS_GROUP_MESSAGES, groupBuilder.build());
  }

  private PendingIntent getInboxPendingIntent(Context context, AccountDao accountDao) {
    Intent inboxIntent = new Intent(context, EmailManagerActivity.class);
    inboxIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    return PendingIntent.getActivity(context, 0, inboxIntent, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  private PendingIntent generateDeletePendingIntent(Context context, int requestCode,
                                                    AccountDao accountDao, Folder localFolder,
                                                    GeneralMessageDetails generalMessageDetails) {
    List<GeneralMessageDetails> generalMessageDetailsList = new ArrayList<>();
    generalMessageDetailsList.add(generalMessageDetails);
    return generateDeletePendingIntent(context, requestCode, accountDao, localFolder, generalMessageDetailsList);
  }

  private PendingIntent generateDeletePendingIntent(Context context, int requestCode,
                                                    AccountDao accountDao, Folder localFolder,
                                                    List<GeneralMessageDetails> generalMessageDetailsList) {
    Intent intent = new Intent(context, MarkMessagesAsOldBroadcastReceiver.class);
    intent.setAction(MarkMessagesAsOldBroadcastReceiver.ACTION_MARK_MESSAGES_AS_OLD);
    intent.putExtra(MarkMessagesAsOldBroadcastReceiver.EXTRA_KEY_EMAIL, accountDao.getEmail());
    intent.putExtra(MarkMessagesAsOldBroadcastReceiver.EXTRA_KEY_LABEL, localFolder.getFolderAlias());

    if (!CollectionUtils.isEmpty(generalMessageDetailsList)) {
      ArrayList<String> uidList = new ArrayList<>();
      for (GeneralMessageDetails generalMessageDetails : generalMessageDetailsList) {
        uidList.add(String.valueOf(generalMessageDetails.getUid()));
      }
      intent.putStringArrayListExtra(MarkMessagesAsOldBroadcastReceiver.EXTRA_KEY_UID_LIST, uidList);
    }

    return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  private Bitmap generateLargeIcon(Context context, GeneralMessageDetails generalMessageDetails) {
    return BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
  }

  private PendingIntent getMessageDetailsPendingIntent(Context context, int requestCode, Folder folder,
                                                       GeneralMessageDetails generalMessageDetails) {
    Intent intent = MessageDetailsActivity.getIntent(context, folder, generalMessageDetails);

    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
    stackBuilder.addParentStack(MessageDetailsActivity.class);
    stackBuilder.addNextIntent(intent);

    return stackBuilder.getPendingIntent(requestCode, PendingIntent.FLAG_UPDATE_CURRENT);
  }
}
