/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jobscheduler

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.AsyncTask
import android.util.Log
import androidx.core.content.FileProvider
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
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.util.FileAndDirectoryUtils
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.gms.common.util.CollectionUtils
import com.sun.mail.imap.IMAPFolder
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.InputStream
import java.lang.ref.WeakReference
import java.util.*
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Session
import javax.mail.Store

/**
 * This realization of [JobService] downloads the attachments for forwarding purposes.
 *
 * @author Denis Bondarenko
 * Date: 09.10.2018
 * Time: 11:48
 * E-mail: DenBond7@gmail.com
 */
//todo-denbond7 need to investigate this https://developer.android.com/topic/libraries/architecture/workmanager
class ForwardedAttachmentsDownloaderJobService : JobService() {
  override fun onCreate() {
    super.onCreate()
    LogsUtil.d(TAG, "onCreate")
  }

  override fun onDestroy() {
    super.onDestroy()
    LogsUtil.d(TAG, "onDestroy")
  }

  override fun onStartJob(jobParameters: JobParameters): Boolean {
    LogsUtil.d(TAG, "onStartJob")
    DownloadForwardedAttachmentsAsyncTask(this).execute(jobParameters)
    return true
  }

  override fun onStopJob(jobParameters: JobParameters): Boolean {
    LogsUtil.d(TAG, "onStopJob")
    jobFinished(jobParameters, true)
    return false
  }

  /**
   * This is an implementation of [AsyncTask] which downloads the forwarded attachments.
   */
  private class DownloadForwardedAttachmentsAsyncTask
  internal constructor(jobService: ForwardedAttachmentsDownloaderJobService)
    : AsyncTask<JobParameters, Boolean, JobParameters>() {
    private val weakRef: WeakReference<ForwardedAttachmentsDownloaderJobService> = WeakReference(jobService)

    private var sess: Session? = null
    private var store: Store? = null
    private var isFailed: Boolean = false
    private var attCacheDir: File? = null
    private var fwdAttsCacheDir: File? = null

    override fun doInBackground(vararg params: JobParameters): JobParameters {
      LogsUtil.d(TAG, "doInBackground")
      try {
        if (weakRef.get() != null) {
          val context = weakRef.get()!!.applicationContext
          val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)

          attCacheDir = File(context.cacheDir, Constants.ATTACHMENTS_CACHE_DIR)

          if (!attCacheDir!!.exists()) {
            if (!attCacheDir!!.mkdirs()) {
              throw IllegalStateException("Create cache directory " + attCacheDir!!.name + " filed!")
            }
          }

          fwdAttsCacheDir = File(attCacheDir, Constants.FORWARDED_ATTACHMENTS_CACHE_DIR)

          if (!fwdAttsCacheDir!!.exists()) {
            if (!fwdAttsCacheDir!!.mkdirs()) {
              throw IllegalStateException("Create cache directory " + fwdAttsCacheDir!!.name + " filed!")
            }
          }

          val account = roomDatabase.accountDao().getActiveAccount()

          if (account != null) {
            val newMsgs = roomDatabase.msgDao().getOutboxMsgsByState(account = account.email,
                msgStateValue = MessageState.NEW_FORWARDED.value)

            if (!CollectionUtils.isEmpty(newMsgs)) {
              sess = OpenStoreHelper.getAccountSess(context, account)
              store = OpenStoreHelper.openStore(context, account, sess!!)
              downloadForwardedAtts(context, account)
            }

            if (store?.isConnected == true) {
              store?.close()
            }
          }
        }

        publishProgress(false)
      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
        publishProgress(true)
      }

      return params[0]
    }

    override fun onPostExecute(jobParameters: JobParameters) {
      LogsUtil.d(TAG, "onPostExecute")
      try {
        if (weakRef.get() != null) {
          weakRef.get()!!.jobFinished(jobParameters, isFailed)
        }
      } catch (e: NullPointerException) {
        e.printStackTrace()
      }

    }

    override fun onProgressUpdate(vararg values: Boolean?) {
      super.onProgressUpdate(*values)
      isFailed = values[0]!!
    }

    private fun downloadForwardedAtts(context: Context, account: AccountEntity) {
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)

