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
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.ui.activity.MainActivity

/**
 * It's a manager which helps to show system notifications.
 *
 * @author Denis Bondarenko
 *         Date: 8/17/19
 *         Time: 4:22 PM
 *         E-mail: DenBond7@gmail.com
 */
class SystemNotificationManager(context: Context) : CustomNotificationManager(context) {
  override val groupName: String = GROUP_NAME_SYSTEM
  override val groupId: Int = NOTIFICATIONS_GROUP_SYSTEM

  /**
   * Show a [Notification] with a warning text which notifies a user about a weak passphrase.
   *
   * @param account An active account
   */
  fun showPassphraseTooLowNotification(account: AccountEntity?) {
    if (account == null) {
      cancel(NOTIFICATION_ID_PASSPHRASE_TOO_WEAK)
    }

    val intent =
      if (account?.hasClientConfigurationProperty(ClientConfiguration.ConfigurationProperty.NO_PRV_BACKUP) == true) {
        Intent(context, MainActivity::class.java)
      } else {
        Intent(
          Intent.ACTION_VIEW,
          Uri.parse("flowcrypt://email.flowcrypt.com/settings/security")
        )
      }

    val pendingIntent =
      PendingIntent.getActivity(
        context,
        System.currentTimeMillis().toInt(),
        intent,
        PendingIntent.FLAG_IMMUTABLE
      )

    val builder = NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_ID_SYSTEM)
      .setSmallIcon(android.R.drawable.stat_sys_warning)
      .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
      .setContentText(context.getString(R.string.warning_passphrase_is_too_weak))
      .setStyle(
        NotificationCompat.BigTextStyle()
          .bigText(context.getString(R.string.warning_passphrase_is_too_weak))
      )
      .setContentIntent(pendingIntent)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setAutoCancel(true)
      .setGroup(GROUP_NAME_SYSTEM)
      .setSubText(account?.email)
      .setOngoing(true)
      .setDefaults(Notification.DEFAULT_ALL)
      .addAction(
        NotificationCompat.Action.Builder(
          0,
          context.getString(R.string.change_pass_phrase),
          pendingIntent
        ).build()
      )

    notify(groupName, groupId, builder.build())
  }

  companion object {
    const val GROUP_NAME_SYSTEM = BuildConfig.APPLICATION_ID + ".GENERAL"
    const val NOTIFICATIONS_GROUP_SYSTEM = -3
    const val NOTIFICATION_ID_PASSPHRASE_TOO_WEAK = 10000001
  }
}
