/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import java.util.*

/**
 * This class can be used to work with datea and time.
 *
 * @author DenBond7
 * Date: 08.06.2017
 * Time: 15:52
 * E-mail: DenBond7@gmail.com
 */

class DateTimeUtil {
  companion object {
    /**
     * Format date and time using the next logic:
     *
     *
     *
     *  * If the day of the date equals the current day, we return only time in a locale time
     * format. For example 10:00 AM
     *  * If the day of the date not equals the current day, we return only date in a locale date
     * format without year. For example Jun 5
     *
     *
     * @param context Interface to global information about an application environment.
     * @param date    This is the date, which we want to format.
     * @return The formatted date.
     */
    @JvmStatic
    fun formatSameDayTime(context: Context, date: Long): String {
      val calendarOfDate = GregorianCalendar()
      calendarOfDate.timeInMillis = date

      val currentCalendar = Calendar.getInstance()

      return if (calendarOfDate.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)) {
        val isTheSameDay = calendarOfDate.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH)
            && calendarOfDate.get(Calendar.DAY_OF_MONTH) == currentCalendar.get(Calendar.DAY_OF_MONTH)
        if (isTheSameDay) {
          DateFormat.getTimeFormat(context).format(calendarOfDate.time)
        } else {
          val flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_ABBREV_MONTH
          DateUtils.formatDateTime(context, calendarOfDate.time.time, flags)
        }
      } else {
        val flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
        DateUtils.formatDateTime(context, calendarOfDate.time.time, flags)
      }
    }
  }
}
