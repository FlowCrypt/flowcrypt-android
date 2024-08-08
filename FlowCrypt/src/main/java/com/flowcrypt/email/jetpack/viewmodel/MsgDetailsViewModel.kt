/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import android.text.TextUtils
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.IMAPStoreManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.MsgsCacheManager
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.gmail.api.GmaiAPIMimeMessage
import com.flowcrypt.email.api.email.javamail.CustomMimeMessage
import com.flowcrypt.email.api.email.javamail.CustomMimeMultipart
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.model.MessageFlag
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.DecryptErrorMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.MsgBlock
import com.flowcrypt.email.api.retrofit.response.model.PublicKeyMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.VerificationResult
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.AttachmentEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.jakarta.mail.isOpenPGPMimeSigned
import com.flowcrypt.email.extensions.uid
import com.flowcrypt.email.jetpack.livedata.SkipInitialValueObserver
import com.flowcrypt.email.jetpack.workmanager.sync.UpdateMsgsSeenStateWorker
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.security.pgp.PgpMsg
import com.flowcrypt.email.ui.adapter.GmailApiLabelsListAdapter
import com.flowcrypt.email.util.OutgoingMessagesManager
import com.flowcrypt.email.util.cache.DiskLruCache
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.SyncTaskTerminatedException
import org.eclipse.angus.mail.imap.IMAPBodyPart
import org.eclipse.angus.mail.imap.IMAPFolder
import org.eclipse.angus.mail.imap.IMAPMessage
import jakarta.mail.BodyPart
import jakarta.mail.FetchProfile
import jakarta.mail.Folder
import jakarta.mail.Multipart
import jakarta.mail.Store
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pgpainless.PGPainless
import org.pgpainless.key.protection.PasswordBasedSecretKeyRingProtector
import org.pgpainless.util.Passphrase
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.Collections
import java.util.LinkedList

/**
 * @author Denys Bondarenko
 */
