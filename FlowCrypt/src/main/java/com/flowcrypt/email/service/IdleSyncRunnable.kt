/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service

import android.content.Context
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.util.LogsUtil
import com.sun.mail.imap.IMAPFolder
import jakarta.mail.event.MessageChangedEvent
import jakarta.mail.event.MessageCountEvent
import jakarta.mail.event.MessageCountListener
import jakarta.mail.search.SubjectTerm
import java.util.concurrent.TimeUnit

/**
 * This is a thread where we do a sync of some IMAP folder.
 *
 * P.S. Currently we support only "INBOX" folder.
 *
 * @author Denys Bondarenko
 */
//todo-denbond7 need to look at com.sun.mail.imap.IdleManager
class IdleSyncRunnable(
  val context: Context, val accountEntity: AccountEntity,
  private val actionsListener: ActionsListener? = null
) : Runnable {
  private val session = OpenStoreHelper.getAccountSess(context, accountEntity)

  @Volatile
  private var store = OpenStoreHelper.getStore(accountEntity, session)
  private var remoteFolder: IMAPFolder? = null

  /**
   * here we can have a lot of checks which help us decide can we run idling(wifi, 3G, a battery level and etc.)
   */
  private val isIdlingAvailable: Boolean = true

  override fun run() {
    Thread.currentThread().name = javaClass.simpleName
    LogsUtil.d(IdleSyncRunnable::class.java.simpleName, " run!")
    idle()
    LogsUtil.d(IdleSyncRunnable::class.java.simpleName, " stopped!")
  }

  /**
   * To interrupt the idle action we should run any IMAP command for the current remote folder. For example search
   */
  fun interruptIdle() {
    LogsUtil.d(IdleSyncRunnable::class.java.simpleName, "interruptIdle")
    Thread {
      Thread.currentThread().name = "IdleStopper"
      try {
        remoteFolder?.search(SubjectTerm("HelloWorld"))
      } catch (e: Exception) {
        //we don't need to store exception details to logs in this case. It's Ok if it fails
      }
    }.start()
  }

  private fun idle(attempt: Int = 0) {
    if (attempt >= ATTEMPT_COUNT) {
      return
    }

    LogsUtil.d(IdleSyncRunnable::class.java.simpleName, "idle: attempt = $attempt")
    store.use {
      val foldersManager = FoldersManager.fromDatabase(context, accountEntity)
      val inboxLocalFolder = foldersManager.findInboxFolder() ?: return@use

      try {
        if (!store.isConnected) {
          EmailUtil.patchingSecurityProvider(context)
          LogsUtil.d(
            IdleSyncRunnable::class.java.simpleName,
            "Not connected. Start a reconnection ..."
          )
          OpenStoreHelper.openStore(context, accountEntity, store)
          LogsUtil.d(IdleSyncRunnable::class.java.simpleName, "Reconnection done")
        }

        LogsUtil.d(IdleSyncRunnable::class.java.simpleName, "Start idling for store $store")

        store.getFolder(inboxLocalFolder.fullName).use { remoteFolder ->
          this.remoteFolder =
            (remoteFolder as IMAPFolder).apply { open(jakarta.mail.Folder.READ_ONLY) }
          actionsListener?.syncFolderState()
          remoteFolder.addMessageCountListener(object : MessageCountListener {
            override fun messagesAdded(e: MessageCountEvent?) {
              actionsListener?.messagesAdded(accountEntity, inboxLocalFolder, remoteFolder, e)
            }

            override fun messagesRemoved(e: MessageCountEvent?) {
              actionsListener?.messagesRemoved(accountEntity, inboxLocalFolder, remoteFolder, e)
            }
          })
          remoteFolder.addMessageChangedListener { messageChangedEvent ->
            actionsListener?.messageChanged(
              accountEntity,
              inboxLocalFolder,
              remoteFolder,
              messageChangedEvent
            )
          }

          while (!Thread.interrupted() && isIdlingAvailable) {
            remoteFolder.idle()
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
        //timeout between attempts
        TimeUnit.MILLISECONDS.sleep(TimeUnit.SECONDS.toMillis(5))
        idle(attempt + 1)
      }
    }
  }

  interface ActionsListener {
    fun syncFolderState()

    fun messageChanged(
      accountEntity: AccountEntity,
      localFolder: LocalFolder,
      remoteFolder: IMAPFolder,
      e: MessageChangedEvent?
    )

    fun messagesAdded(
      accountEntity: AccountEntity,
      localFolder: LocalFolder,
      remoteFolder: IMAPFolder,
      e: MessageCountEvent?
    )

    fun messagesRemoved(
      accountEntity: AccountEntity,
      localFolder: LocalFolder,
      remoteFolder: IMAPFolder,
      e: MessageCountEvent?
    )
  }

  companion object {
    private const val ATTEMPT_COUNT = 5
  }
}
