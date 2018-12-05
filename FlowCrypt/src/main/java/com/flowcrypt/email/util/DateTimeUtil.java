/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util;

import android.content.Context;
import android.text.format.DateFormat;
import android.text.format.DateUtils;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * This class can be used to work with datea and time.
 *
 * @author DenBond7
 * Date: 08.06.2017
 * Time: 15:52
 * E-mail: DenBond7@gmail.com
 */

public class DateTimeUtil {

  /**
   * Format date and time using the next logic:
   * <p>
   * <ul>
   * <li>If the day of the date equals the current day, we return only time in a locale time
   * format. For example 10:00 AM</li>
   * <li>If the day of the date not equals the current day, we return only date in a locale date
   * format without year. For example Jun 5</li>
   * </ul>
   *
   * @param context Interface to global information about an application environment.
   * @param date    This is the date, which we want to format.
   * @return The formatted date.
   */
  public static String formatSameDayTime(Context context, long date) {
    Calendar calendarOfDate = new GregorianCalendar();
    calendarOfDate.setTimeInMillis(date);

    Calendar currentCalendar = Calendar.getInstance();

    if (calendarOfDate.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)) {
      boolean isTheSameDay = calendarOfDate.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH)
          && calendarOfDate.get(Calendar.DAY_OF_MONTH) == currentCalendar.get(Calendar.DAY_OF_MONTH);
      if (isTheSameDay) {
        return DateFormat.getTimeFormat(context).format(calendarOfDate.getTime());
      } else {
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR | DateUtils.FORMAT_ABBREV_MONTH;
        return DateUtils.formatDateTime(context, calendarOfDate.getTime().getTime(), flags);
      }
    } else {
      int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH;
      return DateUtils.formatDateTime(context, calendarOfDate.getTime().getTime(), flags);
    }
  }
}