class MsgDetailsViewModel(
  val localFolder: LocalFolder,
  val messageEntity: MessageEntity,
  application: Application
) : AccountViewModel(application), DefaultLifecycleObserver {
  private val keysStorage: KeysStorageImpl = KeysStorageImpl.getInstance(application)

  private var msgSize: Int = 0
  private var downloadedMsgSize: Int = 0
  private var lastPercentage = 0
  private var currentPercentage = 0
  private var lastUpdateTime = System.currentTimeMillis()

  val passphraseNeededLiveData: MutableLiveData<List<String>> = MutableLiveData()
  val mediatorMsgLiveData: MediatorLiveData<MessageEntity?> = MediatorLiveData()
  private val internalMessageProcessingTriggerLiveData = MediatorLiveData<MessageEntity?>()

  @Volatile
  private var hasAbilityToChangeSeenStatus: Boolean = false

  private val freshMsgLiveData: LiveData<MessageEntity?> =
    roomDatabase.msgDao().getMsgLiveDataById(messageEntity.id ?: -1)

  private val afterKeysStorageUpdatedMsgLiveData: MediatorLiveData<MessageEntity?> =
    MediatorLiveData()

  private val afterKeysUpdatedMsgLiveData: LiveData<MessageEntity?> =
    keysStorage.secretKeyRingsLiveData.switchMap {
      liveData {
        if (it.isNotEmpty()) {
          emit(
            roomDatabase.msgDao().getMsgSuspend(
              account = messageEntity.account,
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
            account = messageEntity.account,
            folder = messageEntity.folder,
            uid = messageEntity.uid
          )
        )
      }
    }
  private val processingMsgLiveData =
    MediatorLiveData<Result<PgpMsg.ProcessedMimeMessageResult?>>()
  private val processingProgressLiveData =
    MutableLiveData<Result<PgpMsg.ProcessedMimeMessageResult?>>()
  private val processingOutgoingMsgLiveData: LiveData<Result<PgpMsg.ProcessedMimeMessageResult?>> =
    internalMessageProcessingTriggerLiveData.switchMap { messageEntity ->
      liveData {
        if (messageEntity?.isOutboxMsg == true) {
          emit(Result.loading())
          emit(Result.loading(resultCode = R.id.progress_id_processing, progress = 70.toDouble()))
          val byteArray = OutgoingMessagesManager.getOutgoingMessageFromFile(
            application,
            requireNotNull(messageEntity.id)
          )
          val processingResult = processingByteArray(byteArray)
          emit(Result.loading(resultCode = R.id.progress_id_processing, progress = 90.toDouble()))
          emit(processingResult)
        }
      }
    }

  private val processingNonOutgoingMsgLiveData: LiveData<Result<PgpMsg.ProcessedMimeMessageResult?>> =
    internalMessageProcessingTriggerLiveData.switchMap { messageEntity ->
      liveData {
        if (messageEntity?.isOutboxMsg == false) {
          emit(Result.loading())
          val existedMsgSnapshot = MsgsCacheManager.getMsgSnapshot(messageEntity.id.toString())
          if (existedMsgSnapshot != null) {
            emit(Result.loading(resultCode = R.id.progress_id_processing, progress = 70.toDouble()))
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
            if (hasAbilityToChangeSeenStatus) {
              setSeenStatusInternal(msgEntity = messageEntity, isSeen = true)
            }
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
                  msgEntity = mediatorMsgLiveData.value ?: messageEntity,
                  localFolder = localFolder,
                  text = processedMimeMessageResult.text,
                  inlineSubject = processedMimeMessageResult.blocks.firstOrNull {
                    it.type == MsgBlock.Type.ENCRYPTED_SUBJECT
                  }?.content?.let {
                    context.getString(R.string.encrypted_subject_template, it)
                  },
                  msgBlocks = processedMimeMessageResult.blocks,
                  encryptionType = if (processedMimeMessageResult.verificationResult.hasEncryptedParts) {
                    MessageEncryptionType.ENCRYPTED
                  } else {
                    MessageEncryptionType.STANDARD
                  },
                  verificationResult = processedMimeMessageResult.verificationResult
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
  private val inlinedAttachmentsMutableStateFlow: MutableStateFlow<List<AttachmentInfo>> =
    MutableStateFlow(emptyList())
  private val inlinedAttachmentsStateFlow: StateFlow<List<AttachmentInfo>> =
    inlinedAttachmentsMutableStateFlow.asStateFlow()

  @OptIn(ExperimentalCoroutinesApi::class)
  private val separatedAttachmentsFlow = roomDatabase.attachmentDao().getAttachmentsFlow(
    account = messageEntity.account,
    accountType = messageEntity.accountType,
    label = messageEntity.folder,
    uid = messageEntity.uid
  ).mapLatest { list ->
    list.map {
      if (localFolder.searchQuery.isNullOrEmpty()) {
        it.toAttInfo()
      } else {
        it.toAttInfo().copy(folder = localFolder.fullName)
      }
    }.filterNot { it.isHidden() }.toMutableList()
  }

  val attachmentsFlow = combine(separatedAttachmentsFlow, inlinedAttachmentsStateFlow) { a, b ->
    a + b
  }

  private val controlledRunnerForSignaturesReverification =
    ControlledRunner<Result<VerificationResult>>()
  private val reVerifySignaturesMutableStateFlow: MutableStateFlow<Result<VerificationResult>> =
    MutableStateFlow(Result.none())
  val reVerifySignaturesStateFlow: StateFlow<Result<VerificationResult>> =
    reVerifySignaturesMutableStateFlow.asStateFlow()

  @OptIn(ExperimentalCoroutinesApi::class)
  val messageGmailApiLabelsFlow: Flow<List<GmailApiLabelsListAdapter.Label>> =
    merge(
      freshMsgLiveData.asFlow().mapLatest { latestMessageEntityRecord ->
        val activeAccount = getActiveAccountSuspend()
        if (activeAccount?.isGoogleSignInAccount == true) {
          val labelEntities =
            roomDatabase.labelDao().getLabelsSuspend(activeAccount.email, activeAccount.accountType)
          MessageEntity.generateColoredLabels(
            latestMessageEntityRecord?.labelIds?.split(MessageEntity.LABEL_IDS_SEPARATOR),
            labelEntities
          )
        } else {
          emptyList()
        }
      },
      activeAccountLiveData.asFlow().mapLatest { account ->
        if (account?.isGoogleSignInAccount == true) {
          val labelEntities =
            roomDatabase.labelDao().getLabelsSuspend(account.email, account.accountType)
          val freshestMessageEntity = roomDatabase.msgDao().getMsgById(messageEntity.id ?: -1)
          val cachedLabelIds =
            freshestMessageEntity?.labelIds?.split(MessageEntity.LABEL_IDS_SEPARATOR)
          try {
            val message = GmailApiHelper.loadMsgInfoSuspend(
              context = getApplication(),
              accountEntity = account,
              msgId = messageEntity.uidAsHEX,
              fields = null,
              format = GmailApiHelper.MESSAGE_RESPONSE_FORMAT_MINIMAL
            )

            val latestLabelIds = message.labelIds
            if (cachedLabelIds == null
              || !(latestLabelIds.containsAll(cachedLabelIds)
                  && cachedLabelIds.containsAll(latestLabelIds))
            ) {
              freshestMessageEntity?.copy(
                labelIds = latestLabelIds.joinToString(MessageEntity.LABEL_IDS_SEPARATOR)
              )?.let { roomDatabase.msgDao().updateSuspend(it) }
            }
            MessageEntity.generateColoredLabels(latestLabelIds, labelEntities)
          } catch (e: Exception) {
            MessageEntity.generateColoredLabels(cachedLabelIds, labelEntities)
          }
        } else {
          emptyList()
        }
      })

  @OptIn(ExperimentalCoroutinesApi::class)
  val messageActionsAvailabilityStateFlow =
    freshMsgLiveData.asFlow().mapLatest { entity ->
      val activeAccount = getActiveAccountSuspend()
        ?: return@mapLatest MessageAction.entries.associateBy({ it }, { false })
      val foldersManager = FoldersManager.fromDatabaseSuspend(getApplication(), activeAccount)
      MessageAction.entries.associateBy({ it }, { false }).toMutableMap().apply {
        val folderType = foldersManager.getFolderByFullName(entity?.folder)?.getFolderType()
        if (activeAccount.isGoogleSignInAccount) {
          val labelIds = entity?.labelIds?.split(MessageEntity.LABEL_IDS_SEPARATOR).orEmpty()
          this[MessageAction.ARCHIVE] = labelIds.contains(JavaEmailConstants.FOLDER_INBOX)
          this[MessageAction.MOVE_TO_INBOX] =
            folderType !in setOf(FoldersManager.FolderType.OUTBOX, FoldersManager.FolderType.SPAM)
                && !labelIds.contains(JavaEmailConstants.FOLDER_INBOX)
          this[MessageAction.CHANGE_LABELS] = folderType != FoldersManager.FolderType.OUTBOX
          this[MessageAction.MARK_AS_NOT_SPAM] =
            folderType in setOf(FoldersManager.FolderType.JUNK, FoldersManager.FolderType.SPAM)
        } else {
          this[MessageAction.MOVE_TO_INBOX] = folderType !in listOf(
            FoldersManager.FolderType.TRASH,
            FoldersManager.FolderType.DRAFTS,
            FoldersManager.FolderType.OUTBOX,
          )
        }
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = MessageAction.entries.associateBy({ it }, { false })
    )

  init {
    afterKeysStorageUpdatedMsgLiveData.addSource(
      afterKeysUpdatedMsgLiveData,
      SkipInitialValueObserver {
        afterKeysStorageUpdatedMsgLiveData.value = it
      })

    afterKeysStorageUpdatedMsgLiveData.addSource(
      afterPassphrasesUpdatedMsgLiveData,
      SkipInitialValueObserver {
        afterKeysStorageUpdatedMsgLiveData.value = it
      }
    )

    mediatorMsgLiveData.addSource(freshMsgLiveData.distinctUntilChanged()) {
      mediatorMsgLiveData.value = it
    }
    mediatorMsgLiveData.addSource(afterKeysStorageUpdatedMsgLiveData) {
      mediatorMsgLiveData.value = it
    }

    internalMessageProcessingTriggerLiveData.addSource(liveData { emit(messageEntity) }) {
      internalMessageProcessingTriggerLiveData.value = it
    }
    internalMessageProcessingTriggerLiveData.addSource(afterKeysStorageUpdatedMsgLiveData) {
      internalMessageProcessingTriggerLiveData.value = it
    }

    processingMsgLiveData.addSource(processingProgressLiveData) { processingMsgLiveData.value = it }
    processingMsgLiveData.addSource(processingOutgoingMsgLiveData) {
      processingMsgLiveData.value = it
    }
    processingMsgLiveData.addSource(processingNonOutgoingMsgLiveData) {
      processingMsgLiveData.value = it
    }
  }

  override fun onResume(owner: LifecycleOwner) {
    super.onResume(owner)
    hasAbilityToChangeSeenStatus = true
    MsgsCacheManager.getMsgSnapshot(messageEntity.id.toString())?.let {
      viewModelScope.launch {
        setSeenStatusInternal(
          msgEntity = messageEntity,
          isSeen = true,
          usePending = true
        )
      }
    }
  }

  override fun onPause(owner: LifecycleOwner) {
    super.onPause(owner)
    hasAbilityToChangeSeenStatus = false
  }

  fun updateInlinedAttachments(attachments: List<AttachmentInfo>) {
    inlinedAttachmentsMutableStateFlow.update { attachments }
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
          val outgoingMsgCount = roomDatabase.msgDao().getOutboxMsgsSuspend(msgEntity.account).size
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
        IMAPStoreManager.getConnection(accountEntity.id)?.store?.let { store ->
          fetchAttachmentsInternal(accountEntity, store)
        }
      }
    }
  }

  fun reVerifySignatures() {
    viewModelScope.launch {
      reVerifySignaturesMutableStateFlow.value = Result.loading()
      reVerifySignaturesMutableStateFlow.value =
        controlledRunnerForSignaturesReverification.cancelPreviousThenRun {
          return@cancelPreviousThenRun reVerifySignaturesInternal()
        }
    }
  }

  fun getMessageActionAvailability(messageAction: MessageAction): Boolean {
    return messageActionsAvailabilityStateFlow.value[messageAction] ?: false
  }

  private suspend fun reVerifySignaturesInternal(): Result<VerificationResult> =
    withContext(Dispatchers.IO) {
      try {
        val existedMsgSnapshot =
          requireNotNull(MsgsCacheManager.getMsgSnapshot(messageEntity.id.toString()))
        val verificationResult =
          requireNotNull(processingMsgSnapshot(existedMsgSnapshot).data?.verificationResult)
        return@withContext Result.success(verificationResult)
      } catch (e: Exception) {
        return@withContext Result.exception(e)
      }
    }

  private suspend fun processingMsgSnapshot(msgSnapshot: DiskLruCache.Snapshot):
      Result<PgpMsg.ProcessedMimeMessageResult?> = withContext(Dispatchers.IO) {
    val uri = msgSnapshot.getUri(0)
    passphraseNeededLiveData.postValue(emptyList())
    val accountEntity = getActiveAccountSuspend()
      ?: throw java.lang.NullPointerException("Account is null")
    if (uri != null) {
      val context: Context = getApplication()
      try {
        val inputStream =
          context.contentResolver.openInputStream(uri) ?: throw java.lang.IllegalStateException()

        val keys = PGPainless.readKeyRing()
          .secretKeyRingCollection(accountEntity.servicePgpPrivateKey)

        val decryptionStream = PgpDecryptAndOrVerify.genDecryptionStream(
          srcInputStream = inputStream,
          secretKeys = keys,
          protector = PasswordBasedSecretKeyRingProtector.forKey(
            keys.first(),
            Passphrase.fromPassword(accountEntity.servicePgpPassphrase)
          )
        )

        val processedMimeMessage = PgpMsg.processMimeMessage(
          context = getApplication(),
          inputStream = decryptionStream
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
      when (block) {
        is PublicKeyMsgBlock -> {
          val keyDetails = block.keyDetails ?: continue
          val recipient = keyDetails.mimeAddresses.firstOrNull()?.address ?: continue
          block.existingRecipientWithPubKeys =
            roomDatabase.recipientDao().getRecipientWithPubKeysByEmailSuspend(recipient)
          try {
            block.existingRecipientWithPubKeys?.publicKeys?.forEach {
              it.pgpKeyRingDetails =
                PgpKey.parseKeys(source = it.publicKey).pgpKeyDetailsList.firstOrNull()
            }
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }

        is DecryptErrorMsgBlock -> {
          if (block.decryptErr?.details?.type == PgpDecryptAndOrVerify.DecryptionErrorType.NEED_PASSPHRASE) {
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
  }

  private suspend fun loadMessageFromServer(messageEntity: MessageEntity): DiskLruCache.Snapshot =
    withContext(Dispatchers.IO) {
      val accountEntity = getActiveAccountSuspend()
        ?: throw java.lang.NullPointerException("Account is null")
      val context: Context = getApplication()
      if (accountEntity.useAPI) {
        if (accountEntity.accountType == AccountEntity.ACCOUNT_TYPE_GOOGLE) {
          val result = GmailApiHelper.executeWithResult {
            val msgFullInfo = GmailApiHelper.loadMsgInfoSuspend(
              context = getApplication(),
              accountEntity = accountEntity,
              msgId = messageEntity.uidAsHEX,
              fields = null,
              format = GmailApiHelper.MESSAGE_RESPONSE_FORMAT_FULL
            )
            msgSize = msgFullInfo.sizeEstimate
            val originalMsg = GmaiAPIMimeMessage(
              message = msgFullInfo,
              context = getApplication(),
              accountEntity = accountEntity
            )
            if (originalMsg.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
              MsgsCacheManager.storeMsg(
                key = messageEntity.id.toString(),
                msg = originalMsg,
                accountEntity = accountEntity
              )
            } else {
              val inputStream = FetchingInputStream(
                GmailApiHelper.getWholeMimeMessageInputStream(
                  context = getApplication(),
                  account = accountEntity,
                  messageEntity = messageEntity
                )
              )
              MsgsCacheManager.storeMsg(
                key = messageEntity.id.toString(),
                inputStream = inputStream,
                accountEntity = accountEntity
              )
            }

            GmailApiHelper.changeLabels(
              context = getApplication(),
              accountEntity = accountEntity,
              ids = listOf(messageEntity.uidAsHEX),
              removeLabelIds = listOf(GmailApiHelper.LABEL_UNREAD)
            )

            Result.success(null)
          }
          if (result.status == Result.Status.SUCCESS) {
            val snapshot =
              MsgsCacheManager.getMsgSnapshotWithRetryStrategy(messageEntity.id.toString())
            return@withContext snapshot
              ?: throw java.lang.NullPointerException("Message not found in the local cache (GOOGLE API)")
          } else throw result.exception ?: java.lang.IllegalStateException(
            context.getString(
              R
                .string.unknown_error
            )
          )
        }
      }

      val connection = IMAPStoreManager.getConnection(accountEntity.id)
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

            if (originalMsg.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART) && !originalMsg.isOpenPGPMimeSigned()) {
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

              MsgsCacheManager.storeMsg(
                key = messageEntity.id.toString(),
                msg = customMsg,
                accountEntity = accountEntity
              )
            } else {
              val cachedMsg = MimeMessage(
                originalMsg.session,
                FetchingInputStream((originalMsg as IMAPMessage).mimeStream)
              )
              MsgsCacheManager.storeMsg(
                key = messageEntity.id.toString(),
                msg = cachedMsg,
                accountEntity = accountEntity
              )
            }

            processingProgressLiveData.postValue(
              Result.loading(
                resultCode = R.id.progress_id_fetching_message,
                progress = 60.toDouble()
              )
            )

            val snapshot =
              MsgsCacheManager.getMsgSnapshotWithRetryStrategy(messageEntity.id.toString())
            return@execute snapshot
              ?: throw java.lang.NullPointerException("Message not found in the local cache(IMAP)")
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
          val innerMultipart = item.content as Multipart
          val innerPart = getPart(innerMultipart) ?: continue
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
  ) = withContext(Dispatchers.IO) {
    if (msgEntity.isSeen == isSeen) return@withContext
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
          val attachments = EmailUtil.getAttsInfoFromPart(msg).mapNotNull { attachmentInfo ->
            AttachmentEntity.fromAttInfo(
              attachmentInfo.copy(
                email = accountEntity.email,
                folder = if (localFolder.searchQuery.isNullOrEmpty()) {
                  localFolder.fullName
                } else {
                  JavaEmailConstants.FOLDER_SEARCH
                },
                uid = msgUid
              ),
              accountType = accountEntity.accountType
            )
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
        val msg = GmailApiHelper.loadMsgInfoSuspend(
          context = getApplication(),
          accountEntity = accountEntity,
          msgId = messageEntity.uidAsHEX,
          format = GmailApiHelper.MESSAGE_RESPONSE_FORMAT_FULL
        )
        val attachments =
          GmailApiHelper.getAttsInfoFromMessagePart(msg.payload).mapNotNull { attachmentInfo ->
            AttachmentEntity.fromAttInfo(
              attachmentInfo = attachmentInfo.copy(
                email = accountEntity.email,
                folder = localFolder.fullName,
                uid = msg.uid
              ),
              accountType = accountEntity.accountType
            )
          }
        FlowCryptRoomDatabase.getDatabase(getApplication()).attachmentDao()
          .insertWithReplaceSuspend(attachments)
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }

  enum class MessageAction {
    DELETE,
    ARCHIVE,
    MOVE_TO_INBOX,
    MOVE_TO_SPAM,
    MARK_AS_NOT_SPAM,
    CHANGE_LABELS
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
