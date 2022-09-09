/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import android.content.Context
import java.io.File

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
    return FileAndDirectoryUtils.getDir(DIRECTORY_CURRENT_MESSAGE, context.cacheDir)
  }

  fun getDraftDirectory(context: Context): File {
    return FileAndDirectoryUtils.getDir(DIRECTORY_DRAFTS, context.cacheDir)
  }
}
