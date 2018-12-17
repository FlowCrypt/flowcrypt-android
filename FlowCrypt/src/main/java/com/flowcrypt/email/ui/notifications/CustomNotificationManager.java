/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.notifications;

import android.app.Notification.InboxStyle;
import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

import androidx.core.content.ContextCompat;

/**
 * A base class for {@link android.app.Notification}
 *
 * @author Denis Bondarenko
 * Date: 27.06.2018
 * Time: 12:09
 * E-mail: DenBond7@gmail.com
 */
public class CustomNotificationManager {
  public static final int NOTIFICATIONS_GROUP_MESSAGES = -1;
  public static final int NOTIFICATIONS_GROUP_ATTACHMENTS = -2;

  /**
   * Prepare formatted line for {@link InboxStyle}
   *
   * @param context  Interface to global information about an application environment.
   * @param username A sender name.
   * @param subject  An incoming message subject.
   * @return A formatted line.
   */
  protected Spannable formatInboxStyleLine(Context context, String username, String subject) {
    Spannable spannable = new SpannableString(username + "   " + subject);
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
    Spannable spannable = new SpannableString(text);
    if (!TextUtils.isEmpty(text)) {
      spannable.setSpan(new ForegroundColorSpan(color), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    return spannable;
  }
}
