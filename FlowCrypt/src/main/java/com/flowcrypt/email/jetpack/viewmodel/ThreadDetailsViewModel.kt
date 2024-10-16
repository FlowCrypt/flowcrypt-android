/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.model.MessageFlag
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.getInReplyTo
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.getMessageId
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.isDraft
import com.flowcrypt.email.extensions.java.lang.printStackTraceIfDebugOnly
import com.flowcrypt.email.model.MessageAction
import com.flowcrypt.email.ui.adapter.MessagesInThreadListAdapter
import com.flowcrypt.email.ui.adapter.MessagesInThreadListAdapter.Message
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
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
  private val localFolder: LocalFolder,
  application: Application
) : AccountViewModel(application) {
  private val controlledRunnerForLoadingMessages =
    ControlledRunner<Result<List<MessagesInThreadListAdapter.Item>>>()
  private val loadMessagesManuallyMutableStateFlow: MutableStateFlow<Result<List<MessagesInThreadListAdapter.Item>>> =
    MutableStateFlow(Result.none())
  private val loadMessagesManuallyStateFlow: StateFlow<Result<List<MessagesInThreadListAdapter.Item>>> =
    loadMessagesManuallyMutableStateFlow.asStateFlow()
  val threadMessageEntityFlow =
    roomDatabase.msgDao().getMessageByIdFlow(threadMessageEntityId)
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
      )

  @OptIn(ExperimentalCoroutinesApi::class)
  val localFolderFlow: StateFlow<LocalFolder?> =
    threadMessageEntityFlow.mapLatest { threadMessageEntity ->
      val activeAccount = getActiveAccountSuspend()
        ?: return@mapLatest null
      val foldersManager = FoldersManager.fromDatabaseSuspend(getApplication(), activeAccount)
      return@mapLatest foldersManager.getFolderByFullName(threadMessageEntity?.folder)
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = null
    )

  @OptIn(ExperimentalCoroutinesApi::class)
  private val threadHeaderFlow = threadMessageEntityFlow.mapLatest {
    prepareThreadHeader(it)
  }.distinctUntilChanged()

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
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = Result.none()
    )

  @OptIn(ExperimentalCoroutinesApi::class)
  val messageActionsAvailabilityStateFlow =
    threadMessageEntityFlow.mapLatest { threadMessageEntity ->
    val activeAccount = getActiveAccountSuspend()
      ?: return@mapLatest MessageAction.entries.associateBy({ it }, { false })
    val foldersManager = FoldersManager.fromDatabaseSuspend(getApplication(), activeAccount)
    MessageAction.entries.associateBy({ it }, { false }).toMutableMap().apply {
      val folderType =
        foldersManager.getFolderByFullName(threadMessageEntity?.folder)?.getFolderType()
      if (activeAccount.isGoogleSignInAccount) {
        val labelIds =
          threadMessageEntity?.labelIds?.split(MessageEntity.LABEL_IDS_SEPARATOR).orEmpty()
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

      this[MessageAction.MARK_UNREAD] =
        !JavaEmailConstants.FOLDER_OUTBOX.equals(threadMessageEntity?.folder, ignoreCase = true)

      if (folderType != null) {
        when (folderType) {
          FoldersManager.FolderType.SENT -> {
            this[MessageAction.DELETE] = true
            this[MessageAction.MOVE_TO_SPAM] =
              AccountEntity.ACCOUNT_TYPE_GOOGLE == activeAccount.accountType
          }

          FoldersManager.FolderType.DRAFTS,
          FoldersManager.FolderType.OUTBOX,
          FoldersManager.FolderType.JUNK,
          FoldersManager.FolderType.SPAM -> {
            this[MessageAction.DELETE] = true
            this[MessageAction.MOVE_TO_SPAM] = false
          }

          else -> {
            this[MessageAction.DELETE] = true
            this[MessageAction.MOVE_TO_SPAM] = true
          }
        }
      } else {
        this[MessageAction.DELETE] = true
        this[MessageAction.MOVE_TO_SPAM] = false
      }

      if (foldersManager.folderTrash == null) {
        this[MessageAction.DELETE] = false
      }
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = MessageAction.entries.associateBy({ it }, { false })
  )

  private val sessionMessageStateMutableStateFlow: MutableStateFlow<MessageState?> =
    MutableStateFlow(null)
  val sessionMessageStateStateFlow: StateFlow<MessageState?> =
    sessionMessageStateMutableStateFlow.asStateFlow()

  init {
    subscribeToAutomaticallyUpdateLabels()
    viewModelScope.launch {
      messagesInThreadFlow.collectLatest {
        if (it.status == Result.Status.SUCCESS) {
          val messageItems = it.data?.filterIsInstance<Message>() ?: return@collectLatest
          if (messageItems.isEmpty()) {
            return@collectLatest
          }

          val threadMessageEntity =
            roomDatabase.msgDao().getMsgById(threadMessageEntityId) ?: return@collectLatest

          val isThreadFullySeen = messageItems.all { item -> item.messageEntity.isSeen }
          if (threadMessageEntity.isSeen != isThreadFullySeen) {
            roomDatabase.msgDao().getMsgById(threadMessageEntityId)?.let { messageEntity ->
              roomDatabase.msgDao().updateSuspend(
                messageEntity.copy(
                  labelIds = messageEntity.labelIds
                    ?.split(MessageEntity.LABEL_IDS_SEPARATOR)
                    ?.toMutableSet()
                    ?.apply {
                      remove(GmailApiHelper.LABEL_UNREAD)
                    }?.joinToString(MessageEntity.LABEL_IDS_SEPARATOR),
                  flags = if (isThreadFullySeen) {
                    if (messageEntity.flags?.contains(MessageFlag.SEEN.value) == true) {
                      messageEntity.flags
                    } else {
                      messageEntity.flags?.plus("${MessageFlag.SEEN.value} ")
                    }
                  } else {
                    messageEntity.flags?.replace(MessageFlag.SEEN.value, "")
                  }
                )
              )
            }
          }
        }
      }
    }
  }

  fun loadMessages() {
    viewModelScope.launch {
      loadMessagesManuallyMutableStateFlow.value = Result.loading()
      loadMessagesManuallyMutableStateFlow.value =
        controlledRunnerForLoadingMessages.cancelPreviousThenRun {
          return@cancelPreviousThenRun loadMessagesInternal()
        }
    }
  }

  fun onMessageClicked(message: Message) {
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

  fun onHeadersDetailsClick(message: Message) {
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

  fun onMessageChanged(messageWithChanges: Message) {
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

  fun getMessageActionAvailability(messageAction: MessageAction): Boolean {
    return messageActionsAvailabilityStateFlow.value[messageAction] ?: false
  }

  fun changeMsgState(newMsgState: MessageState) {
    val freshMsgEntity = threadMessageEntityFlow.value
    freshMsgEntity?.let { messageEntity ->
      viewModelScope.launch {
        val candidate: MessageEntity = when (newMsgState) {
          MessageState.PENDING_MARK_READ -> {
            messageEntity.copy(
              state = newMsgState.value,
              flags = if (messageEntity.flags?.contains(MessageFlag.SEEN.value) == true) {
                messageEntity.flags
              } else {
                messageEntity.flags?.plus("${MessageFlag.SEEN.value} ")
              }
            )
          }

          MessageState.PENDING_MARK_UNREAD -> {
            messageEntity.copy(
              state = newMsgState.value,
              flags = messageEntity.flags?.replace(MessageFlag.SEEN.value, "")
            )
          }

          else -> {
            messageEntity.copy(state = newMsgState.value)
          }
        }

        roomDatabase.msgDao().updateSuspend(candidate)
        sessionMessageStateMutableStateFlow.value = newMsgState
      }
    }
  }

  fun deleteThread() {
    val freshMsgEntity = threadMessageEntityFlow.value
    freshMsgEntity?.let { messageEntity ->
      viewModelScope.launch {
        roomDatabase.msgDao().deleteSuspend(messageEntity)

        val accountEntity = getActiveAccountSuspend() ?: return@launch

        if (JavaEmailConstants.FOLDER_OUTBOX.equals(messageEntity.folder, ignoreCase = true)) {
          val outgoingMsgCount =
            roomDatabase.msgDao().getOutboxMsgsSuspend(messageEntity.account).size
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
      val threadHeader =
        prepareThreadHeader(roomDatabase.msgDao().getMsgById(threadMessageEntityId))

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
        }.reversed()

        //update the actual thread size
        roomDatabase.msgDao()
          .updateSuspend(threadMessageEntity.copy(threadMessagesCount = messagesInThread.size))

        val isOnlyPgpModeEnabled = activeAccount.showOnlyEncrypted ?: false
        val messageEntities = MessageEntity.genMessageEntities(
          context = getApplication(),
          account = activeAccount.email,
          accountType = activeAccount.accountType,
          label = getFolderFullName(),
          msgsList = messagesInThread,
          isNew = false,
          onlyPgpModeEnabled = isOnlyPgpModeEnabled,
          draftIdsMap = emptyMap()
        ) { message, messageEntity ->
          messageEntity.copy(snippet = message.snippet, isVisible = false)
        }

        threadMessageEntity.threadId?.let {
          roomDatabase.msgDao().clearCacheForGmailThread(
            account = activeAccount.email,
            folder = getFolderFullName(),
            threadId = it
          )
        }

        roomDatabase.msgDao().insertWithReplaceSuspend(messageEntities)
        GmailApiHelper.identifyAttachments(
          msgEntities = messageEntities,
          msgs = messagesInThread,
          account = activeAccount,
          localFolder = if (localFolder.searchQuery == null) {
            localFolder
          } else {
            localFolder.copy(fullName = JavaEmailConstants.FOLDER_SEARCH)
          },
          roomDatabase = roomDatabase
        )

        val cachedEntities = roomDatabase.msgDao().getMessagesForGmailThread(
          activeAccount.email,
          getFolderFullName(),
          threadMessageEntity.threadId ?: 0,
        )

        val finalList = messageEntities.map { fromServerMessageEntity ->
          Message(
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

  private suspend fun prepareThreadHeader(messageEntity: MessageEntity?): MessagesInThreadListAdapter.ThreadHeader =
    withContext(Dispatchers.IO) {
      val account =
        getActiveAccountSuspend() ?: return@withContext MessagesInThreadListAdapter.ThreadHeader(
          "",
          emptyList()
        )
      val labelEntities =
        roomDatabase.labelDao().getLabelsSuspend(account.email, account.accountType)
      val cachedLabelIds =
        messageEntity?.labelIds?.split(MessageEntity.LABEL_IDS_SEPARATOR)

      return@withContext try {
        //try to get the last changes from a server
        val latestLabelIds = GmailApiHelper.loadThreadInfo(
          context = getApplication(),
          accountEntity = account,
          threadId = messageEntity?.threadIdAsHEX ?: "",
          fields = listOf("id", "messages/labelIds"),
          format = GmailApiHelper.RESPONSE_FORMAT_MINIMAL
        ).labels
        if (cachedLabelIds == null
          || !(latestLabelIds.containsAll(cachedLabelIds)
              && cachedLabelIds.containsAll(latestLabelIds))
        ) {
          messageEntity?.copy(
            labelIds = latestLabelIds.joinToString(MessageEntity.LABEL_IDS_SEPARATOR)
          )?.let { roomDatabase.msgDao().updateSuspend(it) }
        }
        MessagesInThreadListAdapter.ThreadHeader(
          messageEntity?.subject,
          MessageEntity.generateColoredLabels(latestLabelIds, labelEntities)
        )
      } catch (e: Exception) {
        MessagesInThreadListAdapter.ThreadHeader(
          messageEntity?.subject,
          MessageEntity.generateColoredLabels(cachedLabelIds, labelEntities)
        )
      }
    }

  private fun subscribeToAutomaticallyUpdateLabels() {
    viewModelScope.launch {
      threadHeaderFlow.collectLatest { threadHeader ->
        val existingResult = messagesInThreadFlow.value
        if (existingResult.data.isNullOrEmpty()) {
          return@collectLatest
        } else {
          loadMessagesManuallyMutableStateFlow.update {
            val updatedList = existingResult.data.toMutableList().apply {
              replaceAll { item ->
                if (item.type == MessagesInThreadListAdapter.Type.HEADER) {
                  (item as MessagesInThreadListAdapter.ThreadHeader).copy(labels = threadHeader.labels)
                } else {
                  item
                }
              }
            }
            Result.success(updatedList)
          }
        }
      }
    }
  }

  private fun getFolderFullName() = if (localFolder.searchQuery == null) {
    localFolder.fullName
  } else {
    JavaEmailConstants.FOLDER_SEARCH
  }
}