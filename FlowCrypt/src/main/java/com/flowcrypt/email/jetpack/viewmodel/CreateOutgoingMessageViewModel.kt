/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.java.lang.printStackTraceIfDebugOnly
import com.flowcrypt.email.jetpack.workmanager.DownloadForwardedAttachmentsWorker
import com.flowcrypt.email.jetpack.workmanager.HandlePasswordProtectedMsgWorker
import com.flowcrypt.email.jetpack.workmanager.MessagesSenderWorker
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.service.ProcessingOutgoingMessageInfoHelper
import com.flowcrypt.email.util.FileAndDirectoryUtils
import com.flowcrypt.email.util.OutgoingMessagesManager
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import jakarta.mail.Flags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * @author Denys Bondarenko
 */
class CreateOutgoingMessageViewModel(
  private val outgoingMessageInfo: OutgoingMessageInfo,
  application: Application
) : AccountViewModel(application) {
  private val controlledRunnerForCreatingOutgoingMessage = ControlledRunner<Result<Unit>>()
  private val createOutgoingMessageMutableStateFlow: MutableStateFlow<Result<Unit>> =
    MutableStateFlow(Result.none())
  val createOutgoingMessageStateFlow: StateFlow<Result<Unit>> =
    createOutgoingMessageMutableStateFlow.asStateFlow()

  fun create() {
    viewModelScope.launch {
      val context: Context = getApplication()
      createOutgoingMessageMutableStateFlow.value =
        Result.loading(progressMsg = context.getString(R.string.processing_please_wait))
      createOutgoingMessageMutableStateFlow.value =
        controlledRunnerForCreatingOutgoingMessage.joinPreviousOrRun {
          return@joinPreviousOrRun createOutgoingMessageInternal(context)
        }
    }
  }

  private suspend fun createOutgoingMessageInternal(context: Context): Result<Unit> =
    withContext(Dispatchers.IO) {
      var messageEntity: MessageEntity? = null
      return@withContext try {
        val activeAccount = getActiveAccountSuspend()
          ?: throw IllegalStateException("No active account")

        val replyTo = outgoingMessageInfo.replyToMessageEntityId?.let {
          roomDatabase.msgDao().getMsgById(it)?.replyTo
        }
        messageEntity = outgoingMessageInfo.toMessageEntity(
          folder = JavaEmailConstants.FOLDER_OUTBOX,
          flags = Flags(Flags.Flag.SEEN),
          replyTo = replyTo,
          password = outgoingMessageInfo.password?.let {
            KeyStoreCryptoManager.encrypt(String(it)).toByteArray()
          }
        )
        val messageId = roomDatabase.msgDao().insertSuspend(messageEntity)
        messageEntity = messageEntity.copy(id = messageId, uid = messageId)
        roomDatabase.msgDao().updateSuspend(messageEntity)

        updateOutgoingMsgCount(activeAccount.email, activeAccount.accountType)
        ProcessingOutgoingMessageInfoHelper.process(
          context = context,
          originalOutgoingMessageInfo = outgoingMessageInfo.copy(
            uid = messageId,
            atts = outgoingMessageInfo.atts?.map {
              it.copy(
                email = outgoingMessageInfo.account,
                folder = JavaEmailConstants.FOLDER_OUTBOX,
                uid = messageId,
                type = it.type.ifEmpty { Constants.MIME_TYPE_BINARY_DATA }
              )
            },
            forwardedAtts = outgoingMessageInfo.forwardedAtts?.map {
              it.copy(
                email = outgoingMessageInfo.account,
                folder = JavaEmailConstants.FOLDER_OUTBOX,
                uid = messageId,
                fwdFolder = it.folder,
                fwdUid = it.uid,
                type = it.type.ifEmpty { Constants.MIME_TYPE_BINARY_DATA }
              )
            },
          ),
          messageEntity = messageEntity
        ) { mimeMessage ->
          OutgoingMessagesManager.enqueueOutgoingMessage(
            context,
            requireNotNull(messageEntity.id),
            mimeMessage
          )

          if (outgoingMessageInfo.forwardedAtts?.isNotEmpty() == true) {
            DownloadForwardedAttachmentsWorker.enqueue(context)
          } else {
            val existingMsgEntity = roomDatabase.msgDao().getMsg(
              messageEntity.email, messageEntity.folder, messageEntity.uid
            ) ?: throw IllegalStateException("A message is not exist")
            if (outgoingMessageInfo.encryptionType == MessageEncryptionType.ENCRYPTED
              && outgoingMessageInfo.isPasswordProtected == true
            ) {
              roomDatabase.msgDao()
                .update(existingMsgEntity.copy(state = MessageState.NEW_PASSWORD_PROTECTED.value))
              HandlePasswordProtectedMsgWorker.enqueue(context)
            } else {
              roomDatabase.msgDao()
                .update(existingMsgEntity.copy(state = MessageState.QUEUED.value))
              MessagesSenderWorker.enqueue(context)
            }
          }
        }
        Result.success(Unit)
      } catch (e: Exception) {
        e.printStackTraceIfDebugOnly()
        try {
          //delete unused resources if any exception has occurred
          messageEntity?.let {
            if (messageEntity.id != null) {
              roomDatabase.msgDao().deleteSuspend(messageEntity)
              OutgoingMessagesManager.deleteOutgoingMessage(
                context = getApplication(),
                id = requireNotNull(messageEntity.id),
              )
              val attachmentsCacheDir = File(context.cacheDir, Constants.ATTACHMENTS_CACHE_DIR)
              messageEntity.attachmentsDirectory?.let {
                FileAndDirectoryUtils.deleteDir(File(attachmentsCacheDir, it))
              }
            }
          }
        } catch (e: Exception) {
          e.printStackTraceIfDebugOnly()
        }
        Result.exception(e)
      }
    }

  private suspend fun updateOutgoingMsgCount(
    email: String,
    accountType: String?
  ) {
    val outgoingMsgCount = roomDatabase.msgDao().getOutboxMsgsSuspend(email).size
    val outboxLabel =
      roomDatabase.labelDao().getLabelSuspend(email, accountType, JavaEmailConstants.FOLDER_OUTBOX)

    outboxLabel?.let {
      roomDatabase.labelDao().updateSuspend(it.copy(messagesTotal = outgoingMsgCount))
    }
  }
}