      while (true) {
        val detailsList = roomDatabase.msgDao().getOutboxMsgsByState(account = account.email,
            msgStateValue = MessageState.NEW_FORWARDED.value)

        if (CollectionUtils.isEmpty(detailsList)) {
          break
        }

        val msgEntity = detailsList[0]
        val msgAttsDir = File(attCacheDir, msgEntity.attachmentsDirectory!!)
        try {
          var pubKeys: List<String>? = null
          if (msgEntity.isEncrypted == true) {
            val senderEmail = EmailUtil.getFirstAddressString(msgEntity.from)
            pubKeys = SecurityUtils.getRecipientsPubKeys(context, msgEntity.allRecipients.toMutableList(),
                account, senderEmail)
          }

          val atts = roomDatabase.attachmentDao().getAttachments(account.email,
              JavaEmailConstants.FOLDER_OUTBOX, msgEntity.uid)

          if (CollectionUtils.isEmpty(atts)) {
            roomDatabase.msgDao().update(msgEntity.copy(state = MessageState.QUEUED.value))
            continue
          }

          val msgState = getNewMsgState(context, roomDatabase, msgEntity,
              msgAttsDir, pubKeys, atts)

          val updateResult = roomDatabase.msgDao().update(msgEntity.copy(state = msgState.value))
          if (updateResult > 0) {
            MessagesSenderJobService.schedule(context)
          }
        } catch (e: Exception) {
          e.printStackTrace()
          ExceptionUtil.handleError(e)

          if (!GeneralUtil.isConnected(context)) {
            publishProgress(true)
            break
          }
        }
      }
    }

    private fun getNewMsgState(context: Context, roomDatabase: FlowCryptRoomDatabase,
                               msgEntity: MessageEntity, msgAttsDir: File, pubKeys: List<String>?,
                               atts: List<AttachmentEntity>): MessageState {
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
          attInfo.uri = FileProvider.getUriForFile(context, Constants.FILE_PROVIDER_AUTHORITY, attFile)
        } else if (attInfo.uri == null) {
          FileAndDirectoryUtils.cleanDir(fwdAttsCacheDir)

          if (folder == null) {
            folder = store!!.getFolder(attInfo.fwdFolder) as IMAPFolder
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
                attInfo.uri = FileProvider.getUriForFile(context, Constants.FILE_PROVIDER_AUTHORITY, attFile)
              } else {
                FileAndDirectoryUtils.cleanDir(fwdAttsCacheDir)
                //It means the user has already deleted the current message. We don't need
                // to download other attachments.
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
          updateCandidate?.let { roomDatabase.attachmentDao().update(updateCandidate) }
        }
      }
      return msgState
    }

    private fun downloadFile(msgEntity: MessageEntity, pubKeys: List<String>?, att: AttachmentInfo,
                             tempFile: File, inputStream: InputStream) {
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
          return
        }

        if (encryptedFileResult.apiError != null) {
          ExceptionUtil.handleError(Exception(encryptedFileResult.apiError.msg))
          FileUtils.writeByteArrayToFile(tempFile, byteArrayOf())
          return
        }

        val encryptedBytes = encryptedFileResult.encryptBytes
        FileUtils.writeByteArrayToFile(tempFile, encryptedBytes!!)
      } else {
        FileUtils.copyInputStreamToFile(inputStream, tempFile)
      }
    }
  }

  companion object {
    private val TAG = ForwardedAttachmentsDownloaderJobService::class.java.simpleName

    @JvmStatic
    fun schedule(context: Context) {
      val jobInfoBuilder = JobInfo.Builder(JobIdManager.JOB_TYPE_DOWNLOAD_FORWARDED_ATTACHMENTS,
          ComponentName(context, ForwardedAttachmentsDownloaderJobService::class.java))
          .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
          .setPersisted(true)

      val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

      for (jobInfo in scheduler.allPendingJobs) {
        if (jobInfo.id == JobIdManager.JOB_TYPE_SEND_MESSAGES) {
          //skip schedule a new job if we already have another one
          LogsUtil.d(TAG, "A job has already scheduled! Skip scheduling a new job.")
          return
        }
      }

      val result = scheduler.schedule(jobInfoBuilder.build())
      if (result == JobScheduler.RESULT_SUCCESS) {
        LogsUtil.d(TAG, "A job has scheduled successfully")
      } else {
        val errorMsg = "Error. Can't schedule a job"
        Log.e(TAG, errorMsg)
        ExceptionUtil.handleError(IllegalStateException(errorMsg))
      }
    }
  }
}
