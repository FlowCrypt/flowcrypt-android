/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.paging.Config
import androidx.paging.PagedList
import androidx.paging.toLiveData
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.IMAPStoreManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.gmail.GmailHistoryHandler
import com.flowcrypt.email.api.email.gmail.api.GmaiAPIMimeMessage
import com.flowcrypt.email.api.email.gmail.model.GmailThreadInfo
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.model.MessageFlag
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.AttachmentEntity
import com.flowcrypt.email.database.entity.LabelEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.database.entity.MessageEntity.Companion.LABEL_IDS_SEPARATOR
import com.flowcrypt.email.extensions.isAppForegrounded
import com.flowcrypt.email.extensions.java.lang.printStackTraceIfDebugOnly
import com.flowcrypt.email.extensions.kotlin.toHex
import com.flowcrypt.email.jetpack.workmanager.sync.SyncDraftsWorker
import com.flowcrypt.email.jetpack.workmanager.sync.UploadDraftsWorker
import com.flowcrypt.email.service.MessagesNotificationManager
import com.flowcrypt.email.util.FileAndDirectoryUtils
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.OutgoingMessagesManager
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.gmail.model.ListDraftsResponse
import com.google.api.services.gmail.model.ListMessagesResponse
import jakarta.mail.FetchProfile
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.Store
import jakarta.mail.UIDFolder
import jakarta.mail.internet.InternetAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.angus.mail.imap.IMAPFolder
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.net.HttpURLConnection
import java.util.Properties


/**
 * @author Denys Bondarenko
 */
class MessagesViewModel(application: Application) : AccountViewModel(application) {
  private var searchNextPageToken: String? = null
  private val controlledRunnerForRefreshing = ControlledRunner<Result<Boolean?>>()
  private val controlledRunnerForLoadNextMessages = ControlledRunner<Result<Boolean?>>()

  private val boundaryCallback = object : PagedList.BoundaryCallback<MessageEntity>() {
    override fun onZeroItemsLoaded() {
      super.onZeroItemsLoaded()
      loadMsgsFromRemoteServer()
    }

    override fun onItemAtEndLoaded(itemAtEnd: MessageEntity) {
      super.onItemAtEndLoaded(itemAtEnd)
      loadMsgsFromRemoteServer()
    }
  }

  private val foldersLiveData = MutableLiveData<LocalFolder>()

  var msgsLiveData: LiveData<PagedList<MessageEntity>>? = foldersLiveData.switchMap { localFolder ->
    liveData {
      cancelActionsForPreviousFolder()
      val account = roomDatabase.accountDao().getActiveAccountSuspend()?.email ?: ""

      val label = if (localFolder.searchQuery.isNullOrEmpty()) {
        localFolder.fullName
      } else {
        JavaEmailConstants.FOLDER_SEARCH
      }

      emitSource(
        roomDatabase.msgDao().getMessagesDataSourceFactory(account, label)
          .toLiveData(
            config = Config(pageSize = JavaEmailConstants.COUNT_OF_LOADED_EMAILS_BY_STEP / 3),
            boundaryCallback = boundaryCallback
          )
      )
    }
  }

  val msgStatesLiveData = MutableLiveData<MessageState>()
  var outboxMsgsLiveData: LiveData<List<MessageEntity>> = activeAccountLiveData.switchMap {
    roomDatabase.msgDao().getOutboxMsgsLD(it?.email ?: "")
  }

  val loadMsgsFromRemoteServerLiveData = MutableLiveData<Result<Boolean?>>()
  val refreshMsgsLiveData = MutableLiveData<Result<Boolean?>>()

  val msgsCountLiveData = loadMsgsFromRemoteServerLiveData.switchMap {
    liveData {
      if (it.status != Result.Status.SUCCESS) return@liveData
      val account = roomDatabase.accountDao().getActiveAccountSuspend()?.email ?: return@liveData
      val folder = foldersLiveData.value ?: return@liveData
      val label = if (folder.searchQuery.isNullOrEmpty()) {
        folder.fullName
      } else {
        JavaEmailConstants.FOLDER_SEARCH
      }
      emit(roomDatabase.msgDao().countSuspend(account, label))
    }
  }

