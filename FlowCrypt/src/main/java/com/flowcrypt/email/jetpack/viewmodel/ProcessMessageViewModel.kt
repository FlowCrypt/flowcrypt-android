/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.MsgsCacheManager
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.gmail.api.GmaiAPIMimeMessage
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.model.MessageFlag
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.AttMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.MsgBlock
import com.flowcrypt.email.api.retrofit.response.model.PublicKeyMsgBlock
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.com.flowcrypt.email.util.processing
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.containsLabel
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.isDraft
import com.flowcrypt.email.extensions.java.lang.printStackTraceIfDebugOnly
import com.flowcrypt.email.jetpack.workmanager.sync.UpdateMsgsSeenStateWorker
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.security.pgp.PgpMsg
import com.flowcrypt.email.ui.adapter.MessagesInThreadListAdapter
import com.flowcrypt.email.util.cache.DiskLruCache
import com.flowcrypt.email.util.exception.MessageNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author Denys Bondarenko
 */
class ProcessMessageViewModel(
  private val message: MessagesInThreadListAdapter.Message,
  private val localFolder: LocalFolder,
  private val skipAttachmentsRawData: Boolean,
  application: Application
) : AccountViewModel(application) {
  private val triggerMutableStateFlow: MutableStateFlow<Trigger> = MutableStateFlow(Trigger())
  private val triggerStateFlow: StateFlow<Trigger> = triggerMutableStateFlow.asStateFlow()
  private val triggerFlow: Flow<Trigger> = merge(
    triggerStateFlow,
    flow {
      emit(
        Trigger(
          roomDatabase.msgDao().getMessagesByIDs(
            listOf(requireNotNull(message.messageEntity.id))
          ).firstOrNull()
        )
      )
    }
  )

  @OptIn(ExperimentalCoroutinesApi::class)
  private val processedMimeMessageResultFlow: Flow<Result<PgpMsg.ProcessedMimeMessageResult?>> =
    triggerFlow.flatMapLatest { trigger ->
      flow {
        val messageEntity = trigger.messageEntity ?: return@flow
        val context: Context = getApplication()

        try {
          if (!messageEntity.isOutboxMsg) {
            emit(
              Result.loading(
                progressMsg = context.getString(R.string.processing),
                progress = (-1).toDouble()
              )
            )

            val existingMsgSnapshot = MsgsCacheManager.getMsgSnapshot(messageEntity.id.toString())
            if (existingMsgSnapshot != null) {
              emit(
                Result.loading(
                  progressMsg = context.getString(R.string.processing),
                  resultCode = R.id.progress_id_processing,
                  progress = 70.toDouble()
                )
              )
              UpdateMsgsSeenStateWorker.enqueue(application)
              val processingResult = processingMsgSnapshot(existingMsgSnapshot)
              emit(
                Result.loading(
                  progressMsg = context.getString(R.string.processing),
                  resultCode = R.id.progress_id_processing,
                  progress = 90.toDouble()
                )
              )
              emit(processingResult)
            } else {
              emit(
                Result.loading(
                  progressMsg = context.getString(R.string.connection),
                  resultCode = R.id.progress_id_connecting
                )
              )
              val newMsgSnapshot = try {
                loadMessageFromServer(messageEntity)
              } catch (e: Exception) {
                e.printStackTraceIfDebugOnly()
                emit(Result.exception(e))
                return@flow
              }
              setSeenStatusInternal(msgEntity = messageEntity, isSeen = true)
              emit(
                Result.loading(
                  progressMsg = context.getString(R.string.processing),
                  resultCode = R.id.progress_id_processing,
                  progress = 70.toDouble()
                )
              )
              val processingResult = processingMsgSnapshot(newMsgSnapshot)
              emit(
                Result.loading(
                  progressMsg = context.getString(R.string.processing),
                  resultCode = R.id.progress_id_processing,
                  progress = 90.toDouble()
                )
              )
              emit(processingResult)
            }
          }
        } catch (e: Exception) {
          e.printStackTraceIfDebugOnly()
          emit(Result.exception(e))
        }
      }
    }

  @OptIn(ExperimentalCoroutinesApi::class)
  val processedMessageFlow: Flow<Result<MessagesInThreadListAdapter.Message>> =
    processedMimeMessageResultFlow.flatMapLatest {
    flow {
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
              val messageEntity = roomDatabase.msgDao().getMsgSuspend(
                message.messageEntity.account,
                message.messageEntity.folder,
                message.messageEntity.uid,
              ) ?: message.messageEntity
              val incomingMessageInfo = IncomingMessageInfo(
                msgEntity = messageEntity,
                localFolder = LocalFolder(messageEntity.account, messageEntity.folder),
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

              val activeAccount = getActiveAccountSuspend() ?: error("No active account")

              val attachments = roomDatabase.attachmentDao().getAttachments(
                account = activeAccount.email,
                accountType = activeAccount.accountType,
                label = messageEntity.folder,
                uid = messageEntity.uid
              ).map { it.toAttInfo() }

              val inlinedAttachmentInfoList = mutableListOf<AttachmentInfo>()

              for (block in incomingMessageInfo.msgBlocks ?: emptyList()) {
                when (block) {
                  is AttMsgBlock -> {
                    inlinedAttachmentInfoList.add(
                      block.toAttachmentInfo().copy(
                        email = activeAccount.email,
                        uid = messageEntity.uid,
                        folder = messageEntity.folder,
                        path = "${inlinedAttachmentInfoList.size}"
                      )
                    )
                  }
                }
              }

              Result.success(
                requestCode = it.requestCode,
                data = message.copy(
                  messageEntity = messageEntity,
                  incomingMessageInfo = incomingMessageInfo,
                  attachments = attachments + inlinedAttachmentInfoList
                )
              )
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

  fun retry() {
    viewModelScope.launch {
      triggerMutableStateFlow.value = Trigger(message.messageEntity)
    }
  }

  private suspend fun processingMsgSnapshot(msgSnapshot: DiskLruCache.Snapshot):
      Result<PgpMsg.ProcessedMimeMessageResult?> = withContext(Dispatchers.IO) {
    val accountEntity = getActiveAccountSuspend()
      ?: throw java.lang.NullPointerException("Account is null")

    return@withContext msgSnapshot.processing(
      context = getApplication(),
      accountEntity = accountEntity,
      skipAttachmentsRawData
    ) { blocks ->
      preResultsProcessing(blocks)
    }
  }

  private suspend fun loadMessageFromServer(messageEntity: MessageEntity): DiskLruCache.Snapshot =
    withContext(Dispatchers.IO) {
      val accountEntity = getActiveAccountSuspend()
        ?: throw java.lang.NullPointerException("Account is null")
      val context: Context = getApplication()

      val result = GmailApiHelper.executeWithResult {
        val msgFullInfo = GmailApiHelper.loadMsgInfoSuspend(
          context = getApplication(),
          accountEntity = accountEntity,
          msgId = messageEntity.uidAsHEX,
          fields = null,
          format = GmailApiHelper.RESPONSE_FORMAT_FULL
        )

        if (!msgFullInfo.isDraft() && msgFullInfo.containsLabel(localFolder) == false) {
          throw MessageNotFoundException("Message doesn't contain label = ${localFolder.fullName}")
        }

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
          MsgsCacheManager.storeMsg(
            key = messageEntity.id.toString(),
            inputStream = GmailApiHelper.getWholeMimeMessageInputStream(
              context = getApplication(),
              account = accountEntity,
              messageId = msgFullInfo.id
            ),
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

  data class Trigger(
    val messageEntity: MessageEntity? = null,
    val id: Long = System.currentTimeMillis()
  )
}