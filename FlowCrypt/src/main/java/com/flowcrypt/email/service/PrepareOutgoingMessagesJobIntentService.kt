/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.util.Log
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
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.database.dao.source.ContactsDaoSource
import com.flowcrypt.email.database.dao.source.imap.AttachmentDaoSource
import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource
import com.flowcrypt.email.jobscheduler.ForwardedAttachmentsDownloaderJobService
import com.flowcrypt.email.jobscheduler.JobIdManager
import com.flowcrypt.email.jobscheduler.MessagesSenderJobService
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.NoKeyAvailableException
import com.google.android.gms.common.util.CollectionUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*
import javax.mail.Session
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

  private val msgDaoSource: MessageDaoSource = MessageDaoSource()
  private var sess: Session? = null
  private var account: AccountDao? = null
  private var attsCacheDir: File? = null

  override fun onCreate() {
    super.onCreate()
    LogsUtil.d(TAG, "onCreate")
    account = AccountDaoSource().getActiveAccountInformation(applicationContext)
    sess = OpenStoreHelper.getAccountSess(applicationContext, account)
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
    val accountDao = account ?: return
    if (intent.hasExtra(EXTRA_KEY_OUTGOING_MESSAGE_INFO)) {
      val outgoingMsgInfo = intent.getParcelableExtra<OutgoingMessageInfo>(EXTRA_KEY_OUTGOING_MESSAGE_INFO)
      val uid = outgoingMsgInfo.uid
      val email = accountDao.email
      val label = JavaEmailConstants.FOLDER_OUTBOX

      if (msgDaoSource.getMsg(this, email, label, uid) != null) {
        //todo-DenBond7 need to think about resolving a situation, when a message was created but the
        // attachments were not added.
        return
      }

      LogsUtil.d(TAG, "Received a new job: $outgoingMsgInfo")
      var newMsgUri: Uri? = null

      try {
        setupIfNeeded()
        updateContactsLastUseDateTime(outgoingMsgInfo)

        var pubKeys: List<String>? = null
        if (outgoingMsgInfo.encryptionType === MessageEncryptionType.ENCRYPTED) {
          val senderEmail = outgoingMsgInfo.from
          pubKeys = SecurityUtils.getRecipientsPubKeys(this,
              outgoingMsgInfo.getAllRecipients().toMutableList(), accountDao, senderEmail)
        }

        val rawMsg = EmailUtil.genRawMsgWithoutAtts(outgoingMsgInfo, pubKeys)
        val mimeMsg = MimeMessage(sess, IOUtils.toInputStream(rawMsg, StandardCharsets.UTF_8))

        val msgAttsCacheDir = File(attsCacheDir, UUID.randomUUID().toString())

        val contentValues = prepareContentValues(outgoingMsgInfo, uid, mimeMsg, rawMsg, msgAttsCacheDir)
        newMsgUri = msgDaoSource.addRow(this, contentValues)

        if (newMsgUri != null) {
          val msgsCount = msgDaoSource.getOutboxMsgs(this, email).size
          ImapLabelsDaoSource().updateLabelMsgsCount(this, email, label, msgsCount)

          val hasAtts = !CollectionUtils.isEmpty(outgoingMsgInfo.atts)
              || !CollectionUtils.isEmpty(outgoingMsgInfo.forwardedAtts)

          if (hasAtts) {
            if (!msgAttsCacheDir.exists()) {
              if (!msgAttsCacheDir.mkdir()) {
                Log.e(TAG, "Create cache directory " + attsCacheDir!!.name + " filed!")
                msgDaoSource.updateMsgState(this, email, label, uid, MessageState.ERROR_CACHE_PROBLEM)
                return
              }
            }

            addAttsToCache(outgoingMsgInfo, uid, pubKeys, msgAttsCacheDir)
          }

          if (CollectionUtils.isEmpty(outgoingMsgInfo.forwardedAtts)) {
            msgDaoSource.updateMsgState(this, email, label, uid, MessageState.QUEUED)
            MessagesSenderJobService.schedule(applicationContext)
          } else {
            ForwardedAttachmentsDownloaderJobService.schedule(applicationContext)
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)

        if (newMsgUri == null) {
          val contentValues = MessageDaoSource.prepareContentValues(email, label, uid, outgoingMsgInfo)
          newMsgUri = msgDaoSource.addRow(this, contentValues)
        }

        if (e is NoKeyAvailableException) {
          val errorMsg = if (TextUtils.isEmpty(e.alias)) e.email else e.alias

          val contentValues = ContentValues()
          contentValues.put(MessageDaoSource.COL_STATE, MessageState.ERROR_PRIVATE_KEY_NOT_FOUND.value)
          contentValues.put(MessageDaoSource.COL_ERROR_MSG, errorMsg)

          msgDaoSource.updateMsg(this, email, label, uid, contentValues)
        } else {
          msgDaoSource.updateMsgState(this, email, label, uid, MessageState.ERROR_DURING_CREATION)
        }
      }

      if (newMsgUri != null) {
        val newMsgsCount = msgDaoSource.getOutboxMsgs(this, email).size
        ImapLabelsDaoSource().updateLabelMsgsCount(this, email, label, newMsgsCount)
      }
    }
  }

  private fun prepareContentValues(msgInfo: OutgoingMessageInfo, generatedUID: Long, mimeMsg: MimeMessage,
                                   rawMsg: String, attsCacheDir: File): ContentValues {
    val contentValues = MessageDaoSource.prepareContentValues(account!!.email,
        JavaEmailConstants.FOLDER_OUTBOX, mimeMsg, generatedUID, false)
    val hasAtts = !CollectionUtils.isEmpty(msgInfo.atts) || !CollectionUtils.isEmpty(msgInfo.forwardedAtts)
    val isEncrypted = msgInfo.encryptionType === MessageEncryptionType.ENCRYPTED
    val msgStateValue = if (msgInfo.isForwarded) MessageState.NEW_FORWARDED.value else MessageState.NEW.value

    contentValues.put(MessageDaoSource.COL_RAW_MESSAGE_WITHOUT_ATTACHMENTS, rawMsg)
    contentValues.put(MessageDaoSource.COL_FLAGS, MessageFlag.SEEN.value)
    contentValues.put(MessageDaoSource.COL_IS_MESSAGE_HAS_ATTACHMENTS, hasAtts)
    contentValues.put(MessageDaoSource.COL_IS_ENCRYPTED, isEncrypted)
    contentValues.put(MessageDaoSource.COL_STATE, msgStateValue)
    contentValues.put(MessageDaoSource.COL_ATTACHMENTS_DIRECTORY, attsCacheDir.name)

    return contentValues
  }

  private fun addAttsToCache(msgInfo: OutgoingMessageInfo, uid: Long, pubKeys: List<String>?, attsCacheDir: File) {
    val attDaoSource = AttachmentDaoSource()
    val cachedAtts = ArrayList<AttachmentInfo>()

    val nodeService = NodeRetrofitHelper.getRetrofit()!!.create(NodeService::class.java)
    if (!CollectionUtils.isEmpty(msgInfo.atts)) {
      for (att in msgInfo.atts!!) {
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

          if (msgInfo.encryptionType === MessageEncryptionType.ENCRYPTED) {
            val encryptedTempFile = File(attsCacheDir, att.name!! + Constants.PGP_FILE_EXT)
            val request = EncryptFileRequest(this, origFileUri!!, att.name!!, pubKeys!!)

            val response = nodeService.encryptFile(request).execute()
            val encryptedFileResult = response.body()

            if (encryptedFileResult == null) {
              ExceptionUtil.handleError(NullPointerException("encryptedFileResult == null"))
              continue
            }

            if (encryptedFileResult.error != null) {
              ExceptionUtil.handleError(Exception(encryptedFileResult.error.msg))
              continue
            }

            val encryptedBytes = encryptedFileResult.encryptBytes
            FileUtils.writeByteArrayToFile(encryptedTempFile, encryptedBytes!!)
            val uri = FileProvider.getUriForFile(this, Constants.FILE_PROVIDER_AUTHORITY, encryptedTempFile)
            att.uri = uri
            att.name = encryptedTempFile.name
          } else {
            val cachedAtt = File(attsCacheDir, att.name)
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

    if (!CollectionUtils.isEmpty(msgInfo.forwardedAtts)) {
      for (att in msgInfo.forwardedAtts!!) {
        if (TextUtils.isEmpty(att.type)) {
          att.type = Constants.MIME_TYPE_BINARY_DATA
        }

        if (msgInfo.encryptionType === MessageEncryptionType.ENCRYPTED) {
          val encryptedAtt = att.copy(JavaEmailConstants.FOLDER_OUTBOX)
          encryptedAtt.name = encryptedAtt.name!! + Constants.PGP_FILE_EXT
          cachedAtts.add(encryptedAtt)
        } else {
          cachedAtts.add(att.copy(JavaEmailConstants.FOLDER_OUTBOX))
        }
      }
    }

    attDaoSource.addRows(this, account!!.email, JavaEmailConstants.FOLDER_OUTBOX, uid, cachedAtts)
  }

  private fun setupIfNeeded() {
    if (attsCacheDir == null) {
      attsCacheDir = File(cacheDir, Constants.ATTACHMENTS_CACHE_DIR)
      if (!attsCacheDir!!.exists()) {
        if (!attsCacheDir!!.mkdirs()) {
          throw IllegalStateException("Create cache directory " + attsCacheDir!!.name + " filed!")
        }
      }
    }
  }

  /**
   * Update the [ContactsDaoSource.COL_LAST_USE] field in the [ContactsDaoSource.TABLE_NAME_CONTACTS].
   *
   * @param msgInfo - [OutgoingMessageInfo] which contains information about an outgoing message.
   */
  private fun updateContactsLastUseDateTime(msgInfo: OutgoingMessageInfo) {
    val contactsDaoSource = ContactsDaoSource()

    for (contact in msgInfo.getAllRecipients()) {
      val updateResult = contactsDaoSource.updateLastUse(this, contact)
      if (updateResult == -1) {
        contactsDaoSource.addRow(this, PgpContact(contact, null))
      }
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
    @JvmStatic
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
