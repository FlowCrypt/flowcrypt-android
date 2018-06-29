/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.ui.NotificationChannelManager;
import com.flowcrypt.email.ui.activity.EmailManagerActivity;
import com.flowcrypt.email.ui.activity.SplashActivity;
import com.flowcrypt.email.ui.notifications.CustomNotificationManager;

/**
 * This manager is responsible for displaying messages notifications.
 *
 * @author Denis Bondarenko
 *         Date: 23.06.2018
 *         Time: 12:10
 *         E-mail: DenBond7@gmail.com
 */
public class MessagesNotificationManager extends CustomNotificationManager {
    public static final String GROUP_NAME_FLOWCRYPT_MESSAGES = BuildConfig.APPLICATION_ID + ".MESSAGES";
    private NotificationManager notificationManager;

    public MessagesNotificationManager(Context context) {
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
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

        int groupResourceId = R.drawable.ic_email_encrypted;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (StatusBarNotification statusBarNotification : notificationManager.getActiveNotifications()) {
                if (GROUP_NAME_FLOWCRYPT_MESSAGES.equals(statusBarNotification.getNotification().getGroup())) {
                    groupResourceId = R.drawable.ic_email_multiply_encrypted;
                    break;
                }
            }
        }

        Intent inboxIntent = new Intent(context, EmailManagerActivity.class);
        inboxIntent.putExtra(EmailManagerActivity.EXTRA_KEY_ACCOUNT_DAO, accountDao);
        inboxIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent inboxPendingIntent = PendingIntent.getActivity(
                context, 0, inboxIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder groupBuilder =
                new NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_ID_MESSAGES)
                        .setSmallIcon(groupResourceId)
                        .setContentInfo(accountDao.getEmail())
                        .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                        .setSubText(accountDao.getEmail())
                        .setGroup(GROUP_NAME_FLOWCRYPT_MESSAGES)
                        .setAutoCancel(true)
                        .setContentIntent(inboxPendingIntent)
                        .setGroupSummary(true);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_ID_MESSAGES)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setCategory(NotificationCompat.CATEGORY_EMAIL)
                        .setSmallIcon(R.drawable.ic_email_encrypted)
                        .setLargeIcon(generateLargeIcon(context, generalMessageDetails))
                        .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                        .setContentTitle(EmailUtil.getFirstAddressString(generalMessageDetails.getFrom()))
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(generalMessageDetails.getSubject()))
                        .addAction(generateReplyAction(context))
                        .setGroup(GROUP_NAME_FLOWCRYPT_MESSAGES)
                        .setContentText(generalMessageDetails.getSubject())
                        .setAutoCancel(true)
                        .setContentIntent(inboxPendingIntent)
                        .setSubText(accountDao.getEmail());

        notificationManager.notify(NOTIFICATIONS_GROUP_MESSAGES, groupBuilder.build());
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
