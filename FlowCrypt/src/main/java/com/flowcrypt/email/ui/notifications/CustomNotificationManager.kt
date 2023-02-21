/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.notifications

import android.Manifest
import android.app.Notification
import android.app.Notification.InboxStyle
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * A base class for [android.app.Notification]
 *
 * @author Denis Bondarenko
 * Date: 27.06.2018
 * Time: 12:09
 * E-mail: DenBond7@gmail.com
 */
abstract class CustomNotificationManager(protected var context: Context) {
  protected var notificationManagerCompat: NotificationManagerCompat =
    NotificationManagerCompat.from(context)

  abstract val groupName: String
  abstract val groupId: Int

  open fun notify(tag: String?, id: Int, notification: Notification) {
    val isAndroidTiramisuOrHigh = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    if (isAndroidTiramisuOrHigh &&
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      return
    }

    notificationManagerCompat.notify(tag, id, notification)
  }

  /**
   * Cancel a notification with the given tag.
   *
   * @param tag the string identifier of the notification.
   */
  fun cancel(tag: String) {
    cancel(tag, -1)
  }

  /**
   * Cancel a previously shown notification.
   *
   * @param notificationId the ID of the notification
   */
  fun cancel(notificationId: Int) {
    cancel(groupName, notificationId)
  }

  /**
   * Cancel a notification with the given id.
   *
   * @param tag            the string identifier of the notification.
   * @param notificationId The notification id.
   */
  fun cancel(tag: String?, notificationId: Int) {
    notificationManagerCompat.cancel(tag, notificationId)
    val notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    var messageCount = 0
    for (statusBarNotification in notificationManager.activeNotifications) {
      if (groupName == statusBarNotification.notification.group) {
        messageCount++
      }
    }

    if (messageCount == 1) {
      notificationManager.cancel(groupName, groupId)
    }
  }

  /**
   * Prepare formatted line for [InboxStyle]
   *
   * @param context  Interface to global information about an application environment.
   * @param username A sender name.
   * @param subject  An incoming message subject.
   * @return A formatted line.
   */
  protected fun formatInboxStyleLine(
    context: Context,
    username: String,
    subject: String?
  ): Spannable {
    val builder = StringBuilder()
    if (!TextUtils.isEmpty(username)) {
      builder.append(username).append("   ")
    }

    if (!TextUtils.isEmpty(subject)) {
      builder.append(subject)
    }

    val spannable = SpannableString(builder)
    if (!TextUtils.isEmpty(username)) {
      val color = ContextCompat.getColor(context, android.R.color.black)
      spannable.setSpan(
        ForegroundColorSpan(color),
        0,
        username.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
      )
    }
    return spannable
  }

  /**
   * Format an input text via apply [ForegroundColorSpan] to it.
   *
   * @param text  An input text.
   * @param color A color which will be used for change the text style.
   * @return A formatted text.
   */
  protected fun formatText(text: String?, color: Int): Spannable {
    if (TextUtils.isEmpty(text)) {
      return SpannableString("")
    }

    val spannable = SpannableString(text)
    spannable.setSpan(
      ForegroundColorSpan(color),
      0,
      text!!.length,
      Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    return spannable
  }
}
