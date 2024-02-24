/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.flowcrypt.email.Constants
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.dao.BaseDao
import com.flowcrypt.email.database.entity.AttachmentEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.extensions.java.lang.printStackTraceIfDebugOnly
import com.flowcrypt.email.extensions.replaceWithCachedRecipients
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.security.pgp.PgpEncryptAndOrSign
import com.flowcrypt.email.util.FileAndDirectoryUtils
import jakarta.mail.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * @author Denys Bondarenko
 */
object ProcessingOutgoingMessageInfoHelper {
  suspend fun process(
    context: Context,
    originalOutgoingMessageInfo: OutgoingMessageInfo,
    messageEntity: MessageEntity,
    afterMimeMessageCreatingAction: suspend (message: Message) -> Unit = {}
  ) = withContext(Dispatchers.IO) {
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
    val outgoingMsgInfo = originalOutgoingMessageInfo.replaceWithCachedRecipients(context)
    val accountEntity = roomDatabase.accountDao().getAccount(
      outgoingMsgInfo.account?.lowercase() ?: ""
    )?.withDecryptedInfo() ?: throw IllegalStateException("Account is not defined")

    updateContactsLastUseDateTime(context, outgoingMsgInfo)

    val mimeMessage = EmailUtil.genMessage(
      context = context,
      accountEntity = accountEntity,
      outgoingMsgInfo = outgoingMsgInfo,
      signingRequired = true,
      hideArmorMeta = accountEntity.clientConfiguration?.shouldHideArmorMeta() ?: false
    )

    //todo-denbond7 need to think about storing attachment inside in the MIME message

    val hasAtts =
      outgoingMsgInfo.atts?.isNotEmpty() == true || outgoingMsgInfo.forwardedAtts?.isNotEmpty() == true

    if (hasAtts) {
      val msgAttsCacheDir = File(
        getAttsCacheDir(context), requireNotNull(messageEntity.attachmentsDirectory)
      )
      if (!msgAttsCacheDir.exists()) {
        if (!msgAttsCacheDir.mkdir()) {
          throw IOException("Create cache directory for outgoing attachments failed!")
        }
      }

      addAttsToCache(context, outgoingMsgInfo, msgAttsCacheDir)
    }

    afterMimeMessageCreatingAction.invoke(mimeMessage)
  }

  private fun getAttsCacheDir(context: Context): File {
    val attsCacheDir = File(context.cacheDir, Constants.ATTACHMENTS_CACHE_DIR)
    if (!attsCacheDir.exists()) {
      if (!attsCacheDir.mkdirs()) {
        throw IllegalStateException("Create cache directory " + attsCacheDir.name + " failed!")
      }
    }
    return attsCacheDir
  }

