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
 * @author Denys Bondarenko
 */
object CacheManager {
  private const val DIRECTORY_DRAFTS = "drafts"

  fun getDraftDirectory(context: Context): File {
    return FileAndDirectoryUtils.getDir(DIRECTORY_DRAFTS, context.cacheDir)
  }
}
