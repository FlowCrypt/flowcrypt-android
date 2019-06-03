/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.notifications;

import android.app.Notification.InboxStyle;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/**
 * A base class for {@link android.app.Notification}
 *
 * @author Denis Bondarenko
 * Date: 27.06.2018
 * Time: 12:09
 * E-mail: DenBond7@gmail.com
 */
public abstract class CustomNotificationManager {
  protected Context context;
  protected NotificationManagerCompat notificationManagerCompat;

  public CustomNotificationManager(Context context) {
    this.context = context;
    this.notificationManagerCompat = NotificationManagerCompat.from(context);
  }

  public abstract String getGroupName();

  public abstract int getGroupId();

  /**
   * Cancel a previously shown notification.
   *
   * @param notificationId the ID of the notification
   */
  public void cancel(int notificationId) {
    cancel(null, notificationId);
  }

  /**
   * Cancel a notification with the given id.
   *
   * @param tag            the string identifier of the notification.
   * @param notificationId The notification id.
   */
  public void cancel(@Nullable String tag, int notificationId) {
    notificationManagerCompat.cancel(tag, notificationId);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      NotificationManager notificationManager =
          (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

      if (notificationManager != null) {
        int messageCount = 0;
        for (StatusBarNotification statusBarNotification : notificationManager.getActiveNotifications()) {
          if (getGroupName().equals(statusBarNotification.getNotification().getGroup())) {
            messageCount++;
          }
        }

        if (messageCount == 1) {
          notificationManager.cancel(getGroupId());
        }
      }
    }
  }

  /**
   * Prepare formatted line for {@link InboxStyle}
   *
   * @param context  Interface to global information about an application environment.
   * @param username A sender name.
   * @param subject  An incoming message subject.
   * @return A formatted line.
   */
  protected Spannable formatInboxStyleLine(Context context, String username, String subject) {
    StringBuilder builder = new StringBuilder();
    if (!TextUtils.isEmpty(username)) {
      builder.append(username).append("   ");
    }

    if (!TextUtils.isEmpty(subject)) {
      builder.append(subject);
    }

    Spannable spannable = new SpannableString(builder);
    if (!TextUtils.isEmpty(username)) {
      int color = ContextCompat.getColor(context, android.R.color.black);
      spannable.setSpan(new ForegroundColorSpan(color), 0, username.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    return spannable;
  }

  /**
   * Format an input text via apply {@link ForegroundColorSpan} to it.
   *
   * @param text  An input text.
   * @param color A color which will be used for change the text style.
   * @return A formatted text.
   */
  protected Spannable formatText(String text, int color) {
    if (TextUtils.isEmpty(text)) {
      return new SpannableString("");
    }

    Spannable spannable = new SpannableString(text);
    spannable.setSpan(new ForegroundColorSpan(color), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    return spannable;
  }
}
