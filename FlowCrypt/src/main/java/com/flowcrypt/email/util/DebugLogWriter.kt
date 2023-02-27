/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import com.flowcrypt.email.util.exception.ExceptionUtil
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The debug log writer which will be used to write logs to the file.
 *
 * @author Denys Bondarenko
 */
class DebugLogWriter(private val fileLog: File) {
  private val dateFormat: DateFormat

  init {
    if (fileLog.length() >= MAX_FILE_SIZE) {
      try {
        FileUtils.writeStringToFile(fileLog, "", Charset.defaultCharset(), false)
      } catch (e: IOException) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
      }

    }

    dateFormat = SimpleDateFormat(YYYY_MM_DD_HH_MM_SS_S, Locale.US)
  }

  fun writeLog(message: String) {
    try {
      val date = String.format(LOG_MESSAGE_PATTERN, dateFormat.format(Date()), "")
      FileUtils.writeStringToFile(fileLog, date, Charset.defaultCharset(), true)
      FileUtils.writeStringToFile(fileLog, message, Charset.defaultCharset(), true)
      FileUtils.writeStringToFile(fileLog, "\n", Charset.defaultCharset(), true)
    } catch (e: IOException) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }

  }

  fun resetLogs() {
    try {
      FileUtils.writeStringToFile(fileLog, "", Charset.defaultCharset(), false)
    } catch (e: IOException) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }
  }

  companion object {
    private const val MAX_FILE_SIZE = FileUtils.ONE_MB
    private const val LOG_MESSAGE_PATTERN = "%-20s    %s"
    private const val YYYY_MM_DD_HH_MM_SS_S = "yyyy-MM-dd HH:mm:ss.S"
  }
}
