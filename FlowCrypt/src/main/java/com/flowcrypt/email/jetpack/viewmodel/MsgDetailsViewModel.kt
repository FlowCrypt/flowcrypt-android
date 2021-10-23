/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.IMAPStoreManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.MsgsCacheManager
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.gmail.api.GmaiAPIMimeMessage
import com.flowcrypt.email.api.email.javamail.CustomMimeMessage
import com.flowcrypt.email.api.email.javamail.CustomMimeMultipart
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.model.MessageFlag
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.DecryptErrorMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.MsgBlock
import com.flowcrypt.email.api.retrofit.response.model.PublicKeyMsgBlock
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.AttachmentEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.uid
import com.flowcrypt.email.jetpack.workmanager.sync.UpdateMsgsSeenStateWorker
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.pgp.PgpDecrypt
import com.flowcrypt.email.security.pgp.PgpMsg
import com.flowcrypt.email.ui.activity.SearchMessagesActivity
import com.flowcrypt.email.util.CacheManager
import com.flowcrypt.email.util.FileAndDirectoryUtils
import com.flowcrypt.email.util.cache.DiskLruCache
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.SyncTaskTerminatedException
import com.sun.mail.imap.IMAPBodyPart
import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.IMAPMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.*
import javax.mail.BodyPart
import javax.mail.FetchProfile
import javax.mail.Folder
import javax.mail.Multipart
import javax.mail.Store
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage

/**
 * @author Denis Bondarenko
 *         Date: 12/26/19
 *         Time: 4:45 PM
 *         E-mail: DenBond7@gmail.com
 */
