/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
  private val controlledRunnerForLoadingMessages =
    ControlledRunner<Result<List<MessagesInThreadListAdapter.Item>>>()
  private val loadMessagesManuallyMutableStateFlow: MutableStateFlow<Result<List<MessagesInThreadListAdapter.Item>>> =
    MutableStateFlow(Result.none())
  private val loadMessagesManuallyStateFlow: StateFlow<Result<List<MessagesInThreadListAdapter.Item>>> =
    loadMessagesManuallyMutableStateFlow.asStateFlow()

  val messagesInThreadFlow =
    merge(
      //this flow will be used once to load messages at the start up
      flow {
        emit(Result.loading())
        emit(loadMessagesInternal())
      },
      //this flow will be used to trigger loading messages manually, for example after connection issue
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
        IllegalStateException("Message does not exist")
      )
    val activeAccount =
      getActiveAccountSuspend() ?: return Result.exception(IllegalStateException())
    if (threadMessageEntity.threadIdAsHEX.isNullOrEmpty() || !activeAccount.isGoogleSignInAccount) {
      return Result.success(listOf())
    } else {
      val threadHeader = prepareThreadHeader()

      try {
        val messagesInThread = GmailApiHelper.loadMessagesInThread(
          getApplication(),
          activeAccount,
          threadMessageEntity.threadIdAsHEX
        ).toMutableList().apply {
          //try to put drafts in the right position
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

        //update the actual thread size
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

        return Result.success(listOf(threadHeader) + finalList)
      } catch (e: Exception) {
        e.printStackTraceIfDebugOnly()
        return Result.exception(e)
      }
    }
  }

  private suspend fun prepareThreadHeader(): MessagesInThreadListAdapter.ThreadHeader =
    withContext(Dispatchers.IO) {
      val account =
        getActiveAccountSuspend() ?: return@withContext MessagesInThreadListAdapter.ThreadHeader(
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
        MessagesInThreadListAdapter.ThreadHeader(
          freshestMessageEntity?.subject,
          MessageEntity.generateColoredLabels(latestLabelIds, labelEntities)
        )
      } catch (e: Exception) {
        MessagesInThreadListAdapter.ThreadHeader(
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

  fun onMessageChanged(messageWithChanges: MessagesInThreadListAdapter.Message) {
    val currentValue = messagesInThreadFlow.value
    if (currentValue.status == Result.Status.SUCCESS) {
      val currentList = messagesInThreadFlow.value.data?.toMutableList()
      val cachedMessage = currentList?.firstOrNull { it.id == messageWithChanges.id } ?: return
      if (cachedMessage != messageWithChanges) {
        loadMessagesManuallyMutableStateFlow.update {
          currentList.replaceAll {
            if (it.id == messageWithChanges.id) {
              messageWithChanges
            } else {
              it
            }
          }
          currentValue.copy(data = currentList)
        }
      }
    }
  }
}