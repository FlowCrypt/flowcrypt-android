/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.util

import android.content.Context
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.java.lang.printStackTraceIfDebugOnly
import com.flowcrypt.email.security.KeyStoreCryptoManager
import jakarta.mail.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * @author Denys Bondarenko
 */
object OutgoingMessagesManager {
  private const val DIRECTORY_OUTGOING = "outgoing"

  /**
   * Store [Message] in [DIRECTORY_OUTGOING] folder.
   * This folder is located in the 'files' folder in the app private root directory.
   * Need to add that [Message] will be encrypted by Android KeyStore via [KeyStoreCryptoManager]
   * and the file will use [MessageEntity.id] as a name.
   *
   * @param context              Interface to global information about an application environment.
   * @param id                   This value will be used as a file name.
   * @param mimeMessage          the outgoing MIME message
   */
  suspend fun enqueueOutgoingMessage(
    context: Context,
    id: Long,
    mimeMessage: Message
  ) = withContext(Dispatchers.IO) {
    val directory = getOutgoingMessagesDirectory(context)
    val file = File(directory, "$id")
    file.outputStream().use { outputStream ->
      KeyStoreCryptoManager.encryptOutputStream(outputStream) { cipherOutputStream ->
        cipherOutputStream.use { mimeMessage.writeTo(it) }
      }
    }
  }

  /**
   * Update existing [Message] in [DIRECTORY_OUTGOING] folder.
   * [Message] will be encrypted by Android KeyStore via [KeyStoreCryptoManager]
   * and the file will use [MessageEntity.id] as a name.
   *
   * @param context              Interface to global information about an application environment.
   * @param id                   This value will be used as a file name.
   * @param mimeMessage          the outgoing MIME message
   */
  suspend fun updatedOutgoingMessage(
    context: Context,
    id: Long,
    mimeMessage: Message
  ) = withContext(Dispatchers.IO) {
    val directory = getOutgoingMessagesDirectory(context)
    val file = File(directory, "$id")
    file.outputStream().use { outputStream ->
      KeyStoreCryptoManager.encryptOutputStream(outputStream) { cipherOutputStream ->
        cipherOutputStream.use { mimeMessage.writeTo(it) }
      }
    }
  }

  /**
   * Delete a source of [Message].
   *
   * @param context  Interface to global information about an application environment.
   * @param id       This value will be used as a file name.
   */
  suspend fun deleteOutgoingMessage(
    context: Context,
    id: Long
  ) = withContext(Dispatchers.IO) {
    val directory = getOutgoingMessagesDirectory(context)
    val file = File(directory, "$id")
    FileAndDirectoryUtils.deleteFile(file)
  }

  /**
   * Get stored MIME message as a bytes array.
   * Need to add that the MIME message was encrypted by Android KeyStore via [KeyStoreCryptoManager].
   * Data should be decrypted before usage.
   *
   * @param context Interface to global information about an application environment.
   * @param id      This value will be used as a file name.
   */
  suspend fun getOutgoingMessageFromFile(context: Context, id: Long): ByteArray? =
    withContext(Dispatchers.IO) {
      val file = FileAndDirectoryUtils.getFilesInDir(
        getOutgoingMessagesDirectory(context)
      ).firstOrNull {
        it.name == id.toString()
      }

      file?.inputStream()?.use { inputStream ->
        KeyStoreCryptoManager.getCipherInputStream(inputStream).use { cipherInputStream ->
          return@withContext cipherInputStream.readBytes()
        }
      } ?: return@withContext null
    }

  /**
   * This method check existing cache and delete unused stored [Message] objects
   *
   * @param context Interface to global information about an application environment.
   */
  suspend fun checkAndCleanCache(context: Context) = withContext(Dispatchers.IO) {
    val outgoingMessageIds =
      FlowCryptRoomDatabase.getDatabase(context).msgDao().getAllOutboxMessages()
        .mapNotNull { it.id }

    FileAndDirectoryUtils.getFilesInDir(getOutgoingMessagesDirectory(context)).forEach {
      if (it.name.toLong() !in outgoingMessageIds) {
        try {
          FileAndDirectoryUtils.deleteFile(it)
        } catch (e: Exception) {
          e.printStackTraceIfDebugOnly()
        }
      }
    }
  }

  /**
   * Check if a [Message] with the given name exists.
   *
   * @param context Interface to global information about an application environment.
   * @param id      This value will be used as a file name.
   */
  fun isMessageExist(context: Context, id: Long): Boolean {
    return FileAndDirectoryUtils.getFilesInDir(getOutgoingMessagesDirectory(context)).any {
      it.name == id.toString()
    }
  }

  private fun getOutgoingMessagesDirectory(context: Context): File {
    return FileAndDirectoryUtils.getDir(DIRECTORY_OUTGOING, context.filesDir)
  }
}