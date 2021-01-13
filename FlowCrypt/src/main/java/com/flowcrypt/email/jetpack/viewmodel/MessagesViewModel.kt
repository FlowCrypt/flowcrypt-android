/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
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
import com.flowcrypt.email.api.email.gmail.api.GmaiAPIMimeMessage
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.model.MessageFlag
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.AttachmentEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.uid
import com.flowcrypt.email.jetpack.workmanager.sync.CheckIsLoadedMessagesEncryptedWorker
import com.flowcrypt.email.model.EmailAndNamePair
import com.flowcrypt.email.service.EmailAndNameUpdaterService
import com.flowcrypt.email.service.MessagesNotificationManager
import com.flowcrypt.email.ui.activity.SearchMessagesActivity
import com.flowcrypt.email.util.FileAndDirectoryUtils
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.sun.mail.gimap.GmailRawSearchTerm
import com.sun.mail.imap.IMAPFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.*
import javax.mail.FetchProfile
import javax.mail.Folder
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.Store
import javax.mail.UIDFolder
import javax.mail.internet.InternetAddress
import javax.mail.search.AndTerm
import javax.mail.search.BodyTerm
import javax.mail.search.FromStringTerm
import javax.mail.search.OrTerm
import javax.mail.search.RecipientStringTerm
import javax.mail.search.SearchTerm
import javax.mail.search.StringTerm
import javax.mail.search.SubjectTerm
import kotlin.collections.ArrayList


/**
 * @author Denis Bondarenko
 *         Date: 12/17/19
 *         Time: 4:37 PM
 *         E-mail: DenBond7@gmail.com
 */
class MessagesViewModel(application: Application) : AccountViewModel(application) {
  private var currentLocalFolder: LocalFolder? = null
  private var nextPageToken: String? = null

  val msgStatesLiveData = MutableLiveData<MessageState>()
  var msgsLiveData: LiveData<PagedList<MessageEntity>>? = null
  var outboxMsgsLiveData: LiveData<List<MessageEntity>> = Transformations.switchMap(activeAccountLiveData) {
    roomDatabase.msgDao().getOutboxMsgsLD(it?.email ?: "")
  }

  val loadMsgsFromRemoteServerLiveData = MutableLiveData<Result<Boolean?>>()
  val refreshMsgsLiveData = MutableLiveData<Result<Boolean?>>()

  fun refreshMsgs(localFolder: LocalFolder) {
    viewModelScope.launch {
      val accountEntity = getActiveAccountSuspend()
      accountEntity?.let {
        refreshMsgsLiveData.value = Result.loading()
        val connection = IMAPStoreManager.activeConnections[accountEntity.id]
        if (connection == null) {
          refreshMsgsLiveData.value = Result.exception(NullPointerException("There is no active connection for ${accountEntity.email}"))
        } else {
          refreshMsgsLiveData.value = connection.executeWithResult {
            refreshMsgsInternal(accountEntity, connection.store, localFolder)
          }
        }
      }
    }
  }

  fun loadMsgsFromRemoteServer(localFolder: LocalFolder, totalItemsCount: Int) {
    viewModelScope.launch {
      if (totalItemsCount == 0) {
        nextPageToken = null
      }
      val accountEntity = getActiveAccountSuspend()
      accountEntity?.let {
        loadMsgsFromRemoteServerLiveData.value = Result.loading()
        if (accountEntity.useAPI) {
          loadMsgsFromRemoteServerLiveData.value = if (localFolder.searchQuery.isNullOrEmpty()) {
            loadMsgsFromRemoteServerAndStoreLocally(accountEntity, localFolder, totalItemsCount)
          } else {
            Result.success(true)
            //searchMsgsOnRemoteServerAndStoreLocally(accountEntity, connection.store, localFolder, totalItemsCount)
          }
        } else {
          val connection = IMAPStoreManager.activeConnections[accountEntity.id]
          if (connection == null) {
            loadMsgsFromRemoteServerLiveData.value = Result.exception(NullPointerException("There is no active connection for ${accountEntity.email}"))
          } else {
            loadMsgsFromRemoteServerLiveData.value = connection.executeWithResult {
              if (localFolder.searchQuery.isNullOrEmpty()) {
                loadMsgsFromRemoteServerAndStoreLocally(accountEntity, connection.store, localFolder, totalItemsCount)
              } else {
                searchMsgsOnRemoteServerAndStoreLocally(accountEntity, connection.store, localFolder, totalItemsCount)
              }
            }
          }
        }
      }
    }
  }

