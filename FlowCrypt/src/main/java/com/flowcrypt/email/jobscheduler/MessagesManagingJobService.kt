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
import android.os.AsyncTask
import android.util.Log
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.FolderNotAvailableException
import com.sun.mail.imap.IMAPFolder
import java.lang.ref.WeakReference
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Session
import javax.mail.Store

/**
 * This [JobService] manages messages (archiving, deleting, total deleting and etc).
 *
 * @author Denis Bondarenko
 *         Date: 10/9/19
 *         Time: 7:05 PM
 *         E-mail: DenBond7@gmail.com
 */
class MessagesManagingJobService : JobService() {

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
    MoveMessagesAsyncTask(this).execute(jobParameters)
    return true
  }

  override fun onStopJob(jobParameters: JobParameters): Boolean {
    LogsUtil.d(TAG, "onStopJob")
    jobFinished(jobParameters, true)
    return false
  }

  /**
   * This is an implementation of [AsyncTask] which sends the outgoing messages.
   */
  private class MoveMessagesAsyncTask internal constructor(jobService: MessagesManagingJobService)
    : AsyncTask<JobParameters, Boolean, JobParameters>() {
    private val weakRef: WeakReference<MessagesManagingJobService> = WeakReference(jobService)

    private var sess: Session? = null
    private var store: Store? = null
    private var isFailed: Boolean = false

    override fun doInBackground(vararg params: JobParameters): JobParameters {
      LogsUtil.d(TAG, "doInBackground")
      try {
        if (weakRef.get() != null) {
          val context = weakRef.get()?.applicationContext
          val account = AccountDaoSource().getActiveAccountInformation(context)
          val msgDaoSource = MessageDaoSource()

          if (context != null && account != null) {
            val candidatesForArchiving = msgDaoSource.getMsgsWithState(context, account.email,
                MessageState.PENDING_ARCHIVING)

            val candidatesForMarkUnread = msgDaoSource.getMsgsWithState(context, account.email,
                MessageState.PENDING_MARK_UNREAD)

            val candidatesForMarkRead = msgDaoSource.getMsgsWithState(context, account.email,
                MessageState.PENDING_MARK_READ)

            val candidatesForDeleting = msgDaoSource.getMsgsWithState(context, account.email,
                MessageState.PENDING_DELETING)

            val candidatesForMovingToInbox = msgDaoSource.getMsgsWithState(context, account.email,
                MessageState.PENDING_MOVE_TO_INBOX)

            if (candidatesForArchiving.isNotEmpty()
                || candidatesForMarkUnread.isNotEmpty()
                || candidatesForMarkRead.isNotEmpty()
                || candidatesForDeleting.isNotEmpty()
                || candidatesForMovingToInbox.isNotEmpty()) {
              sess = OpenStoreHelper.getAccountSess(context, account)
              store = OpenStoreHelper.openStore(context, account, sess!!)
            }

            if (candidatesForArchiving.isNotEmpty()) {
              archiveMsgs(context, account, msgDaoSource, store!!)
            }

            if (candidatesForMarkUnread.isNotEmpty()) {
              changeMsgsReadState(context, account, msgDaoSource, store!!, MessageState.PENDING_MARK_UNREAD)
            }

            if (candidatesForMarkRead.isNotEmpty()) {
              changeMsgsReadState(context, account, msgDaoSource, store!!, MessageState.PENDING_MARK_READ)
            }

            if (candidatesForDeleting.isNotEmpty()) {
              deleteMsgs(context, account, msgDaoSource, store!!)
            }

            if (candidatesForMovingToInbox.isNotEmpty()) {
              moveMsgsToInbox(context, account, msgDaoSource, store!!)
            }

            store?.close()
          }
        }

        publishProgress(false)
      } catch (e: Exception) {
        e.printStackTrace()
        publishProgress(true)
      }

      return params[0]
    }

    override fun onPostExecute(jobParameters: JobParameters) {
      LogsUtil.d(TAG, "onPostExecute")
      try {
        weakRef.get()?.jobFinished(jobParameters, isFailed)
      } catch (e: NullPointerException) {
        e.printStackTrace()
      }

    }

    override fun onProgressUpdate(vararg values: Boolean?) {
      super.onProgressUpdate(*values)
      isFailed = values[0] ?: true
    }

    fun archiveMsgs(context: Context, account: AccountDao, msgDaoSource: MessageDaoSource, store: Store) {
      val foldersManager = FoldersManager.fromDatabase(context, account.email)
      val inboxFolder = foldersManager.findInboxFolder() ?: return
      val allMsgsFolder = foldersManager.folderAll ?: return

      val srcFolder = store.getFolder(inboxFolder.fullName) as IMAPFolder
      val destFolder = store.getFolder(allMsgsFolder.fullName) as IMAPFolder

      if (!srcFolder.exists()) {
        throw FolderNotAvailableException("The invalid source folder: \"${srcFolder}\"")
      }

      srcFolder.open(Folder.READ_WRITE)

      if (!destFolder.exists()) {
        throw FolderNotAvailableException("The invalid destination folder: \"$destFolder\"")
      }

      destFolder.open(Folder.READ_WRITE)

      while (true) {
        val candidatesForArchiving = msgDaoSource.getMsgsWithState(context, account.email,
            MessageState.PENDING_ARCHIVING)

        if (candidatesForArchiving.isEmpty()) {
          break
        } else {
          val uidList = candidatesForArchiving.map { it.uid.toLong() }
          val msgs: List<Message> = srcFolder.getMessagesByUID(uidList.toLongArray()).filterNotNull()

          if (msgs.isNotEmpty()) {
            srcFolder.moveMessages(msgs.toTypedArray(), destFolder)
            msgDaoSource.deleteMsgsByUID(context, account.email, inboxFolder.fullName, uidList)
          }
        }
      }

      destFolder.close(false)
      srcFolder.close(false)
    }

    fun deleteMsgs(context: Context, account: AccountDao, msgDaoSource: MessageDaoSource,
                   store: Store) {
      val foldersManager = FoldersManager.fromDatabase(context, account.email)
      val trash = foldersManager.folderTrash ?: return

      while (true) {
        val candidatesForDeleting = msgDaoSource.getMsgsWithState(context, account.email,
            MessageState.PENDING_DELETING)

        if (candidatesForDeleting.isEmpty()) {
          break
        } else {
          val setOfFolders = candidatesForDeleting.map { it.label }.toSet()

          for (folder in setOfFolders) {
            val filteredMsgs = candidatesForDeleting.filter { it.label == folder }

            if (filteredMsgs.isEmpty() || JavaEmailConstants.FOLDER_OUTBOX.equals(folder, ignoreCase = true)) {
              continue
            }

            val uidList = filteredMsgs.map { it.uid.toLong() }
            moveMsg(folder, trash.fullName, store, uidList.toLongArray())
            msgDaoSource.deleteMsgsByUID(context, account.email, folder, uidList)
          }
        }
      }
    }

    fun moveMsgsToInbox(context: Context, account: AccountDao, msgDaoSource: MessageDaoSource,
                        store: Store) {
      val foldersManager = FoldersManager.fromDatabase(context, account.email)
      val inbox = foldersManager.findInboxFolder() ?: return

      while (true) {
        val candidatesForMovingToInbox = msgDaoSource.getMsgsWithState(context, account.email,
            MessageState.PENDING_MOVE_TO_INBOX)

        if (candidatesForMovingToInbox.isEmpty()) {
          break
        } else {
          val setOfFolders = candidatesForMovingToInbox.map { it.label }.toSet()

          for (folder in setOfFolders) {
            val filteredMsgs = candidatesForMovingToInbox.filter { it.label == folder }

            if (filteredMsgs.isEmpty() || JavaEmailConstants.FOLDER_OUTBOX.equals(folder, ignoreCase = true)) {
              continue
            }

            val uidList = filteredMsgs.map { it.uid.toLong() }
            moveMsg(folder, inbox.fullName, store, uidList.toLongArray())
            msgDaoSource.deleteMsgsByUID(context, account.email, folder, uidList)
          }
        }
      }
    }

    private fun changeMsgsReadState(context: Context, account: AccountDao, msgDaoSource:
    MessageDaoSource, store: Store, state: MessageState) {
      while (true) {
        val candidatesForMark = msgDaoSource.getMsgsWithState(context, account.email, state)

        if (candidatesForMark.isEmpty()) {
          break
        } else {
          val setOfFolders = candidatesForMark.map { it.label }.toSet()

          for (folder in setOfFolders) {
            val filteredMsgs = candidatesForMark.filter { it.label == folder }

            if (filteredMsgs.isEmpty()) {
              continue
            }

            val imapFolder = store.getFolder(folder) as IMAPFolder
            imapFolder.open(Folder.READ_WRITE)

            if (!imapFolder.exists()) {
              continue
            }

            val uidList = filteredMsgs.map { it.uid.toLong() }
            val msgs: List<Message> = imapFolder.getMessagesByUID(uidList.toLongArray()).filterNotNull()

            if (msgs.isNotEmpty()) {
              val value = state == MessageState.PENDING_MARK_READ
              imapFolder.setFlags(msgs.toTypedArray(), Flags(Flags.Flag.SEEN), value)
              for (uid in uidList) {
                val msg = msgDaoSource.getMsg(context, account.email, folder, uid)

                if (msg?.msgState == state) {
                  msgDaoSource.updateMsgState(context, account.email, folder, uid, MessageState.NONE)
                }
              }
            }

            imapFolder.close()
          }
        }
      }
    }

    private fun moveMsg(srcFolderFullName: String, destFolderFullName: String, store: Store,
                        uids: LongArray) {
      val remoteSrcFolder = store.getFolder(srcFolderFullName) as IMAPFolder
      val remoteDestFolder = store.getFolder(destFolderFullName) as IMAPFolder

      if (!remoteSrcFolder.exists()) {
        return
      }

      remoteSrcFolder.open(Folder.READ_WRITE)

      val msgs: List<Message> = remoteSrcFolder.getMessagesByUID(uids).filterNotNull()

      if (msgs.isNotEmpty()) {
        if (!remoteDestFolder.exists()) {
          remoteSrcFolder.close(false)
          return
        }

        remoteDestFolder.open(Folder.READ_WRITE)
        remoteSrcFolder.moveMessages(msgs.toTypedArray(), remoteDestFolder)
        remoteDestFolder.close(false)
      }

      remoteSrcFolder.close(false)
    }
  }

  companion object {

    private val TAG = MessagesManagingJobService::class.java.simpleName

    @JvmStatic
    fun schedule(context: Context?) {
      context ?: return

      val jobInfoBuilder = JobInfo.Builder(JobIdManager.JOB_TYPE_MOVE_MESSAGES,
          ComponentName(context, MessagesManagingJobService::class.java))
          .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
          .setPersisted(true)

      val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

      for (jobInfo in scheduler.allPendingJobs) {
        if (jobInfo.id == JobIdManager.JOB_TYPE_MOVE_MESSAGES) {
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
