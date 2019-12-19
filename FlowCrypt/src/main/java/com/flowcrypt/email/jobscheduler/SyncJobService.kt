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
import android.content.Context
import android.content.OperationApplicationException
import android.os.AsyncTask
import android.os.Build
import android.os.RemoteException
import android.util.Log
import android.util.LongSparseArray
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.api.email.sync.tasks.SyncFolderSyncTask
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.service.MessagesNotificationManager
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.sun.mail.imap.IMAPFolder
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.TimeUnit
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.Store

/**
 * This is an implementation of [JobService]. Here we are going to do syncing INBOX folder of an active account.
 *
 * @author Denis Bondarenko
 * Date: 20.06.2018
 * Time: 12:40
 * E-mail: DenBond7@gmail.com
 */
class SyncJobService : JobService(), SyncListener {
  private var messagesNotificationManager: MessagesNotificationManager? = null

  override val context: Context
    get() = applicationContext

  override fun onCreate() {
    super.onCreate()
    LogsUtil.d(TAG, "onCreate")
    this.messagesNotificationManager = MessagesNotificationManager(this)
  }

  override fun onDestroy() {
    super.onDestroy()
    LogsUtil.d(TAG, "onDestroy")
  }

  override fun onStartJob(jobParameters: JobParameters): Boolean {
    LogsUtil.d(TAG, "onStartJob")
    CheckNewMessagesJobTask(this).execute(jobParameters)
    return true
  }

  override fun onStopJob(jobParameters: JobParameters): Boolean {
    LogsUtil.d(TAG, "onStopJob")
    jobFinished(jobParameters, true)
    return false
  }

  override fun onMsgWithBackupToKeyOwnerSent(account: AccountDao, ownerKey: String, requestCode: Int, isSent: Boolean) {

  }

  override fun onPrivateKeysFound(account: AccountDao, keys: List<NodeKeyDetails>, ownerKey: String, requestCode: Int) {

  }

  override fun onMsgSent(account: AccountDao, ownerKey: String, requestCode: Int, isSent: Boolean) {

  }

  override fun onMsgsMoved(account: AccountDao, srcFolder: IMAPFolder, destFolder: IMAPFolder,
                           msgs: List<Message>, ownerKey: String, requestCode: Int) {

  }

  override fun onMsgMoved(account: AccountDao, srcFolder: IMAPFolder, destFolder: IMAPFolder,
                          msg: Message?, ownerKey: String, requestCode: Int) {

  }

  override fun onMsgDetailsReceived(account: AccountDao, localFolder: LocalFolder, remoteFolder: IMAPFolder,
                                    uid: Long, id: Long, msg: Message?, ownerKey: String, requestCode: Int) {

  }

  override fun onMsgsReceived(account: AccountDao, localFolder: LocalFolder, remoteFolder: IMAPFolder,
                              msgs: Array<Message>, ownerKey: String, requestCode: Int) {

  }

  override fun onSearchMsgsReceived(account: AccountDao, localFolder: LocalFolder, remoteFolder: IMAPFolder,
                                    msgs: Array<Message>, ownerKey: String, requestCode: Int) {

  }