class MsgDetailsViewModel(
  val localFolder: LocalFolder,
  val messageEntity: MessageEntity,
  application: Application
) : AccountViewModel(application) {
  private val keysStorage: KeysStorageImpl = KeysStorageImpl.getInstance(application)

  private var msgSize: Int = 0
  private var downloadedMsgSize: Int = 0
  private var lastPercentage = 0
  private var currentPercentage = 0
  private var lastUpdateTime = System.currentTimeMillis()

  val passphraseNeededLiveData: MutableLiveData<List<String>> = MutableLiveData()

  val freshMsgLiveData: LiveData<MessageEntity?> = roomDatabase.msgDao().getMsgLiveData(
    account = messageEntity.email,
    folder = messageEntity.folder,
    uid = messageEntity.uid
  )

  private val initMsgLiveData: LiveData<MessageEntity?> = liveData {
    emit(
      roomDatabase.msgDao().getMsgSuspend(
        account = messageEntity.email,
        folder = messageEntity.folder,
        uid = messageEntity.uid
      )
    )
  }

  private val afterKeysStorageUpdatedMsgLiveData: MediatorLiveData<MessageEntity?> =
    MediatorLiveData()

  private val afterKeysUpdatedMsgLiveData: LiveData<MessageEntity?> =
    keysStorage.secretKeyRingsLiveData.switchMap {
      liveData {
        if (it.isNotEmpty()) {
          emit(
            roomDatabase.msgDao().getMsgSuspend(
              account = messageEntity.email,
              folder = messageEntity.folder,
              uid = messageEntity.uid
            )
          )
        }
      }
    }

  private val afterPassphrasesUpdatedMsgLiveData: LiveData<MessageEntity?> =
    keysStorage.passphrasesUpdatesLiveData.switchMap {
      liveData {
        emit(
          roomDatabase.msgDao().getMsgSuspend(
            account = messageEntity.email,
            folder = messageEntity.folder,
            uid = messageEntity.uid
          )
        )
      }
    }

  private val mediatorMsgLiveData: MediatorLiveData<MessageEntity?> = MediatorLiveData()

  private val processingMsgLiveData =
    MediatorLiveData<Result<PgpMsg.ProcessedMimeMessageResult?>>()
  private val processingProgressLiveData =
    MutableLiveData<Result<PgpMsg.ProcessedMimeMessageResult?>>()
  private val processingOutgoingMsgLiveData: LiveData<Result<PgpMsg.ProcessedMimeMessageResult?>> =
    mediatorMsgLiveData.switchMap { messageEntity ->
      liveData {
        if (messageEntity?.isOutboxMsg() == true) {
          emit(Result.loading())
          emit(Result.loading(resultCode = R.id.progress_id_processing, progress = 70.toDouble()))
          val processingResult =
            processingByteArray(messageEntity.rawMessageWithoutAttachments?.toByteArray())
          emit(Result.loading(resultCode = R.id.progress_id_processing, progress = 90.toDouble()))
          emit(processingResult)
        }
      }
    }

  private val processingNonOutgoingMsgLiveData: LiveData<Result<PgpMsg.ProcessedMimeMessageResult?>> =
    mediatorMsgLiveData.switchMap { messageEntity ->
      liveData {
        if (messageEntity?.isOutboxMsg() == false) {
          emit(Result.loading())
          val existedMsgSnapshot = MsgsCacheManager.getMsgSnapshot(messageEntity.id.toString())
          if (existedMsgSnapshot != null) {
            emit(Result.loading(resultCode = R.id.progress_id_processing, progress = 70.toDouble()))
            setSeenStatusInternal(msgEntity = messageEntity, isSeen = true, usePending = true)
            UpdateMsgsSeenStateWorker.enqueue(application)
            val processingResult = processingMsgSnapshot(existedMsgSnapshot)
            emit(Result.loading(resultCode = R.id.progress_id_processing, progress = 90.toDouble()))
            emit(processingResult)
          } else {
            emit(Result.loading(resultCode = R.id.progress_id_connecting, progress = 5.toDouble()))
            val newMsgSnapshot = try {
              loadMessageFromServer(messageEntity)
            } catch (e: Exception) {
              e.printStackTrace()
              emit(Result.exception(e))
              return@liveData
            }
            setSeenStatusInternal(msgEntity = messageEntity, isSeen = true)
            emit(Result.loading(resultCode = R.id.progress_id_processing, progress = 70.toDouble()))
            val processingResult = processingMsgSnapshot(newMsgSnapshot)
            emit(Result.loading(resultCode = R.id.progress_id_processing, progress = 90.toDouble()))
            emit(processingResult)
          }
        }
      }
    }

  val incomingMessageInfoLiveData: LiveData<Result<IncomingMessageInfo>> =
    processingMsgLiveData.switchMap {
      liveData {
        val context: Context = getApplication()
        val result = when (it.status) {
          Result.Status.LOADING -> {
            Result.loading(
              requestCode = it.requestCode,
              resultCode = it.resultCode,
              progressMsg = it.progressMsg,
              progress = it.progress
            )
          }

          Result.Status.SUCCESS -> {
            val processedMimeMessageResult = it.data
            if (processedMimeMessageResult != null) {
              try {
                val msgInfo = IncomingMessageInfo(
                  msgEntity = messageEntity,
                  text = processedMimeMessageResult.text,
                  //subject = parseDecryptedMsgResult.subject,
                  msgBlocks = processedMimeMessageResult.blocks,
                  encryptionType = if (processedMimeMessageResult.isReplyEncrypted) {
                    MessageEncryptionType.ENCRYPTED
                  } else {
                    MessageEncryptionType.STANDARD
                  }
                )
                Result.success(requestCode = it.requestCode, data = msgInfo)
              } catch (e: Exception) {
                Result.exception(requestCode = it.requestCode, throwable = e)
              }
            } else {
              Result.exception(
                requestCode = it.requestCode,
                throwable = IllegalStateException(context.getString(R.string.unknown_error))
              )
            }
          }

          Result.Status.ERROR -> {
            Result.exception(
              requestCode = it.requestCode,
              throwable = RuntimeException()
            )
          }

          Result.Status.EXCEPTION -> {
            Result.exception(
              requestCode = it.requestCode, throwable = it.exception
                ?: IllegalStateException(context.getString(R.string.unknown_error))
            )
          }

          Result.Status.NONE -> {
            Result.none()
          }
        }

        emit(result)
      }
    }

  val msgStatesLiveData = MutableLiveData<MessageState>()
  val attsLiveData = roomDatabase.attachmentDao().getAttachmentsLD(
    account = messageEntity.email,
    label = messageEntity.folder,
    uid = messageEntity.uid
  )

  init {
    afterKeysStorageUpdatedMsgLiveData.addSource(afterKeysUpdatedMsgLiveData) {
      afterKeysStorageUpdatedMsgLiveData.value = it
    }
    afterKeysStorageUpdatedMsgLiveData.addSource(afterPassphrasesUpdatedMsgLiveData) {
      afterKeysStorageUpdatedMsgLiveData.value = it
    }

    mediatorMsgLiveData.addSource(initMsgLiveData) { mediatorMsgLiveData.value = it }
    //here we resolve a situation when a user updates private keys.
    // To prevent errors we skip the first call
    mediatorMsgLiveData.addSource(
      afterKeysStorageUpdatedMsgLiveData,
      object : Observer<MessageEntity?> {
        var isFirstCall = true
        override fun onChanged(messageEntity: MessageEntity?) {
          if (isFirstCall) {
            isFirstCall = false
          } else {
            mediatorMsgLiveData.value = messageEntity
          }
        }
      })

    processingMsgLiveData.addSource(processingProgressLiveData) { processingMsgLiveData.value = it }
    processingMsgLiveData.addSource(processingOutgoingMsgLiveData) {
      processingMsgLiveData.value = it
    }
    processingMsgLiveData.addSource(processingNonOutgoingMsgLiveData) {
      processingMsgLiveData.value = it
    }
  }

  fun changeMsgState(newMsgState: MessageState) {
    val freshMsgEntity = mediatorMsgLiveData.value
    freshMsgEntity?.let { msgEntity ->
      viewModelScope.launch {
        val candidate: MessageEntity = when (newMsgState) {
          MessageState.PENDING_MARK_READ -> {
            msgEntity.copy(
              state = newMsgState.value,
              flags = if (msgEntity.flags?.contains(MessageFlag.SEEN.value) == true) {
                msgEntity.flags
              } else {
                msgEntity.flags?.plus("${MessageFlag.SEEN.value} ")
              }
            )
          }

          MessageState.PENDING_MARK_UNREAD -> {
            msgEntity.copy(
              state = newMsgState.value,
              flags = msgEntity.flags?.replace(MessageFlag.SEEN.value, "")
            )
          }

          else -> {
            msgEntity.copy(state = newMsgState.value)
          }
        }

        roomDatabase.msgDao().updateSuspend(candidate)
        msgStatesLiveData.postValue(newMsgState)
      }
    }
  }

  fun deleteMsg() {
    val freshMsgEntity = mediatorMsgLiveData.value
    freshMsgEntity?.let { msgEntity ->
      viewModelScope.launch {
        roomDatabase.msgDao().deleteSuspend(msgEntity)

        val accountEntity = getActiveAccountSuspend() ?: return@launch

        if (JavaEmailConstants.FOLDER_OUTBOX.equals(localFolder.fullName, ignoreCase = true)) {
          val outgoingMsgCount = roomDatabase.msgDao().getOutboxMsgsSuspend(msgEntity.email).size
          val outboxLabel = roomDatabase.labelDao().getLabelSuspend(
            account = accountEntity.email,
            accountType = accountEntity.accountType,
            label = JavaEmailConstants.FOLDER_OUTBOX
          )

          outboxLabel?.let {
            roomDatabase.labelDao().updateSuspend(it.copy(messagesTotal = outgoingMsgCount))
          }
        }
      }
    }
  }

  fun fetchAttachments() {
    viewModelScope.launch {
      val accountEntity = getActiveAccountSuspend() ?: return@launch
      if (accountEntity.useAPI) {
        if (accountEntity.accountType == AccountEntity.ACCOUNT_TYPE_GOOGLE) {
          fetchAttachmentsInternal(accountEntity)
        }
      } else {
        IMAPStoreManager.activeConnections[accountEntity.id]?.store?.let { store ->
          fetchAttachmentsInternal(accountEntity, store)
        }
      }
    }
  }

  private suspend fun processingMsgSnapshot(msgSnapshot: DiskLruCache.Snapshot):
      Result<PgpMsg.ProcessedMimeMessageResult?> = withContext(Dispatchers.IO) {
    val uri = msgSnapshot.getUri(0)
    passphraseNeededLiveData.postValue(emptyList())
    if (uri != null) {
      val context: Context = getApplication()
      try {
        FileAndDirectoryUtils.cleanDir(CacheManager.getCurrentMsgTempDir())

        val inputStream =
          context.contentResolver.openInputStream(uri) ?: throw java.lang.IllegalStateException()

        val processedMimeMessage = PgpMsg.processMimeMessage(
          context = getApplication(),
          inputStream = KeyStoreCryptoManager.getCipherInputStream(inputStream)
        )
        preResultsProcessing(processedMimeMessage.blocks)
        return@withContext Result.success(processedMimeMessage)
      } catch (e: Exception) {
        return@withContext Result.exception(e)
      }
    } else {
      val byteArray = msgSnapshot.getByteArray(0)
      return@withContext processingByteArray(byteArray)
    }
  }

  private suspend fun processingByteArray(rawMimeBytes: ByteArray?):
      Result<PgpMsg.ProcessedMimeMessageResult?> = withContext(Dispatchers.IO) {
    return@withContext if (rawMimeBytes == null) {
      Result.exception(throwable = IllegalArgumentException("empty byte array"))
    } else {
      try {
        FileAndDirectoryUtils.cleanDir(CacheManager.getCurrentMsgTempDir())

        val processedMimeMessageResult =
          PgpMsg.processMimeMessage(getApplication(), rawMimeBytes.inputStream())
        preResultsProcessing(processedMimeMessageResult.blocks)
        return@withContext Result.success(processedMimeMessageResult)
      } catch (e: Exception) {
        return@withContext Result.exception(throwable = e)
      }
    }
  }

  private suspend fun preResultsProcessing(blocks: List<MsgBlock>) {
    for (block in blocks) {
      if (block is PublicKeyMsgBlock) {
        val keyDetails = block.keyDetails ?: continue
        val pgpContact = keyDetails.primaryPgpContact
        val contactEntity = roomDatabase.contactsDao().getContactByEmailSuspend(pgpContact.email)
        //block.existingPgpContact = contactEntity?.toPgpContact()
      }

      if (block is DecryptErrorMsgBlock) {
        if (block.decryptErr?.details?.type == PgpDecrypt.DecryptionErrorType.NEED_PASSPHRASE) {
          val fingerprints = block.decryptErr.fingerprints ?: emptyList()
          if (fingerprints.isEmpty()) {
            ExceptionUtil.handleError(IllegalStateException("Fingerprints were not provided"))
          } else {
            passphraseNeededLiveData.postValue(fingerprints)
          }
        }
      }
    }
  }

  private suspend fun loadMessageFromServer(messageEntity: MessageEntity): DiskLruCache.Snapshot =
    withContext(Dispatchers.IO) {
      val accountEntity = getActiveAccountSuspend()
        ?: throw java.lang.NullPointerException("Account is null")
      val context: Context = getApplication()
      if (accountEntity.useAPI) {
        if (accountEntity.accountType == AccountEntity.ACCOUNT_TYPE_GOOGLE) {
          val result = GmailApiHelper.executeWithResult {
            val msgFullInfo = GmailApiHelper.loadMsgFullInfoSuspend(
              getApplication(),
              accountEntity, messageEntity.uidAsHEX, null
            )
            msgSize = msgFullInfo.sizeEstimate
            val originalMsg = GmaiAPIMimeMessage(
              message = msgFullInfo,
              context = getApplication(),
              accountEntity = accountEntity
            )
            if (originalMsg.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
              MsgsCacheManager.storeMsg(messageEntity.id.toString(), originalMsg)
            } else {
              val inputStream = FetchingInputStream(
                GmailApiHelper.getWholeMimeMessageInputStream(
                  getApplication(),
                  accountEntity,
                  messageEntity
                )
              )
              MsgsCacheManager.storeMsg(messageEntity.id.toString(), inputStream)
            }

            Result.success(null)
          }
          if (result.status == Result.Status.SUCCESS) {
            return@withContext MsgsCacheManager.getMsgSnapshot(messageEntity.id.toString())
              ?: throw java.lang.NullPointerException("Message not found in the local cache")
          } else throw result.exception ?: java.lang.IllegalStateException(
            context.getString(
              R
                .string.unknown_error
            )
          )
        }
      }

      val connection = IMAPStoreManager.activeConnections[accountEntity.id]
      if (connection == null) {
        throw java.lang.NullPointerException("There is no active connection for ${accountEntity.email}")
      } else {
        return@withContext connection.execute { store ->
          store.getFolder(localFolder.fullName).use {
            val imapFolder = it as IMAPFolder
            processingProgressLiveData.postValue(
              Result.loading(
                resultCode = R.id.progress_id_connecting,
                progress = 10.toDouble()
              )
            )
            imapFolder.open(Folder.READ_WRITE)
            processingProgressLiveData.postValue(
              Result.loading(
                resultCode = R.id.progress_id_connecting,
                progress = 20.toDouble()
              )
            )

            val originalMsg = imapFolder.getMessageByUID(messageEntity.uid) as? MimeMessage
              ?: throw java.lang.NullPointerException("Message not found")

            val fetchProfile = FetchProfile()
            fetchProfile.add(FetchProfile.Item.SIZE)
            fetchProfile.add(FetchProfile.Item.CONTENT_INFO)
            fetchProfile.add(IMAPFolder.FetchProfileItem.HEADERS)
            imapFolder.fetch(arrayOf(originalMsg), fetchProfile)

            msgSize = originalMsg.size

            if (originalMsg.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
              val rawHeaders = TextUtils.join("\n", Collections.list(originalMsg.allHeaderLines))
              if (rawHeaders.isNotEmpty()) downloadedMsgSize += rawHeaders.length
              val customMsg = CustomMimeMessage(connection.session, rawHeaders)

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

              MsgsCacheManager.storeMsg(messageEntity.id.toString(), customMsg)
            } else {
              val cachedMsg = MimeMessage(
                originalMsg.session,
                FetchingInputStream((originalMsg as IMAPMessage).mimeStream)
              )
              MsgsCacheManager.storeMsg(messageEntity.id.toString(), cachedMsg)
            }

            processingProgressLiveData.postValue(
              Result.loading(
                resultCode = R.id.progress_id_fetching_message,
                progress = 60.toDouble()
              )
            )

            return@execute MsgsCacheManager.getMsgSnapshot(messageEntity.id.toString())
              ?: throw java.lang.NullPointerException("Message not found in the local cache")
          }
        }
      }
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
          if (EmailUtil.isPartAllowed(item)) {
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
          if (EmailUtil.isPartAllowed(item)) {
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

  private fun sendProgress() {
    if (msgSize > 0) {
      currentPercentage = (downloadedMsgSize * 100 / msgSize)
      val isUpdateNeeded =
        System.currentTimeMillis() - lastUpdateTime >= MIN_UPDATE_PROGRESS_INTERVAL
      if (currentPercentage - lastPercentage >= 1 && isUpdateNeeded) {
        lastPercentage = currentPercentage
        lastUpdateTime = System.currentTimeMillis()
        val value = (currentPercentage * 40 / 100) + 20
        processingProgressLiveData.postValue(
          Result.loading(
            resultCode = R.id.progress_id_fetching_message,
            progress = value.toDouble()
          )
        )
      }
    }
  }

  private suspend fun setSeenStatusInternal(
    msgEntity: MessageEntity,
    isSeen: Boolean,
    usePending: Boolean = false
  ) {
    roomDatabase.msgDao().updateSuspend(
      msgEntity.copy(
        state = if (usePending) {
          if (isSeen) MessageState.PENDING_MARK_READ.value else MessageState.PENDING_MARK_UNREAD.value
        } else msgEntity.state,
        flags = if (isSeen) {
          if (msgEntity.flags?.contains(MessageFlag.SEEN.value) == true) {
            msgEntity.flags
          } else {
            msgEntity.flags?.plus("${MessageFlag.SEEN.value} ")
          }
        } else {
          msgEntity.flags?.replace(MessageFlag.SEEN.value, "")
        }
      )
    )
  }

  private suspend fun fetchAttachmentsInternal(accountEntity: AccountEntity, store: Store) =
    withContext(Dispatchers.IO) {
      try {
        store.getFolder(localFolder.fullName).use { folder ->
          val imapFolder = (folder as IMAPFolder).apply { open(Folder.READ_ONLY) }
          val msg = imapFolder.getMessageByUID(messageEntity.uid) as? MimeMessage
            ?: return@withContext

          val fetchProfile = FetchProfile()
          fetchProfile.add(FetchProfile.Item.SIZE)
          fetchProfile.add(FetchProfile.Item.CONTENT_INFO)
          imapFolder.fetch(arrayOf(msg), fetchProfile)

          val msgUid = messageEntity.uid
          val attachments = EmailUtil.getAttsInfoFromPart(msg).mapNotNull {
            AttachmentEntity.fromAttInfo(it.apply {
              email = accountEntity.email
              this.folder =
                if (localFolder.searchQuery.isNullOrEmpty()) localFolder.fullName else SearchMessagesActivity.SEARCH_FOLDER_NAME
              uid = msgUid
            })
          }

          FlowCryptRoomDatabase.getDatabase(getApplication()).attachmentDao()
            .insertWithReplaceSuspend(attachments)
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }

  private suspend fun fetchAttachmentsInternal(accountEntity: AccountEntity) =
    withContext(Dispatchers.IO) {
      try {
        val msg = GmailApiHelper.loadMsgFullInfoSuspend(
          getApplication(),
          accountEntity,
          messageEntity.uidAsHEX
        )
        val attachments = GmailApiHelper.getAttsInfoFromMessagePart(msg.payload).mapNotNull {
          AttachmentEntity.fromAttInfo(it.apply {
            this.email = accountEntity.email
            this.folder = localFolder.fullName
            this.uid = msg.uid
          })
        }
        FlowCryptRoomDatabase.getDatabase(getApplication()).attachmentDao()
          .insertWithReplaceSuspend(attachments)
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }

  /**
   * This class will be used to identify the fetching progress.
   */
  inner class FetchingInputStream(val stream: InputStream) : BufferedInputStream(stream) {
    override fun read(b: ByteArray, off: Int, len: Int): Int {
      if (!viewModelScope.isActive) {
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
  }
}
