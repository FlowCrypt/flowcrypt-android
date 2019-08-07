/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import android.text.TextUtils
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.dao.source.AccountDao
import com.sun.mail.imap.IMAPFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import javax.mail.BodyPart
import javax.mail.Folder
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.Session
import javax.mail.Store
import javax.mail.internet.InternetHeaders
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage

/**
 * This task load detail information of a message. Currently, it loads only non-attachment parts
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

    val originalMsg: MimeMessage = imapFolder.getMessageByUID(uid) as MimeMessage
    val allHeaders = Collections.list<String>(originalMsg.allHeaderLines)
    val rawHeaders = TextUtils.join("\n", allHeaders)

    if (originalMsg.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
      fetchContent(originalMsg.content as Multipart)
    }

    val customMsg = CustomMimeMessage(session, rawHeaders)

    val multipart = originalMsg.content as? Multipart
    if (multipart != null) {
      removeAttParts(multipart)
      customMsg.setContent(multipart)
    } else {
      customMsg.setContent(originalMsg.content, JavaEmailConstants.MIME_TYPE_TEXT_PLAIN)
    }
    customMsg.saveChanges()

    val outputStream = ByteArrayOutputStream()
    customMsg.writeTo(outputStream)

    listener.onMsgDetailsReceived(account, localFolder, imapFolder, uid, customMsg, outputStream.toByteArray(),
        ownerKey, requestCode)
    imapFolder.close(false)
  }

  /**
   * Remove parts with a disposition = [Part.ATTACHMENT] from the original [Multipart]
   *
   * @param multipart The input [Multipart] object
   */
  private fun removeAttParts(multipart: Multipart) {
    val removeCandidates = mutableListOf<BodyPart>()
    val numberOfParts = multipart.count
    for (partCount in 0 until numberOfParts) {
      val bodyPart = multipart.getBodyPart(partCount)
      if (bodyPart is MimeBodyPart) {
        if (bodyPart.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
          removeAttParts(bodyPart.content as Multipart)
        } else {
          if (Part.ATTACHMENT.equals(bodyPart.disposition, ignoreCase = true)) {
            removeCandidates.add(bodyPart)
          }
        }
      }
    }

    for (candidate in removeCandidates) {
      multipart.removeBodyPart(candidate)
    }
  }

  /**
   * Fetch only non-attachment parts as [Multipart].
   *
   * @param message The parent [Multipart].
   * @return [Multipart] with already cached parts.
   */
  private fun fetchContent(multipart: Multipart) {
    val numberOfParts = multipart.count
    for (partCount in 0 until numberOfParts) {
      val bodyPart = multipart.getBodyPart(partCount)
      if (bodyPart is MimeBodyPart) {
        if (bodyPart.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
          fetchContent(bodyPart.content as Multipart)
        } else {
          if (!Part.ATTACHMENT.equals(bodyPart.disposition, ignoreCase = true)) {
            bodyPart.content// just fetch this part and add to cache
          }
        }
      }
    }
  }

  /**
   * It's a custom realization of [MimeMessage] which has an own realization of creation [InternetHeaders]
   */
  class CustomMimeMessage constructor(session: Session, rawMessage: String?) : MimeMessage(session) {
    init {
      headers = InternetHeaders(ByteArrayInputStream(rawMessage?.toByteArray() ?: "".toByteArray()))
    }
  }
}
