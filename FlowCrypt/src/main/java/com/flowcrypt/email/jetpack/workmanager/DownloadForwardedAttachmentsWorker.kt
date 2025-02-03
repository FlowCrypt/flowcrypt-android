/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.workmanager

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkerParameters
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.protocol.ImapProtocolUtil
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.AttachmentEntity
import com.flowcrypt.email.extensions.kotlin.toHex
import com.flowcrypt.email.util.FileAndDirectoryUtils
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import jakarta.mail.Folder
import jakarta.mail.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import org.eclipse.angus.mail.imap.IMAPFolder
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * This realization of [CoroutineWorker] downloads the attachments for forwarding purposes.
 *
 * @author Denys Bondarenko
 */
class DownloadForwardedAttachmentsWorker(context: Context, params: WorkerParameters) :
  BaseWorker(context, params) {
  private val attachmentsCacheDir =
    File(applicationContext.cacheDir, Constants.ATTACHMENTS_CACHE_DIR)
  private val forwardedAttachmentsCacheDir =
    File(attachmentsCacheDir, Constants.FORWARDED_ATTACHMENTS_CACHE_DIR)

  override suspend fun doWork(): Result =
    withContext(Dispatchers.IO) {
      LogsUtil.d(TAG, "doWork")
      if (isStopped) {
        return@withContext Result.success()
      }

      try {
        if (!attachmentsCacheDir.exists()) {
          if (!attachmentsCacheDir.mkdirs()) {
            throw IllegalStateException(
              "Creating cache directory ${attachmentsCacheDir.name} failed!"
            )
          }
        }

        if (!forwardedAttachmentsCacheDir.exists()) {
          if (!forwardedAttachmentsCacheDir.mkdirs()) {
            throw IllegalStateException(
              "Creating cache directory ${forwardedAttachmentsCacheDir.name} failed!"
            )
          }
        }

        val account = roomDatabase.accountDao().getActiveAccountSuspend()?.withDecryptedInfo()
          ?: return@withContext Result.success()

        val newForwardedMessages = roomDatabase.msgDao().getOutboxMsgsByStatesSuspend(
          account = account.email,
          msgStates = listOf(MessageState.NEW_FORWARDED.value)
        )

        if (newForwardedMessages.isNotEmpty()) {
          if (account.useAPI) {
            when (account.accountType) {
              AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
                downloadForwardedAttachments(account)
              }

              else -> throw IllegalStateException("Unsupported provider")
            }
          } else {
            OpenStoreHelper.openStore(
              applicationContext,
              account,
              OpenStoreHelper.getAccountSess(applicationContext, account)
            ).use { store ->
              downloadForwardedAttachments(account, store)
            }
          }
        }

        return@withContext rescheduleIfActiveAccountWasChanged(account)
      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
        return@withContext Result.failure()
      }
    }

  private suspend fun downloadForwardedAttachments(account: AccountEntity, store: Store? = null) =
    withContext(Dispatchers.IO) {
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)

      while (true) {
        val detailsList = roomDatabase.msgDao().getOutboxMsgsByStatesSuspend(
          account = account.email,
          msgStates = listOf(MessageState.NEW_FORWARDED.value)
        ).takeIf { it.isNotEmpty() } ?: break

        try {
          val msgEntity = detailsList.first()
          val tempDirectoryForForwardedAttachments = File(
            forwardedAttachmentsCacheDir, requireNotNull(msgEntity.attachmentsDirectory)
          )

          val attachmentEntities = roomDatabase.attachmentDao().getAttachments(
            account = account.email,
            accountType = account.accountType,
            label = JavaEmailConstants.FOLDER_OUTBOX,
            uid = msgEntity.uid
          ).filter { it.isForwarded }

          if (attachmentEntities.isEmpty()) {
            roomDatabase.msgDao().updateSuspend(msgEntity.copy(state = MessageState.QUEUED.value))
            continue
          } else {
            if (!tempDirectoryForForwardedAttachments.exists()) {
              if (!tempDirectoryForForwardedAttachments.mkdirs()) {
                throw IllegalStateException(
                  "Creating cache directory ${tempDirectoryForForwardedAttachments.name} failed!"
                )
              }
            }
          }

          val msgState = getNewMsgState(
            account = account,
            parentDirectory = tempDirectoryForForwardedAttachments,
            attachmentEntities = attachmentEntities,
            store = store
          )

          val updateResult = roomDatabase.msgDao().updateSuspend(
            msgEntity.copy(
              state = if (
                msgState == MessageState.QUEUED
                && msgEntity.isEncrypted == true
                && msgEntity.isPasswordProtected
              ) {
                MessageState.NEW_PASSWORD_PROTECTED.value
              } else {
                msgState.value
              }
            )
          )

          if (updateResult > 0) {
            if (msgState != MessageState.QUEUED) {
              GeneralUtil.notifyUserAboutProblemWithOutgoingMsgs(applicationContext, account)
            }

            if (msgEntity.isEncrypted == true && msgEntity.isPasswordProtected) {
              HandlePasswordProtectedMsgWorker.enqueue(applicationContext)
            } else {
              MessagesSenderWorker.enqueue(applicationContext)
            }
          }
        } catch (e: Exception) {
          e.printStackTrace()
          ExceptionUtil.handleError(e)

          if (!GeneralUtil.isConnected(applicationContext)) {
            throw e
          }
        }
      }
    }

  private suspend fun getNewMsgState(
    account: AccountEntity,
    parentDirectory: File,
    attachmentEntities: List<AttachmentEntity>,
    store: Store?
  ): MessageState =
    withContext(Dispatchers.IO) {
      return@withContext if (account.useAPI) {
        when (account.accountType) {
          AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
            val msg = attachmentEntities.first().forwardedUid?.toHex()
              ?.let {
                GmailApiHelper.loadMsgInfoSuspend(
                  context = applicationContext,
                  accountEntity = account,
                  msgId = it,
                  format = GmailApiHelper.RESPONSE_FORMAT_FULL
                )
              }
              ?: return@withContext MessageState.ERROR_ORIGINAL_MESSAGE_MISSING

            loadAttachments(attachmentEntities, parentDirectory) { attachmentEntity ->
              GmailApiHelper.getAttPartByPath(msg.payload, neededPath = attachmentEntity.path)
                ?.let { attPart ->
                  GmailApiHelper.getAttInputStream(
                    applicationContext,
                    account,
                    requireNotNull(attachmentEntity.forwardedUid?.toHex()),
                    attPart.body.attachmentId
                  )
                }
            }
          }

          else -> throw IllegalStateException("Unsupported provider")
        }
      } else {
        store?.let {
          store.getFolder(attachmentEntities.first().forwardedFolder).use { folder ->
            val imapFolder = (folder as IMAPFolder).apply { open(Folder.READ_ONLY) }
            val fwdMsg =
              attachmentEntities.first().forwardedUid?.let { uid -> imapFolder.getMessageByUID(uid) }
              ?: return@withContext MessageState.ERROR_ORIGINAL_MESSAGE_MISSING
            loadAttachments(attachmentEntities, parentDirectory) { attachmentEntity ->
              ImapProtocolUtil.getAttPartByPath(
                fwdMsg,
                neededPath = attachmentEntity.path
              )?.inputStream
            }
          }
        } ?: throw NullPointerException("Store == null")
      }
    }

  private suspend fun loadAttachments(
    attachmentEntities: List<AttachmentEntity>,
    parentDirectory: File,
    action: suspend (attachmentEntity: AttachmentEntity) -> InputStream?
  ): MessageState = withContext(Dispatchers.IO) {
    var msgState = MessageState.QUEUED
    FileAndDirectoryUtils.cleanDir(parentDirectory)
    for (attachmentEntity in attachmentEntities) {
      if (!attachmentEntity.isForwarded) {
        continue
      }

      val attName = attachmentEntity.name

      val attFile = File(parentDirectory, attName)
      val exists = attFile.exists()

      var uri: Uri?
      when {
        exists -> {
          uri = Uri.fromFile(attFile)
        }

        attachmentEntity.fileUri == null -> {
          val inputStream = action.invoke(attachmentEntity)
          val tempFile = File(parentDirectory, UUID.randomUUID().toString())
          if (inputStream != null) {
            inputStream.use { srcStream ->
              FileOutputStream(tempFile).use { destStream ->
                srcStream.copyTo(destStream)
              }
            }

            if (parentDirectory.exists()) {
              FileUtils.moveFile(tempFile, attFile)
              uri = Uri.fromFile(attFile)
            } else {
              FileAndDirectoryUtils.cleanDir(parentDirectory)
              //It means the user has already deleted the current message. We don't need to download other attachments.
              break
            }
          } else {
            msgState = MessageState.ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND
            break
          }
        }

        else -> {
          uri = attachmentEntity.fileUri.toUri()
        }
      }

      if (uri != null) {
        FlowCryptRoomDatabase
          .getDatabase(applicationContext)
          .attachmentDao()
          .updateSuspend(attachmentEntity.copy(fileUri = uri.toString()))
      }
    }

    return@withContext msgState
  }

  companion object {
    private val TAG = DownloadForwardedAttachmentsWorker::class.java.simpleName
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".DOWNLOAD_FORWARDED_ATTACHMENTS"

    fun enqueue(context: Context) {
      enqueueWithDefaultParameters<DownloadForwardedAttachmentsWorker>(
        context = context,
        uniqueWorkName = GROUP_UNIQUE_TAG,
        existingWorkPolicy = ExistingWorkPolicy.KEEP
      )
    }
  }
}
