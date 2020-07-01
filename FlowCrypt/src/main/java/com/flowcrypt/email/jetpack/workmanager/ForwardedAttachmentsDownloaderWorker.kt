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
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.protocol.ImapProtocolUtil
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.api.retrofit.node.NodeRetrofitHelper
import com.flowcrypt.email.api.retrofit.node.NodeService
import com.flowcrypt.email.api.retrofit.request.node.EncryptFileRequest
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.AttachmentEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.jetpack.viewmodel.AccountViewModel
import com.flowcrypt.email.jobscheduler.MessagesSenderJobService
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.ui.notifications.ErrorNotificationManager
import com.flowcrypt.email.util.FileAndDirectoryUtils
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.gms.common.util.CollectionUtils
import com.sun.mail.imap.IMAPFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.InputStream
import java.util.*
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Store

/**
 * This realization of [CoroutineWorker] downloads the attachments for forwarding purposes.
 *
 * @author Denis Bondarenko
 * Date: 09.10.2018
 * Time: 11:48
 * E-mail: DenBond7@gmail.com
 */
class ForwardedAttachmentsDownloaderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
  override suspend fun doWork(): Result =
      withContext(Dispatchers.IO) {
        LogsUtil.d(TAG, "doWork")
        if (isStopped) {
          return@withContext Result.success()
        }

        var store: Store? = null

        try {
          val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)

          val attCacheDir = File(applicationContext.cacheDir, Constants.ATTACHMENTS_CACHE_DIR)
          if (!attCacheDir.exists()) {
            if (!attCacheDir.mkdirs()) {
              throw IllegalStateException("Create cache directory " + attCacheDir.name + " filed!")
            }
          }

          val fwdAttsCacheDir = File(attCacheDir, Constants.FORWARDED_ATTACHMENTS_CACHE_DIR)
          if (!fwdAttsCacheDir.exists()) {
            if (!fwdAttsCacheDir.mkdirs()) {
              throw IllegalStateException("Create cache directory " + fwdAttsCacheDir.name + " filed!")
            }
          }

          val account = AccountViewModel.getAccountEntityWithDecryptedInfoSuspend(
              roomDatabase.accountDao().getActiveAccount())

          if (account != null) {
            val newMsgs = roomDatabase.msgDao().getOutboxMsgsByStatesSuspend(
                account = account.email,
                msgStates = listOf(MessageState.NEW_FORWARDED.value)
            )

            if (!CollectionUtils.isEmpty(newMsgs)) {
              val sess = OpenStoreHelper.getAccountSess(applicationContext, account)
              store = OpenStoreHelper.openStore(applicationContext, account, sess)
              downloadForwardedAtts(account, attCacheDir, fwdAttsCacheDir, store)
            }
          }

          return@withContext Result.success()
        } catch (e: Exception) {
          e.printStackTrace()
          ExceptionUtil.handleError(e)
          return@withContext Result.failure()
        } finally {
          store?.close()
        }
      }

  private suspend fun downloadForwardedAtts(account: AccountEntity, attCacheDir: File,
                                            fwdAttsCacheDir: File, store: Store) =
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
            var pubKeys: List<String>? = null
            if (msgEntity.isEncrypted == true) {
              val senderEmail = EmailUtil.getFirstAddressString(msgEntity.from)
              pubKeys = SecurityUtils.getRecipientsPubKeys(applicationContext,
                  msgEntity.allRecipients.toMutableList(), account, senderEmail)
            }

            val atts = roomDatabase.attachmentDao().getAttachmentsSuspend(account.email,
                JavaEmailConstants.FOLDER_OUTBOX, msgEntity.uid)

            if (CollectionUtils.isEmpty(atts)) {
              roomDatabase.msgDao().updateSuspend(msgEntity.copy(state = MessageState.QUEUED.value))
              continue
            }

            val msgState = getNewMsgState(msgEntity, msgAttsDir, pubKeys, atts, fwdAttsCacheDir, store)

            val updateResult = roomDatabase.msgDao().updateSuspend(msgEntity.copy(state = msgState.value))
            if (updateResult > 0) {
              if (msgState != MessageState.QUEUED) {
                val failedOutgoingMsgsCount = roomDatabase.msgDao().getFailedOutgoingMsgsCountSuspend(account.email)
                if (failedOutgoingMsgsCount > 0) {
                  ErrorNotificationManager(applicationContext).notifyUserAboutProblemWithOutgoingMsg(account, failedOutgoingMsgsCount)
                }
              }

              MessagesSenderJobService.schedule(applicationContext)
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

  private suspend fun getNewMsgState(msgEntity: MessageEntity, msgAttsDir: File, pubKeys: List<String>?,
                                     atts: List<AttachmentEntity>, fwdAttsCacheDir: File, store: Store): MessageState =
      withContext(Dispatchers.IO) {
        var folder: IMAPFolder? = null
        var fwdMsg: Message? = null

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
            attInfo.uri = FileProvider.getUriForFile(applicationContext, Constants.FILE_PROVIDER_AUTHORITY, attFile)
          } else if (attInfo.uri == null) {
            FileAndDirectoryUtils.cleanDir(fwdAttsCacheDir)

            if (folder == null) {
              folder = store.getFolder(attInfo.fwdFolder) as IMAPFolder
              folder.open(Folder.READ_ONLY)
            }

            if (fwdMsg == null) {
              fwdMsg = folder.getMessageByUID(attInfo.fwdUid.toLong())
            }

            if (fwdMsg == null) {
              msgState = MessageState.ERROR_ORIGINAL_MESSAGE_MISSING
              break
            }

            val part = ImapProtocolUtil.getAttPartByPath(fwdMsg, neededPath = attachmentEntity.path)
            val tempFile = File(fwdAttsCacheDir, UUID.randomUUID().toString())

            if (part != null) {
              val inputStream = part.inputStream
              if (inputStream != null) {
                downloadFile(msgEntity, pubKeys, attInfo, tempFile, inputStream)

                if (msgAttsDir.exists()) {
                  FileUtils.moveFile(tempFile, attFile)
                  attInfo.uri = FileProvider.getUriForFile(applicationContext, Constants.FILE_PROVIDER_AUTHORITY, attFile)
                } else {
                  FileAndDirectoryUtils.cleanDir(fwdAttsCacheDir)
                  //It means the user has already deleted the current message. We don't need to download other attachments.
                  break
                }
              } else {
                msgState = MessageState.ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND
                break
              }
            } else {
              msgState = MessageState.ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND
              break
            }
          }

          if (attInfo.uri != null) {
            val updateCandidate = AttachmentEntity.fromAttInfo(attInfo)?.copy(id = attachmentEntity.id)
            updateCandidate?.let { FlowCryptRoomDatabase.getDatabase(applicationContext).attachmentDao().updateSuspend(it) }
          }
        }
        return@withContext msgState
      }

  private suspend fun downloadFile(msgEntity: MessageEntity, pubKeys: List<String>?, att: AttachmentInfo,
                                   tempFile: File, inputStream: InputStream) =
      withContext(Dispatchers.IO) {
        if (msgEntity.isEncrypted == true) {
          val originalBytes = IOUtils.toByteArray(inputStream)
          val fileName = FilenameUtils.removeExtension(att.name)
          val nodeService = NodeRetrofitHelper.getRetrofit()!!.create(NodeService::class.java)
          val request = EncryptFileRequest(originalBytes, fileName, pubKeys!!)

          val response = nodeService.encryptFile(request).execute()
          val encryptedFileResult = response.body()

          if (encryptedFileResult == null) {
            ExceptionUtil.handleError(NullPointerException("encryptedFileResult == null"))
            FileUtils.writeByteArrayToFile(tempFile, byteArrayOf())
            return@withContext
          }

          if (encryptedFileResult.apiError != null) {
            ExceptionUtil.handleError(Exception(encryptedFileResult.apiError.msg))
            FileUtils.writeByteArrayToFile(tempFile, byteArrayOf())
            return@withContext
          }

          val encryptedBytes = encryptedFileResult.encryptBytes
          FileUtils.writeByteArrayToFile(tempFile, encryptedBytes!!)
        } else {
          FileUtils.copyInputStreamToFile(inputStream, tempFile)
        }
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
              ExistingWorkPolicy.REPLACE,
              OneTimeWorkRequestBuilder<ForwardedAttachmentsDownloaderWorker>()
                  .setConstraints(constraints)
                  .build()
          )
    }
  }
}