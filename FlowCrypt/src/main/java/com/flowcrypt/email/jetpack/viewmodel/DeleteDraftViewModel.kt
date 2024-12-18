/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.model.MessageFlag
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.database.entity.MessageEntity.Companion.LABEL_IDS_SEPARATOR
import com.flowcrypt.email.extensions.java.lang.printStackTraceIfDebugOnly
import com.google.api.services.gmail.model.Thread
import jakarta.mail.internet.InternetAddress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/**
 * @author Denys Bondarenko
 */
class DeleteDraftViewModel(
  private val messageEntityId: Long,
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
            listOf(messageEntityId)
          ).firstOrNull()
        )
      )
    }
  )

  @OptIn(ExperimentalCoroutinesApi::class)
  val deleteDraftResultFlow: Flow<Result<Long?>> =
    triggerFlow.flatMapLatest { trigger ->
      flow {
        val messageEntity = trigger.messageEntity ?: return@flow
        val context: Context = getApplication()

        try {
          val accountEntity = getActiveAccountSuspend() ?: error("No active account")
          emit(
            Result.loading(
              progressMsg = context.getString(R.string.processing),
              progress = (-1).toDouble()
            )
          )

          GmailApiHelper.deleteMsgsPermanently(
            context = getApplication(),
            accountEntity = accountEntity,
            ids = listOf(messageEntity.uidAsHEX)
          )

          if (messageEntity.threadId != null) {
            val threadMessageEntity = roomDatabase.msgDao().getThreadMessageEntity(
              account = messageEntity.account,
              folder = messageEntity.folder,
              threadId = messageEntity.threadId
            )

            if (threadMessageEntity != null) {
              val gmailThreadInfo = GmailApiHelper.loadGmailThreadInfoInParallel(
                context = context,
                accountEntity = accountEntity,
                threads = listOf(Thread().apply { id = messageEntity.threadIdAsHEX }),
                format = GmailApiHelper.RESPONSE_FORMAT_FULL
              ).firstOrNull()

              if (gmailThreadInfo != null) {
                val updateCandidate = MessageEntity.genMessageEntities(
                  context = context,
                  account = accountEntity.email,
                  accountType = accountEntity.accountType,
                  label = threadMessageEntity.folder,
                  msgsList = listOf(gmailThreadInfo.lastMessage),
                  isNew = false,
                  onlyPgpModeEnabled = accountEntity.showOnlyEncrypted ?: false
                ) { _, entity ->
                  entity.copy(
                    id = threadMessageEntity.id,
                    uid = threadMessageEntity.uid,
                    subject = gmailThreadInfo.subject,
                    threadMessagesCount = gmailThreadInfo.messagesCount,
                    threadDraftsCount = gmailThreadInfo.draftsCount,
                    labelIds = gmailThreadInfo.labels.joinToString(separator = LABEL_IDS_SEPARATOR),
                    hasAttachments = gmailThreadInfo.hasAttachments,
                    fromAddresses = InternetAddress.toString(gmailThreadInfo.recipients.toTypedArray()),
                    hasPgp = gmailThreadInfo.hasPgpThings,
                    flags = if (gmailThreadInfo.hasUnreadMessages) {
                      threadMessageEntity.flags?.replace(MessageFlag.SEEN.value, "")
                    } else {
                      if (threadMessageEntity.flags?.contains(MessageFlag.SEEN.value) == true) {
                        threadMessageEntity.flags
                      } else {
                        threadMessageEntity.flags.plus("${MessageFlag.SEEN.value} ")
                      }
                    }
                  )
                }

                roomDatabase.msgDao().updateSuspend(updateCandidate)
              }
            }
          }

          emit(Result.success(messageEntityId))
        } catch (e: Exception) {
          e.printStackTraceIfDebugOnly()
          emit(Result.exception(e))
        }
      }
    }

  fun retry() {
    viewModelScope.launch {
      val messageEntity = roomDatabase.msgDao().getMessagesByIDs(
        listOf(messageEntityId)
      ).firstOrNull()
      triggerMutableStateFlow.value = Trigger(messageEntity)
    }
  }

  data class Trigger(
    val messageEntity: MessageEntity? = null,
    val id: Long = System.currentTimeMillis()
  )
}