/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import android.text.TextUtils
import android.util.Base64
import android.util.Base64OutputStream
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.MsgsCacheManager
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.util.exception.SyncTaskTerminatedException
import com.sun.mail.imap.IMAPBodyPart
import com.sun.mail.imap.IMAPFolder
import okio.buffer
import org.apache.commons.io.FilenameUtils
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import javax.crypto.CipherOutputStream
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
                                 uniqueId: String,
                                 private val localFolder: LocalFolder,
                                 private val uid: Long,
                                 private val id: Long,
                                 resetConnection: Boolean) :
    BaseSyncTask(ownerKey, requestCode, uniqueId, resetConnection) {

  private var msgSize: Int = 0
  private var downloadedMsgSize: Int = 0
  private var listener: SyncListener? = null
  private var account: AccountEntity? = null
  private var lastPercentage = 0
  private var currentPercentage = 0
  private var lastUpdateTime = System.currentTimeMillis()

  override fun runIMAPAction(account: AccountEntity, session: Session, store: Store, listener: SyncListener) {
    this.account = account
    this.listener = listener
    val imapFolder = store.getFolder(localFolder.fullName) as IMAPFolder
    listener.onActionProgress(account, ownerKey, requestCode, R.id.progress_id_connecting, 10)
    imapFolder.open(Folder.READ_WRITE)
    listener.onActionProgress(account, ownerKey, requestCode, R.id.progress_id_connecting, 20)

    val originalMsg = imapFolder.getMessageByUID(uid) as? MimeMessage

    if (originalMsg == null) {
      listener.onMsgDetailsReceived(account, localFolder, imapFolder, uid, id, null, ownerKey, requestCode)
      imapFolder.close(false)
      return
    }

    msgSize = originalMsg.size

    val fetchProfile = FetchProfile()
    fetchProfile.add(FetchProfile.Item.SIZE)
    fetchProfile.add(FetchProfile.Item.CONTENT_INFO)
    fetchProfile.add(IMAPFolder.FetchProfileItem.HEADERS)
    imapFolder.fetch(arrayOf(originalMsg), fetchProfile)

    val rawHeaders = TextUtils.join("\n", Collections.list<String>(originalMsg.allHeaderLines))
    if (rawHeaders.isNotEmpty()) downloadedMsgSize += rawHeaders.length
    val customMsg = CustomMimeMessage(session, rawHeaders)

    val originalMultipart = originalMsg.content as? Multipart
    if (originalMultipart != null) {
      val modifiedMultipart = CustomMimeMultipart(customMsg.contentType)
      buildFromSource(originalMultipart, modifiedMultipart)
      customMsg.setContent(modifiedMultipart)
    } else {
      customMsg.setContent(originalMsg.content, originalMsg.contentType)
      downloadedMsgSize += originalMsg.size
    }

    customMsg.saveChanges()
    customMsg.setMessageId(originalMsg.messageID ?: "")
    storeMsg(id.toString(), customMsg)

    listener.onActionProgress(account, ownerKey, requestCode, R.id.progress_id_fetching_message, 60)
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
            candidates.add(MimeBodyPart(FetchingInputStream(item.mimeStream)))
          } else {
            if (item.size > 0) downloadedMsgSize += item.size
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
            candidates.add(MimeBodyPart(FetchingInputStream(item.mimeStream)))
          } else {
            if (item.size > 0) downloadedMsgSize += item.size
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
      if (item.fileName?.matches("(?i)(cryptup|flowcrypt)-backup-[a-z0-9]+\\.(asc|key)".toRegex()) == true) {
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

    val bufferedSink = editor.newSink().buffer()
    val outputStreamOfBufferedSink = ProgressOutputStream(bufferedSink.outputStream())
    val cipherForEncryption = KeyStoreCryptoManager.getCipherForEncryption()
    val base64OutputStream = Base64OutputStream(outputStreamOfBufferedSink, KeyStoreCryptoManager.BASE64_FLAGS)
    val outputStream = CipherOutputStream(base64OutputStream, cipherForEncryption)

    try {
      outputStream.use {
        outputStreamOfBufferedSink.write(Base64.encodeToString(cipherForEncryption.iv, KeyStoreCryptoManager.BASE64_FLAGS).toByteArray())
        outputStreamOfBufferedSink.write("\n".toByteArray())
        msg.writeTo(it)
        bufferedSink.flush()
        editor.commit()
      }

      MsgsCacheManager.diskLruCache[key] ?: throw IOException("No space left on device")
    } catch (e: SyncTaskTerminatedException) {
      e.printStackTrace()
      editor.abort()
    }
  }

  private fun sendProgress() {
    if (msgSize > 0) {
      currentPercentage = (downloadedMsgSize * 100 / msgSize)
      val isUpdateNeeded = System.currentTimeMillis() - lastUpdateTime >= MIN_UPDATE_PROGRESS_INTERVAL
      if (currentPercentage - lastPercentage >= 1 && isUpdateNeeded) {
        lastPercentage = currentPercentage
        lastUpdateTime = System.currentTimeMillis()
        account?.let {
          val value = (currentPercentage * 40 / 100) + 20
          listener?.onActionProgress(it, ownerKey, requestCode, R.id.progress_id_fetching_message, value)
        }
      }
    }
  }

  /**
   * It's a custom realization of [MimeMessage] which has an own realization of creation [InternetHeaders]
   */
  class CustomMimeMessage constructor(session: Session, rawHeaders: String?) : MimeMessage(session) {
    init {
      headers = InternetHeaders(ByteArrayInputStream(rawHeaders?.toByteArray() ?: "".toByteArray()))
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
  inner class ProgressOutputStream(val out: OutputStream) : BufferedOutputStream(out) {
    override fun write(b: ByteArray) {
      if (Thread.interrupted()) {
        throw SyncTaskTerminatedException()
      }
      super.write(b)
    }

    override fun write(b: Int) {
      if (Thread.interrupted()) {
        throw SyncTaskTerminatedException()
      }

      super.write(b)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
      if (Thread.interrupted()) {
        throw SyncTaskTerminatedException()
      }

      super.write(b, off, len)
    }
  }

  /**
   * This class will be used to identify the fetching progress.
   */
  inner class FetchingInputStream(val stream: InputStream) : BufferedInputStream(stream) {
    override fun read(b: ByteArray, off: Int, len: Int): Int {
      if (Thread.interrupted()) {
        throw SyncTaskTerminatedException()
      }

      val value = super.read(b, off, len)
      if (value != -1) {
        downloadedMsgSize += value
        sendProgress()
      }
      return value
    }
  }

  companion object {
    private const val MIN_UPDATE_PROGRESS_INTERVAL = 500

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
