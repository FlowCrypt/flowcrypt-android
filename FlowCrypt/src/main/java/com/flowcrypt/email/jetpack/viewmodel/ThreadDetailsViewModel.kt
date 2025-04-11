/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.MsgsCacheManager
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.gmail.model.GmailThreadInfo
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.model.MessageFlag
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.extensions.com.flowcrypt.email.util.processing
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.filteredMessages
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.getInReplyTo
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.getMessageId
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.isDraft
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.toThreadInfo
import com.flowcrypt.email.extensions.java.lang.printStackTraceIfDebugOnly
import com.flowcrypt.email.model.MessageAction
import com.flowcrypt.email.ui.adapter.MessagesInThreadListAdapter
import com.flowcrypt.email.ui.adapter.MessagesInThreadListAdapter.Message
import com.flowcrypt.email.util.RecipientLookUpManager
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import com.flowcrypt.email.util.exception.ThreadNotFoundException
import jakarta.mail.Message.RecipientType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
  private val controlledRunnerForLoadingMessages = ControlledRunner<Result<Data>>()
  private val loadMessagesManuallyMutableStateFlow: MutableStateFlow<Result<Data>> =
    MutableStateFlow(Result.none())
  private val loadMessagesManuallyStateFlow: StateFlow<Result<Data>> =
    loadMessagesManuallyMutableStateFlow.asStateFlow()
  val threadMessageEntityFlow =
    roomDatabase.msgDao().getMessageByIdFlow(threadMessageEntityId)
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
      )

  private val recipientLookUpManager = RecipientLookUpManager(
    application = application,
    roomDatabase = roomDatabase
  ) { recipientInfo ->
    sessionFromRecipientsMutableStateFlow.update {
      it.toMutableSet().apply { add(recipientInfo) }
    }
  }

  private val sessionFromRecipientsMutableStateFlow: MutableStateFlow<Set<RecipientLookUpManager.RecipientInfo>> =
    MutableStateFlow(emptySet())
  val sessionFromRecipientsStateFlow: StateFlow<Set<RecipientLookUpManager.RecipientInfo>> =
    sessionFromRecipientsMutableStateFlow.asStateFlow()

  val allOutboxMessagesFlow = roomDatabase.msgDao().getAllOutboxMessagesFlow()

  @OptIn(ExperimentalCoroutinesApi::class)
  private val threadHeaderFlow = threadMessageEntityFlow.mapLatest {
    prepareThreadHeader(it)
  }.distinctUntilChanged()

  val messagesInThreadFlow =
    merge(
      //this flow will be used once to load messages at the start up
      flow {
        emit(Result.loading())
        emit(loadMessagesInternal(clearCache = true))
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

      if (foldersManager.folderTrash == null || localFolder.isDrafts) {
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
      keepThreadMessageEntityFresh()
    }

    viewModelScope.launch {
      setupAutoSignatureReVerification()
    }
  }

  fun loadMessages(
    clearCache: Boolean = false,
    silentUpdate: Boolean = false,
    delayInMilliseconds: Long = 0
  ) {
    viewModelScope.launch {
      if (!silentUpdate) {
        loadMessagesManuallyMutableStateFlow.value = Result.loading()
      }

      if (delayInMilliseconds > 0) {
        delay(delayInMilliseconds)
      }

      loadMessagesManuallyMutableStateFlow.value =
        controlledRunnerForLoadingMessages.cancelPreviousThenRun {
          return@cancelPreviousThenRun loadMessagesInternal(
            clearCache = clearCache,
            silentUpdate = silentUpdate
          )
        }
    }
  }

  fun deleteMessageFromCache(message: Message) {
    viewModelScope.launch {
      if (loadMessagesManuallyMutableStateFlow.value.status == Result.Status.SUCCESS) {
        loadMessagesManuallyMutableStateFlow.value.data?.list?.let { list ->
          if (list.contains(message)) {
            loadMessagesManuallyMutableStateFlow.update {
              Result.success(Data(silentUpdate = true, list = list - message))
            }
          }
        }
      }
    }
  }

  fun onMessageClicked(message: Message) {
    val currentValue = messagesInThreadFlow.value
    if (currentValue.status == Result.Status.SUCCESS) {
      val currentList = messagesInThreadFlow.value.data?.list?.toMutableList() ?: return
      loadMessagesManuallyMutableStateFlow.update {
        currentList.replaceAll {
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
        currentValue.copy(data = currentValue.data?.copy(list = currentList.toList()))
      }
    }
  }

  fun onHeadersDetailsClick(message: Message) {
    val currentValue = messagesInThreadFlow.value
    if (currentValue.status == Result.Status.SUCCESS) {
      val currentList = messagesInThreadFlow.value.data?.list?.toMutableList() ?: return
      loadMessagesManuallyMutableStateFlow.update {
        currentList.replaceAll {
          if (it.id == message.id) {
            message.copy(isHeadersDetailsExpanded = !message.isHeadersDetailsExpanded)
          } else it
        }
        currentValue.copy(data = currentValue.data?.copy(list = currentList.toList()))
      }
    }
  }

  fun onMessageChanged(messageWithChanges: Message) {
    val currentValue = messagesInThreadFlow.value
    if (currentValue.status == Result.Status.SUCCESS) {
      val currentList = messagesInThreadFlow.value.data?.list?.toMutableList()
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
          currentValue.copy(data = currentValue.data?.copy(list = currentList.toList()))
        }
      }
    }
  }

  fun getMessageActionAvailability(messageAction: MessageAction): Boolean {
    return messageActionsAvailabilityStateFlow.value[messageAction] == true
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
              flags = messageEntity.flagsStringAfterRemoveSome(MessageFlag.SEEN.value)
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

  fun fetchAndUpdateInfoAboutRecipients(from: Collection<String>) {
    viewModelScope.launch {
      from.forEach {
        recipientLookUpManager.enqueue(
          RecipientLookUpManager.RecipientInfo(
            FROM,
            RecipientWithPubKeys(
              RecipientEntity(id = System.currentTimeMillis(), email = it),
              emptyList()
            )
          )
        )
      }
    }
  }

  class FROM : RecipientType("From")

  private suspend fun loadMessagesInternal(
    clearCache: Boolean = false,
    silentUpdate: Boolean = false
  ): Result<Data> {
    val threadMessageEntity =
      roomDatabase.msgDao().getMsgById(threadMessageEntityId) ?: return Result.exception(
        IllegalStateException("Message does not exist")
      )
    val activeAccount =
      getActiveAccountSuspend() ?: return Result.exception(IllegalStateException())
    if (threadMessageEntity.threadIdAsHEX.isNullOrEmpty() || !activeAccount.isGoogleSignInAccount) {
      return Result.success(Data(silentUpdate = silentUpdate, list = listOf()))
    } else {
      try {
        val thread = GmailApiHelper.getThread(
          context = getApplication(),
          accountEntity = activeAccount,
          threadId = threadMessageEntity.threadIdAsHEX,
          format = GmailApiHelper.RESPONSE_FORMAT_FULL
        ) ?: error("Thread not found")

        val threadInfo = thread.toThreadInfo(getApplication(), activeAccount, localFolder)

        if (!threadInfo.labels.contains(localFolder.fullName) && !localFolder.isAll) {
          val context: Context = getApplication()
          throw ThreadNotFoundException(context.getString(R.string.thread_was_deleted_or_moved))
        }

        //keep thread info updated in the local cache
        roomDatabase.msgDao().updateSuspend(
          MessageEntity.genMessageEntities(
            context = getApplication(),
            account = activeAccount.email,
            accountType = activeAccount.accountType,
            label = getFolderFullName(),
            msgsList = listOf(threadInfo.lastMessage),
            isNew = false,
            onlyPgpModeEnabled = activeAccount.showOnlyEncrypted == true
          ) { message, messageEntity ->
            if (message.threadId == threadMessageEntity.threadIdAsHEX) {
              messageEntity.toUpdatedThreadCopy(threadMessageEntity, threadInfo)
            } else messageEntity
          })

        val messagesInThread = thread.filteredMessages(localFolder).toMutableList().apply {
          //try to put drafts in the right position
          val drafts = filter { it.isDraft() }
          drafts.forEach { draft ->
            val inReplyToValue = draft.getInReplyTo()
            val inReplyToMessage = firstOrNull { it.getMessageId() == inReplyToValue }

            if (inReplyToMessage != null) {
              val inReplyToMessagePosition = indexOf(inReplyToMessage)
              if (inReplyToMessagePosition != -1) {
                remove(draft)
                if (size >= inReplyToMessagePosition + 1) {
                  add(inReplyToMessagePosition + 1, draft)
                } else {
                  add(draft)
                }
              }
            }
          }
        }

        val fetchedDraftIdsMap = if (messagesInThread.any { it.isDraft() }) {
          GmailApiHelper.loadBaseDraftInfoInParallel(
            context = getApplication(),
            accountEntity = activeAccount,
            messages = messagesInThread.filter { it.isDraft() }
          ).associateBy({ it.message.id }, { it.id })
        } else emptyMap()

        val isOnlyPgpModeEnabled = activeAccount.showOnlyEncrypted == true
        val messageEntitiesBasedOnServerResult = MessageEntity.genMessageEntities(
          context = getApplication(),
          account = activeAccount.email,
          accountType = activeAccount.accountType,
          label = getFolderFullName(),
          msgsList = messagesInThread,
          isNew = false,
          onlyPgpModeEnabled = isOnlyPgpModeEnabled,
          draftIdsMap = fetchedDraftIdsMap
        ) { message, messageEntity ->
          messageEntity.copy(snippet = message.snippet, isVisible = false)
        }
        val setOfUIDsBasedOnServerResult = messageEntitiesBasedOnServerResult.map { it.uid }.toSet()

        if (clearCache) {
          threadMessageEntity.threadId?.let {
            roomDatabase.msgDao().clearCacheForGmailThread(
              account = activeAccount.email,
              folder = getFolderFullName(),
              threadId = it
            )
          }
        }

        var cachedEntities = roomDatabase.msgDao().getMessagesForGmailThread(
          activeAccount.email,
          getFolderFullName(),
          threadMessageEntity.threadId ?: 0,
        )

        val candidatesToBeInserted = mutableListOf<MessageEntity>()
        val candidatesToBeUpdated = mutableListOf<MessageEntity>()
        messageEntitiesBasedOnServerResult.forEach { entity ->
          val existingVersion = cachedEntities.firstOrNull { it.uid == entity.uid }
          if (existingVersion == null) {
            candidatesToBeInserted.add(entity)
          } else if (existingVersion.copy(id = null) != entity) {
            candidatesToBeUpdated.add(entity.copy(id = existingVersion.id))
            if (existingVersion.flagsValueSet != entity.flagsValueSet) {
              MsgsCacheManager.removeMessage(existingVersion.id.toString())
            }
          }
        }
        val candidatesToBeDeleted =
          cachedEntities.filter { it.uid !in setOfUIDsBasedOnServerResult }

        roomDatabase.msgDao().insertSuspend(candidatesToBeInserted)
        roomDatabase.msgDao().updateSuspend(candidatesToBeUpdated)
        roomDatabase.msgDao().deleteSuspend(candidatesToBeDeleted)

        GmailApiHelper.identifyAttachments(
          msgEntities = messageEntitiesBasedOnServerResult,
          msgs = messagesInThread,
          account = activeAccount,
          localFolder = if (localFolder.searchQuery == null) {
            localFolder
          } else {
            localFolder.copy(fullName = JavaEmailConstants.FOLDER_SEARCH)
          },
          roomDatabase = roomDatabase
        )

        //get the freshest info from the local database
        cachedEntities = roomDatabase.msgDao().getMessagesForGmailThread(
          activeAccount.email,
          getFolderFullName(),
          threadMessageEntity.threadId ?: 0,
        )

        val currentList = messagesInThreadFlow.value.data?.list ?: emptyList()
        val finalList = messageEntitiesBasedOnServerResult.map { fromServerMessageEntity ->
          val messageEntity = fromServerMessageEntity.copy(
            id = cachedEntities.firstOrNull { it.uid == fromServerMessageEntity.uid }?.id
          )
          val messageItemBasedOnDataFromServer = Message(
            messageEntity = messageEntity,
            type = MessagesInThreadListAdapter.Type.MESSAGE_COLLAPSED,
            isHeadersDetailsExpanded = false,
            attachments = emptyList()
          )

          // this code prevent redundant UI updates
          currentList.firstOrNull {
            it.id == messageItemBasedOnDataFromServer.id
          }?.let { item ->
            val existingMessageItem = item as Message
            if (existingMessageItem.messageEntity.flagsValueSet != messageEntity.flagsValueSet) {
              messageItemBasedOnDataFromServer
            } else {
              existingMessageItem.copy(messageEntity = messageEntity)
            }
          } ?: messageItemBasedOnDataFromServer
        }

        val threadHeader = prepareThreadHeader(
          messageEntity = roomDatabase.msgDao().getMsgById(threadMessageEntityId),
          threadInfo = threadInfo
        )

        return Result.success(
          Data(
            silentUpdate = silentUpdate,
            list = listOf(threadHeader) + finalList
          )
        )
      } catch (e: Exception) {
        e.printStackTraceIfDebugOnly()
        return Result.exception(e)
      }
    }
  }

  private suspend fun prepareThreadHeader(
    messageEntity: MessageEntity?,
    threadInfo: GmailThreadInfo? = null
  ): MessagesInThreadListAdapter.ThreadHeader =
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
        val latestLabelIds = (threadInfo ?: GmailApiHelper.loadThreadInfo(
          context = getApplication(),
          accountEntity = account,
          localFolder = localFolder,
          threadId = messageEntity?.threadIdAsHEX ?: "",
          fields = listOf("id", "messages/labelIds"),
          format = GmailApiHelper.RESPONSE_FORMAT_MINIMAL
        ))?.labels ?: error("Thread not found")
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
        if (existingResult.data?.list.isNullOrEmpty()) {
          return@collectLatest
        } else {
          loadMessagesManuallyMutableStateFlow.update {
            val existingValue = existingResult.data ?: Data(false, emptyList())
            val updatedList = existingValue.list.toMutableList().apply {
              replaceAll { item ->
                if (item.type == MessagesInThreadListAdapter.Type.HEADER) {
                  (item as MessagesInThreadListAdapter.ThreadHeader).copy(labels = threadHeader.labels)
                } else {
                  item
                }
              }
            }
            Result.success(existingValue.copy(list = updatedList))
          }
        }
      }
    }
  }

  private suspend fun keepThreadMessageEntityFresh() {
    messagesInThreadFlow.collectLatest {
      if (it.status == Result.Status.SUCCESS) {
        val messageItems = it.data?.list?.filterIsInstance<Message>() ?: return@collectLatest
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
                    messageEntity.flags?.plus(" ${MessageFlag.SEEN.value}")
                  }
                } else {
                  messageEntity.flagsStringAfterRemoveSome(MessageFlag.SEEN.value)
                }
              )
            )
          }
        }
      }
    }
  }

  private suspend fun setupAutoSignatureReVerification() {
    sessionFromRecipientsStateFlow.collectLatest {
      if (!recipientLookUpManager.hasActiveJobs() && it.isNotEmpty()) {
        val recipientsWithUsablePubKey = sessionFromRecipientsStateFlow.value
          .filter { recipientInfo ->
            recipientInfo.recipientWithPubKeys.hasUsablePubKey()
          }.map { recipientInfo ->
            recipientInfo.recipientWithPubKeys.recipient.email
          }

        val messagesWithActiveSignatureVerification = messagesInThreadFlow.value.data?.list
          ?.filterIsInstance<Message>()
          ?.filter { message ->
            message.hasActiveSignatureVerification
          } ?: emptyList()

        messagesWithActiveSignatureVerification.forEach { message ->
          val messageFromAddresses = message.incomingMessageInfo?.getFrom()
            ?.map { internetAddress ->
              internetAddress.address.lowercase()
            } ?: emptyList()

          if (recipientsWithUsablePubKey.containsAll(messageFromAddresses)) {
            try {
              val messageEntity = message.messageEntity
              val existedMsgSnapshot =
                requireNotNull(MsgsCacheManager.getMsgSnapshot(messageEntity.id.toString()))
              val verificationResult = requireNotNull(
                existedMsgSnapshot.processing(
                  context = getApplication(),
                  accountEntity = getActiveAccountSuspend() ?: error("Account is null"),
                  skipAttachmentsRawData = true
                ).data?.verificationResult
              )
              onMessageChanged(
                message.copy(
                  incomingMessageInfo = message.incomingMessageInfo?.copy(
                    verificationResult = verificationResult
                  ),
                  hasActiveSignatureVerification = false
                )
              )
            } catch (e: Exception) {
              onMessageChanged(message.copy(hasActiveSignatureVerification = false))
            }
          } else {
            onMessageChanged(message.copy(hasActiveSignatureVerification = false))
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

  data class Data(
    val silentUpdate: Boolean,
    val list: List<MessagesInThreadListAdapter.Item>,
    val timeSnapshot: Long = System.currentTimeMillis()
  )

  companion object {
    val FROM = FROM()
  }
}