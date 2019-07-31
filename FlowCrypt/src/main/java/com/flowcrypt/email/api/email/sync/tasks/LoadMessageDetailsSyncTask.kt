/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.imap.AttachmentDaoSource
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.sun.mail.iap.Argument
import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.protocol.BODY
import com.sun.mail.imap.protocol.BODYSTRUCTURE
import com.sun.mail.imap.protocol.FetchResponse
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Session
import javax.mail.Store

/**
 * This task load a detail information of some message. At now this task creates and executes
 * command like this "UID FETCH xxxxxxxxxxxxx (RFC822.SIZE BODY[]<0.1024000>)". We request first
 * 1000kb of a message, and if the message is small, we'll get the whole MIME message. If
 * larger than 1000kb, we'll get only the first part of it.
 *
 * @param localFolder      The local localFolder implementation.
 * @param uid         The [com.sun.mail.imap.protocol.UID] of [javax.mail.Message]
 *
 * @author DenBond7
 * Date: 26.06.2017
 * Time: 17:41
 * E-mail: DenBond7@gmail.com
 */

class LoadMessageDetailsSyncTask(ownerKey: String,
                                 requestCode: Int,
                                 private val localFolder: LocalFolder,
                                 private val uid: Long) : BaseSyncTask(ownerKey, requestCode) {

  override fun runIMAPAction(account: AccountDao, session: Session, store: Store, listener: SyncListener) {
    val imapFolder = store.getFolder(localFolder.fullName) as IMAPFolder
    imapFolder.open(Folder.READ_WRITE)

    val rawMsg = imapFolder.doCommand { imapProtocol ->
      var rawMimeBytes: ByteArray? = null

      val args = Argument()
      val list = Argument()
      list.writeString("RFC822.SIZE")
      list.writeString("BODY[]<0.1024000>")
      list.writeString("BODYSTRUCTURE")
      args.writeArgument(list)

      val responses = imapProtocol.command("UID FETCH $uid", args)
      val serverStatusResponse = responses[responses.size - 1]

      if (serverStatusResponse.isOK) {
        for (response in responses) {
          if (response !is FetchResponse) {
            continue
          }

          val body = response.getItem(BODY::class.java)
          body?.byteArrayInputStream?.let { rawMimeBytes = it.readBytes() }

          val bodystructure = response.getItem(BODYSTRUCTURE::class.java)
          bodystructure?.let {
            AttachmentDaoSource().updateAttsTable(listener.context, account.email, localFolder.fullName, uid, it)
          }
        }

        if (rawMimeBytes == null) {
          ExceptionUtil.handleError(IllegalStateException(
              "LoadMessageDetailsSyncTask:Server response = $serverStatusResponse" +
                  "\n There is no FetchResponse with right BODY" +
                  "\n responses count = ${responses.size}"))
        }

      } else {
        ExceptionUtil.handleError(IllegalStateException(
            "LoadMessageDetailsSyncTask:Server response = $serverStatusResponse"))
      }

      imapProtocol.notifyResponseHandlers(responses)
      imapProtocol.handleResult(serverStatusResponse)

      rawMimeBytes
    } as? ByteArray ?: throw IllegalStateException("An error occurred during receiving the message details")

    val message = imapFolder.getMessageByUID(uid)
    message?.setFlag(Flags.Flag.SEEN, true)

    listener.onMsgDetailsReceived(account, localFolder, imapFolder, uid, message, rawMsg, ownerKey, requestCode)

    imapFolder.close(false)
  }
}
