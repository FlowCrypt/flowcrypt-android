/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.workmanager

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.protocol.ImapProtocolUtil
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.kotlin.toHex
import com.flowcrypt.email.jetpack.viewmodel.AccountViewModel
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import com.flowcrypt.email.service.attachment.AttachmentNotificationManager
import com.flowcrypt.email.util.FileAndDirectoryUtils
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.ManualHandledException
import com.sun.mail.imap.IMAPFolder
import jakarta.mail.Folder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import java.io.File
import java.io.InputStream

/**
 * @author Denys Bondarenko
 */
class DownloadAttachmentWorker(context: Context, params: WorkerParameters) :
  BaseWorker(context, params) {
  override suspend fun doWork() = withContext(Dispatchers.IO) {
    LogsUtil.d(TAG, "doWork")

    val email = inputData.getString(KEY_EMAIL) ?: ""
    val folder = inputData.getString(KEY_FOLDER) ?: ""
    val uid = inputData.getLong(KEY_UID, -1)
    val path = inputData.getString(KEY_PATH) ?: ""

    val attachmentEntity = roomDatabase.attachmentDao().getAttachment(email, folder, uid, path)
      ?: return@withContext Result.failure()

    return@withContext DownloadAttachmentTask(
      context = applicationContext,
      att = attachmentEntity.toAttInfo()
    ).run()
  }

  private class DownloadAttachmentTask(
    private val context: Context,
    private val att: AttachmentInfo
  ) {
    private val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
    private val notificationManager = AttachmentNotificationManager(context)
    private var finalFileName = att.getSafeName()
    private var attTempFile: File = File.createTempFile("tmp", null, context.externalCacheDir)

    suspend fun run(): Result = withContext(Dispatchers.IO) {
      try {
        val email = att.email
        if (email.isNullOrEmpty()) {
          notificationManager.loadingCanceledByUser(att.copy(name = finalFileName))
          return@withContext Result.failure()
        }

        val account = AccountViewModel.getAccountEntityWithDecryptedInfo(
          roomDatabase.accountDao().getAccount(email)
        )
        if (account == null) {
          notificationManager.loadingCanceledByUser(att.copy(name = finalFileName))
          return@withContext Result.failure()
        }

        if (att.uri != null) {
          context.contentResolver.openInputStream(att.uri)?.use { inputStream ->
            attTempFile.outputStream().use { fileOut ->
              inputStream.copyTo(fileOut)
            }
            attTempFile = decryptFileIfNeeded(context, attTempFile)
            if (!isActive) {
              val uri = storeFileToSharedFolder(context, attTempFile)
              notificationManager.downloadCompleted(
                context = context,
                attInfo = att.copy(name = finalFileName),
                uri = uri,
                useContentApp = account.isHandlingAttachmentRestricted()
              )
              LogsUtil.d(TAG, att.getSafeName() + " is downloaded")
            }

            return@withContext Result.success()
          }
        }

        if (account.useAPI) {
          when (account.accountType) {
            AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
              val msg = GmailApiHelper.loadMsgFullInfo(context, account, att.uid.toHex())
              val attPart = GmailApiHelper.getAttPartByPath(msg.payload, neededPath = att.path)
                ?: throw ManualHandledException(context.getString(R.string.attachment_not_found))

              GmailApiHelper.getAttInputStream(
                context = context,
                accountEntity = account,
                msgId = att.uid.toHex(),
                attId = attPart.body.attachmentId
              ).use { inputStream ->
                handleAttachmentInputStream(
                  inputStream = inputStream,
                  useContentApp = account.isHandlingAttachmentRestricted()
                )
              }
            }

            else -> throw ManualHandledException("Unsupported provider")
          }
        } else {
          val session = OpenStoreHelper.getAttsSess(context, account)
          OpenStoreHelper.openStore(context, account, session).use { store ->
            val label = roomDatabase.labelDao().getLabel(email, account.accountType, att.folder!!)
              ?: if (roomDatabase.accountDao().getAccount(email) == null) {
                notificationManager.loadingCanceledByUser(att.copy(name = finalFileName))
                store.close()
                return@withContext Result.failure()
              } else throw ManualHandledException("Folder \"" + att.folder + "\" not found in the local cache")

            store.getFolder(label.name).use { folder ->
              val remoteFolder = (folder as IMAPFolder).apply { open(Folder.READ_ONLY) }
              val msg = remoteFolder.getMessageByUID(att.uid)
                ?: throw ManualHandledException(context.getString(R.string.no_message_with_this_attachment))

              ImapProtocolUtil.getAttPartByPath(
                part = msg,
                neededPath = att.path
              )?.inputStream?.let { inputStream ->
                handleAttachmentInputStream(
                  inputStream = inputStream,
                  useContentApp = account.isHandlingAttachmentRestricted()
                )
              } ?: throw ManualHandledException(context.getString(R.string.attachment_not_found))
            }
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
        notificationManager.errorHappened(context, att.copy(name = finalFileName), e)
        return@withContext Result.failure()
      } finally {
        deleteTempFile(attTempFile)
      }

      return@withContext Result.success()
    }

    private suspend fun handleAttachmentInputStream(
      inputStream: InputStream,
      useContentApp: Boolean = false
    ) = withContext(Dispatchers.IO) {
      downloadFile(attTempFile, inputStream)

      if (!isActive) {
        notificationManager.loadingCanceledByUser(att.copy(name = finalFileName))
      } else {
        attTempFile = decryptFileIfNeeded(context, attTempFile)
        if (Thread.currentThread().isInterrupted) {
          notificationManager.loadingCanceledByUser(att.copy(name = finalFileName))
        } else {
          val uri = storeFileToSharedFolder(context, attTempFile)
          notificationManager.downloadCompleted(
            context = context,
            attInfo = att.copy(name = finalFileName),
            uri = uri,
            useContentApp = useContentApp
          )
        }
      }
    }

    private fun storeFileToSharedFolder(context: Context, attFile: File): Uri {
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        storeFileUsingScopedStorage(context, attFile)
      } else {
        storeLegacy(attFile, context)
      }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun storeFileUsingScopedStorage(context: Context, attFile: File): Uri {
      val resolver = context.contentResolver
      val fileExtension = FilenameUtils.getExtension(finalFileName).lowercase()
      val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)

      val contentValues = ContentValues().apply {
        put(MediaStore.DownloadColumns.DISPLAY_NAME, finalFileName)
        put(MediaStore.DownloadColumns.SIZE, attFile.length())
        put(MediaStore.DownloadColumns.MIME_TYPE, mimeType)
        put(MediaStore.Downloads.IS_PENDING, 1)
      }

      val fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

      requireNotNull(fileUri)

      //we should check maybe a file is already exist. Then we will use the file name from the system
      val cursor =
        resolver.query(fileUri, arrayOf(MediaStore.DownloadColumns.DISPLAY_NAME), null, null, null)
      cursor?.let {
        if (it.moveToFirst()) {
          val nameIndex = it.getColumnIndex(MediaStore.DownloadColumns.DISPLAY_NAME)
          if (nameIndex != -1) {
            val nameFromSystem = it.getString(nameIndex)
            if (nameFromSystem != finalFileName) {
              finalFileName = nameFromSystem
            }
          }
        }
      }
      cursor?.close()

      val srcInputStream = attFile.inputStream()
      val destOutputStream = resolver.openOutputStream(fileUri)
        ?: throw IllegalArgumentException("provided URI could not be opened")
      srcInputStream.use { srcStream ->
        destOutputStream.use { outStream -> srcStream.copyTo(outStream) }
      }

      //notify the system that the file is ready
      resolver.update(fileUri, ContentValues().apply {
        put(MediaStore.Downloads.IS_PENDING, 0)
      }, null, null)

      return fileUri
    }

    /**
     * We use this method to support saving files on Android 9 and less which uses an old approach.
     */
    private fun storeLegacy(attFile: File, context: Context): Uri {
      val fileName = finalFileName
      val fileDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
      var sharedFile = File(fileDir, fileName)
      sharedFile = if (sharedFile.exists()) {
        FileAndDirectoryUtils.createFileWithIncreasedIndex(fileDir, fileName)
      } else {
        sharedFile
      }

      finalFileName = sharedFile.name
      val srcInputStream = attFile.inputStream()
      val destOutputStream = sharedFile.outputStream()
      srcInputStream.use { srcStream ->
        destOutputStream.use { outStream -> srcStream.copyTo(outStream) }
      }
      return FileProvider.getUriForFile(context, Constants.FILE_PROVIDER_AUTHORITY, sharedFile)
    }

    private suspend fun downloadFile(
      file: File,
      inputStream: InputStream
    ) = withContext(Dispatchers.IO) {
      try {
        file.outputStream().use { outputStream ->
          val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
          var count = 0.0
          val size = att.encodedSize.toDouble()
          var numberOfReadBytes: Int
          var lastPercentage = 0
          var currentPercentage = 0
          var elapsedTime: Long
          val startTime: Long = System.currentTimeMillis()
          var lastUpdateTime = startTime
          updateProgress(currentPercentage, 0)
          while (isActive) {
            numberOfReadBytes = inputStream.read(buffer)

            if (IOUtils.EOF == numberOfReadBytes) {
              break
            }

            if (!Thread.currentThread().isInterrupted) {
              outputStream.write(buffer, 0, numberOfReadBytes)
              count += numberOfReadBytes.toDouble()
              currentPercentage = (count / size * 100f).toInt()
              val isUpdateNeeded =
                System.currentTimeMillis() - lastUpdateTime >= MIN_UPDATE_PROGRESS_INTERVAL
              if (currentPercentage - lastPercentage >= 1 && isUpdateNeeded) {
                lastPercentage = currentPercentage
                lastUpdateTime = System.currentTimeMillis()
                elapsedTime = lastUpdateTime - startTime
                val predictLoadingTime = (elapsedTime * size / count).toLong()
                updateProgress(currentPercentage, predictLoadingTime - elapsedTime)
              }
            } else {
              break
            }
          }

          updateProgress(100, 0)
        }
      } finally {
      }
    }

    /**
     * Do decryption of the downloaded file if needed.
     *
     * @param context Interface to global information about an application environment;
     * @param file    The downloaded file which can be encrypted.
     * @return The decrypted or the original file.
     */
    private suspend fun decryptFileIfNeeded(
      context: Context, file: File
    ): File = withContext(Dispatchers.IO) {
      if (!file.exists()) {
        throw NullPointerException("Error. The file is missing")
      }

      if (!SecurityUtils.isPossiblyEncryptedData(finalFileName)) {
        return@withContext file
      }

      file.inputStream().use { inputStream ->
        val decryptedFile = File.createTempFile("tmp", null, context.externalCacheDir)
        val pgpSecretKeyRings = KeysStorageImpl.getInstance(context).getPGPSecretKeyRings()
        val pgpSecretKeyRingCollection = PGPSecretKeyRingCollection(pgpSecretKeyRings)
        val protector = KeysStorageImpl.getInstance(context).getSecretKeyRingProtector()

        try {
          val messageMetadata = PgpDecryptAndOrVerify.decrypt(
            srcInputStream = inputStream,
            destOutputStream = decryptedFile.outputStream(),
            secretKeys = pgpSecretKeyRingCollection,
            protector = protector
          )

          finalFileName = FilenameUtils.getBaseName(att.getSafeName()).ifEmpty {
            messageMetadata.filename ?: ""
          }

          return@withContext decryptedFile
        } catch (e: Exception) {
          deleteTempFile(decryptedFile)
          throw e
        } finally {
          deleteTempFile(file)
        }
      }
    }

    /**
     * Remove the file which not downloaded fully.
     *
     * @param attachmentFile The file which will be removed.
     */
    private fun deleteTempFile(attachmentFile: File?) {
      if (attachmentFile != null && attachmentFile.exists()) {
        if (!attachmentFile.delete()) {
          LogsUtil.d(TAG, "Cannot delete a file: $attachmentFile")
        } else {
          LogsUtil.d(TAG, "Canceled attachment \"$attachmentFile\" was deleted")
        }
      }
    }

    private fun updateProgress(currentPercentage: Int, timeLeft: Long) {
      if (!Thread.currentThread().isInterrupted) {
        notificationManager.updateLoadingProgress(
          context = context, attInfo = att.copy(name = finalFileName),
          progress = currentPercentage,
          timeLeftInMillisecond = timeLeft
        )
      }
    }

    companion object {
      private const val MIN_UPDATE_PROGRESS_INTERVAL = 500
      private const val DEFAULT_BUFFER_SIZE = 1024 * 16
    }
  }

  companion object {
    private val TAG = DownloadAttachmentWorker::class.java.simpleName
    private const val KEY_EMAIL = "EMAIL"
    private const val KEY_FOLDER = "FOLDER"
    private const val KEY_UID = "UID"
    private const val KEY_PATH = "PATH"

    fun enqueue(context: Context, attachmentInfo: AttachmentInfo) {
      val constraints = Constraints.Builder()
        .build()

      WorkManager
        .getInstance(context.applicationContext)
        .enqueueUniqueWork(
          attachmentInfo.uniqueStringId,
          ExistingWorkPolicy.KEEP,
          OneTimeWorkRequestBuilder<DownloadAttachmentWorker>()
            .setConstraints(constraints)
            .setInputData(
              workDataOf(
                KEY_EMAIL to attachmentInfo.email,
                KEY_FOLDER to attachmentInfo.folder,
                KEY_UID to attachmentInfo.uid,
                KEY_PATH to attachmentInfo.path
              )
            )
            .build()
        )
    }
  }
}
