/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import android.text.TextUtils
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.MsgsCacheManager
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.util.exception.SyncTaskTerminatedException
import com.sun.mail.imap.IMAPBodyPart
import com.sun.mail.imap.IMAPFolder
import okio.Okio
import org.apache.commons.io.FilenameUtils
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*
import javax.mail.BodyPart
import javax.mail.FetchProfile
import javax.mail.Folder
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.Session
import javax.mail.Store
import javax.mail.internet.InternetHeaders
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

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
                                 private val uid: Long,
                                 private val id: Long,
                                 resetConnection: Boolean) : BaseSyncTask(ownerKey, requestCode, resetConnection) {

  override fun runIMAPAction(account: AccountDao, session: Session, store: Store, listener: SyncListener) {
    val imapFolder = store.getFolder(localFolder.fullName) as IMAPFolder
    imapFolder.open(Folder.READ_WRITE)

    val originalMsg = imapFolder.getMessageByUID(uid) as? MimeMessage

    if (originalMsg == null) {
      listener.onMsgDetailsReceived(account, localFolder, imapFolder, uid, id, null, ownerKey, requestCode)
      imapFolder.close(false)
      return
    }

    val fetchProfile = FetchProfile()
    fetchProfile.add(FetchProfile.Item.SIZE)
    fetchProfile.add(FetchProfile.Item.CONTENT_INFO)
    fetchProfile.add(IMAPFolder.FetchProfileItem.HEADERS)
    imapFolder.fetch(arrayOf(originalMsg), fetchProfile)

    val customMsg = CustomMimeMessage(session, TextUtils.join("\n", Collections.list<String>(originalMsg.allHeaderLines)))

    val originalMultipart = originalMsg.content as? Multipart
    if (originalMultipart != null) {
      val modifiedMultipart = CustomMimeMultipart(customMsg.contentType)
      buildFromSource(originalMultipart, modifiedMultipart)
      customMsg.setContent(modifiedMultipart)
    } else {
      customMsg.setContent(originalMsg.content, originalMsg.contentType)
    }
    customMsg.saveChanges()
    customMsg.setMessageId(originalMsg.messageID ?: "")
    storeMsg(id.toString(), customMsg)

    listener.onMsgDetailsReceived(account, localFolder, imapFolder, uid, id, customMsg, ownerKey, requestCode)
    imapFolder.close(false)
  }

  private fun buildFromSource(sourceMultipart: Multipart, resultMultipart: Multipart) {
    val candidates = LinkedList<BodyPart>()
    val numberOfParts = sourceMultipart.count
    for (partCount in 0 until numberOfParts) {
      val item = sourceMultipart.getBodyPart(partCount)

      if (item is IMAPBodyPart) {
        if (item.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
          val innerMutlipart = item.content as Multipart
          val innerPart = getPart(innerMutlipart) ?: continue
          candidates.add(innerPart)
        } else {
          if (isPartAllowed(item)) {
            candidates.add(MimeBodyPart(item.mimeStream))
          }
        }
      }
    }

    for (candidate in candidates) {
      resultMultipart.addBodyPart(candidate)
    }
  }

  private fun getPart(originalMultipart: Multipart): BodyPart? {
    val part = originalMultipart.parent
    val headers = part.getHeader("Content-Type")
    val contentType = headers?.first() ?: part.contentType

    val candidates = LinkedList<BodyPart>()
    val numberOfParts = originalMultipart.count
    for (partCount in 0 until numberOfParts) {
      val item = originalMultipart.getBodyPart(partCount)
      if (item is IMAPBodyPart) {
        if (item.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
          val innerPart = getPart(item.content as Multipart)
          innerPart?.let { candidates.add(it) }
        } else {
          if (isPartAllowed(item)) {
            candidates.add(MimeBodyPart(item.mimeStream))
          }
        }
      }
    }

    if (candidates.isEmpty()) {
      return null
    }

    val newMultiPart = CustomMimeMultipart(contentType)

    for (candidate in candidates) {
      newMultiPart.addBodyPart(candidate)
    }

    val bodyPart = MimeBodyPart()
    bodyPart.setContent(newMultiPart)

    return bodyPart
  }

  private fun isPartAllowed(item: MimeBodyPart): Boolean {
    var result = true
    if (Part.ATTACHMENT.equals(item.disposition, ignoreCase = true)) {
      result = false

      //match allowed files
      if (item.fileName in ALLOWED_FILE_NAMES) {
        result = true
      }

      //match private keys
      if (item.fileName?.matches("(?i)(cryptup|flowcrypt)-backup-[a-z0-9]+\\.key".toRegex()) == true) {
        result = true
      }

      //match public keys
      if (item.fileName?.matches("(?i)^(0|0x)?[A-F0-9]{8}([A-F0-9]{8})?.*\\.(asc|key)\$".toRegex()) == true) {
        result = true
      }

      //allow download keys less than 100kb
      if (FilenameUtils.getExtension(item.fileName) in KEYS_EXTENSIONS && item.size < 102400) {
        result = true
      }

      //match signature
      if (item.isMimeType("application/pgp-signature")) {
        result = true
      }
    }

    return result
  }

  private fun storeMsg(key: String, msg: MimeMessage) {
    val editor = MsgsCacheManager.diskLruCache.edit(key) ?: return

    val bufferedSink = Okio.buffer(editor.newSink(0))
    val outputStream = bufferedSink.outputStream()
    val progressOutputStream = ProgressOutputStream(outputStream)
    try {
      msg.writeTo(progressOutputStream)
      bufferedSink.flush()
      editor.commit()

      MsgsCacheManager.diskLruCache.get(key) ?: throw IOException("No space left on device")
    } catch (e: SyncTaskTerminatedException) {
      e.printStackTrace()
      editor.abort()
    }
  }

  /**
   * It's a custom realization of [MimeMessage] which has an own realization of creation [InternetHeaders]
   */
  class CustomMimeMessage constructor(session: Session, rawMessage: String?) : MimeMessage(session) {
    init {
      headers = InternetHeaders(ByteArrayInputStream(rawMessage?.toByteArray() ?: "".toByteArray()))
    }

    fun setMessageId(msgId: String) {
      setHeader("Message-ID", msgId)
    }
  }

  class CustomMimeMultipart constructor(contentType: String) : MimeMultipart() {
    init {
      this.contentType = contentType
    }
  }

  /**
   * This class itself simply overrides all methods of [OutputStream] with versions that pass
   * all requests to the underlying output stream.
   */
  inner class ProgressOutputStream(val out: OutputStream) : OutputStream() {
    override fun write(b: ByteArray?) {
      if (isCancelled) {
        throw SyncTaskTerminatedException()
      }
      out.write(b)
    }

    override fun write(b: Int) {
      if (isCancelled) {
        throw SyncTaskTerminatedException()
      }

      out.write(b)
    }

    override fun write(b: ByteArray?, off: Int, len: Int) {
      if (isCancelled) {
        throw SyncTaskTerminatedException()
      }

      out.write(b, off, len)
    }
  }

  companion object {
    val ALLOWED_FILE_NAMES = arrayOf(
        "PGPexch.htm.pgp",
        "PGPMIME version identification",
        "Version.txt",
        "PGPMIME Versions Identification",
        "signature.asc",
        "msg.asc",
        "message",
        "message.asc",
        "encrypted.asc",
        "encrypted.eml.pgp",
        "Message.pgp"
    )

    val KEYS_EXTENSIONS = arrayOf(
        "asc",
        "key"
    )
  }
}
