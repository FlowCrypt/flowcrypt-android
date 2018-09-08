/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.flowcrypt.email.R;

/**
 * This manager does job of register {@link NotificationChannel} of the app. The {@link NotificationChannel} was
 * added in the {@link Build.VERSION_CODES#O} and doesn't work on previous Android versions.
 *
 * @author Denis Bondarenko
 *         Date: 17.10.2017
 *         Time: 12:12
 *         E-mail: DenBond7@gmail.com
 */

public class NotificationChannelManager {
    public static final String CHANNEL_ID_ATTACHMENTS = "Attachments";
    public static final String CHANNEL_ID_MESSAGES = "Messages";

    /**
     * Register {@link NotificationChannel}(s) of the app in the system.
     *
     * @param context Interface to global information about an application environment.
     */
    public static void registerNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(generateAttachmentsNotificationChannel(context));
                notificationManager.createNotificationChannel(generateMessagesNotificationChannel(context));
            }
        }
    }

    /**
     * Generate messages notification channel.
     *
     * @param context Interface to global information about an application environment.
     * @return Generated {@link NotificationChannel}
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel generateMessagesNotificationChannel(Context context) {
        CharSequence name = context.getString(R.string.messages);
        String description = context.getString(R.string.messages_notification_channel);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID_MESSAGES, name, importance);
        notificationChannel.setDescription(description);
        notificationChannel.enableLights(true);
        notificationChannel.enableVibration(true);

        return notificationChannel;
    }

    /**
     * Generate attachments notification channel.
     *
     * @param context Interface to global information about an application environment.
     * @return Generated {@link NotificationChannel}
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel generateAttachmentsNotificationChannel(Context context) {
        CharSequence name = context.getString(R.string.attachments);
        String description = context.getString(R.string.download_attachments_notification_channel);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID_ATTACHMENTS, name, importance);
        notificationChannel.setDescription(description);
        notificationChannel.enableLights(false);
        notificationChannel.enableVibration(false);

        return notificationChannel;
    }
}
