/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import androidx.core.app.JobIntentService
import androidx.core.content.FileProvider
import com.flowcrypt.email.Constants
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.MessageFlag
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.api.retrofit.node.NodeRetrofitHelper
import com.flowcrypt.email.api.retrofit.node.NodeService
import com.flowcrypt.email.api.retrofit.request.node.EncryptFileRequest
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.AttachmentEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.jetpack.workmanager.ForwardedAttachmentsDownloaderWorker
import com.flowcrypt.email.jobscheduler.JobIdManager
import com.flowcrypt.email.jobscheduler.MessagesSenderWorker
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.ui.notifications.ErrorNotificationManager
import com.flowcrypt.email.util.FileAndDirectoryUtils
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.ForceHandlingException
import com.flowcrypt.email.util.exception.NoKeyAvailableException
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*
import javax.mail.internet.MimeMessage

/**
 * This service creates a new outgoing message using the given [OutgoingMessageInfo].
 *
 * @author DenBond7
 * Date: 22.05.2017
 * Time: 22:25
 * E-mail: DenBond7@gmail.com
 */
class PrepareOutgoingMessagesJobIntentService : JobIntentService() {
  override fun onCreate() {
    super.onCreate()
    LogsUtil.d(TAG, "onCreate")
  }

  override fun onDestroy() {
    super.onDestroy()
    LogsUtil.d(TAG, "onDestroy")
  }

  override fun onStopCurrentWork(): Boolean {
    LogsUtil.d(TAG, "onStopCurrentWork")
    return super.onStopCurrentWork()
  }

  override fun onHandleWork(intent: Intent) {
    LogsUtil.d(TAG, "onHandleWork")

    val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)
    val outgoingMsgInfo = intent.getParcelableExtra<OutgoingMessageInfo>(EXTRA_KEY_OUTGOING_MESSAGE_INFO)
        ?: return
    val accountEntity = roomDatabase.accountDao().getAccount(outgoingMsgInfo.account.toLowerCase(Locale.US))
        ?: return
    val sess = OpenStoreHelper.getAccountSess(applicationContext, accountEntity)

    val uid = outgoingMsgInfo.uid
    val email = accountEntity.email
    val label = JavaEmailConstants.FOLDER_OUTBOX

    if (roomDatabase.msgDao().getMsg(email, label, uid) != null) {
      ExceptionUtil.handleError(ForceHandlingException(
          IllegalStateException("Message with the same uid is already exists")))
      return
    }

    LogsUtil.d(TAG, "Preparing a new message with subject: ${outgoingMsgInfo.subject}")
    var newMsgId: Long = -1

