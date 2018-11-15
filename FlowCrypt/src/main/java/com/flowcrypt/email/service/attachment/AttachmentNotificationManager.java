/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.attachment;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.DateUtils;

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
  private static final int MAX_CONTENT_TITLE_LENGTH = 30;
  private static final int MAX_FILE_SIZE_IN_PERCENTAGE = 100;
  private NotificationManager notificationManager;

  public AttachmentNotificationManager(Context context) {
    this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
  }

  /**
   * Show a {@link android.app.Notification} which notify that a new attachment was added to the loading queue.
   *
   * @param context Interface to global information about an application environment.
   * @param attInfo {@link AttachmentInfo} object which contains a detail information about an attachment.
   */
  public void attachmentAddedToLoadQueue(Context context, AttachmentInfo attInfo) {
    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context,
        NotificationChannelManager.CHANNEL_ID_ATTACHMENTS)
        .setWhen(attInfo.getOrderNumber())
        .setShowWhen(false)
        .setProgress(0, 0, true)
        .setAutoCancel(false)
        .setOngoing(true)
        .addAction(generateCancelDownloadNotificationAction(context, attInfo))
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle(prepareContentTitle(attInfo))
        .setContentText(context.getString(R.string.waiting_to_load))
        .setSubText(attInfo.getEmail())
        .setOnlyAlertOnce(true)
        .setDefaults(Notification.DEFAULT_ALL);
    notificationManager.notify(attInfo.getId(), attInfo.getUid(), mBuilder.build());
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
    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context,
        NotificationChannelManager.CHANNEL_ID_ATTACHMENTS)
        .setProgress(MAX_FILE_SIZE_IN_PERCENTAGE, progress, timeLeftInMillisecond == 0)
        .setWhen(attInfo.getOrderNumber())
        .setShowWhen(false)
        .setAutoCancel(false)
        .setOngoing(true)
        .addAction(generateCancelDownloadNotificationAction(context, attInfo))
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle(prepareContentTitle(attInfo))
        .setContentText(DateUtils.formatElapsedTime(timeLeftInMillisecond / DateUtils.SECOND_IN_MILLIS))
        .setSubText(attInfo.getEmail())
        .setOnlyAlertOnce(true)
        .setDefaults(Notification.DEFAULT_ALL);
    notificationManager.notify(attInfo.getId(), attInfo.getUid(), mBuilder.build());
  }

  /**
   * Show a {@link android.app.Notification} which notify that an attachment was downloaded.
   *
   * @param context Interface to global information about an application environment.
   * @param attInfo {@link AttachmentInfo} object which contains a detail information about an attachment.
   * @param uri     The {@link Uri} of the downloaded attachment.
   */
  public void downloadComplete(Context context, AttachmentInfo attInfo, Uri uri) {
    Intent intentOpenFile = new Intent(Intent.ACTION_VIEW, uri);
    intentOpenFile.setAction(Intent.ACTION_VIEW);
    intentOpenFile.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intentOpenFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

    PendingIntent pendingIntentOpenFile = PendingIntent.getActivity(context, 0, intentOpenFile, 0);

    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context,
        NotificationChannelManager.CHANNEL_ID_ATTACHMENTS)
        .setProgress(0, 0, false)
        .setAutoCancel(true)
        .setOngoing(false)
        .setWhen(attInfo.getOrderNumber())
        .setShowWhen(false)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle(prepareContentTitle(attInfo))
        .setContentText(context.getString(R.string.download_complete))
        .setContentIntent(pendingIntentOpenFile)
        .setSubText(attInfo.getEmail());
    notificationManager.notify(attInfo.getId(), attInfo.getUid(), mBuilder.build());
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
    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context,
        NotificationChannelManager.CHANNEL_ID_ATTACHMENTS)
        .setProgress(0, 0, false)
        .setAutoCancel(true)
        .setOngoing(false)
        .addAction(generateCancelDownloadNotificationAction(context, attInfo))
        .addAction(generateRetryDownloadNotificationAction(context, attInfo))
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)// TODO-denbond7: 18.08.2017 Need to show
/// error icon
        .setContentTitle(prepareContentTitle(attInfo))
        .setContentText(TextUtils.isEmpty(e.getMessage())
            ? context.getString(R.string.error_occurred_please_try_again) : e.getMessage())
        .setSubText(attInfo.getEmail());
    notificationManager.notify(attInfo.getId(), attInfo.getUid(), mBuilder.build());
  }

  /**
   * Cancel a {@link android.app.Notification} when user clicked on the "Cancel" button.
   *
   * @param attInfo The {@link AttachmentInfo} object.
   */
  public void loadCanceledByUser(AttachmentInfo attInfo) {
    notificationManager.cancel(attInfo.getId(), attInfo.getUid());
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
  private NotificationCompat.Action generateCancelDownloadNotificationAction(Context context, AttachmentInfo attInfo) {
    Intent intent = new Intent(context, AttachmentDownloadManagerService.class);
    intent.setAction(AttachmentDownloadManagerService.ACTION_CANCEL_DOWNLOAD_ATTACHMENT);
    intent.putExtra(AttachmentDownloadManagerService.EXTRA_KEY_ATTACHMENT_INFO, attInfo);

    PendingIntent cancelDownloadPendingIntent = PendingIntent.getService(context, new Random().nextInt(), intent, 0);
    return new NotificationCompat.Action.Builder(0, context.getString(R.string.cancel),
        cancelDownloadPendingIntent).build();
  }

  /**
   * Generate a retry download an attachment {@link NotificationCompat.Action}.
   *
   * @param context Interface to global information about an application environment.
   * @param attInfo {@link AttachmentInfo} object which contains a detail information about an attachment.
   * @return The created {@link NotificationCompat.Action}.
   */
  @NonNull
  private NotificationCompat.Action generateRetryDownloadNotificationAction(Context context, AttachmentInfo attInfo) {
    Intent intent = new Intent(context, AttachmentDownloadManagerService.class);
    intent.setAction(AttachmentDownloadManagerService.ACTION_RETRY_DOWNLOAD_ATTACHMENT);
    intent.putExtra(AttachmentDownloadManagerService.EXTRA_KEY_ATTACHMENT_INFO, attInfo);

    PendingIntent cancelDownloadPendingIntent = PendingIntent.getService(context, new Random().nextInt(), intent, 0);

    return new NotificationCompat.Action.Builder(0, context.getString(R.string.retry),
        cancelDownloadPendingIntent).build();
  }
}
