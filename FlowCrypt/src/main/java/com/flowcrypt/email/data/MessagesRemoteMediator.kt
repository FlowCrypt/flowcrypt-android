/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.data

import android.content.Context
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.IMAPStoreManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.gmail.api.GmaiAPIMimeMessage
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.AttachmentEntity
import com.flowcrypt.email.database.entity.LabelEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.jetpack.viewmodel.AccountViewModel
import com.flowcrypt.email.jetpack.workmanager.sync.CheckIsLoadedMessagesEncryptedWorker
import com.flowcrypt.email.model.EmailAndNamePair
import com.flowcrypt.email.service.EmailAndNameUpdaterService
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.sun.mail.imap.IMAPFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Arrays
import java.util.Properties
import javax.mail.FetchProfile
import javax.mail.Folder
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.Store
import javax.mail.UIDFolder
import javax.mail.internet.InternetAddress

/**
 * @author Denis Bondarenko
 *         Date: 5/17/22
 *         Time: 11:13 AM
 *         E-mail: DenBond7@gmail.com
 */
@OptIn(ExperimentalPagingApi::class)
class MessagesRemoteMediator(
  private val context: Context,
  private val roomDatabase: FlowCryptRoomDatabase,
  private val localFolder: LocalFolder? = null,
) : RemoteMediator<Int, MessageEntity>() {
  private var searchNextPageToken: String? = null

  override suspend fun load(
    loadType: LoadType,
    state: PagingState<Int, MessageEntity>
  ): MediatorResult {
    if (loadType == LoadType.PREPEND || localFolder == null || localFolder.isOutbox()) {
      return MediatorResult.Success(endOfPaginationReached = true)
    }
    val activeAccountWithProtectedData = roomDatabase.accountDao().getActiveAccountSuspend()
    val accountEntity =
      AccountViewModel.getAccountEntityWithDecryptedInfoSuspend(activeAccountWithProtectedData)
        ?: return MediatorResult.Success(endOfPaginationReached = true)

    val totalItemsCount = roomDatabase.msgDao().getMsgsCount(
      account = accountEntity.email,
      label = if (localFolder.searchQuery.isNullOrEmpty()) {
        localFolder.fullName
      } else {
        JavaEmailConstants.FOLDER_SEARCH
      }
    )

    if (loadType == LoadType.REFRESH && totalItemsCount != 0) {
      return MediatorResult.Success(endOfPaginationReached = false)
    }

    try {
      val actionResult = fetchAndCacheMessages(
        accountEntity = accountEntity,
        localFolder = localFolder,
        totalItemsCount = totalItemsCount,
        pageSize = state.config.pageSize
      )

      if (actionResult.status == Result.Status.EXCEPTION) {
        return MediatorResult.Error(requireNotNull(actionResult.exception))
      }

      return MediatorResult.Success(endOfPaginationReached = (actionResult.data ?: 0) == 0)
    } catch (exception: Exception) {
      return MediatorResult.Error(exception)
    }
  }

  private suspend fun fetchAndCacheMessages(
    accountEntity: AccountEntity,
    localFolder: LocalFolder,
    totalItemsCount: Int,
    pageSize: Int
  ) = if (accountEntity.useAPI) {
    GmailApiHelper.executeWithResult {
      if (localFolder.searchQuery.isNullOrEmpty()) {
        loadMsgsFromRemoteServerAndStoreLocally(
          accountEntity,
          localFolder,
          totalItemsCount,
          pageSize
        )
      } else {
        searchMsgsOnRemoteServerAndStoreLocally(
          accountEntity,
          localFolder,
          totalItemsCount,
          pageSize
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
          totalItemsCount,
          pageSize
        )
      } else {
        searchMsgsOnRemoteServerAndStoreLocally(
          accountEntity,
          store,
          localFolder,
          totalItemsCount,
          pageSize
        )
      }
    }
      ?: Result.exception(NullPointerException("There is no active connection for ${accountEntity.email}"))
  }

  private suspend fun loadMsgsFromRemoteServerAndStoreLocally(
    accountEntity: AccountEntity,
    store: Store,
    localFolder: LocalFolder,
    countOfAlreadyLoadedMsgs: Int,
    pageSize: Int
  ): Result<Int?> = withContext(Dispatchers.IO) {
    var countOfFetchedMsgs = 0
    store.getFolder(localFolder.fullName).use { folder ->
      val imapFolder = folder as IMAPFolder
      imapFolder.open(Folder.READ_ONLY)

      val countOfLoadedMsgs = when {
        countOfAlreadyLoadedMsgs < 0 -> 0
        else -> countOfAlreadyLoadedMsgs
      }

      val isEncryptedModeEnabled = accountEntity.showOnlyEncrypted
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
      val startCandidate = end - pageSize + 1
      val start = when {
        startCandidate < 1 -> 1
        else -> startCandidate
      }
      val folderName = imapFolder.fullName

      roomDatabase.labelDao()
        .getLabelSuspend(accountEntity.email, accountEntity.accountType, folderName)?.let {
          roomDatabase.labelDao().updateSuspend(it.copy(messagesTotal = msgsCount))
        }
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

        countOfFetchedMsgs = msgs.size
        handleReceivedMsgs(accountEntity, localFolder, folder, msgs)
      }
    }

    return@withContext Result.success(countOfFetchedMsgs)
  }

  private suspend fun loadMsgsFromRemoteServerAndStoreLocally(
    accountEntity: AccountEntity,
    localFolder: LocalFolder,
    totalItemsCount: Int,
    pageSize: Int
  ): Result<Int?> = withContext(Dispatchers.IO) {
    var countOfFetchedMsgs = 0
    when (accountEntity.accountType) {
      AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
        val labelEntity: LabelEntity? = roomDatabase.labelDao()
          .getLabelSuspend(accountEntity.email, accountEntity.accountType, localFolder.fullName)
        val messagesBaseInfo = GmailApiHelper.loadMsgsBaseInfo(
          context = context,
          accountEntity = accountEntity,
          localFolder = localFolder,
          maxResults = pageSize.toLong(),
          nextPageToken = if (totalItemsCount > 0) labelEntity?.nextPageToken else null
        )

        if (messagesBaseInfo.messages?.isNotEmpty() == true) {
          val msgs = GmailApiHelper.loadMsgsInParallel(
            context, accountEntity, messagesBaseInfo.messages
              ?: emptyList(), localFolder
          )
          countOfFetchedMsgs = msgs.size
          handleReceivedMsgs(accountEntity, localFolder, msgs)
        }

        labelEntity?.let {
          roomDatabase.labelDao()
            .updateSuspend(it.copy(nextPageToken = messagesBaseInfo.nextPageToken))
        }
      }
    }

    return@withContext Result.success(countOfFetchedMsgs)
  }

  private suspend fun handleReceivedMsgs(
    account: AccountEntity, localFolder: LocalFolder,
    msgs: List<com.google.api.services.gmail.model.Message>
  ) = withContext(Dispatchers.IO) {
    val email = account.email
    val folder = localFolder.fullName

    val isEncryptedModeEnabled = account.showOnlyEncrypted ?: false
    val msgEntities = MessageEntity.genMessageEntities(
      context = context,
      email = email,
      label = folder,
      msgsList = msgs,
      isNew = false,
      areAllMsgsEncrypted = isEncryptedModeEnabled
    )

    roomDatabase.msgDao().insertWithReplaceSuspend(msgEntities)
    GmailApiHelper.identifyAttachments(msgEntities, msgs, account, localFolder, roomDatabase)
    val session = Session.getInstance(Properties())
    updateLocalContactsIfNeeded(messages = msgs
      .filter { it.labelIds.contains(GmailApiHelper.LABEL_SENT) }
      .map { GmaiAPIMimeMessage(session, it) }.toTypedArray()
    )
  }

  private suspend fun handleReceivedMsgs(
    account: AccountEntity, localFolder: LocalFolder,
    remoteFolder: IMAPFolder, msgs: Array<Message>
  ) = withContext(Dispatchers.IO) {
    val email = account.email
    val folder = localFolder.fullName

    val isEncryptedModeEnabled = account.showOnlyEncrypted ?: false
    val msgEntities = MessageEntity.genMessageEntities(
      context = context,
      email = email,
      label = folder,
      folder = remoteFolder,
      msgs = msgs,
      isNew = false,
      areAllMsgsEncrypted = isEncryptedModeEnabled
    )

    roomDatabase.msgDao().insertWithReplaceSuspend(msgEntities)

    if (!isEncryptedModeEnabled) {
      CheckIsLoadedMessagesEncryptedWorker.enqueue(context, localFolder)
    }

    identifyAttachments(msgEntities, msgs, remoteFolder, account, localFolder, roomDatabase)
    updateLocalContactsIfNeeded(remoteFolder, msgs)
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

  private suspend fun updateLocalContactsIfNeeded(
    imapFolder: IMAPFolder? = null,
    messages: Array<Message>
  ) = withContext(Dispatchers.IO) {
    try {
      val isSentFolder = imapFolder?.attributes?.contains("\\Sent") ?: true

      if (isSentFolder) {
        val emailAndNamePairs = ArrayList<EmailAndNamePair>()
        for (message in messages) {
          emailAndNamePairs.addAll(getEmailAndNamePairs(message))
        }

        EmailAndNameUpdaterService.enqueueWork(context, emailAndNamePairs)
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

  private suspend fun searchMsgsOnRemoteServerAndStoreLocally(
    accountEntity: AccountEntity,
    localFolder: LocalFolder,
    totalItemsCount: Int,
    pageSize: Int
  ): Result<Int?> = withContext(Dispatchers.IO) {
    var countOfFetchedMsgs = 0
    when (accountEntity.accountType) {
      AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
        val messagesBaseInfo = GmailApiHelper.loadMsgsBaseInfoUsingSearch(
          context = context,
          accountEntity = accountEntity,
          localFolder = localFolder,
          maxResults = pageSize.toLong(),
          nextPageToken = if (totalItemsCount > 0) searchNextPageToken else null
        )

        if (messagesBaseInfo.messages?.isNotEmpty() == true) {
          val msgs = GmailApiHelper.loadMsgsInParallel(
            context, accountEntity, messagesBaseInfo.messages
              ?: emptyList(), localFolder
          )
          countOfFetchedMsgs = msgs.size
          handleSearchResults(
            accountEntity,
            localFolder.copy(fullName = JavaEmailConstants.FOLDER_SEARCH),
            msgs
          )
        }

        searchNextPageToken = messagesBaseInfo.nextPageToken
      }
    }

    return@withContext Result.success(countOfFetchedMsgs)
  }

  private suspend fun searchMsgsOnRemoteServerAndStoreLocally(
    accountEntity: AccountEntity, store: Store,
    localFolder: LocalFolder,
    countOfAlreadyLoadedMsgs: Int,
    pageSize: Int
  ): Result<Int?> = withContext(Dispatchers.IO) {
    var countOfFetchedMsgs = 0
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
      val startCandidate = end - pageSize + 1
      val start = when {
        startCandidate < 1 -> 1
        else -> startCandidate
      }

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
        countOfFetchedMsgs = bufferedMsgs.size
        handleSearchResults(accountEntity, localFolder, imapFolder, bufferedMsgs)
      }
    }

    return@withContext Result.success(countOfFetchedMsgs)
  }

  private suspend fun handleSearchResults(
    account: AccountEntity, localFolder: LocalFolder,
    remoteFolder: IMAPFolder, msgs: Array<Message>
  ) = withContext(Dispatchers.IO) {
    val email = account.email
    val isEncryptedModeEnabled = account.showOnlyEncrypted ?: false
    val searchLabel = JavaEmailConstants.FOLDER_SEARCH

    val msgEntities = MessageEntity.genMessageEntities(
      context = context,
      email = email,
      label = searchLabel,
      folder = remoteFolder,
      msgs = msgs,
      isNew = false,
      areAllMsgsEncrypted = isEncryptedModeEnabled
    )

    roomDatabase.msgDao().insertWithReplaceSuspend(msgEntities)

    if (!isEncryptedModeEnabled) {
      CheckIsLoadedMessagesEncryptedWorker.enqueue(context, localFolder)
    }

    updateLocalContactsIfNeeded(remoteFolder, msgs)
  }

  private suspend fun handleSearchResults(
    account: AccountEntity, localFolder: LocalFolder,
    msgs: List<com.google.api.services.gmail.model.Message>
  ) = withContext(Dispatchers.IO) {
    val email = account.email
    val label = localFolder.fullName

    val isEncryptedModeEnabled = account.showOnlyEncrypted ?: false
    val msgEntities = MessageEntity.genMessageEntities(
      context = context,
      email = email,
      label = label,
      msgsList = msgs,
      isNew = false,
      areAllMsgsEncrypted = isEncryptedModeEnabled
    )

    roomDatabase.msgDao().insertWithReplaceSuspend(msgEntities)
    GmailApiHelper.identifyAttachments(msgEntities, msgs, account, localFolder, roomDatabase)
    val session = Session.getInstance(Properties())
    updateLocalContactsIfNeeded(messages = msgs
      .filter { it.labelIds.contains(GmailApiHelper.LABEL_SENT) }
      .map { GmaiAPIMimeMessage(session, it) }.toTypedArray()
    )
  }
}
