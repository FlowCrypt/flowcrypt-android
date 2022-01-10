/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.workmanager

import android.content.Context
import androidx.core.content.FileProvider
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.flowcrypt.email.Constants
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.protocol.ImapProtocolUtil
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.AttachmentEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.kotlin.toHex
import com.flowcrypt.email.jetpack.viewmodel.AccountViewModel
import com.flowcrypt.email.util.FileAndDirectoryUtils
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.ManualHandledException
import com.google.android.gms.common.util.CollectionUtils
import com.sun.mail.imap.IMAPFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*
import javax.mail.Folder
import javax.mail.Store

/**
 * This realization of [CoroutineWorker] downloads the attachments for forwarding purposes.
 *
 * @author Denis Bondarenko
 * Date: 09.10.2018
 * Time: 11:48
 * E-mail: DenBond7@gmail.com
 */
class ForwardedAttachmentsDownloaderWorker(context: Context, params: WorkerParameters) :
  BaseWorker(context, params) {
  private val attCacheDir = File(applicationContext.cacheDir, Constants.ATTACHMENTS_CACHE_DIR)
  private val fwdAttsCacheDir = File(attCacheDir, Constants.FORWARDED_ATTACHMENTS_CACHE_DIR)

  override suspend fun doWork(): Result =
    withContext(Dispatchers.IO) {
      LogsUtil.d(TAG, "doWork")
      if (isStopped) {
        return@withContext Result.success()
      }

      try {
        if (!attCacheDir.exists()) {
          if (!attCacheDir.mkdirs()) {
            throw IllegalStateException("Create cache directory " + attCacheDir.name + " filed!")
          }
        }

        if (!fwdAttsCacheDir.exists()) {
          if (!fwdAttsCacheDir.mkdirs()) {
            throw IllegalStateException("Create cache directory " + fwdAttsCacheDir.name + " filed!")
          }
        }

        val account = AccountViewModel.getAccountEntityWithDecryptedInfoSuspend(
          roomDatabase.accountDao().getActiveAccountSuspend()
        )
          ?: return@withContext Result.success()

        val newMsgs = roomDatabase.msgDao().getOutboxMsgsByStatesSuspend(
          account = account.email,
          msgStates = listOf(MessageState.NEW_FORWARDED.value)
        )

        if (!CollectionUtils.isEmpty(newMsgs)) {
          if (account.useAPI) {
            when (account.accountType) {
              AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
                downloadForwardedAtts(account)
              }

              else -> throw ManualHandledException("Unsupported provider")
            }
          } else {
            OpenStoreHelper.openStore(
              applicationContext,
              account,
              OpenStoreHelper.getAccountSess(applicationContext, account)
            ).use { store ->
              downloadForwardedAtts(account, store)
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

  private suspend fun downloadForwardedAtts(account: AccountEntity, store: Store? = null) =
    withContext(Dispatchers.IO) {
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)

      while (true) {
        val detailsList = roomDatabase.msgDao().getOutboxMsgsByStatesSuspend(
          account = account.email,
          msgStates = listOf(MessageState.NEW_FORWARDED.value)
        )

        if (CollectionUtils.isEmpty(detailsList)) {
          break
        }

        val msgEntity = detailsList[0]
        val msgAttsDir = File(attCacheDir, msgEntity.attachmentsDirectory!!)
        try {
          val atts = roomDatabase.attachmentDao().getAttachmentsSuspend(
            account.email,
            JavaEmailConstants.FOLDER_OUTBOX, msgEntity.uid
          ).filter { it.isForwarded }

          if (atts.isEmpty()) {
            roomDatabase.msgDao().updateSuspend(msgEntity.copy(state = MessageState.QUEUED.value))
            continue
          }

          val msgState = getNewMsgState(account, msgEntity, msgAttsDir, atts, store)

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
    account: AccountEntity, msgEntity: MessageEntity,
    msgAttsDir: File, atts: List<AttachmentEntity>, store: Store?
  ): MessageState =
    withContext(Dispatchers.IO) {
      return@withContext if (account.useAPI) {
        when (account.accountType) {
          AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
            val msg = atts.first().forwardedUid?.toHex()
              ?.let { GmailApiHelper.loadMsgFullInfoSuspend(applicationContext, account, it) }
              ?: return@withContext MessageState.ERROR_ORIGINAL_MESSAGE_MISSING

            loadAttachments(account, msgEntity, atts, msgAttsDir) { attachmentEntity ->
              GmailApiHelper.getAttPartByPath(msg.payload, neededPath = attachmentEntity.path)
                ?.let { attPart ->
                  GmailApiHelper.getAttInputStream(
                    applicationContext,
                    account,
                    attachmentEntity.uid.toHex(),
                    attPart.body.attachmentId
                  )
                }
            }
          }

          else -> throw ManualHandledException("Unsupported provider")
        }
      } else {
        store?.let {
          store.getFolder(atts.first().forwardedFolder).use { folder ->
            val imapFolder = (folder as IMAPFolder).apply { open(Folder.READ_ONLY) }
            val fwdMsg = atts.first().forwardedUid?.let { uid -> imapFolder.getMessageByUID(uid) }
              ?: return@withContext MessageState.ERROR_ORIGINAL_MESSAGE_MISSING
            loadAttachments(account, msgEntity, atts, msgAttsDir) { attachmentEntity ->
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
    account: AccountEntity, msgEntity: MessageEntity,
    atts: List<AttachmentEntity>, msgAttsDir: File,
    action: suspend (attachmentEntity: AttachmentEntity)
    -> InputStream?
  ): MessageState = withContext(Dispatchers.IO) {
    var msgState = MessageState.QUEUED
    for (attachmentEntity in atts) {
      val attInfo = attachmentEntity.toAttInfo()

      if (!attInfo.isForwarded) {
        continue
      }

      val attName = attachmentEntity.name

      val attFile = File(msgAttsDir, attName)
      val exists = attFile.exists()

      if (exists) {
        attInfo.uri =
          FileProvider.getUriForFile(applicationContext, Constants.FILE_PROVIDER_AUTHORITY, attFile)
      } else if (attInfo.uri == null) {
        FileAndDirectoryUtils.cleanDir(fwdAttsCacheDir)
        val inputStream = action.invoke(attachmentEntity)
        val tempFile = File(fwdAttsCacheDir, UUID.randomUUID().toString())
        if (inputStream != null) {
          inputStream.use { srcStream ->
            FileOutputStream(tempFile).use { destStream ->
              srcStream.copyTo(destStream)
            }
          }

          if (msgAttsDir.exists()) {
            FileUtils.moveFile(tempFile, attFile)
            attInfo.uri = FileProvider.getUriForFile(
              applicationContext,
              Constants.FILE_PROVIDER_AUTHORITY,
              attFile
            )
          } else {
            FileAndDirectoryUtils.cleanDir(fwdAttsCacheDir)
            //It means the user has already deleted the current message. We don't need to download other attachments.
            break
          }
        } else {
          msgState = MessageState.ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND
          break
        }
      }

      if (attInfo.uri != null) {
        val updateCandidate = AttachmentEntity.fromAttInfo(attInfo)?.copy(id = attachmentEntity.id)
        updateCandidate?.let {
          FlowCryptRoomDatabase.getDatabase(applicationContext).attachmentDao().updateSuspend(it)
        }
      }
    }

    return@withContext msgState
  }

  companion object {
    private val TAG = ForwardedAttachmentsDownloaderWorker::class.java.simpleName
    val NAME = ForwardedAttachmentsDownloaderWorker::class.java.simpleName

    fun enqueue(context: Context) {
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

      WorkManager
        .getInstance(context.applicationContext)
        .enqueueUniqueWork(
          NAME,
          ExistingWorkPolicy.KEEP,
          OneTimeWorkRequestBuilder<ForwardedAttachmentsDownloaderWorker>()
            .setConstraints(constraints)
            .build()
        )
    }
  }
}
