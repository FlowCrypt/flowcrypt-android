/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import android.app.Application
import androidx.annotation.UiThread
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
  const val CURRENT_MESSAGE_TEMP_DIR = "current_msg"

  var cacheDir: File? = null
    private set

  @UiThread
  fun init(app: Application?) {
    if (app != null) {
      cacheDir = app.cacheDir
    }
  }

  /**
   * Get a temp directory for the current active message
   *
   * @param context Interface to global information about an application environment;
   * @return directory or null if the parent directory doesn't exist
   */
  fun getCurrentMsgTempDir(): File? {
    if (cacheDir == null) {
      return null
    }

    val dir = File(cacheDir, CURRENT_MESSAGE_TEMP_DIR)

    if (!dir.exists()) {
      if (!dir.mkdir()) {
        throw IOException("Couldn't create a temp directory for the current message")
      }
    }

    return dir
  }
}
