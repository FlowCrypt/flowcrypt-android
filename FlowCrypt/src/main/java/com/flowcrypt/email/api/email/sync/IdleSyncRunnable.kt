/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync

import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.sun.mail.imap.IMAPFolder
import com.sun.mail.util.MailConnectException
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.mail.FolderClosedException
import javax.mail.MessagingException
import javax.mail.event.MessageChangedEvent
import javax.mail.event.MessageChangedListener
import javax.mail.event.MessageCountEvent
import javax.mail.event.MessageCountListener
import javax.mail.search.SubjectTerm

/**
 * This is a thread where we do a sync of some IMAP folder.
 *
 * P.S. Currently we support only "INBOX" folder.
 *
 * @author Denis Bondarenko
 *         Date: 10/17/19
 *         Time: 3:33 PM
 *         E-mail: DenBond7@gmail.com
 */
class IdleSyncRunnable constructor(syncListener: SyncListener,
                                   private val emailSyncManager: EmailSyncManager) :
    BaseSyncRunnable(syncListener), MessageCountListener, MessageChangedListener {
  private var localFolder: LocalFolder? = null
  private var remoteFolder: IMAPFolder? = null

  /**
   * here we can have a lot of checks which help us decide can we run idling(wifi, 3G, a battery level and etc.)
   */
  private val isIdlingAvailable: Boolean
    get() = true

  override fun run() {
    LogsUtil.d(tag, " run!")
    Thread.currentThread().name = javaClass.simpleName

    val activeAccount = FlowCryptRoomDatabase.getDatabase(syncListener.context).accountDao().getActiveAccount()
    activeAccount?.let {
      val foldersManager = FoldersManager.fromDatabase(syncListener.context, activeAccount.email)
      localFolder = foldersManager.findInboxFolder() ?: return

      idle(it)
      closeConn()
    }

    LogsUtil.d(tag, " stopped!")
  }

  override fun messagesAdded(e: MessageCountEvent) {
    LogsUtil.d(tag, "messagesAdded: " + e.messages.size)
    //todo-denbond7 maybe will be better to use callback
    localFolder?.let { emailSyncManager.loadNewMsgs("", 0, it) }
  }

  override fun messagesRemoved(messageCountEvent: MessageCountEvent) {
    LogsUtil.d(tag, "messagesRemoved")
    syncFolderState()
  }

  override fun messageChanged(e: MessageChangedEvent) {
    LogsUtil.d(tag, "messageChanged")
    val local = localFolder ?: return
    val remote = remoteFolder ?: return
    val msg = e.message
    if (msg != null && e.messageChangeType == MessageChangedEvent.FLAGS_CHANGED) {
      try {
        val roomDatabase = FlowCryptRoomDatabase.getDatabase(syncListener.context)
        val userName = msg.folder.store.urlName.username?.toLowerCase(Locale.getDefault()) ?: return
        val account = roomDatabase.accountDao().getAccount(userName) ?: return
        roomDatabase.msgDao().updateLocalMsgFlags(account.email, local.fullName, remote.getUID(msg), msg.flags)
        syncListener.onMsgChanged(account, local, remote, msg, "", 0)
      } catch (msgException: MessagingException) {
        msgException.printStackTrace()
      }
    }
  }

  /**
   * To interrupt the idle action we should run any IMAP command for the current remote folder. For example search
   */
  fun interruptIdle() {
    LogsUtil.d(tag, "interruptIdle")
    Thread(Runnable {
      Thread.currentThread().name = "IdleStopper"
      try {
        remoteFolder?.search(SubjectTerm("HelloWorld"))
      } catch (e: Exception) {
        //we don't need to store exception details to logs in this case. It's Ok if it will fail
      }
    }).start()
  }

  internal fun idle(accountEntity: AccountEntity) {
    try {
      resetConnIfNeeded(accountEntity, false, null)
      while (!GeneralUtil.isConnected(syncListener.context)) {
        try {
          //wait while a connection will be established
          TimeUnit.MILLISECONDS.sleep(TimeUnit.SECONDS.toMillis(30))
        } catch (interruptedException: InterruptedException) {
          interruptedException.printStackTrace()
        }
      }

      if (!isConnected) {
        LogsUtil.d(tag, "Not connected. Start a reconnection ...")
        openConnToStore(accountEntity)
        LogsUtil.d(tag, "Reconnection done")
      }

      val activeStore = store ?: return

      LogsUtil.d(tag, "Start idling for store $activeStore")

      remoteFolder = activeStore.getFolder(localFolder?.fullName) as IMAPFolder
      remoteFolder?.open(javax.mail.Folder.READ_ONLY)

      syncFolderState()

      remoteFolder?.addMessageCountListener(this)
      remoteFolder?.addMessageChangedListener(this)

      while (!Thread.interrupted() && isIdlingAvailable) {
        remoteFolder?.idle()
      }
    } catch (e: Exception) {
      e.printStackTrace()
      if (e is FolderClosedException || e is MailConnectException || e is IOException) {
        idle(accountEntity)
      } else if (e is MessagingException) {
        if ("IDLE not supported" == e.message) {
          LogsUtil.d(tag, "IDLE not supported!")
        }
      } else {
        ExceptionUtil.handleError(e)
      }
    }
  }

  private fun syncFolderState() {
    //todo-denbond7 maybe will be better to use callback
    localFolder?.let {
      emailSyncManager.refreshMsgs("", 0, it)
    }
  }
}