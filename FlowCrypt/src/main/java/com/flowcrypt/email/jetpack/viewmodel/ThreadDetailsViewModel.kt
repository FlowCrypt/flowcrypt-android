/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.getInReplyTo
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.getMessageId
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.isDraft
import com.flowcrypt.email.extensions.java.lang.printStackTraceIfDebugOnly
import com.flowcrypt.email.ui.adapter.MessagesInThreadListAdapter
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author Denys Bondarenko
 */
class ThreadDetailsViewModel(
  private val threadMessageEntityId: Long,
  application: Application
) : AccountViewModel(application) {

  val messageFlow: Flow<MessageEntity?> =
    roomDatabase.msgDao().getMessageByIdFlow(threadMessageEntityId).distinctUntilChanged()

  private val controlledRunnerForLoadingMessages =
    ControlledRunner<Result<List<MessagesInThreadListAdapter.Item>>>()
  private val loadMessagesManuallyMutableStateFlow: MutableStateFlow<Result<List<MessagesInThreadListAdapter.Item>>> =
    MutableStateFlow(Result.none())
  private val loadMessagesManuallyStateFlow: StateFlow<Result<List<MessagesInThreadListAdapter.Item>>> =
    loadMessagesManuallyMutableStateFlow.asStateFlow()

  val messagesInThreadFlow =
    merge(
      flow {
        emit(Result.loading())
        emit(loadMessagesInternal())
      },
      loadMessagesManuallyStateFlow
    ).stateIn(
      scope = viewModelScope,
      started = WhileSubscribed(5000),
      initialValue = Result.none()
    )

  fun loadMessages() {
    viewModelScope.launch {
      loadMessagesManuallyMutableStateFlow.value = Result.loading()
      loadMessagesManuallyMutableStateFlow.value =
        controlledRunnerForLoadingMessages.cancelPreviousThenRun {
          return@cancelPreviousThenRun loadMessagesInternal()
        }
    }
  }

  private suspend fun loadMessagesInternal(): Result<List<MessagesInThreadListAdapter.Item>> {
    val threadMessageEntity =
      roomDatabase.msgDao().getMsgById(threadMessageEntityId) ?: return Result.exception(
        IllegalStateException()
      )
    val activeAccount =
      getActiveAccountSuspend() ?: return Result.exception(IllegalStateException())
    if (threadMessageEntity.threadIdAsHEX.isNullOrEmpty() || !activeAccount.isGoogleSignInAccount) {
      return Result.success(listOf())
    } else {
      val header = prepareHeader()

      try {
        val messagesInThread = GmailApiHelper.loadMessagesInThread(
          getApplication(),
          activeAccount,
          threadMessageEntity.threadIdAsHEX
        ).toMutableList().apply {
          //put drafts in the right position
          val drafts = filter { it.isDraft() }
          drafts.forEach { draft ->
            val inReplyToValue = draft.getInReplyTo()
            val inReplyToMessage = firstOrNull { it.getMessageId() == inReplyToValue }

            if (inReplyToMessage != null) {
              val inReplyToMessagePosition = indexOf(inReplyToMessage)
              if (inReplyToMessagePosition != -1) {
                remove(draft)
                add(inReplyToMessagePosition + 1, draft)
              }
            }
          }
        }

        roomDatabase.msgDao()
          .updateSuspend(threadMessageEntity.copy(threadMessagesCount = messagesInThread.size))

        val isOnlyPgpModeEnabled = activeAccount.showOnlyEncrypted ?: false
        val messageEntities = MessageEntity.genMessageEntities(
          context = getApplication(),
          account = activeAccount.email,
          accountType = activeAccount.accountType,
          label = GmailApiHelper.LABEL_INBOX, //fix me
          msgsList = messagesInThread,
          isNew = false,
          onlyPgpModeEnabled = isOnlyPgpModeEnabled,
          draftIdsMap = emptyMap()
        ) { message, messageEntity ->
          messageEntity.copy(snippet = message.snippet, isVisible = false)
        }

        roomDatabase.msgDao().clearCacheForGmailThread(
          account = activeAccount.email,
          folder = GmailApiHelper.LABEL_INBOX, //fix me
          threadId = threadMessageEntity.threadIdAsHEX
        )

        roomDatabase.msgDao().insertWithReplaceSuspend(messageEntities)
        GmailApiHelper.identifyAttachments(
          msgEntities = messageEntities,
          msgs = messagesInThread,
          account = activeAccount,
          localFolder = LocalFolder(activeAccount.email, GmailApiHelper.LABEL_INBOX),//fix me
          roomDatabase = roomDatabase
        )

        val cachedEntities = roomDatabase.msgDao().getMessagesForGmailThread(
          activeAccount.email,
          GmailApiHelper.LABEL_INBOX,//fix me
          threadMessageEntity.threadId ?: 0,
        )

        //val attachments = roomDatabase.attachmentDao().getAttachments()

        val finalList = messageEntities.map { fromServerMessageEntity ->
          MessagesInThreadListAdapter.Message(
            messageEntity = fromServerMessageEntity.copy(id = cachedEntities.firstOrNull {
              it.uid == fromServerMessageEntity.uid
            }?.id),
            type = MessagesInThreadListAdapter.Type.MESSAGE_COLLAPSED,
            isHeadersDetailsExpanded = false,
            attachments = emptyList()
          )
        }

        return Result.success(listOf(header) + finalList)
      } catch (e: Exception) {
        e.printStackTraceIfDebugOnly()
        return Result.exception(e)
      }
    }
  }

  private suspend fun prepareHeader(): MessagesInThreadListAdapter.Header =
    withContext(Dispatchers.IO) {
      val account =
        getActiveAccountSuspend() ?: return@withContext MessagesInThreadListAdapter.Header(
          "",
          emptyList()
        )
      val labelEntities =
        roomDatabase.labelDao().getLabelsSuspend(account.email, account.accountType)
      val freshestMessageEntity = roomDatabase.msgDao().getMsgById(threadMessageEntityId)
      val cachedLabelIds =
        freshestMessageEntity?.labelIds?.split(MessageEntity.LABEL_IDS_SEPARATOR)

      return@withContext try {
        //try to get the last changes from a server
        val latestLabelIds = GmailApiHelper.loadThreadInfo(
          context = getApplication(),
          accountEntity = account,
          threadId = freshestMessageEntity?.threadIdAsHEX ?: "",
          fields = listOf("id", "messages/labelIds"),
          format = GmailApiHelper.RESPONSE_FORMAT_MINIMAL
        ).labels
        if (cachedLabelIds == null
          || !(latestLabelIds.containsAll(cachedLabelIds)
              && cachedLabelIds.containsAll(latestLabelIds))
        ) {
          freshestMessageEntity?.copy(
            labelIds = latestLabelIds.joinToString(MessageEntity.LABEL_IDS_SEPARATOR)
          )?.let { roomDatabase.msgDao().updateSuspend(it) }
        }
        MessagesInThreadListAdapter.Header(
          freshestMessageEntity?.subject,
          MessageEntity.generateColoredLabels(latestLabelIds, labelEntities)
        )
      } catch (e: Exception) {
        MessagesInThreadListAdapter.Header(
          freshestMessageEntity?.subject,
          MessageEntity.generateColoredLabels(cachedLabelIds, labelEntities)
        )
      }
    }

  fun onMessageClicked(message: MessagesInThreadListAdapter.Message) {
    val currentValue = messagesInThreadFlow.value
    if (currentValue.status == Result.Status.SUCCESS) {
      loadMessagesManuallyMutableStateFlow.update {
        val currentList = messagesInThreadFlow.value.data?.toMutableList()
        currentList?.replaceAll {
          if (it.id == message.id) {
            message.copy(
              type = if (message.type == MessagesInThreadListAdapter.Type.MESSAGE_EXPANDED) {
                MessagesInThreadListAdapter.Type.MESSAGE_COLLAPSED
              } else {
                MessagesInThreadListAdapter.Type.MESSAGE_EXPANDED
              }
            )
          } else it
        }
        currentValue.copy(data = currentList)
      }
    }
  }

  fun onHeadersDetailsClick(message: MessagesInThreadListAdapter.Message) {
    val currentValue = messagesInThreadFlow.value
    if (currentValue.status == Result.Status.SUCCESS) {
      loadMessagesManuallyMutableStateFlow.update {
        val currentList = messagesInThreadFlow.value.data?.toMutableList()
        currentList?.replaceAll {
          if (it.id == message.id) {
            message.copy(isHeadersDetailsExpanded = !message.isHeadersDetailsExpanded)
          } else it
        }
        currentValue.copy(data = currentList)
      }
    }
  }

  fun onMessageProcessed(incomingMessageInfo: IncomingMessageInfo) {
    val currentValue = messagesInThreadFlow.value
    if (currentValue.status == Result.Status.SUCCESS) {
      loadMessagesManuallyMutableStateFlow.update {
        val currentList = messagesInThreadFlow.value.data?.toMutableList()
        currentList?.replaceAll {
          val messageEntity = (it as? MessagesInThreadListAdapter.Message)?.messageEntity
          if (messageEntity?.uid == incomingMessageInfo.msgEntity.uid) {
            it.copy(incomingMessageInfo = incomingMessageInfo)
          } else {
            it
          }
        }
        currentValue.copy(data = currentList)
      }
    }
  }
}