  private fun addAttsToCache(
    context: Context,
    outgoingMsgInfo: OutgoingMessageInfo,
    attsCacheDir: File
  ) {
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
    val cachedAtts = ArrayList<AttachmentInfo>()
    var pubKeys: List<String>? = null

    if (outgoingMsgInfo.encryptionType === MessageEncryptionType.ENCRYPTED) {
      val senderEmail = requireNotNull(outgoingMsgInfo.from?.address)
      val recipients = outgoingMsgInfo.getAllRecipients().toMutableList()
      pubKeys = mutableListOf()
      pubKeys.addAll(SecurityUtils.getRecipientsUsablePubKeys(context, recipients))
      pubKeys.addAll(SecurityUtils.getSenderPublicKeys(context, senderEmail))
    }

    for (attachmentInfo in outgoingMsgInfo.atts ?: emptyList()) {
      var uri: Uri?
      var name: String? = null

      val origFileUri = attachmentInfo.uri
      var originalFileInputStream: InputStream? = null
      if (origFileUri != null) {
        originalFileInputStream = context.contentResolver.openInputStream(origFileUri)
      } else if (attachmentInfo.rawData?.isNotEmpty() == true) {
        originalFileInputStream = ByteArrayInputStream(attachmentInfo.rawData)
      }

      if (originalFileInputStream == null) {
        throw IllegalStateException("The file stream is null")
      }

      val originalAttName = attachmentInfo.getSafeName()
      if (attachmentInfo.isEncryptionAllowed && outgoingMsgInfo.encryptionType === MessageEncryptionType.ENCRYPTED) {
        val fileName = originalAttName + "." + Constants.PGP_FILE_EXT
        var encryptedTempFile = File(attsCacheDir, fileName)

        if (encryptedTempFile.exists()) {
          encryptedTempFile = FileAndDirectoryUtils.createFileWithIncreasedIndex(
            attsCacheDir, encryptedTempFile.name
          )
        }
        PgpEncryptAndOrSign.encryptAndOrSign(
          srcInputStream = originalFileInputStream,
          destOutputStream = encryptedTempFile.outputStream(),
          pubKeys = requireNotNull(pubKeys),
          fileName = originalAttName,
        )
        uri = FileProvider.getUriForFile(
          context, Constants.FILE_PROVIDER_AUTHORITY, encryptedTempFile
        )
        name = encryptedTempFile.name
      } else {
        var cachedAtt = File(attsCacheDir, originalAttName)
        if (cachedAtt.exists()) {
          cachedAtt =
            FileAndDirectoryUtils.createFileWithIncreasedIndex(attsCacheDir, cachedAtt.name)
        }

        FileUtils.copyInputStreamToFile(originalFileInputStream, cachedAtt)
        uri = FileProvider.getUriForFile(context, Constants.FILE_PROVIDER_AUTHORITY, cachedAtt)
      }

      cachedAtts.add(
        attachmentInfo.copy(
          type = attachmentInfo.type.ifEmpty { Constants.MIME_TYPE_BINARY_DATA },
          uri = uri,
          name = name ?: attachmentInfo.name
        )
      )

      if (origFileUri != null) {
        if (Constants.FILE_PROVIDER_AUTHORITY.equals(origFileUri.authority, true)) {
          context.contentResolver.delete(origFileUri, null, null)
        }
      }
    }

    for (candidate in outgoingMsgInfo.forwardedAtts ?: emptyList()) {
      if (candidate.isEncryptionAllowed
        && outgoingMsgInfo.encryptionType === MessageEncryptionType.ENCRYPTED
      ) {
        cachedAtts.add(candidate.copy(name = candidate.name + "." + Constants.PGP_FILE_EXT))
      } else {
        cachedAtts.add(candidate)
      }
    }

    roomDatabase.attachmentDao().insert(cachedAtts.mapNotNull { AttachmentEntity.fromAttInfo(it) })
  }

  /**
   * Update 'last_use' field in "contacts" table.
   *
   * @param outgoingMessageInfo - [OutgoingMessageInfo] which contains information about an outgoing message.
   */
  private suspend fun updateContactsLastUseDateTime(
    context: Context,
    outgoingMessageInfo: OutgoingMessageInfo
  ) = withContext(Dispatchers.IO) {
    try {
      val recipientDao = FlowCryptRoomDatabase.getDatabase(context).recipientDao()
      val allRecipients = outgoingMessageInfo.getAllRecipients().map { it.lowercase() }
      BaseDao.doOperationViaStepsSuspend(list = allRecipients) { recipients: Collection<String> ->
        val entities = recipientDao.getRecipientsByEmails(recipients)
        recipientDao.updateSuspend(entities.map { it.copy(lastUse = outgoingMessageInfo.timestamp) })
        val existingRecipients = entities.map { it.email }.toSet()
        val recipientsToBeAdded = recipients - existingRecipients
        recipientDao.insertSuspend(recipientsToBeAdded.map { email ->
          RecipientEntity(
            email = email,
            lastUse = outgoingMessageInfo.timestamp
          )
        })
        entities.size + recipientsToBeAdded.size
      }
    } catch (e: Exception) {
      e.printStackTraceIfDebugOnly()
    }
  }
}