  fun loadMsgsFromLocalCache(lifecycleOwner: LifecycleOwner, localFolder: LocalFolder?,
                             observer: Observer<PagedList<MessageEntity>>,
                             boundaryCallback: PagedList.BoundaryCallback<MessageEntity>,
                             forceClearFolderCache: Boolean = false,
                             deleteAllMsgs: Boolean = false) {
    viewModelScope.launch {
      val label = if (localFolder?.searchQuery.isNullOrEmpty()) {
        localFolder?.fullName ?: ""
      } else {
        SearchMessagesActivity.SEARCH_FOLDER_NAME
      }

      val resetObserver = {
        val isSearchFolder = label == SearchMessagesActivity.SEARCH_FOLDER_NAME
        (currentLocalFolder?.fullName == localFolder?.fullName).not() || isSearchFolder ||
            deleteAllMsgs || forceClearFolderCache
      }

      if (resetObserver()) {
        nextPageToken = null
        msgsLiveData?.removeObserver(observer)
      }

      if (deleteAllMsgs) {
        roomDatabase.msgDao().deleteAllExceptOutgoing(getActiveAccountSuspend()?.email)
      } else if (forceClearFolderCache) {
        roomDatabase.msgDao().delete(getActiveAccountSuspend()?.email, label)
      }

      if (resetObserver()) {
        msgsLiveData = Transformations.switchMap(activeAccountLiveData) {
          val account = it?.email ?: ""
          roomDatabase.msgDao().getMessagesDataSourceFactory(account, label)
              .toLiveData(
                  config = Config(
                      pageSize = JavaEmailConstants.COUNT_OF_LOADED_EMAILS_BY_STEP / 3),
                  boundaryCallback = boundaryCallback)
        }

        msgsLiveData?.observe(lifecycleOwner, observer)
        currentLocalFolder = localFolder
      }
    }
  }

  fun cleanFolderCache(folderName: String?) {
    viewModelScope.launch {
      roomDatabase.msgDao().delete(getActiveAccountSuspend()?.email, folderName)
    }
  }

