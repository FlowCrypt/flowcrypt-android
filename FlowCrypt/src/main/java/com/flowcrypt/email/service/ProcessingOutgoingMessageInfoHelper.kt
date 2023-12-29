/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.flowcrypt.email.Constants
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.AttachmentEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.extensions.replaceWithCachedRecipients
import com.flowcrypt.email.jetpack.workmanager.ForwardedAttachmentsDownloaderWorker
import com.flowcrypt.email.jetpack.workmanager.HandlePasswordProtectedMsgWorker
import com.flowcrypt.email.jetpack.workmanager.MessagesSenderWorker
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.security.pgp.PgpEncryptAndOrSign
import com.flowcrypt.email.ui.notifications.ErrorNotificationManager
import com.flowcrypt.email.util.FileAndDirectoryUtils
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.ForceHandlingException
import com.flowcrypt.email.util.exception.NoKeyAvailableException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID

/**
 * @author Denys Bondarenko
 */
object ProcessingOutgoingMessageInfoHelper {
  suspend fun process(
    context: Context,
    outgoingMessageInfo: OutgoingMessageInfo,
    messageEntity: MessageEntity,
    doLastAction: () -> Unit = {}
  ) = withContext(Dispatchers.IO) {
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
    val outgoingMsgInfo = outgoingMessageInfo.replaceWithCachedRecipients(context)
    val accountEntity = roomDatabase.accountDao().getAccount(
      outgoingMsgInfo.account?.lowercase() ?: ""
    ) ?: return@withContext

    val uid = outgoingMsgInfo.uid

    try {
      updateContactsLastUseDateTime(context, outgoingMsgInfo)

      val msg = EmailUtil.genMessage(
        context = context,
        accountEntity = accountEntity,
        outgoingMsgInfo = outgoingMsgInfo,
        signingRequired = true,
        hideArmorMeta = accountEntity.clientConfiguration?.shouldHideArmorMeta() ?: false
      )

      val attsCacheDir = getAttsCacheDir(context)
      val msgAttsCacheDir = File(attsCacheDir, UUID.randomUUID().toString())

      val out = ByteArrayOutputStream()
      msg.writeTo(out)

      //todo-denbond7 need to think about that. It'll be better to store a message as a file
      roomDatabase.msgDao().updateSuspend(
        messageEntity.copy(
          rawMessageWithoutAttachments = String(out.toByteArray()),
          attachmentsDirectory = attsCacheDir.name
        )
      )

      val hasAtts = outgoingMsgInfo.atts?.isNotEmpty() == true
          || outgoingMsgInfo.forwardedAtts?.isNotEmpty() == true

      if (hasAtts) {
        if (!msgAttsCacheDir.exists()) {
          if (!msgAttsCacheDir.mkdir()) {
            throw IOException("Create cache directory for outgoing attachments failed!")
          }
        }

        addAttsToCache(context, accountEntity, outgoingMsgInfo, uid, msgAttsCacheDir)
      }

      if (outgoingMsgInfo.forwardedAtts?.isNotEmpty() == true) {
        ForwardedAttachmentsDownloaderWorker.enqueue(context)
      } else {
        val existingMsgEntity = roomDatabase.msgDao().getMsg(
          messageEntity.email, messageEntity.folder, messageEntity.uid
        ) ?: throw IllegalStateException("A message is not exist")
        if (outgoingMsgInfo.encryptionType == MessageEncryptionType.ENCRYPTED
          && outgoingMsgInfo.isPasswordProtected == true
        ) {
          roomDatabase.msgDao()
            .update(existingMsgEntity.copy(state = MessageState.NEW_PASSWORD_PROTECTED.value))
          HandlePasswordProtectedMsgWorker.enqueue(context)
        } else {
          roomDatabase.msgDao().update(existingMsgEntity.copy(state = MessageState.QUEUED.value))
          MessagesSenderWorker.enqueue(context)
        }
      }

      doLastAction.invoke()
    } catch (e: Exception) {
      e.printStackTrace()
      ExceptionUtil.handleError(ForceHandlingException(e))

      when (e) {
        is NoKeyAvailableException -> {
          roomDatabase.msgDao().update(
            messageEntity.copy(
              state = MessageState.ERROR_PRIVATE_KEY_NOT_FOUND.value,
              errorMsg = if (TextUtils.isEmpty(e.alias)) e.email else e.alias
            )
          )
        }

        else -> {
          roomDatabase.msgDao().update(
            messageEntity.copy(
              state = MessageState.ERROR_DURING_CREATION.value,
              errorMsg = e.message
            )
          )
        }
      }

      val failedOutgoingMsgsCount =
        roomDatabase.msgDao().getFailedOutgoingMsgsCount(accountEntity.email)
      if (failedOutgoingMsgsCount > 0) {
        ErrorNotificationManager(context).notifyUserAboutProblemWithOutgoingMsgs(
          accountEntity,
          failedOutgoingMsgsCount
        )
      }

      throw e
    }
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
    accountEntity: AccountEntity,
    outgoingMsgInfo: OutgoingMessageInfo,
    uid: Long, attsCacheDir: File
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

    if (outgoingMsgInfo.atts?.isNotEmpty() == true) {
      val outgoingAttachmentInfoList = outgoingMsgInfo.atts.map { attachmentInfo ->
        attachmentInfo.copy(
          email = accountEntity.email,
          folder = JavaEmailConstants.FOLDER_OUTBOX,
          uid = uid
        )
      }

      for (attachmentInfo in outgoingAttachmentInfoList) {
        var uri: Uri?
        var name: String? = null

        try {
          val origFileUri = attachmentInfo.uri
          var originalFileInputStream: InputStream? = null
          if (origFileUri != null) {
            originalFileInputStream = context.contentResolver.openInputStream(origFileUri)
          } else if (attachmentInfo.rawData?.isNotEmpty() == true) {
            originalFileInputStream = ByteArrayInputStream(attachmentInfo.rawData)
          }

          if (originalFileInputStream == null) {
            continue
          }

          val originalAttName = attachmentInfo.getSafeName()
          if (attachmentInfo.isEncryptionAllowed &&
            outgoingMsgInfo.encryptionType === MessageEncryptionType.ENCRYPTED
          ) {
            val fileName = originalAttName + "." + Constants.PGP_FILE_EXT
            var encryptedTempFile = File(attsCacheDir, fileName)

            if (encryptedTempFile.exists()) {
              encryptedTempFile = FileAndDirectoryUtils.createFileWithIncreasedIndex(
                attsCacheDir,
                encryptedTempFile.name
              )
            }
            PgpEncryptAndOrSign.encryptAndOrSign(
              srcInputStream = originalFileInputStream,
              destOutputStream = encryptedTempFile.outputStream(),
              pubKeys = requireNotNull(pubKeys),
              fileName = originalAttName,
            )
            uri = FileProvider.getUriForFile(
              context,
              Constants.FILE_PROVIDER_AUTHORITY,
              encryptedTempFile
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
        } catch (e: Exception) {
          e.printStackTrace()
          ExceptionUtil.handleError(e)
        }
      }
    }

    if (outgoingMsgInfo.forwardedAtts?.isNotEmpty() == true) {
      for (attachmentInfo in outgoingMsgInfo.forwardedAtts) {
        val candidate = attachmentInfo.copy(
          folder = JavaEmailConstants.FOLDER_OUTBOX,
          uid = uid,
          fwdFolder = attachmentInfo.folder,
          fwdUid = attachmentInfo.uid,
          type = attachmentInfo.type.ifEmpty { Constants.MIME_TYPE_BINARY_DATA },
          orderNumber = 0
        )
        if (attachmentInfo.isEncryptionAllowed
          && outgoingMsgInfo.encryptionType === MessageEncryptionType.ENCRYPTED
        ) {
          cachedAtts.add(candidate.copy(name = candidate.name + "." + Constants.PGP_FILE_EXT))
        } else {
          cachedAtts.add(candidate)
        }
      }
    }

    roomDatabase.attachmentDao().insert(cachedAtts.mapNotNull { AttachmentEntity.fromAttInfo(it) })
  }

  /**
   * Update 'last_use' field in "contacts" table.
   *
   * @param outgoingMessageInfo - [OutgoingMessageInfo] which contains information about an outgoing message.
   */
  private fun updateContactsLastUseDateTime(
    context: Context,
    outgoingMessageInfo: OutgoingMessageInfo
  ) {
    try {
      val recipientDao = FlowCryptRoomDatabase.getDatabase(context).recipientDao()
      //todo-denbond7 we can improve it to use a single request to the local database
      for (email in outgoingMessageInfo.getAllRecipients()) {
        val recipientEntity = recipientDao.getRecipientByEmail(email)
        if (recipientEntity == null) {
          recipientDao.insert(RecipientEntity(email = email))
        } else {
          recipientDao.update(recipientEntity.copy(lastUse = System.currentTimeMillis()))
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }
  }
}