  override fun onRefreshMsgsReceived(account: AccountDao, localFolder: LocalFolder,
                                     remoteFolder: IMAPFolder, newMsgs: Array<Message>,
                                     updateMsgs: Array<Message>, ownerKey: String, requestCode: Int) {
    try {
      val msgDaoSource = MessageDaoSource()
      val folderName = localFolder.fullName
      val mapOfUIDsAndMsgsFlags = msgDaoSource.getMapOfUIDAndMsgFlags(applicationContext, account.email, folderName)
      val uidSet = HashSet(mapOfUIDsAndMsgsFlags.keys)
      val deleteCandidatesUIDs = EmailUtil.genDeleteCandidates(uidSet, remoteFolder, updateMsgs)

      val generalMsgDetailsBeforeUpdate = msgDaoSource.getNewMsgs(applicationContext, account.email, folderName)

      msgDaoSource.deleteMsgsByUID(applicationContext, account.email, folderName, deleteCandidatesUIDs)

      msgDaoSource.updateMsgsByUID(applicationContext, account.email, folderName, remoteFolder,
          EmailUtil.genUpdateCandidates(mapOfUIDsAndMsgsFlags, remoteFolder, updateMsgs))

      val detailsAfterUpdate = msgDaoSource.getNewMsgs(applicationContext,
          account.email, folderName)

      val detailsDeleteCandidates = LinkedList(generalMsgDetailsBeforeUpdate)
      detailsDeleteCandidates.removeAll(detailsAfterUpdate)

      val isInbox = FoldersManager.getFolderType(localFolder) === FoldersManager.FolderType.INBOX
      if (!GeneralUtil.isAppForegrounded() && isInbox) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          for ((_, _, uid) in detailsDeleteCandidates) {
            messagesNotificationManager!!.cancel(uid)
          }
        } else {
          if (!detailsDeleteCandidates.isEmpty()) {
            val unseenMsgs = msgDaoSource.getUIDOfUnseenMsgs(this, account.email, folderName)
            messagesNotificationManager!!.notify(this, account, localFolder, detailsAfterUpdate, unseenMsgs, true)
          }
        }
      }
    } catch (e: RemoteException) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    } catch (e: MessagingException) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    } catch (e: OperationApplicationException) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }

  }

  override fun onFoldersInfoReceived(account: AccountDao, folders: Array<javax.mail.Folder>, ownerKey: String,
                                     requestCode: Int) {

  }

  override fun onError(account: AccountDao, errorType: Int, e: Exception, ownerKey: String, requestCode: Int) {

  }

  override fun onActionProgress(account: AccountDao, ownerKey: String, requestCode: Int,
                                resultCode: Int, value: Int) {

  }

  override fun onMsgChanged(account: AccountDao, localFolder: LocalFolder, remoteFolder: IMAPFolder, msg: Message,
                            ownerKey: String, requestCode: Int) {

  }

  override fun onIdentificationToEncryptionCompleted(account: AccountDao, localFolder: LocalFolder,
                                                     remoteFolder: IMAPFolder, ownerKey: String, requestCode: Int) {

  }

  override fun onNewMsgsReceived(account: AccountDao, localFolder: LocalFolder, remoteFolder: IMAPFolder,
                                 newMsgs: Array<Message>, msgsEncryptionStates: LongSparseArray<Boolean>,
                                 ownerKey: String, requestCode: Int) {
    try {
      val context = applicationContext
      val isEncryptedModeEnabled = AccountDaoSource().isEncryptedModeEnabled(context, account.email)

      val msgDaoSource = MessageDaoSource()
      val folderName = localFolder.fullName
      val mapOfUIDAndMsgFlags = msgDaoSource.getMapOfUIDAndMsgFlags(context, account.email, folderName)

      val uids = HashSet(mapOfUIDAndMsgFlags.keys)

      val newCandidates = EmailUtil.genNewCandidates(uids, remoteFolder, newMsgs)

      val msgEntities = MessageEntity.genMessageEntities(
          context = context,
          email = account.email,
          label = folderName,
          folder = remoteFolder,
          msgs = newCandidates,
          msgsEncryptionStates = msgsEncryptionStates,
          isNew = !GeneralUtil.isAppForegrounded(),
          areAllMsgsEncrypted = isEncryptedModeEnabled
      )

      FlowCryptRoomDatabase.getDatabase(context).msgDao().insert(msgEntities)

      if (!GeneralUtil.isAppForegrounded()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && newCandidates.isEmpty()) {
          return
        }

        val newMsgsList = msgDaoSource.getNewMsgs(applicationContext,
            account.email, folderName)
        val unseenUIDs = msgDaoSource.getUIDOfUnseenMsgs(this, account.email, folderName)

        messagesNotificationManager?.notify(this, account, localFolder, newMsgsList, unseenUIDs,
            false)
      }
    } catch (e: MessagingException) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }

  }

  override fun onAttsInfoReceived(account: AccountDao, localFolder: LocalFolder, remoteFolder: IMAPFolder, uid: Long,
                                  ownerKey: String, requestCode: Int) {
  }

  /**
   * This is a worker. Here we will do sync in the background thread. If the sync will be failed we'll schedule it
   * again.
   */
  private class CheckNewMessagesJobTask internal constructor(syncJobService: SyncJobService)
    : AsyncTask<JobParameters, Boolean, JobParameters>() {
    private val weakRef: WeakReference<SyncJobService> = WeakReference(syncJobService)

    private var sess: Session? = null
    private var store: Store? = null
    private var isFailed: Boolean = false

    override fun doInBackground(vararg params: JobParameters): JobParameters {
      LogsUtil.d(TAG, "doInBackground")

      try {
        if (weakRef.get() != null) {
          val context = weakRef.get()!!.applicationContext
          val account = AccountDaoSource().getActiveAccountInformation(context)

          if (account != null) {
            val foldersManager = FoldersManager.fromDatabase(context, account.email)
            val localFolder = foldersManager.findInboxFolder()

            if (localFolder != null) {
              sess = OpenStoreHelper.getAccountSess(context, account)
              store = OpenStoreHelper.openStore(context, account, sess!!)

              SyncFolderSyncTask("", 0, localFolder).runIMAPAction(account, sess!!, store!!, weakRef.get()!!)

              if (store != null) {
                store!!.close()
              }
            }
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
        publishProgress(true)
      }

      publishProgress(false)
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
  }

  companion object {
    private val INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(15)
    private val TAG = SyncJobService::class.java.simpleName

    @JvmStatic
    fun schedule(context: Context) {
      val serviceName = ComponentName(context, SyncJobService::class.java)
      val jobInfoBuilder = JobInfo.Builder(JobIdManager.JOB_TYPE_SYNC, serviceName)
          .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
          .setPeriodic(INTERVAL_MILLIS)
          .setPersisted(true)

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        jobInfoBuilder.setRequiresBatteryNotLow(true)
      }

      val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
      val result = scheduler.schedule(jobInfoBuilder.build())
      if (result == JobScheduler.RESULT_SUCCESS) {
        LogsUtil.d(TAG, "A job scheduled successfully")
      } else {
        val errorMsg = "Error. Can't schedule a job"
        Log.e(TAG, errorMsg)
        ExceptionUtil.handleError(IllegalStateException(errorMsg))
      }
    }
  }
}
