/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.attachment;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateUtils;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.AttachmentInfo;

/**
 * This manager is responsible for displaying attachment notifications.
 *
 * @author Denis Bondarenko
 *         Date: 17.08.2017
 *         Time: 17:30
 *         E-mail: DenBond7@gmail.com
 */

public class AttachmentNotificationManager {
    private static final int REQUEST_CODE_CANCEL_LOADING = 0;
    private static final int REQUEST_CODE_RETRY = 1;
    private static final int MAX_FILE_SIZE_IN_PERCENTAGE = 100;
    private NotificationManager notificationManager;

    public AttachmentNotificationManager(Context context) {
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * Show a {@link android.app.Notification} which notify that a new attachment was added to the loading queue.
     *
     * @param context        Interface to global information about an application environment.
     * @param startId        The notification unique identification id.
     * @param attachmentInfo {@link AttachmentInfo} object which contains a detail information about an attachment.
     */
    public void attachmentAddedToLoadQueue(Context context, int startId, AttachmentInfo attachmentInfo) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setProgress(0, 0, true)
                .setAutoCancel(false)
                .setOngoing(true)
                .addAction(generateCancelDownloadNotificationAction(context, startId))
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(attachmentInfo.getName())
                .setContentText(context.getString(R.string.waiting_to_load))
                .setSubText(attachmentInfo.getEmail());
        notificationManager.notify(startId, mBuilder.build());
    }

    /**
     * Update a {@link android.app.Notification} download progress.
     *
     * @param context               Interface to global information about an application environment.
     * @param startId               The notification unique identification id.
     * @param attachmentInfo        {@link AttachmentInfo} object which contains a detail information about an
     *                              attachment.
     * @param progress              The attachment loading progress in percentage.
     * @param timeLeftInMillisecond The time left in millisecond (approximately).
     */
    public void updateLoadingProgress(Context context, int startId, AttachmentInfo attachmentInfo,
                                      int progress, long timeLeftInMillisecond) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setProgress(MAX_FILE_SIZE_IN_PERCENTAGE, progress, false)
                .setAutoCancel(false)
                .setOngoing(true)
                .addAction(generateCancelDownloadNotificationAction(context, startId))
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(attachmentInfo.getName())
                .setContentText(DateUtils.formatElapsedTime(timeLeftInMillisecond / DateUtils.SECOND_IN_MILLIS))
                .setSubText(attachmentInfo.getEmail());
        notificationManager.notify(startId, mBuilder.build());
    }

    /**
     * Show a {@link android.app.Notification} which notify that an attachment was downloaded.
     *
     * @param context        Interface to global information about an application environment.
     * @param startId        The notification unique identification id.
     * @param attachmentInfo {@link AttachmentInfo} object which contains a detail information about an attachment.
     * @param uri            The {@link Uri} of the downloaded attachment.
     */
    public void downloadComplete(Context context, int startId, AttachmentInfo attachmentInfo, Uri uri) {
        //// TODO-denbond7: 18.08.2017 Temporary disable open downloaded file. See https://developer.android
        // .com/training/secure-file-sharing/index.html to resolve it.

        /*Intent intentOpenFile = new Intent(Intent.ACTION_VIEW, uri);
        intentOpenFile.setAction(Intent.ACTION_VIEW);
        intentOpenFile.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intentOpenFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        PendingIntent pendingIntentOpenFile = PendingIntent.getActivity(context, 0, intentOpenFile, 0);*/

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setProgress(0, 0, false)
                .setAutoCancel(true)
                .setOngoing(false)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(attachmentInfo.getName())
                .setContentText(context.getString(R.string.download_complete))
                //.setContentIntent(pendingIntentOpenFile)
                .setSubText(attachmentInfo.getEmail());
        notificationManager.notify(startId, mBuilder.build());
    }

    /**
     * Show a {@link android.app.Notification} which notify that an error happened while we loading an attachment.
     * The user can cancel current loading or retry loading again.
     *
     * @param context        Interface to global information about an application environment.
     * @param startId        The notification unique identification id.
     * @param attachmentInfo {@link AttachmentInfo} object which contains a detail information about an attachment.
     * @param e              The {@link Exception} which contains a detail information about happened error..
     */
    public void errorHappened(Context context, int startId, AttachmentInfo attachmentInfo, Exception e) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setProgress(0, 0, false)
                .setAutoCancel(true)
                .setOngoing(false)
                .addAction(generateCancelDownloadNotificationAction(context, startId))
                .addAction(generateRetryDownloadNotificationAction(context, startId, attachmentInfo))
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)// TODO-denbond7: 18.08.2017 Need to show
/// error icon
                .setContentTitle(attachmentInfo.getName())
                .setContentText(e.getMessage())
                .setSubText(attachmentInfo.getEmail());
        notificationManager.notify(startId, mBuilder.build());
    }

    /**
     * Cancel a {@link android.app.Notification} when user clicked on the "Cancel" button.
     *
     * @param startId The notification unique identification id.
     */
    public void loadCanceledByUser(int startId) {
        notificationManager.cancel(startId);
    }

    /**
     * Generate a cancel download an attachment {@link NotificationCompat.Action}.
     *
     * @param context Interface to global information about an application environment.
     * @param startId The notification unique identification id.
     * @return The created {@link NotificationCompat.Action}.
     */
    @NonNull
    private NotificationCompat.Action generateCancelDownloadNotificationAction(Context context, int startId) {
        //// TODO: 18.08.2017 Need to setup this function
        PendingIntent cancelDownloadPendingIntent = PendingIntent.getService(context, REQUEST_CODE_CANCEL_LOADING,
                new Intent(), 0);

        return new NotificationCompat.Action.Builder(0, context.getString(R.string
                .cancel), cancelDownloadPendingIntent).build();
    }

    /**
     * Generate a retry download an attachment {@link NotificationCompat.Action}.
     *
     * @param context        Interface to global information about an application environment.
     * @param startId        The notification unique identification id.
     * @param attachmentInfo {@link AttachmentInfo} object which contains a detail information about an attachment.
     * @return The created {@link NotificationCompat.Action}.
     */
    @NonNull
    private NotificationCompat.Action generateRetryDownloadNotificationAction(Context context, int startId,
                                                                              AttachmentInfo attachmentInfo) {
        //// TODO: 18.08.2017 Need to setup this function
        PendingIntent cancelDownloadPendingIntent = PendingIntent.getService(context, REQUEST_CODE_RETRY,
                new Intent(), 0);

        return new NotificationCompat.Action.Builder(0, context.getString(R.string
                .retry), cancelDownloadPendingIntent).build();
    }
}
