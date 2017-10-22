/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
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
            }
        }
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
