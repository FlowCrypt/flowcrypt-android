/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import android.content.Context
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.sun.mail.imap.IMAPFolder
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.FolderNotFoundException
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.Store

/**
 * This task mark candidates as read/unread.
 *
 * @author Denis Bondarenko
 *         Date: 10/18/19
 *         Time: 12:31 PM
 *         E-mail: DenBond7@gmail.com
 */
class ChangeMsgsReadStateSyncTask(ownerKey: String, requestCode: Int) : BaseSyncTask(ownerKey, requestCode) {

  override fun runIMAPAction(account: AccountEntity, session: Session, store: Store, listener: SyncListener) {
    val context = listener.context

    changeMsgsReadState(context, account, store, MessageState.PENDING_MARK_UNREAD)
    changeMsgsReadState(context, account, store, MessageState.PENDING_MARK_READ)

    listener.onActionCompleted(account, ownerKey, requestCode)
  }

  private fun changeMsgsReadState(context: Context, account: AccountEntity, store: Store, state: MessageState) {
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
    val candidatesForMark = roomDatabase.msgDao().getMsgsWithState(account.email, state.value)

    if (candidatesForMark.isNotEmpty()) {
      val setOfFolders = candidatesForMark.map { it.folder }.toSet()

      for (folder in setOfFolders) {
        val filteredMsgs = candidatesForMark.filter { it.folder == folder }

        if (filteredMsgs.isEmpty()) {
          continue
        }

        try {
          val imapFolder = store.getFolder(folder) as IMAPFolder
          imapFolder.open(Folder.READ_WRITE)

          val uidList = filteredMsgs.map { it.uid }
          val msgs: List<Message> = imapFolder.getMessagesByUID(uidList.toLongArray()).filterNotNull()

          if (msgs.isNotEmpty()) {
            val value = state == MessageState.PENDING_MARK_READ
            imapFolder.setFlags(msgs.toTypedArray(), Flags(Flags.Flag.SEEN), value)
            for (uid in uidList) {
              val msgEntity = roomDatabase.msgDao().getMsg(account.email, folder, uid)

              if (msgEntity?.msgState == state) {
                roomDatabase.msgDao().update(msgEntity.copy(state = MessageState.NONE.value))
              }
            }
          }

          imapFolder.close()
        } catch (e: Exception) {
          e.printStackTrace()
          when (e) {
            is FolderNotFoundException -> {
              //don't send ACRA reports
            }

            is MessagingException -> {
              val msg = e.message ?: ""
              if (!msg.equals("folder cannot contain messages", true)) {
                ExceptionUtil.handleError(e)
              }
            }

            else -> ExceptionUtil.handleError(e)
          }
        }
      }
    }
  }
}