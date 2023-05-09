/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.flowcrypt.email.Constants
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.util.CacheManager
import com.flowcrypt.email.util.FileAndDirectoryUtils
import org.apache.commons.io.FileUtils
import java.io.File

/**
 * @author Denys Bondarenko
 */
fun AttachmentInfo.useFileProviderToGenerateUri(context: Context): Pair<File, Uri> {
  val tempDir = CacheManager.getCurrentMsgTempDirectory(context)
  val fileName = FileAndDirectoryUtils.normalizeFileName(getSafeName())
  val file = if (fileName.isNullOrEmpty()) {
    File.createTempFile("tmp", null, tempDir)
  } else {
    val fileCandidate = File(tempDir, fileName)
    if (!fileCandidate.exists()) {
      FileUtils.writeByteArrayToFile(fileCandidate, rawData)
    }
    fileCandidate
  }
  val uri = FileProvider.getUriForFile(context, Constants.FILE_PROVIDER_AUTHORITY, file)
  return Pair(file, uri)
}
