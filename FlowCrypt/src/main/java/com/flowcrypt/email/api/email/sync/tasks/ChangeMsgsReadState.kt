/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import android.content.Context
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource
import com.sun.mail.imap.IMAPFolder
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
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
class ChangeMsgsReadState(ownerKey: String, requestCode: Int) : BaseSyncTask(ownerKey, requestCode) {

  override fun runIMAPAction(account: AccountDao, session: Session, store: Store, listener: SyncListener) {
    val context = listener.context
    val msgDaoSource = MessageDaoSource()

    changeMsgsReadState(context, account, msgDaoSource, store, MessageState.PENDING_MARK_UNREAD)
    changeMsgsReadState(context, account, msgDaoSource, store, MessageState.PENDING_MARK_READ)
  }

  private fun changeMsgsReadState(context: Context, account: AccountDao,
                                  msgDaoSource: MessageDaoSource, store: Store, state: MessageState) {
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
}