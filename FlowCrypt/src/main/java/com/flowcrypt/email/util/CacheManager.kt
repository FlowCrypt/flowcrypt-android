/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import android.content.Context
import java.io.File
import java.io.IOException

/**
 * This class will help to get references to the application specific directories on
 * the filesystem.
 *
 * @author Denis Bondarenko
 *         Date: 7/12/19
 *         Time: 1:01 PM
 *         E-mail: DenBond7@gmail.com
 */
object CacheManager {
  private const val DIRECTORY_CURRENT_MESSAGE = "current_msg"
  private const val DIRECTORY_DRAFTS = "drafts"

  /**
   * Get a temp directory for the current active message
   *
   * @param context Interface to global information about an application environment;
   * @return directory
   */
  fun getCurrentMsgTempDirectory(context: Context): File {
    return getDirectory(context, DIRECTORY_CURRENT_MESSAGE)
  }

  fun getDraftDirectory(context: Context): File {
    return getDirectory(context, DIRECTORY_DRAFTS)
  }

  private fun getDirectory(context: Context, directoryName: String): File {
    val dir = File(context.cacheDir, directoryName)

    if (!dir.exists()) {
      if (!dir.mkdir()) {
        throw IOException("Couldn't create a temp directory for the current message")
      }
    }

    return dir
  }
}
