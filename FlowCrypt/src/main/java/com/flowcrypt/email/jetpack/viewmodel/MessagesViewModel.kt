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
import com.flowcrypt.email.api.email.IMAPStoreManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.model.MessageFlag
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.AttachmentEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.jetpack.workmanager.sync.CheckIsLoadedMessagesEncryptedSyncTask
import com.flowcrypt.email.model.EmailAndNamePair
import com.flowcrypt.email.service.EmailAndNameUpdaterService
import com.flowcrypt.email.ui.activity.SearchMessagesActivity
import com.flowcrypt.email.util.FileAndDirectoryUtils
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

  val msgStatesLiveData = MutableLiveData<MessageState>()
  var msgsLiveData: LiveData<PagedList<MessageEntity>>? = null
  var outboxMsgsLiveData: LiveData<List<MessageEntity>> = Transformations.switchMap(activeAccountLiveData) {
    roomDatabase.msgDao().getOutboxMsgsLD(it?.email ?: "")
  }

  val loadMsgsFromRemoteServerLiveData = MutableLiveData<Result<Boolean?>>()

  fun loadMsgsFromRemoteServer(localFolder: LocalFolder, totalItemsCount: Int) {
    viewModelScope.launch {
      val accountEntity = getActiveAccountSuspend()
      accountEntity?.let {
        loadMsgsFromRemoteServerLiveData.value = Result.loading()
        val connection = IMAPStoreManager.activeConnections[accountEntity.id]
        if (connection == null) {
          loadMsgsFromRemoteServerLiveData.value = Result.exception(NullPointerException("There is no active connection for ${accountEntity.email}"))
        } else {
          loadMsgsFromRemoteServerLiveData.value = connection.execute {
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
      loadMsgsFromRemoteServerLiveData.postValue(Result.loading(resultCode = R.id.progress_id_opening_store))
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
      roomDatabase.labelDao().getLabelSuspend(accountEntity.email, folderName)?.let {
        roomDatabase.labelDao().update(it.copy(msgsCount = msgsCount))
      }

      loadMsgsFromRemoteServerLiveData.postValue(Result.loading(resultCode = R.id.progress_id_getting_list_of_emails))
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
      CheckIsLoadedMessagesEncryptedSyncTask.enqueue(getApplication(), localFolder)
    }

    identifyAttachments(msgEntities, msgs, remoteFolder, account, localFolder, roomDatabase)
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
              this.uid = uid.toInt()
            })
          })
        }
      }

      roomDatabase.attachmentDao().insertWithReplaceSuspend(attachments)
      updateLocalContactsIfNeeded(remoteFolder, msgs)
    } catch (e: Exception) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }
  }

  private fun updateLocalContactsIfNeeded(imapFolder: IMAPFolder, messages: Array<Message>) {
    try {
      val isSentFolder = listOf(*imapFolder.attributes).contains("\\Sent")

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

      loadMsgsFromRemoteServerLiveData.postValue(Result.loading(resultCode = R.id.progress_id_getting_list_of_emails))

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
      CheckIsLoadedMessagesEncryptedSyncTask.enqueue(getApplication(), localFolder)
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