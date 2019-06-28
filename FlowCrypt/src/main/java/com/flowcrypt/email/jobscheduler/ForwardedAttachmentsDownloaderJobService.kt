/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jobscheduler

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.os.AsyncTask
import android.util.Log
import androidx.core.content.FileProvider
import com.flowcrypt.email.Constants
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.GeneralMessageDetails
import com.flowcrypt.email.api.email.protocol.ImapProtocolUtil
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.api.retrofit.node.NodeRetrofitHelper
import com.flowcrypt.email.api.retrofit.node.NodeService
import com.flowcrypt.email.api.retrofit.request.node.EncryptFileRequest
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.database.dao.source.imap.AttachmentDaoSource
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource
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

          val account = AccountDaoSource().getActiveAccountInformation(context)
          val msgDaoSource = MessageDaoSource()

          if (account != null) {
            val newMsgs = msgDaoSource.getOutboxMsgs(context, account.email,
                MessageState.NEW_FORWARDED)

            if (!CollectionUtils.isEmpty(newMsgs)) {
              sess = OpenStoreHelper.getAccountSess(context, account)
              store = OpenStoreHelper.openStore(context, account, sess!!)
              downloadForwardedAtts(context, account, msgDaoSource)
            }

            if (store != null && store!!.isConnected) {
              store!!.close()
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

    private fun downloadForwardedAtts(context: Context, account: AccountDao, daoSource: MessageDaoSource) {
      val attDaoSource = AttachmentDaoSource()

      while (true) {
        val detailsList = daoSource.getOutboxMsgs(context, account.email,
            MessageState.NEW_FORWARDED)

        if (CollectionUtils.isEmpty(detailsList)) {
          break
        }

        val details = detailsList[0]
        val detEmail = details.email
        val detLabel = details.label
        val msgAttsDir = File(attCacheDir, details.attsDir)
        try {
          var pubKeys: List<String>? = null
          if (details.isEncrypted) {
            val senderEmail = EmailUtil.getFirstAddressString(details.from)
            pubKeys = SecurityUtils.getRecipientsPubKeys(context, details.allRecipients.toMutableList(),
                account, senderEmail)
          }

          val atts = attDaoSource.getAttInfoList(context, account.email,
              JavaEmailConstants.FOLDER_OUTBOX, details.uid.toLong())

          if (CollectionUtils.isEmpty(atts)) {
            daoSource.updateMsgState(context, detEmail, detLabel, details.uid.toLong(), MessageState.QUEUED)
            continue
          }

          val msgState = getNewMsgState(context, attDaoSource, details, msgAttsDir, pubKeys, atts)

          val updateResult = daoSource.updateMsgState(context, detEmail, detLabel, details.uid.toLong(), msgState)
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

    private fun getNewMsgState(context: Context, attDaoSource: AttachmentDaoSource,
                               details: GeneralMessageDetails, msgAttsDir: File, pubKeys: List<String>?,
                               atts: List<AttachmentInfo>): MessageState {
      var folder: IMAPFolder? = null
      var fwdMsg: Message? = null

      var msgState = MessageState.QUEUED

      for (att in atts) {
        if (!att.isForwarded) {
          continue
        }

        val attFile = File(msgAttsDir, att.name)
        val exists = attFile.exists()

        if (exists) {
          att.uri = FileProvider.getUriForFile(context, Constants.FILE_PROVIDER_AUTHORITY, attFile)
        } else if (att.uri == null) {
          FileAndDirectoryUtils.cleanDir(fwdAttsCacheDir)

          if (folder == null) {
            folder = store!!.getFolder(att.fwdFolder) as IMAPFolder
            folder.open(Folder.READ_ONLY)
          }

          if (fwdMsg == null) {
            fwdMsg = folder.getMessageByUID(att.fwdUid.toLong())
          }

          if (fwdMsg == null) {
            msgState = MessageState.ERROR_ORIGINAL_MESSAGE_MISSING
            break
          }

          val msgNumber = fwdMsg.messageNumber
          val part = ImapProtocolUtil.getAttPartById(folder, msgNumber, fwdMsg, att.id!!)

          val tempFile = File(fwdAttsCacheDir, UUID.randomUUID().toString())

          if (part != null) {
            val inputStream = part.inputStream
            if (inputStream != null) {
              downloadFile(details, pubKeys, att, tempFile, inputStream)

              if (msgAttsDir.exists()) {
                FileUtils.moveFile(tempFile, attFile)
                att.uri = FileProvider.getUriForFile(context, Constants.FILE_PROVIDER_AUTHORITY, attFile)
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

        if (att.uri != null) {
          val contentValues = ContentValues()
          contentValues.put(AttachmentDaoSource.COL_FILE_URI, att.uri!!.toString())
          attDaoSource.update(context, att.email, att.folder, att.uid.toLong(), att.id!!, contentValues)
        }
      }
      return msgState
    }

    private fun downloadFile(details: GeneralMessageDetails, pubKeys: List<String>?, att: AttachmentInfo,
                             tempFile: File, inputStream: InputStream) {
      if (details.isEncrypted) {
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

        if (encryptedFileResult.error != null) {
          ExceptionUtil.handleError(Exception(encryptedFileResult.error.msg))
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
