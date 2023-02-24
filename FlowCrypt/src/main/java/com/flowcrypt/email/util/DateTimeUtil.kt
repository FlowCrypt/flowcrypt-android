/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * This class can be used to work with datea and time.
 *
 * @author Denys Bondarenko
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
    fun formatSameDayTime(context: Context?, date: Long?): String {
      val calendarOfDate = GregorianCalendar()
      calendarOfDate.timeInMillis = date ?: return ""

      val currentCalendar = Calendar.getInstance()

      return if (calendarOfDate.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)) {
        val isTheSameDay = calendarOfDate.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH)
            && calendarOfDate.get(Calendar.DAY_OF_MONTH) == currentCalendar.get(Calendar.DAY_OF_MONTH)
        if (isTheSameDay) {
          DateFormat.getTimeFormat(context).format(calendarOfDate.time)
        } else {
          val flags =
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_ABBREV_MONTH
          DateUtils.formatDateTime(context, calendarOfDate.time.time, flags)
        }
      } else {
        val flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
        DateUtils.formatDateTime(context, calendarOfDate.time.time, flags)
      }
    }

    /**
     * Get unified date format for PGP keys that uses UTC timezone.
     * https://github.com/FlowCrypt/flowcrypt-android/issues/2121
     */
    fun getPgpDateFormat(context: Context?): java.text.DateFormat {
      return (if (context != null) {
        DateFormat.getMediumDateFormat(context)
      } else {
        java.text.DateFormat.getDateInstance()
      }).apply {
        timeZone = TimeZone.getTimeZone("UTC")
      }
    }
  }
}
