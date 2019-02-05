/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.attachment;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.ui.NotificationChannelManager;
import com.flowcrypt.email.ui.notifications.CustomNotificationManager;

import java.util.Random;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

/**
 * This manager is responsible for displaying attachment notifications.
 *
 * @author Denis Bondarenko
 * Date: 17.08.2017
 * Time: 17:30
 * E-mail: DenBond7@gmail.com
 */

public class AttachmentNotificationManager extends CustomNotificationManager {
  public static final String GROUP_NAME_ATTACHMENTS = BuildConfig.APPLICATION_ID + ".ATTACHMENTS";
  public static final int NOTIFICATIONS_GROUP_ATTACHMENTS = -2;
  private static final int MAX_CONTENT_TITLE_LENGTH = 30;
  private static final int MAX_FILE_SIZE_IN_PERCENTAGE = 100;

  public AttachmentNotificationManager(Context context) {
    super(context);
  }

  @Override
  public String getGroupName() {
    return GROUP_NAME_ATTACHMENTS;
  }

  @Override
  public int getGroupId() {
    return NOTIFICATIONS_GROUP_ATTACHMENTS;
  }

  /**
   * Show a {@link android.app.Notification} which notify that a new attachment was added to the loading queue.
   *
   * @param context Interface to global information about an application environment.
   * @param attInfo {@link AttachmentInfo} object which contains a detail information about an attachment.
   */
  public void attachmentAddedToLoadQueue(Context context, AttachmentInfo attInfo) {
    NotificationCompat.Builder builder = genDefBuilder(context, attInfo);
    builder.setProgress(0, 0, true)
        .addAction(genCancelDownloadAction(context, attInfo))
        .setOnlyAlertOnce(true);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      prepareAndShowNotificationsGroup(context, attInfo, true);
      builder.setGroup(GROUP_NAME_ATTACHMENTS).setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY);
    } else {
      builder.setWhen(attInfo.getOrderNumber()).setShowWhen(false);
    }

    notificationManagerCompat.notify(attInfo.getId(), attInfo.getUid(), builder.build());
  }

  /**
   * Update a {@link android.app.Notification} download progress.
   *
   * @param context               Interface to global information about an application environment.
   * @param attInfo               {@link AttachmentInfo} object which contains a detail information about an
   *                              attachment.
   * @param progress              The attachment loading progress in percentage.
   * @param timeLeftInMillisecond The time left in millisecond (approximately).
   */
  public void updateLoadingProgress(Context context, AttachmentInfo attInfo, int progress, long timeLeftInMillisecond) {
    NotificationCompat.Builder builder = genDefBuilder(context, attInfo);

    builder.setProgress(MAX_FILE_SIZE_IN_PERCENTAGE, progress, timeLeftInMillisecond == 0)
        .addAction(genCancelDownloadAction(context, attInfo))
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentText(DateUtils.formatElapsedTime(timeLeftInMillisecond / DateUtils.SECOND_IN_MILLIS))
        .setOnlyAlertOnce(true);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      builder.setGroup(GROUP_NAME_ATTACHMENTS).setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY);
    } else {
      builder.setWhen(attInfo.getOrderNumber()).setShowWhen(false);
    }

    notificationManagerCompat.notify(attInfo.getId(), attInfo.getUid(), builder.build());

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      prepareAndShowNotificationsGroup(context, attInfo, true);
    }
  }

  /**
   * Show a {@link android.app.Notification} which notify that an attachment was downloaded.
   *
   * @param context Interface to global information about an application environment.
   * @param attInfo {@link AttachmentInfo} object which contains a detail information about an attachment.
   * @param uri     The {@link Uri} of the downloaded attachment.
   */
  public void downloadCompleted(Context context, AttachmentInfo attInfo, Uri uri) {
    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
    intent.setAction(Intent.ACTION_VIEW);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

    NotificationCompat.Builder builder = genDefBuilder(context, attInfo);

    builder.setProgress(0, 0, false)
        .setAutoCancel(true)
        .setOngoing(false)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentText(context.getString(R.string.download_complete))
        .setContentIntent(pendingIntent);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      builder.setGroup(GROUP_NAME_ATTACHMENTS).setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN);
    } else {
      builder.setWhen(attInfo.getOrderNumber()).setShowWhen(false);
    }

    notificationManagerCompat.notify(attInfo.getId(), attInfo.getUid(), builder.build());

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      prepareAndShowNotificationsGroup(context, attInfo, false);
    }
  }

  /**
   * Show a {@link android.app.Notification} which notify that an error happened while we loading an attachment.
   * The user can cancel current loading or retry loading again.
   *
   * @param context Interface to global information about an application environment.
   * @param attInfo {@link AttachmentInfo} object which contains a detail information about an attachment.
   * @param e       The {@link Exception} which contains a detail information about happened error..
   */
  public void errorHappened(Context context, AttachmentInfo attInfo, Exception e) {
    NotificationCompat.Builder builder = genDefBuilder(context, attInfo);

    String contentText = TextUtils.isEmpty(e.getMessage()) ? context.getString(R.string
        .error_occurred_please_try_again) : e.getMessage();

    builder.setProgress(0, 0, false)
        .setAutoCancel(true)
        .setOngoing(false)
        .addAction(genCancelDownloadAction(context, attInfo))
        .addAction(genRetryDownloadingAction(context, attInfo))
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentText(contentText);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      builder.setGroup(GROUP_NAME_ATTACHMENTS).setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN);
    } else {
      builder.setWhen(attInfo.getOrderNumber()).setShowWhen(false);
    }

    notificationManagerCompat.notify(attInfo.getId(), attInfo.getUid(), builder.build());

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      prepareAndShowNotificationsGroup(context, attInfo, false);
    }
  }

  /**
   * Cancel a {@link android.app.Notification} when user clicked on the "Cancel" button.
   *
   * @param attInfo The {@link AttachmentInfo} object.
   */
  public void loadingCanceledByUser(AttachmentInfo attInfo) {
    cancel(attInfo.getId(), attInfo.getUid());
  }

  /**
   * Prepare a content title for a notification.
   *
   * @param attachmentInfo The {@link AttachmentInfo} object.
   * @return The prepared title.
   */
  private String prepareContentTitle(AttachmentInfo attachmentInfo) {
    String contentTitle = attachmentInfo.getName();
    if (!TextUtils.isEmpty(contentTitle) && contentTitle.length() > MAX_CONTENT_TITLE_LENGTH) {
      contentTitle = contentTitle.substring(0, MAX_CONTENT_TITLE_LENGTH) + "...";
    }
    return contentTitle;
  }

  /**
   * Generate a cancel download an attachment {@link NotificationCompat.Action}.
   *
   * @param context Interface to global information about an application environment.
   * @return The created {@link NotificationCompat.Action}.
   */
  @NonNull
  private NotificationCompat.Action genCancelDownloadAction(Context context, AttachmentInfo attInfo) {
    Intent intent = new Intent(context, AttachmentDownloadManagerService.class);
    intent.setAction(AttachmentDownloadManagerService.ACTION_CANCEL_DOWNLOAD_ATTACHMENT);
    intent.putExtra(AttachmentDownloadManagerService.EXTRA_KEY_ATTACHMENT_INFO, attInfo);

    PendingIntent pendingIntent = PendingIntent.getService(context, new Random().nextInt(), intent, 0);
    return new NotificationCompat.Action.Builder(0, context.getString(R.string.cancel), pendingIntent).build();
  }

  /**
   * Generate a retry download an attachment {@link NotificationCompat.Action}.
   *
   * @param context Interface to global information about an application environment.
   * @param attInfo {@link AttachmentInfo} object which contains a detail information about an attachment.
   * @return The created {@link NotificationCompat.Action}.
   */
  @NonNull
  private NotificationCompat.Action genRetryDownloadingAction(Context context, AttachmentInfo attInfo) {
    Intent intent = new Intent(context, AttachmentDownloadManagerService.class);
    intent.setAction(AttachmentDownloadManagerService.ACTION_RETRY_DOWNLOAD_ATTACHMENT);
    intent.putExtra(AttachmentDownloadManagerService.EXTRA_KEY_ATTACHMENT_INFO, attInfo);

    PendingIntent pendingIntent = PendingIntent.getService(context, new Random().nextInt(), intent, 0);
    return new NotificationCompat.Action.Builder(0, context.getString(R.string.retry), pendingIntent).build();
  }

  /**
   * Generate {@link NotificationCompat.Builder} with common values.
   *
   * @param context Interface to global information about an application environment.
   * @param attInfo {@link AttachmentInfo} object which contains a detail information about an attachment.
   * @return Generated {@link NotificationCompat.Builder}.
   */
  private NotificationCompat.Builder genDefBuilder(Context context, AttachmentInfo attInfo) {
    return new NotificationCompat.Builder(context,
        NotificationChannelManager.CHANNEL_ID_ATTACHMENTS)
        .setAutoCancel(false)
        .setOngoing(true)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle(prepareContentTitle(attInfo))
        .setContentText(context.getString(R.string.waiting_to_load))
        .setSubText(attInfo.getEmail())
        .setDefaults(Notification.DEFAULT_ALL);
  }

  /**
   * Prepare and show the notifications group.
   *
   * @param context           Interface to global information about an application environment.
   * @param attInfo           {@link AttachmentInfo} object which contains a detail information about an attachment.
   * @param isProgressEnabled true if need to use the progress icon as default, otherwise false.
   */
  private void prepareAndShowNotificationsGroup(Context context, AttachmentInfo attInfo, boolean isProgressEnabled) {
    int groupResourceId = isProgressEnabled ? android.R.drawable.stat_sys_download
        : android.R.drawable.stat_sys_download_done;

    NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

    for (StatusBarNotification stBarNotification : manager.getActiveNotifications()) {
      if (!TextUtils.isEmpty(stBarNotification.getTag()) && !stBarNotification.getTag().equals(attInfo.getId())
          && stBarNotification.getId() != attInfo.getUid()) {
        Notification notification = stBarNotification.getNotification();
        if (GROUP_NAME_ATTACHMENTS.equals(notification.getGroup())) {
          Bundle extras = notification.extras;
          if (extras != null && extras.getInt(Notification.EXTRA_PROGRESS_MAX) == MAX_FILE_SIZE_IN_PERCENTAGE
              && extras.getInt(Notification.EXTRA_PROGRESS) > 0) {
            groupResourceId = android.R.drawable.stat_sys_download;
            break;
          }
        }
      }
    }

    NotificationCompat.Builder groupBuilder = new NotificationCompat.Builder(context,
        NotificationChannelManager.CHANNEL_ID_ATTACHMENTS)
        .setSmallIcon(groupResourceId)
        .setGroup(GROUP_NAME_ATTACHMENTS)
        .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
        .setDefaults(Notification.DEFAULT_ALL)
        .setAutoCancel(true)
        .setGroupSummary(true);
    notificationManagerCompat.notify(NOTIFICATIONS_GROUP_ATTACHMENTS, groupBuilder.build());
  }
}