  fun switchFolder(newFolder: LocalFolder, deleteAllMsgs: Boolean, forceClearFolderCache: Boolean) {
    if (foldersLiveData.value == newFolder && !deleteAllMsgs && !forceClearFolderCache) {
      return
    }

    if (newFolder.isDrafts) {
      SyncDraftsWorker.enqueue(getApplication())
      UploadDraftsWorker.enqueue(getApplication())
    }

    viewModelScope.launch {
      val label = if (newFolder.searchQuery.isNullOrEmpty()) {
        newFolder.fullName
      } else {
        JavaEmailConstants.FOLDER_SEARCH
      }

      val accountEntity = getActiveAccountSuspend()
      if (accountEntity != null) {
        when {
          deleteAllMsgs -> {
            roomDatabase.msgDao().deleteAllExceptOutgoingAndDraft(getApplication(), accountEntity)
          }

          forceClearFolderCache -> {
            roomDatabase.msgDao().delete(accountEntity.email, label)
          }
        }

        when (accountEntity.accountType) {
          AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
            if (FoldersManager.FolderType.INBOX != newFolder.getFolderType()) {
              clearHistoryIdForLabel(accountEntity, label)
            }
          }
        }
      }

      foldersLiveData.value = newFolder
    }
  }

  fun refreshMsgs(localFolder: LocalFolder) {
    viewModelScope.launch {
      val accountEntity = getActiveAccountSuspend()
      accountEntity?.let {
        refreshMsgsLiveData.value = Result.loading()
        refreshMsgsLiveData.value = controlledRunnerForRefreshing.cancelPreviousThenRun {
          return@cancelPreviousThenRun if (accountEntity.useAPI) {
            GmailApiHelper.executeWithResult {
              refreshMsgsInternal(accountEntity, localFolder)
            }
          } else {
            IMAPStoreManager.getConnection(accountEntity.id)?.executeWithResult { store ->
              refreshMsgsInternal(accountEntity, store, localFolder)
            } ?: Result.exception(
              NullPointerException(
                "There is no active connection for ${accountEntity.email}"
              )
            )
          }
        }

        if (localFolder.isDrafts) {
          SyncDraftsWorker.enqueue(getApplication())
          UploadDraftsWorker.enqueue(getApplication())
        }
      }
    }
  }

  fun loadMsgsFromRemoteServer() {
    viewModelScope.launch {
      val localFolder = foldersLiveData.value ?: return@launch
      if (localFolder.isOutbox) {
        loadMsgsFromRemoteServerLiveData.value = Result.success(true)
        return@launch
      }

      val accountEntity = roomDatabase.accountDao().getActiveAccountSuspend()?.withDecryptedInfo()
      accountEntity?.let {
        val totalItemsCount = roomDatabase.msgDao().getMsgsCount(
          accountEntity.email,
          if (localFolder.searchQuery.isNullOrEmpty()) localFolder.fullName else JavaEmailConstants.FOLDER_SEARCH
        )

        loadMsgsFromRemoteServerLiveData.value = Result.loading()
        loadMsgsFromRemoteServerLiveData.value = Result.loading(
          progress = 10.0,
          resultCode = R.id.progress_id_start_of_loading_new_messages
        )
        loadMsgsFromRemoteServerLiveData.value =
          controlledRunnerForLoadNextMessages.cancelPreviousThenRun {
            return@cancelPreviousThenRun if (accountEntity.useAPI) {
              GmailApiHelper.executeWithResult {
                if (localFolder.searchQuery.isNullOrEmpty()) {
                  loadMsgsFromRemoteServerAndStoreLocally(
                    accountEntity,
                    localFolder,
                    totalItemsCount
                  )
                } else {
                  searchMsgsOnRemoteServerAndStoreLocally(
                    accountEntity,
                    localFolder,
                    totalItemsCount
                  )
                }
              }
            } else {
              IMAPStoreManager.getConnection(accountEntity.id)?.executeWithResult { store ->
                if (localFolder.searchQuery.isNullOrEmpty()) {
                  loadMsgsFromRemoteServerAndStoreLocally(
                    accountEntity,
                    store,
                    localFolder,
                    totalItemsCount
                  )
                } else {
                  searchMsgsOnRemoteServerAndStoreLocally(
                    accountEntity,
                    store,
                    localFolder,
                    totalItemsCount
                  )
                }
              }
                ?: Result.exception(NullPointerException("There is no active connection for ${accountEntity.email}"))
            }
          }
      }
    }
  }

  fun deleteOutgoingMsgs(entities: Iterable<MessageEntity>) {
    val app = getApplication<Application>()

    viewModelScope.launch {
      var needUpdateOutboxLabel = false
      for (entity in entities) {
        val isMsgDeleted = with(entity) {
          roomDatabase.msgDao().deleteOutgoingMsg(account, folder, uid) > 0
        }

        if (isMsgDeleted) {
          //delete outgoing info if exist
          entity.id?.let { id ->
            OutgoingMessagesManager.deleteOutgoingMessage(getApplication(), id)
          }

          needUpdateOutboxLabel = true
          if (entity.hasAttachments == true) {
            try {
              val parentDirName = entity.attachmentsDirectory
              parentDirName?.let {
                val dir = File(File(app.cacheDir, Constants.ATTACHMENTS_CACHE_DIR), it)
                FileAndDirectoryUtils.deleteDir(dir)
              }
            } catch (e: IOException) {
              e.printStackTrace()
            }
          }
        }
      }

      if (needUpdateOutboxLabel) {
        updateOutboxMsgsCount(getActiveAccountSuspend())
      }
    }
  }

  fun changeMsgsState(
    ids: Collection<Long>, localFolder: LocalFolder, newMsgState: MessageState,
    notifyMsgStatesListener: Boolean = true
  ) {
    viewModelScope.launch {
      val entities = roomDatabase.msgDao().getMsgsByIDSuspend(localFolder.account,
        localFolder.fullName, ids.map { it })

      if (localFolder.isOutbox) {
        if (newMsgState == MessageState.PENDING_DELETING) {
          deleteOutgoingMsgs(entities)
          return@launch
        }
      }

      val candidates = prepareCandidates(entities, newMsgState)
      roomDatabase.msgDao().updateSuspend(candidates)
      if (notifyMsgStatesListener) {
        msgStatesLiveData.postValue(newMsgState)
      }
    }
  }

  private fun prepareCandidates(
    entities: Iterable<MessageEntity>,
    newMsgState: MessageState
  ): Iterable<MessageEntity> {
    val candidates = mutableListOf<MessageEntity>()

    for (msgEntity in entities) {
      if (msgEntity.msgState in listOf(
          MessageState.SENDING,
          MessageState.SENT_WITHOUT_LOCAL_COPY,
          MessageState.QUEUED_MAKE_COPY_IN_SENT_FOLDER
        )
      ) {
        continue
      }

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
            flags = msgEntity.flagsStringAfterRemoveSome(MessageFlag.SEEN.value)
          )
        }

        else -> {
          msgEntity.copy(state = newMsgState.value)
        }
      }

      candidates.add(candidate)
    }

    return candidates
  }

  private suspend fun loadMsgsFromRemoteServerAndStoreLocally(
    accountEntity: AccountEntity, store: Store,
    localFolder: LocalFolder,
    countOfAlreadyLoadedMsgs: Int
  ): Result<Boolean?> = withContext(Dispatchers.IO) {
    store.getFolder(localFolder.fullName).use { folder ->
      val imapFolder = folder as IMAPFolder
      loadMsgsFromRemoteServerLiveData.postValue(
        Result.loading(
          progress = 70.0,
          resultCode = R.id.progress_id_opening_store
        )
      )
      imapFolder.open(Folder.READ_ONLY)

      val countOfLoadedMsgs = when {
        countOfAlreadyLoadedMsgs < 0 -> 0
        else -> countOfAlreadyLoadedMsgs
      }

      val isOnlyPgpModeEnabled = accountEntity.showOnlyEncrypted
      var foundMsgs: Array<Message> = emptyArray()
      var msgsCount = 0

      if (isOnlyPgpModeEnabled == true) {
        foundMsgs = imapFolder.search(EmailUtil.genPgpThingsSearchTerm(accountEntity))
        foundMsgs?.let {
          msgsCount = foundMsgs.size
        }
      } else {
        msgsCount = imapFolder.messageCount
      }

      val end = msgsCount - countOfLoadedMsgs
      val startCandidate = end - JavaEmailConstants.COUNT_OF_LOADED_EMAILS_BY_STEP + 1
      val start = when {
        startCandidate < 1 -> 1
        else -> startCandidate
      }
      val folderName = imapFolder.fullName

      roomDatabase.labelDao()
        .getLabelSuspend(accountEntity.email, accountEntity.accountType, folderName)?.let {
          roomDatabase.labelDao().updateSuspend(it.copy(messagesTotal = msgsCount))
        }

      loadMsgsFromRemoteServerLiveData.postValue(
        Result.loading(
          progress = 80.0,
          resultCode = R.id.progress_id_getting_list_of_emails
        )
      )
      if (end < 1) {
        handleReceivedMessages(accountEntity, localFolder, imapFolder)
      } else {
        val msgs: Array<Message> = if (isOnlyPgpModeEnabled == true) {
          foundMsgs.copyOfRange(start - 1, end)
        } else {
          imapFolder.getMessages(start, end)
        }

        //fetch base details
        EmailUtil.fetchMsgs(imapFolder, msgs)

        //here we do additional search over fetched messages(over the content) to check PGP things
        val hasPgpAfterAdditionalSearchSet =
          imapFolder.search(EmailUtil.genPgpThingsSearchTerm(accountEntity), msgs)
            .map { imapFolder.getUID(it) }.toSet()

        handleReceivedMessages(
          account = accountEntity,
          localFolder = localFolder,
          remoteFolder = folder,
          msgs = msgs,
          hasPgpAfterAdditionalSearchSet = hasPgpAfterAdditionalSearchSet
        )
      }
    }

    return@withContext Result.success(true)
  }

  private suspend fun loadMsgsFromRemoteServerAndStoreLocally(
    accountEntity: AccountEntity,
    localFolder: LocalFolder,
    totalItemsCount: Int
  ): Result<Boolean?> = withContext(Dispatchers.IO) {
    when (accountEntity.accountType) {
      AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
        val labelEntity: LabelEntity? = roomDatabase.labelDao()
          .getLabelSuspend(accountEntity.email, accountEntity.accountType, localFolder.fullName)
        loadMsgsFromRemoteServerLiveData.postValue(
          Result.loading(
            progress = 20.0,
            resultCode = R.id.progress_id_gmail_list
          )
        )

        val messages: List<com.google.api.services.gmail.model.Message>
        val nextPageToken: String?
        val draftIdsMap: Map<String, String>
        val gmailThreadInfoList: List<GmailThreadInfo>

        if (accountEntity.useConversationMode) {
          val threadsResponse = GmailApiHelper.loadThreads(
            context = getApplication(),
            accountEntity = accountEntity,
            localFolder = localFolder,
            nextPageToken = if (totalItemsCount > 0) labelEntity?.nextPageToken else null
          )

          gmailThreadInfoList = GmailApiHelper.loadGmailThreadInfoInParallel(
            context = getApplication(),
            accountEntity = accountEntity,
            localFolder = localFolder,
            threads = threadsResponse.threads ?: emptyList(),
            format = GmailApiHelper.RESPONSE_FORMAT_FULL
          )

          messages = gmailThreadInfoList.map { it.lastMessage }
          nextPageToken = threadsResponse.nextPageToken
          draftIdsMap = emptyMap()
        } else {
          gmailThreadInfoList = emptyList()
          val messagesBaseInfo = GmailApiHelper.loadMsgsBaseInfo(
            context = getApplication(),
            accountEntity = accountEntity,
            localFolder = localFolder,
            nextPageToken = if (totalItemsCount > 0) labelEntity?.nextPageToken else null
          )

          draftIdsMap = when (messagesBaseInfo) {
            is ListDraftsResponse -> messagesBaseInfo.drafts?.associateBy(
              { it.message.id },
              { it.id }) ?: emptyMap()

            else -> emptyMap()
          }

          messages = when (messagesBaseInfo) {
            is ListMessagesResponse -> messagesBaseInfo.messages ?: emptyList()
            is ListDraftsResponse -> messagesBaseInfo.drafts?.map { it.message } ?: emptyList()
            else -> emptyList()
          }

          nextPageToken = when (messagesBaseInfo) {
            is ListMessagesResponse -> messagesBaseInfo.nextPageToken
            is ListDraftsResponse -> messagesBaseInfo.nextPageToken
            else -> null
          }
        }

        loadMsgsFromRemoteServerLiveData.postValue(
          Result.loading(
            progress = 70.0,
            resultCode = R.id.progress_id_gmail_msgs_info
          )
        )

        if (messages.isNotEmpty()) {
          val msgs = GmailApiHelper.loadMsgsInParallel(
            getApplication(), accountEntity, messages, localFolder
          )
          loadMsgsFromRemoteServerLiveData.postValue(
            Result.loading(
              progress = 90.0,
              resultCode = R.id.progress_id_gmail_msgs_info
            )
          )
          handleReceivedMessages(
            accountEntity,
            localFolder,
            msgs,
            draftIdsMap
          ) { message, messageEntity ->
            if (accountEntity.useConversationMode) {
              val thread = gmailThreadInfoList.firstOrNull { it.id == message.threadId }
              if (thread != null) {
                messageEntity.copy(
                  subject = thread.subject,
                  threadMessagesCount = thread.messagesCount,
                  threadDraftsCount = thread.draftsCount,
                  labelIds = thread.labels.joinToString(separator = LABEL_IDS_SEPARATOR),
                  hasAttachments = thread.hasAttachments,
                  fromAddresses = InternetAddress.toString(
                    thread.recipients.toTypedArray()
                  ),
                  hasPgp = thread.hasPgpThings,
                  //we need to identify threads by UID. But we can't have uid == threadId
                  //as the first message in a thread has such a situation.
                  //So we will use Long.MAX_VALUE to save 'threadId' modified
                  //and will be able to recreate it.
                  uid = Long.MAX_VALUE - (messageEntity.threadId ?: System.nanoTime())
                )
              } else {
                messageEntity
              }
            } else {
              messageEntity
            }
          }
        } else {
          loadMsgsFromRemoteServerLiveData.postValue(
            Result.loading(
              progress = 90.0,
              resultCode = R.id.progress_id_gmail_msgs_info
            )
          )
        }
        labelEntity?.let {
          roomDatabase.labelDao().updateSuspend(it.copy(nextPageToken = nextPageToken))
        }
      }
    }

    return@withContext Result.success(true)
  }

  private suspend fun handleReceivedMessages(
    account: AccountEntity,
    localFolder: LocalFolder,
    msgs: List<com.google.api.services.gmail.model.Message>,
    draftIdsMap: Map<String, String> = emptyMap(),
    messageEntityModificationAction: (
      message: com.google.api.services.gmail.model.Message,
      messageEntity: MessageEntity
    ) -> MessageEntity
  ) = withContext(Dispatchers.IO) {
    val email = account.email
    val folder = localFolder.fullName

    val isOnlyPgpModeEnabled = account.showOnlyEncrypted == true
    val msgEntities = MessageEntity.genMessageEntities(
      context = getApplication(),
      account = email,
      accountType = account.accountType,
      label = folder,
      msgsList = msgs,
      isNew = false,
      onlyPgpModeEnabled = isOnlyPgpModeEnabled,
      draftIdsMap = draftIdsMap
    ) { message, messageEntity ->
      messageEntityModificationAction.invoke(message, messageEntity)
    }

    roomDatabase.msgDao().insertWithReplaceSuspend(msgEntities)
    GmailApiHelper.identifyAttachments(msgEntities, msgs, account, localFolder, roomDatabase)
    val session = Session.getInstance(Properties())
    GeneralUtil.updateLocalContactsIfNeeded(
      context = getApplication(),
      messages = msgs
      .filter { it.labelIds?.contains(GmailApiHelper.LABEL_SENT) == true }
      .map { GmaiAPIMimeMessage(session, it) }.toTypedArray()
    )
  }

  private suspend fun handleReceivedMessages(
    account: AccountEntity,
    localFolder: LocalFolder,
    remoteFolder: IMAPFolder,
    msgs: Array<Message> = emptyArray(),
    hasPgpAfterAdditionalSearchSet: Set<Long> = emptySet()
  ) = withContext(Dispatchers.IO) {
    val isOnlyPgpModeEnabled = account.showOnlyEncrypted == true
    val msgEntities = MessageEntity.genMessageEntities(
      context = getApplication(),
      account = account.email,
      accountType = account.accountType,
      label = localFolder.fullName,
      folder = remoteFolder,
      msgs = msgs,
      isNew = false,
      isOnlyPgpModeEnabled = isOnlyPgpModeEnabled,
      hasPgpAfterAdditionalSearchSet = hasPgpAfterAdditionalSearchSet
    )

    roomDatabase.msgDao().insertWithReplaceSuspend(msgEntities)

    identifyAttachments(msgEntities, msgs, remoteFolder, account, localFolder, roomDatabase)
    GeneralUtil.updateLocalContactsIfNeeded(
      context = getApplication(),
      imapFolder = remoteFolder,
      messages = msgs
    )
  }

  private suspend fun identifyAttachments(
    msgEntities: List<MessageEntity>, msgs: Array<Message>,
    remoteFolder: IMAPFolder, account: AccountEntity, localFolder:
    LocalFolder, roomDatabase: FlowCryptRoomDatabase
  ) = withContext(Dispatchers.IO) {
    try {
      val savedMsgUIDsSet = msgEntities.map { it.uid }.toSet()
      val attachments = mutableListOf<AttachmentEntity>()
      for (msg in msgs) {
        if (remoteFolder.getUID(msg) in savedMsgUIDsSet) {
          val uid = remoteFolder.getUID(msg)
          attachments.addAll(EmailUtil.getAttsInfoFromPart(msg).mapNotNull { attachmentInfo ->
            AttachmentEntity.fromAttInfo(
              attachmentInfo = attachmentInfo.copy(
                email = account.email,
                folder = localFolder.fullName,
                uid = uid
              ),
              accountType = account.accountType
            )
          })
        }
      }

      roomDatabase.attachmentDao().insertWithReplaceSuspend(attachments)
    } catch (e: Exception) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }
  }

  private suspend fun searchMsgsOnRemoteServerAndStoreLocally(
    accountEntity: AccountEntity,
    localFolder: LocalFolder,
    totalItemsCount: Int
  ): Result<Boolean?> = withContext(Dispatchers.IO) {
    when (accountEntity.accountType) {
      AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
        loadMsgsFromRemoteServerLiveData.postValue(
          Result.loading(
            progress = 20.0,
            resultCode = R.id.progress_id_gmail_list
          )
        )

        val nextPageToken: String?
        val gmailThreadInfoList: List<GmailThreadInfo>
        val messages = if (accountEntity.useConversationMode) {
          val threadsResponse = GmailApiHelper.loadThreads(
            context = getApplication(),
            accountEntity = accountEntity,
            localFolder = localFolder,
            nextPageToken = if (totalItemsCount > 0) searchNextPageToken else null
          )
          nextPageToken = threadsResponse.nextPageToken

          if (threadsResponse.threads.isNullOrEmpty()) {
            gmailThreadInfoList = emptyList()
            null
          } else {
            gmailThreadInfoList = GmailApiHelper.loadGmailThreadInfoInParallel(
              context = getApplication(),
              accountEntity = accountEntity,
              localFolder = localFolder,
              threads = threadsResponse.threads ?: emptyList(),
              format = GmailApiHelper.RESPONSE_FORMAT_FULL
            )

            val messages = gmailThreadInfoList.map { it.lastMessage }
            val msgs = GmailApiHelper.loadMsgsInParallel(
              getApplication(), accountEntity, messages, localFolder
            )
            loadMsgsFromRemoteServerLiveData.postValue(
              Result.loading(
                progress = 90.0,
                resultCode = R.id.progress_id_gmail_msgs_info
              )
            )
            msgs
          }
        } else {
          gmailThreadInfoList = emptyList()
          val messagesBaseInfo = GmailApiHelper.loadMsgsBaseInfoUsingSearch(
            getApplication(), accountEntity,
            localFolder, if (totalItemsCount > 0) searchNextPageToken else null
          )
          nextPageToken = messagesBaseInfo.nextPageToken
          loadMsgsFromRemoteServerLiveData.postValue(
            Result.loading(
              progress = 70.0,
              resultCode = R.id.progress_id_gmail_msgs_info
            )
          )

          if (messagesBaseInfo.messages.isNullOrEmpty()) {
            null
          } else {
            GmailApiHelper.loadMsgsInParallel(
              getApplication(), accountEntity, messagesBaseInfo.messages
                ?: emptyList(), localFolder
            )
          }
        }

        loadMsgsFromRemoteServerLiveData.postValue(
          Result.loading(
            progress = 90.0,
            resultCode = R.id.progress_id_gmail_msgs_info
          )
        )

        if (!messages.isNullOrEmpty()) {
          handleSearchResults(
            accountEntity,
            localFolder.copy(fullName = JavaEmailConstants.FOLDER_SEARCH),
            messages
          ) { message, messageEntity ->
            if (accountEntity.useConversationMode) {
              val thread = gmailThreadInfoList.firstOrNull { it.id == message.threadId }
              if (thread != null) {
                messageEntity.copy(
                  subject = thread.subject,
                  threadMessagesCount = thread.messagesCount,
                  threadDraftsCount = thread.draftsCount,
                  labelIds = thread.labels.joinToString(separator = LABEL_IDS_SEPARATOR),
                  hasAttachments = thread.hasAttachments,
                  fromAddresses = InternetAddress.toString(
                    thread.recipients.toTypedArray()
                  ),
                  hasPgp = thread.hasPgpThings,
                  //we need to identify threads by UID. But we can't have uid == threadId
                  //as the first message in a thread has such a situation.
                  //So we will use Long.MAX_VALUE to save 'threadId' modified
                  //and will be able to recreate it.
                  uid = Long.MAX_VALUE - (messageEntity.threadId ?: System.nanoTime())
                )
              } else {
                messageEntity
              }
            } else {
              messageEntity
            }
          }
        }

        searchNextPageToken = nextPageToken
      }
    }

    return@withContext Result.success(true)
  }


  private suspend fun searchMsgsOnRemoteServerAndStoreLocally(
    accountEntity: AccountEntity, store: Store,
    localFolder: LocalFolder,
    countOfAlreadyLoadedMsgs: Int
  ): Result<Boolean?> = withContext(Dispatchers.IO) {
    store.getFolder(localFolder.fullName).use { folder ->
      val imapFolder = folder as IMAPFolder
      imapFolder.open(Folder.READ_ONLY)

      val countOfLoadedMsgs = when {
        countOfAlreadyLoadedMsgs < 0 -> 0
        else -> countOfAlreadyLoadedMsgs
      }

      val foundMsgs = imapFolder.search(EmailUtil.generateSearchTerm(accountEntity, localFolder))

      val messagesCount = foundMsgs.size
      val end = messagesCount - countOfLoadedMsgs
      val startCandidate = end - JavaEmailConstants.COUNT_OF_LOADED_EMAILS_BY_STEP + 1
      val start = when {
        startCandidate < 1 -> 1
        else -> startCandidate
      }

      loadMsgsFromRemoteServerLiveData.postValue(
        Result.loading(
          progress = 80.0,
          resultCode = R.id.progress_id_getting_list_of_emails
        )
      )

      if (end < 1) {
        handleSearchResults(account = accountEntity, remoteFolder = imapFolder)
      } else {
        val bufferedMsgs = foundMsgs.copyOfRange(start - 1, end)

        //fetch base details
        EmailUtil.fetchMsgs(imapFolder, bufferedMsgs)

//here we do additional search over fetched messages(over the content) to check PGP things
        val hasPgpAfterAdditionalSearchSet =
          imapFolder.search(
            EmailUtil.genPgpThingsSearchTerm(accountEntity),
            bufferedMsgs
          ).map { imapFolder.getUID(it) }.toSet()

        handleSearchResults(
          account = accountEntity,
          remoteFolder = imapFolder,
          msgs = bufferedMsgs,
          hasPgpAfterAdditionalSearchSet = hasPgpAfterAdditionalSearchSet
        )
      }
    }

    return@withContext Result.success(true)
  }

  private suspend fun handleSearchResults(
    account: AccountEntity,
    remoteFolder: IMAPFolder,
    msgs: Array<Message> = emptyArray(),
    hasPgpAfterAdditionalSearchSet: Set<Long> = emptySet()
  ) = withContext(Dispatchers.IO) {
    val msgEntities = MessageEntity.genMessageEntities(
      context = getApplication(),
      account = account.email,
      accountType = account.accountType,
      label = JavaEmailConstants.FOLDER_SEARCH,
      folder = remoteFolder,
      msgs = msgs,
      isNew = false,
      isOnlyPgpModeEnabled = account.showOnlyEncrypted == true,
      hasPgpAfterAdditionalSearchSet = hasPgpAfterAdditionalSearchSet
    )

    roomDatabase.msgDao().insertWithReplaceSuspend(msgEntities)

    GeneralUtil.updateLocalContactsIfNeeded(
      context = getApplication(),
      imapFolder = remoteFolder,
      messages = msgs
    )
  }

  private suspend fun handleSearchResults(
    account: AccountEntity, localFolder: LocalFolder,
    msgs: List<com.google.api.services.gmail.model.Message>,
    messageEntityModificationAction: (
      message: com.google.api.services.gmail.model.Message,
      messageEntity: MessageEntity
    ) -> MessageEntity
  ) = withContext(Dispatchers.IO) {
    val isOnlyPgpModeEnabled = account.showOnlyEncrypted == true
    val msgEntities = MessageEntity.genMessageEntities(
      context = getApplication(),
      account = account.email,
      accountType = account.accountType,
      label = localFolder.fullName,
      msgsList = msgs,
      isNew = false,
      onlyPgpModeEnabled = isOnlyPgpModeEnabled
    ) { message, messageEntity ->
      messageEntityModificationAction.invoke(message, messageEntity)
    }

    roomDatabase.msgDao().insertWithReplaceSuspend(msgEntities)
    GmailApiHelper.identifyAttachments(msgEntities, msgs, account, localFolder, roomDatabase)
    val session = Session.getInstance(Properties())
    GeneralUtil.updateLocalContactsIfNeeded(
      context = getApplication(),
      messages = msgs
      .filter { it.labelIds?.contains(GmailApiHelper.LABEL_SENT) == true }
        .map { GmaiAPIMimeMessage(session, it) }
        .toTypedArray()
    )
  }

  private suspend fun refreshMsgsInternal(
    accountEntity: AccountEntity,
    localFolder: LocalFolder
  ): Result<Boolean?> = withContext(Dispatchers.IO) {
    val newestMsg =
      roomDatabase.msgDao().getNewestMsgOrThread(accountEntity, localFolder.fullName)
    try {
      val labelEntity = roomDatabase.labelDao()
        .getLabelSuspend(accountEntity.email, accountEntity.accountType, localFolder.fullName)
      val labelEntityHistoryId = BigInteger(labelEntity?.historyId ?: "0")
      val msgEntityHistoryId = BigInteger(newestMsg?.historyId ?: "0")

      val historyList = GmailApiHelper.loadHistoryInfo(
        context = getApplication(),
        accountEntity = accountEntity,
        localFolder = localFolder,
        historyId = labelEntityHistoryId.max(msgEntityHistoryId)
      )

      GmailHistoryHandler.handleHistory(
        context = getApplication(),
        accountEntity = accountEntity,
        localFolder = localFolder,
        historyList = historyList
      )
    } catch (e: Exception) {
      e.printStackTraceIfDebugOnly()
      when (e) {
        is GoogleJsonResponseException -> {
          if (localFolder.getFolderType() == FoldersManager.FolderType.INBOX
            && e.details.code == HttpURLConnection.HTTP_NOT_FOUND
            && e.details.errors.any { it.reason.equals("notFound", true) }
          ) {
            //client must perform a full sync
            //https://developers.google.com/gmail/api/guides/sync#partial_synchronization
            roomDatabase.msgDao().delete(accountEntity.email, localFolder.fullName)
          }
        }

        else -> return@withContext Result.exception(e)
      }
    }

    return@withContext Result.success(true)
  }

  private suspend fun refreshMsgsInternal(
    accountEntity: AccountEntity,
    store: Store,
    localFolder: LocalFolder
  ): Result<Boolean?> = withContext(Dispatchers.IO) {
    store.getFolder(localFolder.fullName).use { folder ->
      val imapFolder = folder as IMAPFolder
      imapFolder.open(Folder.READ_ONLY)
      val folderName = localFolder.fullName

      val newestCachedUID = roomDatabase.msgDao()
        .getLastUIDOfMsgForLabelSuspend(accountEntity.email, folderName) ?: 0
      val oldestCachedUID = roomDatabase.msgDao()
        .getOldestUIDOfMsgForLabelSuspend(accountEntity.email, folderName) ?: 0
      val cachedUIDSet =
        roomDatabase.msgDao().getUIDsForLabel(accountEntity.email, folderName).toSet()
      val updatedMsgs = EmailUtil.getUpdatedMsgsByUID(
        imapFolder,
        oldestCachedUID.toLong(),
        newestCachedUID.toLong()
      )

      val newMsgsAfterLastInLocalCache = if (accountEntity.showOnlyEncrypted == true) {
        val foundMsgs = imapFolder.search(EmailUtil.genPgpThingsSearchTerm(accountEntity))

        val fetchProfile = FetchProfile()
        fetchProfile.add(UIDFolder.FetchProfileItem.UID)

        imapFolder.fetch(foundMsgs, fetchProfile)

        val newMsgsList = mutableListOf<Message>()

        for (message in foundMsgs) {
          if (imapFolder.getUID(message) > newestCachedUID) {
            newMsgsList.add(message)
          }
        }

        EmailUtil.fetchMsgs(imapFolder, newMsgsList.toTypedArray())
      } else {
        val newestMsgsFromFetchExceptExisted =
          imapFolder.getMessagesByUID(newestCachedUID.toLong(), UIDFolder.LASTUID)
            .filterNot { imapFolder.getUID(it) in cachedUIDSet }
            .filterNotNull()
        val msgs =
          newestMsgsFromFetchExceptExisted + updatedMsgs.filter { imapFolder.getUID(it) !in cachedUIDSet }
        EmailUtil.fetchMsgs(imapFolder, msgs.toTypedArray())
      }

      //here we do additional search over fetched messages(over the content) to check PGP things
      val hasPgpAfterAdditionalSearchSet =
        imapFolder.search(
          EmailUtil.genPgpThingsSearchTerm(accountEntity),
          newMsgsAfterLastInLocalCache
        ).map { imapFolder.getUID(it) }.toSet()

      handleRefreshedMsgs(
        accountEntity = accountEntity,
        localFolder = localFolder,
        remoteFolder = imapFolder,
        newMsgs = newMsgsAfterLastInLocalCache,
        hasPgpAfterAdditionalSearchSet = hasPgpAfterAdditionalSearchSet,
        updatedMsgs = updatedMsgs
      )
    }

    return@withContext Result.success(true)
  }

  private suspend fun handleRefreshedMsgs(
    accountEntity: AccountEntity,
    localFolder: LocalFolder,
    remoteFolder: IMAPFolder,
    newMsgs: Array<Message>,
    hasPgpAfterAdditionalSearchSet: Set<Long>,
    updatedMsgs: Array<Message>
  ) = withContext(Dispatchers.IO) {
    val context: Context = getApplication()
    val email = accountEntity.email
    val folderName = localFolder.fullName

    val mapOfUIDAndMsgFlags = roomDatabase.msgDao().getMapOfUIDAndMsgFlagsSuspend(email, folderName)
    val msgsUIDs = HashSet(mapOfUIDAndMsgFlags.keys)
    val deleteCandidatesUIDs = EmailUtil.genDeleteCandidates(msgsUIDs, remoteFolder, updatedMsgs)

    roomDatabase.msgDao().deleteByUIDsSuspend(accountEntity.email, folderName, deleteCandidatesUIDs)

    val folderType = FoldersManager.getFolderType(localFolder)
    if (!context.isAppForegrounded() && folderType === FoldersManager.FolderType.INBOX) {
      val notificationManager = MessagesNotificationManager(getApplication())
      for (uid in deleteCandidatesUIDs) {
        notificationManager.cancel(uid.toHex())
      }
    }

    val newCandidates = EmailUtil.genNewCandidates(msgsUIDs, remoteFolder, newMsgs)

    val isOnlyPgpModeEnabled = accountEntity.showOnlyEncrypted == true
    val isNew = !context.isAppForegrounded() && folderType === FoldersManager.FolderType.INBOX

    val msgEntities = MessageEntity.genMessageEntities(
      context = getApplication(),
      account = email,
      accountType = accountEntity.accountType,
      label = folderName,
      folder = remoteFolder,
      msgs = newCandidates,
      isNew = isNew,
      isOnlyPgpModeEnabled = isOnlyPgpModeEnabled,
      hasPgpAfterAdditionalSearchSet = hasPgpAfterAdditionalSearchSet
    )

    roomDatabase.msgDao().insertWithReplaceSuspend(msgEntities)

    val updateCandidates =
      EmailUtil.genUpdateCandidates(mapOfUIDAndMsgFlags, remoteFolder, updatedMsgs)
        .associate { remoteFolder.getUID(it) to it.flags }
    roomDatabase.msgDao().updateFlagsSuspend(accountEntity.email, folderName, updateCandidates)

    GeneralUtil.updateLocalContactsIfNeeded(getApplication(), remoteFolder, newCandidates)
  }

  private suspend fun clearHistoryIdForLabel(accountEntity: AccountEntity, label: String) {
    val labelEntity: LabelEntity? =
      roomDatabase.labelDao().getLabelSuspend(accountEntity.email, accountEntity.accountType, label)
    labelEntity?.let { roomDatabase.labelDao().updateSuspend(it.copy(historyId = null)) }
  }

  private suspend fun cancelActionsForPreviousFolder() {
    refreshMsgsLiveData.value = controlledRunnerForRefreshing.cancelPreviousThenRun {
      return@cancelPreviousThenRun Result.none()
    }
    loadMsgsFromRemoteServerLiveData.value =
      controlledRunnerForLoadNextMessages.cancelPreviousThenRun {
        return@cancelPreviousThenRun Result.none()
      }
  }
}
