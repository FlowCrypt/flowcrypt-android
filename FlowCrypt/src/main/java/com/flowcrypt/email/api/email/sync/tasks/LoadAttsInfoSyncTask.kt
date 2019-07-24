/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.imap.AttachmentDaoSource
import com.sun.mail.iap.Argument
import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.protocol.BODYSTRUCTURE
import com.sun.mail.imap.protocol.FetchResponse
import javax.mail.Folder
import javax.mail.Session
import javax.mail.Store

/**
 * This task helps download attachments info of some message
 *
 * @author Denis Bondarenko
 *         Date: 7/24/19
 *         Time: 3:06 PM
 *         E-mail: DenBond7@gmail.com
 */
class LoadAttsInfoSyncTask(ownerKey: String,
                           requestCode: Int,
                           private val localFolder: LocalFolder,
                           private val uid: Long) : BaseSyncTask(ownerKey, requestCode) {

  override fun runIMAPAction(account: AccountDao, session: Session, store: Store, listener: SyncListener) {
    val imapFolder = store.getFolder(localFolder.fullName) as IMAPFolder
    imapFolder.open(Folder.READ_ONLY)

    imapFolder.doCommand { imapProtocol ->
      val args = Argument()
      val list = Argument()
      list.writeString("BODYSTRUCTURE")
      args.writeArgument(list)

      val responses = imapProtocol.command("UID FETCH $uid", args)
      val serverStatusResponse = responses[responses.size - 1]

      if (serverStatusResponse.isOK) {
        for (response in responses) {
          if (response !is FetchResponse) {
            continue
          }

          val bodystructure = response.getItem(BODYSTRUCTURE::class.java)
          bodystructure?.let { AttachmentDaoSource().updateAttsTable(listener.context, account.email, localFolder.fullName, uid, it) }
        }
      }

      imapProtocol.notifyResponseHandlers(responses)
      imapProtocol.handleResult(serverStatusResponse)
    }

    listener.onAttsInfoReceived(account, localFolder, imapFolder, uid, ownerKey, requestCode)

    imapFolder.close(false)
  }
}
