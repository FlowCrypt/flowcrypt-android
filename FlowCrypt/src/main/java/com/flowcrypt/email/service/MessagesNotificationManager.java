/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.ui.NotificationChannelManager;
import com.flowcrypt.email.ui.activity.SplashActivity;

/**
 * This manager is responsible for displaying messages notifications.
 *
 * @author Denis Bondarenko
 * Date: 23.06.2018
 * Time: 12:10
 * E-mail: DenBond7@gmail.com
 */
public class MessagesNotificationManager {
    private NotificationManagerCompat notificationManager;

    public MessagesNotificationManager(Context context) {
        this.notificationManager = NotificationManagerCompat.from(context);
    }

    /**
     * Show a {@link Notification} of an incoming message.
     *
     * @param context               Interface to global information about an application environment.
     * @param accountDao            An {@link AccountDao} object which contains information about an email account.
     * @param generalMessageDetails A model which consists information about some message.
     */
    public void newMessagesReceived(Context context, AccountDao accountDao,
                                    GeneralMessageDetails generalMessageDetails) {

        if (accountDao == null || generalMessageDetails == null) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                NotificationChannelManager.CHANNEL_ID_MESSAGES)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_EMAIL)
                .setSmallIcon(R.drawable.ic_email_encrypted)
                .setLargeIcon(generateLargeIcon(context, generalMessageDetails))
                .setColor(context.getResources().getColor(R.color.colorPrimary))
                .setContentTitle(EmailUtil.getFirstAddressString(generalMessageDetails.getFrom()))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(generalMessageDetails.getSubject()))
                .addAction(generateReplyAction(context))
                .setContentText(generalMessageDetails.getSubject());

        builder.setSubText(accountDao.getEmail());
        notificationManager.notify(generalMessageDetails.getUid(), builder.build());
    }

    private Bitmap generateLargeIcon(Context context, GeneralMessageDetails generalMessageDetails) {
        return BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
    }

    private NotificationCompat.Action generateReplyAction(Context context) {
        Intent intent = new Intent(context, SplashActivity.class);

        PendingIntent cancelDownloadPendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        return new NotificationCompat.Action.Builder(0, context.getString(R.string
                .reply), cancelDownloadPendingIntent).build();
    }
}
