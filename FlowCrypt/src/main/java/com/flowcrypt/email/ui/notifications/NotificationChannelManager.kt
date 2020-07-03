/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.flowcrypt.email.R

/**
 * This manager does job of register [NotificationChannel] of the app. The [NotificationChannel] was
 * added in the [Build.VERSION_CODES.O] and doesn't work on previous Android versions.
 *
 * @author Denis Bondarenko
 * Date: 17.10.2017
 * Time: 12:12
 * E-mail: DenBond7@gmail.com
 */
object NotificationChannelManager {
  const val CHANNEL_ID_ATTACHMENTS = "Attachments"
  const val CHANNEL_ID_MESSAGES = "Messages"
  const val CHANNEL_ID_SYSTEM = "System"
  const val CHANNEL_ID_ERRORS = "Errors"
  const val CHANNEL_ID_SYNC = "Sync"

  /**
   * Register [NotificationChannel](s) of the app in the system.
   *
   * @param context Interface to global information about an application environment.
   */
  fun registerNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

      notificationManager.createNotificationChannel(genGeneralNotificationChannel(context))
      notificationManager.createNotificationChannel(genAttsNotificationChannel(context))
      notificationManager.createNotificationChannel(genMsgsNotificationChannel(context))
      notificationManager.createNotificationChannel(genErrorNotificationChannel(context))
      notificationManager.createNotificationChannel(genSyncNotificationChannel(context))
    }
  }

  /**
   * Generate messages notification channel.
   *
   * @param context Interface to global information about an application environment.
   * @return Generated [NotificationChannel]
   */
  @RequiresApi(api = Build.VERSION_CODES.O)
  private fun genMsgsNotificationChannel(context: Context): NotificationChannel {
    val name = context.getString(R.string.messages)
    val description = context.getString(R.string.messages_notification_channel)
    val importance = NotificationManager.IMPORTANCE_DEFAULT

    val notificationChannel = NotificationChannel(CHANNEL_ID_MESSAGES, name, importance)
    notificationChannel.description = description
    notificationChannel.enableLights(true)
    notificationChannel.enableVibration(true)

    return notificationChannel
  }

  /**
   * Generate attachments notification channel.
   *
   * @param context Interface to global information about an application environment.
   * @return Generated [NotificationChannel]
   */
  @RequiresApi(api = Build.VERSION_CODES.O)
  private fun genAttsNotificationChannel(context: Context): NotificationChannel {
    val name = context.getString(R.string.attachments)
    val description = context.getString(R.string.download_attachments_notification_channel)
    val importance = NotificationManager.IMPORTANCE_DEFAULT

    val notificationChannel = NotificationChannel(CHANNEL_ID_ATTACHMENTS, name, importance)
    notificationChannel.description = description
    notificationChannel.enableLights(false)
    notificationChannel.enableVibration(false)

    return notificationChannel
  }

  /**
   * Generate general notification channel.
   *
   * @param context Interface to global information about an application environment.
   * @return Generated [NotificationChannel]
   */
  @RequiresApi(api = Build.VERSION_CODES.O)
  private fun genGeneralNotificationChannel(context: Context): NotificationChannel {
    val name = context.getString(R.string.system)
    val description = context.getString(R.string.system_notifications_notification_chanel)
    val importance = NotificationManager.IMPORTANCE_DEFAULT

    val notificationChannel = NotificationChannel(CHANNEL_ID_SYSTEM, name, importance)
    notificationChannel.description = description
    notificationChannel.enableLights(true)
    notificationChannel.enableVibration(true)

    return notificationChannel
  }

  /**
   * Generate errors notification channel.
   *
   * @param context Interface to global information about an application environment.
   * @return Generated [NotificationChannel]
   */
  @RequiresApi(api = Build.VERSION_CODES.O)
  private fun genErrorNotificationChannel(context: Context): NotificationChannel {
    val name = context.getString(R.string.errors_notifications)
    val description = context.getString(R.string.errors_notifications_notification_chanel)
    val importance = NotificationManager.IMPORTANCE_HIGH

    val notificationChannel = NotificationChannel(CHANNEL_ID_ERRORS, name, importance)
    notificationChannel.description = description
    notificationChannel.enableLights(true)
    notificationChannel.enableVibration(true)

    return notificationChannel
  }

  /**
   * Generate sync notification channel.
   *
   * @param context Interface to global information about an application environment.
   * @return Generated [NotificationChannel]
   */
  @RequiresApi(api = Build.VERSION_CODES.O)
  private fun genSyncNotificationChannel(context: Context): NotificationChannel {
    val name = context.getString(R.string.sync)
    val description = context.getString(R.string.sync_notifications_notification_chanel)
    val importance = NotificationManager.IMPORTANCE_LOW

    val notificationChannel = NotificationChannel(CHANNEL_ID_SYNC, name, importance)
    notificationChannel.description = description
    notificationChannel.enableLights(false)
    notificationChannel.enableVibration(false)

    return notificationChannel
  }
}
