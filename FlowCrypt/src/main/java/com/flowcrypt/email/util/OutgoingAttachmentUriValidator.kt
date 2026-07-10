/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.flowcrypt.email.Constants
import java.io.File
import java.io.IOException
import java.util.Locale

/**
 * Validates attachment URIs used by outgoing messages.
 *
 * Outgoing flows may only use file:// URIs staged inside FlowCrypt-controlled cache directories
 * for the current compose/send session. Regular content:// URIs stay allowed so the system
 * picker and external providers such as Google Drive continue to work.
 */
object OutgoingAttachmentUriValidator {
  @Throws(IllegalArgumentException::class, IOException::class)
  fun requireAllowedUri(context: Context, uri: Uri) {
    when (uri.scheme?.lowercase(Locale.ROOT)) {
      ContentResolver.SCHEME_CONTENT -> return
      ContentResolver.SCHEME_FILE -> requireAllowedFileUri(context, uri)
      else -> throw IllegalArgumentException("Unsupported attachment URI scheme: ${uri.scheme}")
    }
  }

  @Throws(IOException::class)
  private fun requireAllowedFileUri(context: Context, uri: Uri) {
    val path = uri.path ?: throw IllegalArgumentException("Attachment file URI has no path")
    val candidate = File(path).canonicalFile
    val allowedRoots = listOf(
      File(context.cacheDir, Constants.DRAFT_CACHE_DIR).canonicalFile,
      File(context.cacheDir, Constants.ATTACHMENTS_CACHE_DIR).canonicalFile
    )

    if (allowedRoots.none { candidate.isInOrUnder(it) }) {
      throw IllegalArgumentException("Attachment file URI points outside of FlowCrypt cache")
    }
  }

  private fun File.isInOrUnder(root: File): Boolean {
    return path == root.path || path.startsWith(root.path + File.separator)
  }
}
