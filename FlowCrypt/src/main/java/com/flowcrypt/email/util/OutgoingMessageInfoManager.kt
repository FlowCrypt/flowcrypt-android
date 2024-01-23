/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.util

import android.content.Context
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.java.lang.printStackTraceIfDebugOnly
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

  /**
   * Store [OutgoingMessageInfo] as JSON in [DIRECTORY_OUTGOING_INFO] folder.
   * This folder is located in the 'files' folder in the app private root directory.
   * Later this JSON will be converted to [OutgoingMessageInfo] back and the last one will be used
   * to create a new outgoing message.
   * Need to add that JSON will be encrypted by Android KeyStore via [KeyStoreCryptoManager]
   * and the file will use [MessageEntity.id] as a name.
   *
   * @param context              Interface to global information about an application environment.
   * @param messageEntity        [MessageEntity] object that contains a base info about
  the given [OutgoingMessageInfo]
   * @param outgoingMessageInfo the message outgoing info
   */
  suspend fun enqueueOutgoingMessageInfo(
    context: Context,
    messageEntity: MessageEntity,
    outgoingMessageInfo: OutgoingMessageInfo
  ) = withContext(Dispatchers.IO) {
    val directory = getOutgoingInfoDirectory(context)
    val file = File(directory, "${messageEntity.id}")
    file.outputStream().use { outputStream ->
      KeyStoreCryptoManager.encryptOutputStream(outputStream) { cipherOutputStream ->
        cipherOutputStream.bufferedWriter().use { bufferedWriter ->
          val gson = ApiHelper.getInstance(context).gson
          gson.toJson(outgoingMessageInfo, bufferedWriter)
        }
      }
    }
  }

  /**
   * Delete [OutgoingMessageInfo] after creating a new outgoing message.
   *
   * @param context  Interface to global information about an application environment.
   * @param id       This value will be used as a file name.
   */
  suspend fun deleteOutgoingMessageInfo(
    context: Context,
    id: Long
  ) = withContext(Dispatchers.IO) {
    val directory = getOutgoingInfoDirectory(context)
    val file = File(directory, "$id")
    FileAndDirectoryUtils.deleteFile(file)
  }

  /**
   * Convert stored JSON as a file back to [OutgoingMessageInfo] object. Need to add that JSON
   * was encrypted by Android KeyStore via [KeyStoreCryptoManager]. JSON should be decrypted before
   * converting to [OutgoingMessageInfo] object.
   *
   * @param context Interface to global information about an application environment.
   * @param file    A file that contains encrypted JSON of [OutgoingMessageInfo] object
   */
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

  /**
   * This method check existing cache and delete unused stored [OutgoingMessageInfo] object
   *
   * @param context Interface to global information about an application environment.
   */
  suspend fun checkAndCleanCache(context: Context) = withContext(Dispatchers.IO) {
    val outgoingMessageIds =
      FlowCryptRoomDatabase.getDatabase(context).msgDao().getAllOutboxMessages()
        .mapNotNull { it.id }

    FileAndDirectoryUtils.getFilesInDir(getOutgoingInfoDirectory(context)).forEach {
      if (it.name.toLong() !in outgoingMessageIds) {
        try {
          FileAndDirectoryUtils.deleteFile(it)
        } catch (e: Exception) {
          e.printStackTraceIfDebugOnly()
        }
      }
    }
  }
}