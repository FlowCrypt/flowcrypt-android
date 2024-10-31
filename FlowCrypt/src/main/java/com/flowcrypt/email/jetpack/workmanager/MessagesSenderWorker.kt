/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.workmanager

import android.accounts.AuthenticatorException
import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.api.email.protocol.SmtpProtocolUtil
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.AttachmentEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.jetpack.workmanager.base.BaseMsgWorker
import com.flowcrypt.email.ui.notifications.NotificationChannelManager
import com.flowcrypt.email.util.FileAndDirectoryUtils
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.OutgoingMessagesManager
import com.flowcrypt.email.util.exception.CopyNotSavedInSentFolderException
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.ForceHandlingException
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.common.util.CollectionUtils
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.FileContent
import jakarta.mail.AuthenticationFailedException
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.MessagingException
import jakarta.mail.Session
import jakarta.mail.Store
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.eclipse.angus.mail.imap.IMAPFolder
import org.eclipse.angus.mail.util.MailConnectException
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.net.SocketException
import javax.net.ssl.SSLException

/**
 * @author Denys Bondarenko
 *
 */
class MessagesSenderWorker(context: Context, params: WorkerParameters) :
  BaseMsgWorker(context, params) {

  override suspend fun doWork(): Result =
    withContext(Dispatchers.IO) {
      LogsUtil.d(TAG, "doWork")
      if (isStopped) {
        return@withContext Result.success()
      }

      OutgoingMessagesManager.checkAndCleanCache(applicationContext)

      try {
        val account = roomDatabase.accountDao().getActiveAccountSuspend()?.withDecryptedInfo()
          ?: return@withContext Result.success()

        val attsCacheDir = File(applicationContext.cacheDir, Constants.ATTACHMENTS_CACHE_DIR)

        roomDatabase.msgDao().resetMsgsWithSendingStateSuspend(account.email)

        val queuedMsgs = roomDatabase.msgDao().getOutboxMsgsByStatesSuspend(
          account = account.email,
          msgStates = listOf(MessageState.QUEUED.value)
        )

        val sentButNotSavedMsgs = roomDatabase.msgDao().getOutboxMsgsByStatesSuspend(
          account = account.email,
          msgStates = listOf(
            MessageState.SENT_WITHOUT_LOCAL_COPY.value,
            MessageState.QUEUED_MAKE_COPY_IN_SENT_FOLDER.value
          )
        )

        if (queuedMsgs.isNotEmpty() || sentButNotSavedMsgs.isNotEmpty()) {
          try {
            setForeground(genForegroundInfoInternal(account.email, false))
          } catch (e: IllegalStateException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
              if (e is ForegroundServiceStartNotAllowedException) {
                //see for details https://developer.android.com/topic/libraries/architecture/workmanager/how-to/define-work#coroutineworker
                LogsUtil.d(
                  TAG, "It seems the app started this worker while running in the background." +
                      " We can't show a notification in that case."
                )
              }
            }
          }

          if (account.useAPI) {
            when (account.accountType) {
              AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
                if (!CollectionUtils.isEmpty(queuedMsgs)) {
                  sendQueuedMsgs(account, attsCacheDir)
                }
              }

              else -> throw IllegalArgumentException("Unsupported provider")
            }
          } else {
            val session = OpenStoreHelper.getAccountSess(applicationContext, account)
            OpenStoreHelper.openStore(applicationContext, account, session).use { store ->
              if (!CollectionUtils.isEmpty(queuedMsgs)) {
                sendQueuedMsgs(account, attsCacheDir, session, store)
              }

              if (!CollectionUtils.isEmpty(sentButNotSavedMsgs)) {
                saveCopyOfAlreadySentMsgs(account, session, store, attsCacheDir)
              }
            }
          }
        }

        return@withContext rescheduleIfActiveAccountWasChanged(account)
      } catch (e: Exception) {
        e.printStackTrace()
        when (e) {
          is UserRecoverableAuthException, is UserRecoverableAuthIOException, is AuthenticatorException, is AuthenticationFailedException -> {
            markMsgsWithAuthFailureState(roomDatabase, MessageState.QUEUED)
          }

          else -> {
            val account = roomDatabase.accountDao().getActiveAccountSuspend()
            account?.email?.let {
              roomDatabase.msgDao().resetMsgsWithSendingStateSuspend(account.email)
            }

            ExceptionUtil.handleError(ForceHandlingException(e))
          }
        }

        return@withContext Result.failure()
      } finally {
        val account = roomDatabase.accountDao().getActiveAccountSuspend()
        account?.email?.let {
          roomDatabase.msgDao().resetMsgsWithSendingStateSuspend(account.email)
        }
        LogsUtil.d(TAG, "work was finished")
      }
    }

  override suspend fun getForegroundInfo(): ForegroundInfo {
    /*we should add implementation of this method if we use
    setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).
    Android 11 and less uses it to show a notification*/
    return genForegroundInfoInternal()
  }

  private fun genForegroundInfoInternal(
    email: String? = null,
    isDefault: Boolean = true
  ): ForegroundInfo {
    val notification = NotificationCompat.Builder(
      applicationContext,
      NotificationChannelManager.CHANNEL_ID_SYNC
    )
      .setProgress(0, 0, true)
      .setOngoing(true)
      .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
      .apply {
        val title = applicationContext.getString(
          if (isDefault) R.string.synchronization else R.string.sending_email
        )
        setContentTitle(title)
        if (isDefault) {
          setSmallIcon(R.drawable.ic_synchronization_grey_24dp)
        } else {
          setTicker(title)
          setSmallIcon(R.drawable.ic_sending_email_grey_24dp)
          setSubText(email)
        }
      }.build()

    return ForegroundInfo(
      NOTIFICATION_ID,
      notification,
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        //https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running#specify-foreground-service-types-runtime
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
      } else {
        0
      }
    )
  }

  private suspend fun sendQueuedMsgs(
    account: AccountEntity, attsCacheDir: File, sess: Session? =
      null, store: Store? = null
  ) =
    withContext(Dispatchers.IO) {
      var list: List<MessageEntity>
      var lastMsgUID = 0L
      val email = account.email
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)
      while (true) {
        list = roomDatabase.msgDao().getOutboxMsgsByStatesSuspend(
          account = account.email,
          msgStates = listOf(MessageState.QUEUED.value)
        )

        if (list.isEmpty()) {
          break
        }

        var msgEntity = (list.firstOrNull { it.uid > lastMsgUID } ?: list.first()).apply {
          lastMsgUID = uid
        }

        try {
          roomDatabase.msgDao().resetMsgsWithSendingStateSuspend(account.email)
          roomDatabase.msgDao().updateSuspend(msgEntity.copy(state = MessageState.SENDING.value))
          delay(2000)

          val attachments = roomDatabase.attachmentDao()
            .getAttachments(
              account = email,
              accountType = account.accountType,
              label = JavaEmailConstants.FOLDER_OUTBOX,
              uid = msgEntity.uid
            )
          val isMsgSent = sendMsg(account, msgEntity, attachments, sess, store)

          if (!isMsgSent) {
            continue
          }

          val existingMessageEntity = roomDatabase.msgDao()
            .getMsgSuspend(email, JavaEmailConstants.FOLDER_OUTBOX, msgEntity.uid)
            ?: return@withContext
          msgEntity = existingMessageEntity

          if (msgEntity.msgState == MessageState.SENT) {
            roomDatabase.msgDao().deleteSuspend(msgEntity)
            OutgoingMessagesManager.deleteOutgoingMessage(
              applicationContext,
              requireNotNull(msgEntity.id)
            )

            if (!CollectionUtils.isEmpty(attachments)) {
              deleteMsgAtts(account, attsCacheDir, msgEntity)
            }

            val outgoingMsgCount = roomDatabase.msgDao().getOutboxMsgsSuspend(email).size
            val outboxLabel = roomDatabase.labelDao()
              .getLabelSuspend(email, account.accountType, JavaEmailConstants.FOLDER_OUTBOX)

            outboxLabel?.let {
              roomDatabase.labelDao().updateSuspend(it.copy(messagesTotal = outgoingMsgCount))
            }
          }
        } catch (e: Exception) {
          e.printStackTrace()
          ExceptionUtil.handleError(e)

          if (!GeneralUtil.isConnected(applicationContext)) {
            if (msgEntity.msgState != MessageState.SENT) {
              msgEntity.copy(state = MessageState.QUEUED.value).let {
                roomDatabase.msgDao()
                  .updateSuspend(it)
              }
            }
            throw e
          } else {
            val newMsgState = when (e) {
              is MailConnectException -> {
                MessageState.QUEUED
              }

              is MessagingException -> {
                if (e.cause is SSLException || e.cause is SocketException) {
                  MessageState.QUEUED
                } else {
                  MessageState.ERROR_SENDING_FAILED
                }
              }

              is CopyNotSavedInSentFolderException -> MessageState.ERROR_COPY_NOT_SAVED_IN_SENT_FOLDER

              else -> {
                when (e.cause) {
                  is FileNotFoundException -> MessageState.ERROR_CACHE_PROBLEM

                  else -> MessageState.ERROR_SENDING_FAILED
                }
              }
            }

            msgEntity.copy(state = newMsgState.value, errorMsg = e.message).let {
              roomDatabase.msgDao()
                .updateSuspend(it)
            }
          }

          delay(5000)
        }
      }
    }

  private suspend fun saveCopyOfAlreadySentMsgs(
    account: AccountEntity,
    sess: Session,
    store: Store,
    attsCacheDir: File
  ) =
    withContext(Dispatchers.IO) {
      var list: List<MessageEntity>
      val email = account.email
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)
      while (true) {
        list = roomDatabase.msgDao().getOutboxMsgsByStatesSuspend(
          account = account.email,
          msgStates = listOf(
            MessageState.SENT_WITHOUT_LOCAL_COPY.value,
            MessageState.QUEUED_MAKE_COPY_IN_SENT_FOLDER.value
          )
        )
        if (CollectionUtils.isEmpty(list)) {
          break
        }
        val msgEntity = list.first()
        try {
          val attachments = roomDatabase.attachmentDao().getAttachments(
            account = email,
            accountType = account.accountType,
            label = JavaEmailConstants.FOLDER_OUTBOX,
            uid = msgEntity.uid
          )

          val mimeMsg = EmailUtil.createMimeMsg(applicationContext, sess, msgEntity, attachments)

          roomDatabase.msgDao().resetMsgsWithSendingStateSuspend(account.email)
          roomDatabase.msgDao().updateSuspend(msgEntity.copy(state = MessageState.SENDING.value))
          delay(2000)

          val isMsgSaved = saveCopyOfSentMsg(account, store, mimeMsg)

          if (!isMsgSaved) {
            continue
          }

          roomDatabase.msgDao().deleteSuspend(msgEntity)

          if (attachments.isNotEmpty()) {
            deleteMsgAtts(account, attsCacheDir, msgEntity)
          }
        } catch (e: Exception) {
          e.printStackTrace()
          ExceptionUtil.handleError(e)

          if (!GeneralUtil.isConnected(applicationContext)) {
            roomDatabase.msgDao().updateSuspend(
              msgEntity.copy(
                state = MessageState.SENT_WITHOUT_LOCAL_COPY.value
              )
            )
            throw e
          }

          when (e) {
            is CopyNotSavedInSentFolderException -> {
              roomDatabase.msgDao().updateSuspend(
                msgEntity.copy(
                  state = MessageState.ERROR_COPY_NOT_SAVED_IN_SENT_FOLDER.value,
                  errorMsg = e.message
                )
              )
            }

            else -> {
              when (e.cause) {
                is FileNotFoundException -> {
                  roomDatabase.msgDao().deleteSuspend(msgEntity)
                }

                else -> {
                  roomDatabase.msgDao().updateSuspend(
                    msgEntity.copy(
                      state = MessageState.ERROR_COPY_NOT_SAVED_IN_SENT_FOLDER.value,
                      errorMsg = e.message
                    )
                  )
                }
              }
            }
          }
        }
      }
    }

  private suspend fun deleteMsgAtts(
    account: AccountEntity,
    attsCacheDir: File,
    details: MessageEntity
  ) =
    withContext(Dispatchers.IO) {
      roomDatabase.attachmentDao().deleteAttachments(
        account = account.email,
        accountType = account.accountType,
        label = JavaEmailConstants.FOLDER_OUTBOX,
        uid = details.uid
      )
      details.attachmentsDirectory?.let { FileAndDirectoryUtils.deleteDir(File(attsCacheDir, it)) }
    }

  private suspend fun sendMsg(
    account: AccountEntity, msgEntity: MessageEntity,
    atts: List<AttachmentEntity>, sess: Session?, store: Store?
  ): Boolean =
    withContext(Dispatchers.IO) {
      val mimeMsg = EmailUtil.createMimeMsg(applicationContext, sess, msgEntity, atts)
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)

      when (account.accountType) {
        AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
          if (!account.useAPI && account.email.equals(
              msgEntity.from.firstOrNull()?.address,
              ignoreCase = true
            )
          ) {
            sess ?: throw NullPointerException("Session == null")
            val transport = SmtpProtocolUtil.prepareSmtpTransport(applicationContext, sess, account)
            transport.sendMessage(mimeMsg, mimeMsg.allRecipients)
          } else {
            val gmail = GmailApiHelper.generateGmailApiService(applicationContext, account)
            val copyOfMimeMsg = File.createTempFile("tmp", null, applicationContext.cacheDir)
            try {
              //todo-denbond7 it will be a temporary solution until we will migrate to a new logic
              FileOutputStream(copyOfMimeMsg).use { out ->
                mimeMsg.writeTo(out)
              }

              val threadId = msgEntity.threadIdAsHEX
                ?: mimeMsg.getHeader(JavaEmailConstants.HEADER_IN_REPLY_TO, null)
                  ?.let { replyMsgId ->
                    GmailApiHelper.executeWithResult {
                      com.flowcrypt.email.api.retrofit.response.base.Result.success(
                        GmailApiHelper.getGmailMsgThreadID(gmail, replyMsgId)
                      )
                    }.data
                  }

              var gmailMsg = com.google.api.services.gmail.model.Message().apply {
                this.threadId = threadId
              }

              val mediaContent = FileContent(Constants.MIME_TYPE_RFC822, copyOfMimeMsg)

              gmailMsg = gmail
                .users()
                .messages()
                .send(GmailApiHelper.DEFAULT_USER_ID, gmailMsg, mediaContent)
                .execute()

              if (gmailMsg.id == null) {
                return@withContext false
              }
            } finally {
              if (copyOfMimeMsg.exists()) {
                copyOfMimeMsg.delete()
              }
            }
          }

          roomDatabase.msgDao().updateSuspend(msgEntity.copy(state = MessageState.SENT.value))
          //Gmail automatically save a copy of the sent message.
        }

        AccountEntity.ACCOUNT_TYPE_OUTLOOK -> {
          sess ?: throw NullPointerException("Session == null")
          val outlookTransport =
            SmtpProtocolUtil.prepareSmtpTransport(applicationContext, sess, account)
          outlookTransport.sendMessage(mimeMsg, mimeMsg.allRecipients)
          roomDatabase.msgDao().updateSuspend(msgEntity.copy(state = MessageState.SENT.value))
        }

        else -> {
          sess ?: throw NullPointerException("Session == null")
          val defaultTransport =
            SmtpProtocolUtil.prepareSmtpTransport(applicationContext, sess, account)
          defaultTransport.sendMessage(mimeMsg, mimeMsg.allRecipients)
          roomDatabase.msgDao()
            .updateSuspend(msgEntity.copy(state = MessageState.SENT_WITHOUT_LOCAL_COPY.value))

          store ?: throw NullPointerException("Store == null")
          if (saveCopyOfSentMsg(account, store, mimeMsg)) {
            roomDatabase.msgDao().updateSuspend(msgEntity.copy(state = MessageState.SENT.value))
          }
        }
      }

      return@withContext true
    }

  /**
   * Save a copy of the sent message to the account SENT folder.
   *
   * @param account The object which contains information about an email account.
   * @param store   The connected and opened [Store] object.
   * @param mimeMsg The original [MimeMessage] which will be saved to the SENT folder.
   */
  private suspend fun saveCopyOfSentMsg(
    account: AccountEntity,
    store: Store,
    mimeMsg: MimeMessage
  ): Boolean =
    withContext(Dispatchers.IO) {
      val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, account)
      val sentLocalFolder = foldersManager.findSentFolder()

      if (sentLocalFolder != null) {
        store.getFolder(sentLocalFolder.fullName).use { folder ->
          val sentRemoteFolder = (folder as IMAPFolder).apply {
            if (exists()) {
              open(Folder.READ_WRITE)
            } else throw IllegalArgumentException("The SENT folder doesn't exists. Can't create a copy of the sent message!")
          }
          mimeMsg.setFlag(Flags.Flag.SEEN, true)
          sentRemoteFolder.appendMessages(arrayOf<Message>(mimeMsg))
          return@withContext true
        }
      } else throw CopyNotSavedInSentFolderException(
        "An error occurred during saving a copy of the outgoing message. " +
            "The SENT folder is not defined. Please contact the support: " +
            applicationContext.getString(R.string.support_email) + "\n\nProvider: "
            + account.email.substring(account.email.indexOf("@") + 1)
      )
    }

  companion object {
    private val TAG = MessagesSenderWorker::class.java.simpleName
    private val NOTIFICATION_ID = R.id.notification_id_sending_msgs_worker
    val NAME = MessagesSenderWorker::class.java.simpleName

    fun enqueue(context: Context, forceSending: Boolean = false) {
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

      WorkManager
        .getInstance(context.applicationContext)
        .enqueueUniqueWork(
          NAME,
          if (forceSending) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
          OneTimeWorkRequestBuilder<MessagesSenderWorker>()
            .setConstraints(constraints)
            .apply {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                /*we have to use it due to
                 https://developer.android.com/guide/components/foreground-services#background-start-restrictions
                 We don't use it by default to prevent displaying a notification every time
                 when this job will be started. We should try to show a notification only if we have
                 at least one outgoing message that is actively sending*/
                setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
              }
            }
            .build()
        )
    }
  }
}
