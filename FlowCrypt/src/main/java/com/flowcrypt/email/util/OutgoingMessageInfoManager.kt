/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.util

import android.content.Context
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.security.KeyStoreCryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * @author Denys Bondarenko
 */
object OutgoingMessageInfoManager {
  private const val DIRECTORY_OUTGOING_INFO = "outgoing_info"

  fun getOutgoingInfoDirectory(context: Context): File {
    return FileAndDirectoryUtils.getDir(DIRECTORY_OUTGOING_INFO, context.filesDir)
  }

  suspend fun enqueueOutgoingMessageInfo(
    context: Context,
    uuid: UUID,
    outgoingMessageInfo: OutgoingMessageInfo
  ) = withContext(Dispatchers.IO) {
    val directory = getOutgoingInfoDirectory(context)
    val draftFile = File(directory, "${uuid}_${System.currentTimeMillis()}")
    draftFile.outputStream().use { outputStream ->
      KeyStoreCryptoManager.encryptOutputStream(outputStream) { cipherOutputStream ->
        cipherOutputStream.bufferedWriter().use { bufferedWriter ->
          val gson = ApiHelper.getInstance(context).gson
          gson.toJson(outgoingMessageInfo, bufferedWriter)
        }
      }
    }
  }
}