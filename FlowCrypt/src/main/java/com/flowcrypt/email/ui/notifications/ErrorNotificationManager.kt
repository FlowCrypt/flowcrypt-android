/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.ui.activity.EmailManagerActivity

/**
 * It's a manager which helps to show errors notifications with a high priority.
 *
 * @author Denis Bondarenko
 *         Date: 6/26/20
 *         Time: 5:28 PM
 *         E-mail: DenBond7@gmail.com
 */
class ErrorNotificationManager(context: Context) : CustomNotificationManager(context) {
  override val groupName: String = GROUP_NAME_ERRORS
  override val groupId: Int = NOTIFICATIONS_GROUP_ERROR

  fun notifyUserAboutProblemWithOutgoingMsg(account: AccountEntity, messageCount: Int) {
    val intent = Intent(context, EmailManagerActivity::class.java).apply {
      action = EmailManagerActivity.ACTION_OPEN_OUTBOX_FOLDER
    }

    val pendingIntent = PendingIntent.getActivity(context, System.currentTimeMillis().toInt(), intent, 0)

    val contentText = context.resources.getQuantityString(R.plurals.has_failed_outgoing_msgs,
        messageCount, messageCount)
    val builder = NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_ID_ERRORS)
        .setSmallIcon(R.drawable.ic_create_message_failed_grey_24dp)
        .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
        .setContentText(contentText)
        .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
        .setContentIntent(pendingIntent)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_ERROR)
        .setAutoCancel(true)
        .setGroup(GROUP_NAME_ERRORS)
        .setSubText(account.email)
        .setOngoing(true)
        .setDefaults(Notification.DEFAULT_ALL)
        .setGroup(GROUP_NAME_ERRORS)
        .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)

    prepareAndShowNotificationsGroup(context)

    notificationManagerCompat.notify(groupName, NOTIFICATION_ID_HAS_FAILED_OUTGOING_MSGS, builder.build())
  }

  /**
   * Prepare and show the notifications group.
   *
   * @param context           Interface to global information about an application environment.
   */
  private fun prepareAndShowNotificationsGroup(context: Context) {
    val groupBuilder = NotificationCompat.Builder(
        context, NotificationChannelManager.CHANNEL_ID_ERRORS)
        .setSmallIcon(android.R.drawable.stat_sys_warning)
        .setGroup(GROUP_NAME_ERRORS)
        .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
        .setDefaults(Notification.DEFAULT_ALL)
        .setAutoCancel(true)
        .setGroupSummary(true)
    notificationManagerCompat.notify(groupName, groupId, groupBuilder.build())
  }

  companion object {
    const val GROUP_NAME_ERRORS = BuildConfig.APPLICATION_ID + ".ERRORS"
    const val NOTIFICATIONS_GROUP_ERROR = -4
    const val NOTIFICATION_ID_HAS_FAILED_OUTGOING_MSGS = 100000001
  }
}