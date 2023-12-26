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
    messageId: Long,
    outgoingMessageInfo: OutgoingMessageInfo
  ) = withContext(Dispatchers.IO) {
    val directory = getOutgoingInfoDirectory(context)
    val file = File(directory, "$messageId")
    file.outputStream().use { outputStream ->
      KeyStoreCryptoManager.encryptOutputStream(outputStream) { cipherOutputStream ->
        cipherOutputStream.bufferedWriter().use { bufferedWriter ->
          val gson = ApiHelper.getInstance(context).gson
          gson.toJson(outgoingMessageInfo, bufferedWriter)
        }
      }
    }
  }

  suspend fun deleteOutgoingMessageInfo(
    context: Context,
    messageId: Long
  ) = withContext(Dispatchers.IO) {
    val directory = getOutgoingInfoDirectory(context)
    val file = File(directory, "$messageId")
    FileAndDirectoryUtils.deleteFile(file)
  }

  suspend fun getOutgoingMessageInfoFromFile(context: Context, file: File): OutgoingMessageInfo =
    withContext(Dispatchers.IO) {
      file.inputStream().use { inputStream ->
        KeyStoreCryptoManager.getCipherInputStream(inputStream).use { cipherInputStream ->
          cipherInputStream.bufferedReader().use { bufferedReader ->
            val gson = ApiHelper.getInstance(context).gson
            return@withContext gson.fromJson(bufferedReader, OutgoingMessageInfo::class.java)
          }
        }
      }
    }
}