    try {
      updateContactsLastUseDateTime(outgoingMsgInfo)

      val pubKeys: List<String>? = if (outgoingMsgInfo.encryptionType === MessageEncryptionType.ENCRYPTED) {
        val senderEmail = outgoingMsgInfo.from
        val recipients = outgoingMsgInfo.getAllRecipients().toMutableList()
        SecurityUtils.getRecipientsPubKeys(this, recipients, accountEntity, senderEmail)
      } else null

      val rawMsg = EmailUtil.genRawMsgWithoutAtts(outgoingMsgInfo, pubKeys)
      val mimeMsg = MimeMessage(sess, IOUtils.toInputStream(rawMsg, StandardCharsets.UTF_8))

      val attsCacheDir = getAttsCacheDir()
      val msgAttsCacheDir = File(attsCacheDir, UUID.randomUUID().toString())

      val msgEntity = prepareMessageEntity(accountEntity, outgoingMsgInfo, uid, mimeMsg, rawMsg, msgAttsCacheDir)
      newMsgId = roomDatabase.msgDao().insert(msgEntity)

      if (newMsgId > 0) {
        updateOutgoingMsgCount(email, roomDatabase)

        val hasAtts = outgoingMsgInfo.atts?.isNotEmpty() == true
            || outgoingMsgInfo.forwardedAtts?.isNotEmpty() == true

        if (hasAtts) {
          if (!msgAttsCacheDir.exists()) {
            if (!msgAttsCacheDir.mkdir()) {
              throw IOException("Create cache directory for outgoing attachments filed!")
            }
          }

          addAttsToCache(roomDatabase, accountEntity, outgoingMsgInfo, uid, pubKeys, msgAttsCacheDir)
        }

        if (outgoingMsgInfo.forwardedAtts?.isEmpty() == true) {
          val insertedMsgEntity = roomDatabase.msgDao().getMsg(
              msgEntity.email, msgEntity.folder, msgEntity.uid)
          insertedMsgEntity?.let {
            roomDatabase.msgDao().update(it.copy(state = MessageState.QUEUED.value))
            MessagesSenderWorker.enqueue(applicationContext)
          }
        } else {
          ForwardedAttachmentsDownloaderWorker.enqueue(applicationContext)
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
      ExceptionUtil.handleError(ForceHandlingException(e))

      var msgEntity = roomDatabase.msgDao().getMsg(email, label, uid)
          ?: MessageEntity.genMsgEntity(email, label, uid, outgoingMsgInfo)

      if (newMsgId <= 0) {
        newMsgId = roomDatabase.msgDao().insert(msgEntity)
        msgEntity = msgEntity.copy(id = newMsgId)
      }

      if (newMsgId > 0) {
        when (e) {
          is NoKeyAvailableException -> {
            roomDatabase.msgDao().update(msgEntity.copy(
                state = MessageState.ERROR_PRIVATE_KEY_NOT_FOUND.value,
                errorMsg = if (TextUtils.isEmpty(e.alias)) e.email else e.alias
            ))
          }

          else -> {
            roomDatabase.msgDao().update(msgEntity.copy(
                state = MessageState.ERROR_DURING_CREATION.value,
                errorMsg = e.message
            ))
          }
        }
      } else {
        ExceptionUtil.handleError(IllegalStateException("An error occurred during inserting a new message"))
      }

      val failedOutgoingMsgsCount = roomDatabase.msgDao().getFailedOutgoingMsgsCount(accountEntity.email)
      if (failedOutgoingMsgsCount > 0) {
        ErrorNotificationManager(applicationContext).notifyUserAboutProblemWithOutgoingMsg(accountEntity, failedOutgoingMsgsCount)
      }
    }

    if (newMsgId > 0) {
      updateOutgoingMsgCount(email, roomDatabase)
    }
  }

  private fun getAttsCacheDir(): File {
    val attsCacheDir = File(cacheDir, Constants.ATTACHMENTS_CACHE_DIR)
    if (!attsCacheDir.exists()) {
      if (!attsCacheDir.mkdirs()) {
        throw IllegalStateException("Create cache directory " + attsCacheDir.name + " filed!")
      }
    }
    return attsCacheDir
  }

  private fun updateOutgoingMsgCount(email: String, roomDatabase: FlowCryptRoomDatabase) {
    val outgoingMsgCount = roomDatabase.msgDao().getOutboxMsgs(email).size
    val outboxLabel = roomDatabase.labelDao().getLabel(email, JavaEmailConstants.FOLDER_OUTBOX)

    outboxLabel?.let {
      roomDatabase.labelDao().update(it.copy(msgsCount = outgoingMsgCount))
    }
  }

  private fun prepareMessageEntity(accountEntity: AccountEntity, msgInfo: OutgoingMessageInfo, generatedUID: Long,
                                   mimeMsg: MimeMessage, rawMsg: String, attsCacheDir: File): MessageEntity {

    val messageEntity = MessageEntity.genMsgEntity(accountEntity.email,
        JavaEmailConstants.FOLDER_OUTBOX, mimeMsg, generatedUID, false)

    val hasAtts = msgInfo.atts?.isNotEmpty() == true || msgInfo.forwardedAtts?.isNotEmpty() == true
    val isEncrypted = msgInfo.encryptionType === MessageEncryptionType.ENCRYPTED
    val msgStateValue = if (msgInfo.isForwarded) MessageState.NEW_FORWARDED.value else MessageState.NEW.value

    return messageEntity.copy(
        hasAttachments = hasAtts,
        rawMessageWithoutAttachments = rawMsg,
        flags = MessageFlag.SEEN.value,
        isEncrypted = isEncrypted,
        state = msgStateValue,
        attachmentsDirectory = attsCacheDir.name
    )
  }

  private fun addAttsToCache(roomDatabase: FlowCryptRoomDatabase, accountEntity: AccountEntity,
                             msgInfo: OutgoingMessageInfo,
                             uid: Long, pubKeys: List<String>?, attsCacheDir: File) {
    val cachedAtts = ArrayList<AttachmentInfo>()

    val nodeService = NodeRetrofitHelper.getRetrofit()!!.create(NodeService::class.java)
    if (msgInfo.atts?.isNotEmpty() == true) {
      val outgoingAtts = msgInfo.atts.map {
        it.apply {
          this.email = accountEntity.email
          this.folder = JavaEmailConstants.FOLDER_OUTBOX
          this.uid = uid.toInt()
        }
      }

      for (att in outgoingAtts) {
        if (TextUtils.isEmpty(att.type)) {
          att.type = Constants.MIME_TYPE_BINARY_DATA
        }

        try {
          val origFileUri = att.uri
          var inputStream: InputStream? = null
          if (origFileUri != null) {
            inputStream = contentResolver.openInputStream(origFileUri)
          } else if (!TextUtils.isEmpty(att.rawData)) {
            inputStream = ByteArrayInputStream(att.rawData!!.toByteArray())
          }

          if (inputStream == null) {
            continue
          }

          if (att.isEncryptionAllowed &&
              msgInfo.encryptionType === MessageEncryptionType.ENCRYPTED) {
            val fileName = att.getSafeName() + Constants.PGP_FILE_EXT
            var encryptedTempFile = File(attsCacheDir, fileName)

            if (encryptedTempFile.exists()) {
              encryptedTempFile = FileAndDirectoryUtils.createFileWithIncreasedIndex(attsCacheDir, encryptedTempFile.name)
            }

            val request = EncryptFileRequest(this, origFileUri, FilenameUtils.getBaseName(encryptedTempFile.name), pubKeys!!)

            val response = nodeService.encryptFile(request).execute()
            val encryptedFileResult = response.body()

            if (encryptedFileResult == null) {
              ExceptionUtil.handleError(NullPointerException("encryptedFileResult == null"))
              continue
            }

            if (encryptedFileResult.apiError != null) {
              ExceptionUtil.handleError(Exception(encryptedFileResult.apiError.msg))
              continue
            }

            val encryptedBytes = encryptedFileResult.encryptBytes
            FileUtils.writeByteArrayToFile(encryptedTempFile, encryptedBytes!!)
            val uri = FileProvider.getUriForFile(this, Constants.FILE_PROVIDER_AUTHORITY, encryptedTempFile)
            att.uri = uri
            att.name = encryptedTempFile.name
          } else {
            var cachedAtt = File(attsCacheDir, att.getSafeName())
            if (cachedAtt.exists()) {
              cachedAtt = FileAndDirectoryUtils.createFileWithIncreasedIndex(attsCacheDir, cachedAtt.name)
            }

            FileUtils.copyInputStreamToFile(inputStream, cachedAtt)
            val uri = FileProvider.getUriForFile(this, Constants.FILE_PROVIDER_AUTHORITY, cachedAtt)
            att.uri = uri
          }

          cachedAtts.add(att)
          if (origFileUri != null) {
            if (Constants.FILE_PROVIDER_AUTHORITY.equals(origFileUri.authority!!, ignoreCase = true)) {
              contentResolver.delete(origFileUri, null, null)
            }
          }
        } catch (e: Exception) {
          e.printStackTrace()
          ExceptionUtil.handleError(e)
        }

      }
    }

    if (msgInfo.forwardedAtts?.isNotEmpty() == true) {
      for (att in msgInfo.forwardedAtts) {
        if (att.type.isEmpty()) {
          att.type = Constants.MIME_TYPE_BINARY_DATA
        }

        if (att.isEncryptionAllowed && msgInfo.encryptionType === MessageEncryptionType.ENCRYPTED) {
          val encryptedAtt = att.copy(JavaEmailConstants.FOLDER_OUTBOX, uid.toInt())
          encryptedAtt.name = encryptedAtt.name + Constants.PGP_FILE_EXT
          cachedAtts.add(encryptedAtt)
        } else {
          cachedAtts.add(att.copy(JavaEmailConstants.FOLDER_OUTBOX, uid.toInt()))
        }
      }
    }

    roomDatabase.attachmentDao().insert(cachedAtts.mapNotNull { AttachmentEntity.fromAttInfo(it) })
  }

  /**
   * Update 'last_use' field in "contacts" table.
   *
   * @param msgInfo - [OutgoingMessageInfo] which contains information about an outgoing message.
   */
  private fun updateContactsLastUseDateTime(msgInfo: OutgoingMessageInfo) {
    try {
      val contactsDao = FlowCryptRoomDatabase.getDatabase(applicationContext).contactsDao()

      for (email in msgInfo.getAllRecipients()) {
        val contactEntity = contactsDao.getContactByEmails(email)
        if (contactEntity == null) {
          contactsDao.insert(PgpContact(email, null).toContactEntity())
        } else {
          contactsDao.update(contactEntity.copy(lastUse = System.currentTimeMillis()))
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }
  }

  companion object {
    private val EXTRA_KEY_OUTGOING_MESSAGE_INFO =
        GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_OUTGOING_MESSAGE_INFO",
            PrepareOutgoingMessagesJobIntentService::class.java)
    private val TAG = PrepareOutgoingMessagesJobIntentService::class.java.simpleName

    /**
     * Enqueue a new task for [PrepareOutgoingMessagesJobIntentService].
     *
     * @param context         Interface to global information about an application environment.
     * @param outgoingMsgInfo [OutgoingMessageInfo] which contains information about an outgoing message.
     */
    fun enqueueWork(context: Context, outgoingMsgInfo: OutgoingMessageInfo?) {
      if (outgoingMsgInfo != null) {
        val intent = Intent(context, PrepareOutgoingMessagesJobIntentService::class.java)
        intent.putExtra(EXTRA_KEY_OUTGOING_MESSAGE_INFO, outgoingMsgInfo)

        enqueueWork(context, PrepareOutgoingMessagesJobIntentService::class.java,
            JobIdManager.JOB_TYPE_PREPARE_OUT_GOING_MESSAGE, intent)
      }
    }
  }
}