  fun deleteOutgoingMsgs(entities: Iterable<MessageEntity>) {
    val app = getApplication<Application>()

    viewModelScope.launch {
      var needUpdateOutboxLabel = false
      for (entity in entities) {
        val isMsgDeleted = with(entity) {
          roomDatabase.msgDao().deleteOutgoingMsg(email, folder, uid) > 0
        }

        if (isMsgDeleted) {
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

  fun changeMsgsState(ids: Collection<Long>, localFolder: LocalFolder, newMsgState: MessageState,
                      notifyMsgStatesListener: Boolean = true) {
    viewModelScope.launch {
      val entities = roomDatabase.msgDao().getMsgsByIDSuspend(localFolder.account,
          localFolder.fullName, ids.map { it })

      if (JavaEmailConstants.FOLDER_OUTBOX.equals(localFolder.fullName, ignoreCase = true)) {
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

  private fun prepareCandidates(entities: Iterable<MessageEntity>, newMsgState: MessageState): Iterable<MessageEntity> {
    val candidates = mutableListOf<MessageEntity>()

    for (msgEntity in entities) {
      if (msgEntity.msgState in listOf(MessageState.SENDING, MessageState.SENT_WITHOUT_LOCAL_COPY, MessageState.QUEUED_MAKE_COPY_IN_SENT_FOLDER)) {
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
              })
        }

        MessageState.PENDING_MARK_UNREAD -> {
          msgEntity.copy(
              state = newMsgState.value,
              flags = msgEntity.flags?.replace(MessageFlag.SEEN.value, ""))
        }

        else -> {
          msgEntity.copy(state = newMsgState.value)
        }
      }

      candidates.add(candidate)
    }

    return candidates
  }

  private suspend fun loadMsgsFromRemoteServerAndStoreLocally(accountEntity: AccountEntity, store: Store,
                                                              localFolder: LocalFolder,
                                                              countOfAlreadyLoadedMsgs: Int
  ): Result<Boolean?> = withContext(Dispatchers.IO) {
    store.getFolder(localFolder.fullName).use { folder ->
      val imapFolder = folder as IMAPFolder
      loadMsgsFromRemoteServerLiveData.postValue(Result.loading(progress = 70.0, resultCode = R.id.progress_id_opening_store))
      imapFolder.open(Folder.READ_ONLY)

      val countOfLoadedMsgs = when {
        countOfAlreadyLoadedMsgs < 0 -> 0
        else -> countOfAlreadyLoadedMsgs
      }

      val isEncryptedModeEnabled = accountEntity.isShowOnlyEncrypted
      var foundMsgs: Array<Message> = emptyArray()
      var msgsCount = 0

      if (isEncryptedModeEnabled == true) {
        foundMsgs = imapFolder.search(EmailUtil.genEncryptedMsgsSearchTerm(accountEntity))
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

      val roomDatabase = FlowCryptRoomDatabase.getDatabase(getApplication())
      roomDatabase.labelDao().getLabelSuspend(accountEntity.email, accountEntity.accountType, folderName)?.let {
        roomDatabase.labelDao().update(it.copy(messagesTotal = msgsCount))
      }

      loadMsgsFromRemoteServerLiveData.postValue(Result.loading(
          progress = 80.0,
          resultCode = R.id.progress_id_getting_list_of_emails
      ))
      if (end < 1) {
        handleReceivedMsgs(accountEntity, localFolder, imapFolder, arrayOf())
      } else {
        val msgs: Array<Message> = if (isEncryptedModeEnabled == true) {
          foundMsgs.copyOfRange(start - 1, end)
        } else {
          imapFolder.getMessages(start, end)
        }

        val fetchProfile = FetchProfile()
        fetchProfile.add(FetchProfile.Item.ENVELOPE)
        fetchProfile.add(FetchProfile.Item.FLAGS)
        fetchProfile.add(FetchProfile.Item.CONTENT_INFO)
        fetchProfile.add(UIDFolder.FetchProfileItem.UID)
        imapFolder.fetch(msgs, fetchProfile)

        handleReceivedMsgs(accountEntity, localFolder, folder, msgs)
      }
    }

    return@withContext Result.success(true)
  }

  private suspend fun loadMsgsFromRemoteServerAndStoreLocally(accountEntity: AccountEntity,
                                                              localFolder: LocalFolder,
                                                              countOfAlreadyLoadedMsgs: Int
  ): Result<Boolean?> = withContext(Dispatchers.IO) {
    when (accountEntity.accountType) {
      AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
        loadMsgsFromRemoteServerLiveData.postValue(Result.loading(progress = 20.0, resultCode = R.id.progress_id_gmail_list))
        val messagesBaseInfo = GmailApiHelper.loadMsgsBaseInfo(getApplication(), accountEntity, localFolder, nextPageToken)
        loadMsgsFromRemoteServerLiveData.postValue(Result.loading(progress = 50.0, resultCode = R.id.progress_id_gmail_list))
        nextPageToken = messagesBaseInfo.nextPageToken
        loadMsgsFromRemoteServerLiveData.postValue(Result.loading(progress = 70.0, resultCode = R.id.progress_id_gmail_msgs_info))
        val msgs = GmailApiHelper.loadMsgsShortInfo(getApplication(), accountEntity, messagesBaseInfo, localFolder)
        loadMsgsFromRemoteServerLiveData.postValue(Result.loading(progress = 90.0, resultCode = R.id.progress_id_gmail_msgs_info))
        handleReceivedMsgs(accountEntity, localFolder, msgs)
      }
    }

    return@withContext Result.success(true)
  }

  private suspend fun handleReceivedMsgs(account: AccountEntity, localFolder: LocalFolder,
                                         msgs: List<com.google.api.services.gmail.model.Message>) = withContext(Dispatchers.IO) {
    val email = account.email
    val folder = localFolder.fullName
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(getApplication())

    val isEncryptedModeEnabled = account.isShowOnlyEncrypted ?: false
    val msgEntities = MessageEntity.genMessageEntities(
        context = getApplication(),
        email = email,
        label = folder,
        msgsList = msgs,
        isNew = false,
        areAllMsgsEncrypted = isEncryptedModeEnabled
    )

    roomDatabase.msgDao().insertWithReplaceSuspend(msgEntities)
    identifyAttachments(msgEntities, msgs, account, localFolder, roomDatabase)
    val session = Session.getInstance(Properties())
    updateLocalContactsIfNeeded(messages = msgs
        .filter { it.labelIds.contains(GmailApiHelper.LABEL_SENT) }
        .map { GmaiAPIMimeMessage(session, it) }.toTypedArray())
  }

  private suspend fun handleReceivedMsgs(account: AccountEntity, localFolder: LocalFolder,
                                         remoteFolder: IMAPFolder, msgs: Array<Message>) = withContext(Dispatchers.IO) {
    val email = account.email
    val folder = localFolder.fullName
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(getApplication())

    val isEncryptedModeEnabled = account.isShowOnlyEncrypted ?: false
    val msgEntities = MessageEntity.genMessageEntities(
        context = getApplication(),
        email = email,
        label = folder,
        folder = remoteFolder,
        msgs = msgs,
        isNew = false,
        areAllMsgsEncrypted = isEncryptedModeEnabled
    )

    roomDatabase.msgDao().insertWithReplaceSuspend(msgEntities)

    if (!isEncryptedModeEnabled) {
      CheckIsLoadedMessagesEncryptedWorker.enqueue(getApplication(), localFolder)
    }

    identifyAttachments(msgEntities, msgs, remoteFolder, account, localFolder, roomDatabase)
    updateLocalContactsIfNeeded(remoteFolder, msgs)
  }

  private suspend fun identifyAttachments(msgEntities: List<MessageEntity>, msgs: Array<Message>,
                                          remoteFolder: IMAPFolder, account: AccountEntity, localFolder:
                                          LocalFolder, roomDatabase: FlowCryptRoomDatabase) = withContext(Dispatchers.IO) {
    try {
      val savedMsgUIDsSet = msgEntities.map { it.uid }.toSet()
      val attachments = mutableListOf<AttachmentEntity>()
      for (msg in msgs) {
        if (remoteFolder.getUID(msg) in savedMsgUIDsSet) {
          val uid = remoteFolder.getUID(msg)
          attachments.addAll(EmailUtil.getAttsInfoFromPart(msg).mapNotNull {
            AttachmentEntity.fromAttInfo(it.apply {
              this.email = account.email
              this.folder = localFolder.fullName
              this.uid = uid
            })
          })
        }
      }

      roomDatabase.attachmentDao().insertWithReplaceSuspend(attachments)
    } catch (e: Exception) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }
  }

  private suspend fun identifyAttachments(msgEntities: List<MessageEntity>, msgs: List<com.google.api.services.gmail.model.Message>,
                                          account: AccountEntity, localFolder:
                                          LocalFolder, roomDatabase: FlowCryptRoomDatabase) = withContext(Dispatchers.IO) {
    try {
      val savedMsgUIDsSet = msgEntities.map { it.uid }.toSet()
      val attachments = mutableListOf<AttachmentEntity>()
      for (msg in msgs) {
        if (msg.uid in savedMsgUIDsSet) {
          attachments.addAll(GmailApiHelper.getAttsInfoFromMessagePart(msg.payload).mapNotNull {
            AttachmentEntity.fromAttInfo(it.apply {
              this.email = account.email
              this.folder = localFolder.fullName
              this.uid = msg.uid
            })
          })
        }
      }

      roomDatabase.attachmentDao().insertWithReplaceSuspend(attachments)
    } catch (e: Exception) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }
  }

  private suspend fun updateLocalContactsIfNeeded(imapFolder: IMAPFolder? = null, messages: Array<Message>) = withContext(Dispatchers.IO) {
    try {
      val isSentFolder = imapFolder?.attributes?.contains("\\Sent") ?: true

      if (isSentFolder) {
        val emailAndNamePairs = ArrayList<EmailAndNamePair>()
        for (message in messages) {
          emailAndNamePairs.addAll(getEmailAndNamePairs(message))
        }

        EmailAndNameUpdaterService.enqueueWork(getApplication(), emailAndNamePairs)
      }
    } catch (e: MessagingException) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }
  }

  /**
   * Generate a list of [EmailAndNamePair] objects from the input message.
   * This information will be retrieved from "to" and "cc" headers.
   *
   * @param msg The input [javax.mail.Message].
   * @return <tt>[List]</tt> of EmailAndNamePair objects, which contains information
   * about
   * emails and names.
   * @throws MessagingException when retrieve information about recipients.
   */
  private fun getEmailAndNamePairs(msg: Message): List<EmailAndNamePair> {
    val pairs = ArrayList<EmailAndNamePair>()

    val addressesTo = msg.getRecipients(Message.RecipientType.TO)
    if (addressesTo != null) {
      for (address in addressesTo) {
        val internetAddress = address as InternetAddress
        pairs.add(EmailAndNamePair(internetAddress.address, internetAddress.personal))
      }
    }

    val addressesCC = msg.getRecipients(Message.RecipientType.CC)
    if (addressesCC != null) {
      for (address in addressesCC) {
        val internetAddress = address as InternetAddress
        pairs.add(EmailAndNamePair(internetAddress.address, internetAddress.personal))
      }
    }

    return pairs
  }

  private suspend fun searchMsgsOnRemoteServerAndStoreLocally(accountEntity: AccountEntity, store: Store,
                                                              localFolder: LocalFolder,
                                                              countOfAlreadyLoadedMsgs: Int): Result<Boolean?> = withContext(Dispatchers.IO) {
    store.getFolder(localFolder.fullName).use { folder ->
      val imapFolder = folder as IMAPFolder
      imapFolder.open(Folder.READ_ONLY)

      val countOfLoadedMsgs = when {
        countOfAlreadyLoadedMsgs < 0 -> 0
        else -> countOfAlreadyLoadedMsgs
      }

      val foundMsgs = imapFolder.search(generateSearchTerm(accountEntity, localFolder))

      val messagesCount = foundMsgs.size
      val end = messagesCount - countOfLoadedMsgs
      val startCandidate = end - JavaEmailConstants.COUNT_OF_LOADED_EMAILS_BY_STEP + 1
      val start = when {
        startCandidate < 1 -> 1
        else -> startCandidate
      }

      loadMsgsFromRemoteServerLiveData.postValue(Result.loading(
          progress = 80.0,
          resultCode = R.id.progress_id_getting_list_of_emails
      ))

      if (end < 1) {
        handleSearchResults(accountEntity, localFolder, imapFolder, arrayOf())
      } else {
        val bufferedMsgs = Arrays.copyOfRange(foundMsgs, start - 1, end)

        val fetchProfile = FetchProfile()
        fetchProfile.add(FetchProfile.Item.ENVELOPE)
        fetchProfile.add(FetchProfile.Item.FLAGS)
        fetchProfile.add(FetchProfile.Item.CONTENT_INFO)
        fetchProfile.add(UIDFolder.FetchProfileItem.UID)

        imapFolder.fetch(bufferedMsgs, fetchProfile)

        handleSearchResults(accountEntity, localFolder, imapFolder, bufferedMsgs)
      }
    }

    return@withContext Result.success(true)
  }

  private suspend fun handleSearchResults(account: AccountEntity, localFolder: LocalFolder,
                                          remoteFolder: IMAPFolder, msgs: Array<Message>) = withContext(Dispatchers.IO) {
    val email = account.email
    val isEncryptedModeEnabled = account.isShowOnlyEncrypted ?: false
    val searchLabel = SearchMessagesActivity.SEARCH_FOLDER_NAME

    val msgEntities = MessageEntity.genMessageEntities(
        context = getApplication(),
        email = email,
        label = searchLabel,
        folder = remoteFolder,
        msgs = msgs,
        isNew = false,
        areAllMsgsEncrypted = isEncryptedModeEnabled
    )

    FlowCryptRoomDatabase.getDatabase(getApplication()).msgDao().insertWithReplaceSuspend(msgEntities)

    if (!isEncryptedModeEnabled) {
      CheckIsLoadedMessagesEncryptedWorker.enqueue(getApplication(), localFolder)
    }

    updateLocalContactsIfNeeded(remoteFolder, msgs)
  }

  /**
   * Generate a [SearchTerm] depend on an input [AccountEntity].
   *
   * @param account An input [AccountEntity]
   * @return A generated [SearchTerm].
   */
  private fun generateSearchTerm(account: AccountEntity, localFolder: LocalFolder): SearchTerm {
    val isEncryptedModeEnabled = account.isShowOnlyEncrypted

    if (isEncryptedModeEnabled == true) {
      val searchTerm = EmailUtil.genEncryptedMsgsSearchTerm(account)

      return if (AccountEntity.ACCOUNT_TYPE_GOOGLE.equals(account.accountType, ignoreCase = true)) {
        val stringTerm = searchTerm as StringTerm
        GmailRawSearchTerm(localFolder.searchQuery + " AND (" + stringTerm.pattern + ")")
      } else {
        AndTerm(searchTerm, generateNonGmailSearchTerm(localFolder))
      }
    } else {
      return if (AccountEntity.ACCOUNT_TYPE_GOOGLE.equals(account.accountType, ignoreCase = true)) {
        GmailRawSearchTerm(localFolder.searchQuery)
      } else {
        generateNonGmailSearchTerm(localFolder)
      }
    }
  }

  private suspend fun refreshMsgsInternal(accountEntity: AccountEntity, store: Store, localFolder: LocalFolder): Result<Boolean?> = withContext(Dispatchers.IO) {
    store.getFolder(localFolder.fullName).use { folder ->
      val imapFolder = folder as IMAPFolder
      imapFolder.open(Folder.READ_ONLY)
      val folderName = localFolder.fullName

      val roomDatabase = FlowCryptRoomDatabase.getDatabase(getApplication())

      val newestCachedUID = roomDatabase.msgDao()
          .getLastUIDOfMsgForLabelSuspend(accountEntity.email, folderName) ?: 0
      val oldestCachedUID = roomDatabase.msgDao()
          .getOldestUIDOfMsgForLabelSuspend(accountEntity.email, folderName) ?: 0
      val cachedUIDSet = roomDatabase.msgDao().getUIDsForLabel(accountEntity.email, folderName).toSet()
      val updatedMsgs = EmailUtil.getUpdatedMsgsByUID(imapFolder, oldestCachedUID.toLong(), newestCachedUID.toLong())

      val newMsgsAfterLastInLocalCache = if (accountEntity.isShowOnlyEncrypted == true) {
        val foundMsgs = imapFolder.search(EmailUtil.genEncryptedMsgsSearchTerm(accountEntity))

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
        val newestMsgsFromFetchExceptExisted = imapFolder.getMessagesByUID(newestCachedUID.toLong(), UIDFolder.LASTUID)
            .filterNot { imapFolder.getUID(it) in cachedUIDSet }
            .filterNotNull()
        val msgs = newestMsgsFromFetchExceptExisted + updatedMsgs.filter { imapFolder.getUID(it) !in cachedUIDSet }
        EmailUtil.fetchMsgs(imapFolder, msgs.toTypedArray())
      }

      handleRefreshedMsgs(accountEntity, localFolder, imapFolder, newMsgsAfterLastInLocalCache, updatedMsgs)
    }

    return@withContext Result.success(true)
  }

  private suspend fun handleRefreshedMsgs(accountEntity: AccountEntity, localFolder: LocalFolder,
                                          remoteFolder: IMAPFolder, newMsgs: Array<Message>,
                                          updatedMsgs: Array<Message>) = withContext(Dispatchers.IO) {
    val email = accountEntity.email
    val folderName = localFolder.fullName

    val roomDatabase = FlowCryptRoomDatabase.getDatabase(getApplication())

    val mapOfUIDAndMsgFlags = roomDatabase.msgDao().getMapOfUIDAndMsgFlagsSuspend(email, folderName)
    val msgsUIDs = HashSet(mapOfUIDAndMsgFlags.keys)
    val deleteCandidatesUIDs = EmailUtil.genDeleteCandidates(msgsUIDs, remoteFolder, updatedMsgs)

    roomDatabase.msgDao().deleteByUIDsSuspend(accountEntity.email, folderName, deleteCandidatesUIDs)

    val folderType = FoldersManager.getFolderType(localFolder)
    if (!GeneralUtil.isAppForegrounded() && folderType === FoldersManager.FolderType.INBOX) {
      val notificationManager = MessagesNotificationManager(getApplication())
      for (uid in deleteCandidatesUIDs) {
        notificationManager.cancel(uid.toInt())
      }
    }

    val newCandidates = EmailUtil.genNewCandidates(msgsUIDs, remoteFolder, newMsgs)

    val isEncryptedModeEnabled = accountEntity.isShowOnlyEncrypted ?: false
    val isNew = !GeneralUtil.isAppForegrounded() && folderType === FoldersManager.FolderType.INBOX

    val msgEntities = MessageEntity.genMessageEntities(
        context = getApplication(),
        email = email,
        label = folderName,
        folder = remoteFolder,
        msgs = newCandidates,
        isNew = isNew,
        areAllMsgsEncrypted = isEncryptedModeEnabled
    )

    roomDatabase.msgDao().insertWithReplaceSuspend(msgEntities)

    if (!isEncryptedModeEnabled) {
      CheckIsLoadedMessagesEncryptedWorker.enqueue(getApplication(), localFolder)
    }

    val updateCandidates = EmailUtil.genUpdateCandidates(mapOfUIDAndMsgFlags, remoteFolder, updatedMsgs)
        .map { remoteFolder.getUID(it) to it.flags }.toMap()
    roomDatabase.msgDao().updateFlagsSuspend(accountEntity.email, folderName, updateCandidates)

    updateLocalContactsIfNeeded(remoteFolder, newCandidates)

  }

  private fun generateNonGmailSearchTerm(localFolder: LocalFolder): SearchTerm {
    return OrTerm(arrayOf(
        SubjectTerm(localFolder.searchQuery),
        BodyTerm(localFolder.searchQuery),
        FromStringTerm(localFolder.searchQuery),
        RecipientStringTerm(Message.RecipientType.TO, localFolder.searchQuery),
        RecipientStringTerm(Message.RecipientType.CC, localFolder.searchQuery),
        RecipientStringTerm(Message.RecipientType.BCC, localFolder.searchQuery)
    ))
  }
}