/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.api.email.gmail

import android.content.Context
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.gmail.GmailApiHelper.Companion.LABEL_DRAFT
import com.flowcrypt.email.api.email.gmail.GmailApiHelper.Companion.LABEL_TRASH
import com.flowcrypt.email.api.email.gmail.GmailApiHelper.Companion.labelsToImapFlags
import com.flowcrypt.email.api.email.gmail.api.GmaiAPIMimeMessage
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.database.entity.MessageEntity.Companion.LABEL_IDS_SEPARATOR
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.hasPgp
import com.flowcrypt.email.extensions.isAppForegrounded
import com.flowcrypt.email.extensions.kotlin.toHex
import com.flowcrypt.email.extensions.kotlin.toLongRadix16
import com.flowcrypt.email.extensions.uid
import com.flowcrypt.email.service.MessagesNotificationManager
import com.flowcrypt.email.util.GeneralUtil
import com.google.api.services.gmail.model.History
import com.google.api.services.gmail.model.Message
import com.google.api.services.gmail.model.Thread
import jakarta.mail.Flags
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties

/**
 * @author Denys Bondarenko
 */
object GmailHistoryHandler {
  suspend fun handleHistory(
    context: Context,
    accountEntity: AccountEntity,
    localFolder: LocalFolder,
    historyList: List<History>,
    action: suspend (
      deleteCandidates: Map<Long, String>,
      newCandidatesMap: Map<Long, Message>,
      updateCandidatesMap: Map<Long, Pair<String, Flags>>,
      labelsToBeUpdatedMap: Map<Long, Pair<String, String>>
    ) -> Unit = { _, _, _, _ -> }
  ) = withContext(Dispatchers.IO) {
    processHistory(accountEntity, localFolder, historyList) { deleteCandidates,
                                               newCandidatesMap,
                                               updateCandidatesMap,
                                               labelsToBeUpdatedMap ->
      val applicationContext = context.applicationContext
      if (accountEntity.useConversationMode) {
        handleHistoryForConversationMode(
          context = applicationContext,
          accountEntity = accountEntity,
          localFolder = localFolder,
          deleteCandidates = deleteCandidates,
          newCandidatesMap = newCandidatesMap,
          updateCandidatesMap = updateCandidatesMap,
          labelsToBeUpdatedMap = labelsToBeUpdatedMap
        )
      } else {
        handleHistoryForSeparateMessagesMode(
          context = applicationContext,
          accountEntity = accountEntity,
          localFolder = localFolder,
          deleteCandidates = deleteCandidates,
          newCandidatesMap = newCandidatesMap,
          updateCandidatesMap = updateCandidatesMap,
          labelsToBeUpdatedMap = labelsToBeUpdatedMap
        )
      }

      action.invoke(
        deleteCandidates,
        newCandidatesMap,
        updateCandidatesMap,
        labelsToBeUpdatedMap
      )
    }
  }

  private suspend fun handleHistoryForConversationMode(
    context: Context,
    accountEntity: AccountEntity,
    localFolder: LocalFolder,
    deleteCandidates: Map<Long, String>,
    newCandidatesMap: Map<Long, Message>,
    updateCandidatesMap: Map<Long, Pair<String, Flags>>,
    labelsToBeUpdatedMap: Map<Long, Pair<String, String>>
  ) {
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
    val folderType = FoldersManager.getFolderType(localFolder)
    val threadIds = mutableSetOf<String>().apply {
      addAll(deleteCandidates.values.toSet())
      addAll(updateCandidatesMap.entries.map { it.value.first }.toSet())
      addAll(labelsToBeUpdatedMap.entries.map { it.value.first }.toSet())
    }

    val gmailThreadsListWithBaseInfo = GmailApiHelper.loadGmailThreadInfoInParallel(
      context = context,
      accountEntity = accountEntity,
      localFolder = localFolder,
      threads = threadIds.map { Thread().apply { id = it } },
      format = GmailApiHelper.RESPONSE_FORMAT_FULL,
      fields = GmailApiHelper.THREAD_BASE_INFO
    )

    val threadIdListToBeUpdated = mutableSetOf<String>()

    //delete remotely trashed threads locally
    if (deleteCandidates.isNotEmpty()) {
      val threadIdsToBeDeleted = deleteCandidates.values.toSet()
      val gmailThreadInfoList =
        gmailThreadsListWithBaseInfo.filter { it.id in threadIdsToBeDeleted }

      /*
      Delete threads from the active folder if all messages in a tread doesn't have
      the requested label.
      It will cover a situation when some email client works with separate messages.
      */
      val toBeDeletedLocally =
        gmailThreadInfoList.filter { !it.labels.contains(localFolder.fullName) }.toSet()

      roomDatabase.msgDao().deleteByUIDsSuspend(
        account = accountEntity.email,
        label = localFolder.fullName,
        msgsUID = toBeDeletedLocally.map { Long.MAX_VALUE - it.id.toLongRadix16 })

      //need to update the left threads
      val toBeUpdated = gmailThreadInfoList - toBeDeletedLocally
      threadIdListToBeUpdated.addAll(toBeUpdated.map { it.id })
    }

    //insert new threads or update the existing
    val newCandidates = newCandidatesMap.values
    if (newCandidates.isNotEmpty() || threadIdListToBeUpdated.isNotEmpty()) {
      val uniqueThreadIdList = newCandidates.map { it.threadId }.toSet() + threadIdListToBeUpdated
      val existingThreads = roomDatabase.msgDao().getMsgsByUIDs(
        account = accountEntity.email,
        label = localFolder.fullName,
        uidList = uniqueThreadIdList.map { Long.MAX_VALUE - it.toLongRadix16 })
      val existingThreadIds = existingThreads.mapNotNull { it.threadIdAsHEX }.toSet()

      val gmailThreadInfoList = GmailApiHelper.loadGmailThreadInfoInParallel(
        context = context,
        accountEntity = accountEntity,
        localFolder = localFolder,
        threads = uniqueThreadIdList.map { Thread().apply { id = it } },
        format = GmailApiHelper.RESPONSE_FORMAT_FULL
      )

      val threadsToBeAdded = gmailThreadInfoList
        .filter { it.id !in existingThreadIds }
        .map { it.lastMessage }

      if (threadsToBeAdded.isNotEmpty()) {
        //insert new threads
        val isNew =
          !context.isAppForegrounded() && folderType == FoldersManager.FolderType.INBOX

        val msgEntities = MessageEntity.genMessageEntities(
          context = context,
          account = accountEntity.email,
          accountType = accountEntity.accountType,
          label = localFolder.fullName,
          msgsList = threadsToBeAdded,
          isNew = isNew,
          onlyPgpModeEnabled = accountEntity.showOnlyEncrypted == true
        ) { message, messageEntity ->
          val thread = gmailThreadInfoList.firstOrNull { it.id == message.threadId }
            ?: return@genMessageEntities messageEntity
          messageEntity.copy(
            uid = Long.MAX_VALUE - thread.id.toLongRadix16,
            subject = thread.subject,
            threadMessagesCount = thread.messagesCount,
            threadDraftsCount = thread.draftsCount,
            labelIds = thread.labels.joinToString(separator = LABEL_IDS_SEPARATOR),
            hasAttachments = thread.hasAttachments,
            fromAddresses = InternetAddress.toString(thread.recipients.toTypedArray()),
            hasPgp = thread.hasPgpThings
          )
        }
        roomDatabase.msgDao().insertWithReplaceSuspend(msgEntities)
      }

      if (existingThreads.isNotEmpty()) {
        //update existing threads
        val threadsToBeUpdated = existingThreads.mapNotNull { threadMessageEntity ->
          gmailThreadInfoList.firstOrNull {
            it.id == threadMessageEntity.threadIdAsHEX
          }?.let { thread ->
            val isNew = !context.isAppForegrounded()
                && folderType == FoldersManager.FolderType.INBOX
                && thread.id !in threadIdListToBeUpdated

            MessageEntity.genMessageEntities(
              context = context,
              account = accountEntity.email,
              accountType = accountEntity.accountType,
              label = localFolder.fullName,
              msgsList = listOf(thread.lastMessage),
              isNew = isNew,
              onlyPgpModeEnabled = accountEntity.showOnlyEncrypted == true
            ) { message, messageEntity ->
              if (message.threadId == thread.id) {
                messageEntity.toUpdatedThreadCopy(threadMessageEntity, thread)
              } else messageEntity
            }
          }
        }
        roomDatabase.msgDao().updateSuspend(threadsToBeUpdated.flatten())
      }
    }

    //update 'unread' state
    if (updateCandidatesMap.isNotEmpty()) {
      val threadIdsToBeUpdated = updateCandidatesMap.entries.map { it.value.first }.toSet()
      val gmailThreadInfoList =
        gmailThreadsListWithBaseInfo.filter { it.id in threadIdsToBeUpdated }

      val flagsMap = gmailThreadInfoList.associateBy(
        { Long.MAX_VALUE - it.id.toLongRadix16 },
        { labelsToImapFlags(it.labels) })

      roomDatabase.msgDao()
        .updateFlagsSuspend(accountEntity.email, localFolder.fullName, flagsMap)
    }

    //update 'labels'
    if (labelsToBeUpdatedMap.isNotEmpty()) {
      val threadIdsToBeUpdated = labelsToBeUpdatedMap.entries.map { it.value.first }.toSet()
      val gmailThreadInfoList =
        gmailThreadsListWithBaseInfo.filter { it.id in threadIdsToBeUpdated }
      val labelsMap = gmailThreadInfoList.associateBy(
        { Long.MAX_VALUE - it.id.toLongRadix16 },
        { it.labels.joinToString(separator = LABEL_IDS_SEPARATOR) })

      roomDatabase.msgDao()
        .updateGmailLabels(accountEntity.email, localFolder.fullName, labelsMap)
    }
  }

  private suspend fun handleHistoryForSeparateMessagesMode(
    context: Context,
    accountEntity: AccountEntity,
    localFolder: LocalFolder,
    deleteCandidates: Map<Long, String>,
    newCandidatesMap: Map<Long, Message>,
    updateCandidatesMap: Map<Long, Pair<String, Flags>>,
    labelsToBeUpdatedMap: Map<Long, Pair<String, String>>
  ) {
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
    val folderType = FoldersManager.getFolderType(localFolder)
    roomDatabase.msgDao()
      .deleteByUIDsSuspend(accountEntity.email, localFolder.fullName, deleteCandidates.keys)

    if (folderType == FoldersManager.FolderType.INBOX) {
      val notificationManager = MessagesNotificationManager(context)
      for (entry in deleteCandidates) {
        notificationManager.cancel(entry.key.toHex())
      }
    }

    val newCandidates = newCandidatesMap.values
    if (newCandidates.isNotEmpty()) {
      val messages = GmailApiHelper.loadMsgsInParallel(
        context, accountEntity,
        newCandidates.toList(), localFolder
      ).run {
        if (accountEntity.showOnlyEncrypted == true) {
          filter { it.hasPgp() }
        } else this
      }

      val draftIdsMap = if (localFolder.isDrafts) {
        val drafts =
          GmailApiHelper.loadBaseDraftInfoInParallel(context, accountEntity, messages)
        val fetchedDraftIdsMap = drafts.associateBy({ it.message.id }, { it.id })
        if (fetchedDraftIdsMap.size != messages.size) {
          throw IllegalStateException(
            context.getString(R.string.fetching_drafts_info_failed)
          )
        }
        fetchedDraftIdsMap
      } else emptyMap()

      val isNew =
        !context.isAppForegrounded() && folderType == FoldersManager.FolderType.INBOX

      val msgEntities = MessageEntity.genMessageEntities(
        context = context,
        account = accountEntity.email,
        accountType = accountEntity.accountType,
        label = localFolder.fullName,
        msgsList = messages,
        isNew = isNew,
        onlyPgpModeEnabled = accountEntity.showOnlyEncrypted == true,
        draftIdsMap = draftIdsMap
      )

      roomDatabase.msgDao().insertWithReplaceSuspend(msgEntities)
      GmailApiHelper.identifyAttachments(
        msgEntities = msgEntities,
        msgs = messages,
        account = accountEntity,
        localFolder = localFolder,
        roomDatabase = roomDatabase
      )
    }

    roomDatabase.msgDao()
      .updateFlagsSuspend(
        accountEntity.email,
        localFolder.fullName,
        updateCandidatesMap.entries.associate { it.key to it.value.second })

    roomDatabase.msgDao()
      .updateGmailLabels(
        accountEntity.email,
        localFolder.fullName,
        labelsToBeUpdatedMap.entries.associate { it.key to it.value.second })

    if (folderType == FoldersManager.FolderType.SENT) {
      val session = Session.getInstance(Properties())
      GeneralUtil.updateLocalContactsIfNeeded(
        context = context,
        messages = newCandidates
          .filter { it.labelIds?.contains(GmailApiHelper.LABEL_SENT) == true }
          .map { GmaiAPIMimeMessage(session, it) }.toTypedArray()
      )
    }
  }

  private suspend fun processHistory(
    accountEntity: AccountEntity,
    localFolder: LocalFolder,
    historyList: List<History>,
    action: suspend (
      deleteCandidates: Map<Long, String>,
      newCandidatesMap: Map<Long, Message>,
      updateCandidatesMap: Map<Long, Pair<String, Flags>>,
      labelsToBeUpdatedMap: Map<Long, Pair<String, String>>
    ) -> Unit
  ) = withContext(Dispatchers.IO)
  {
    val deleteCandidates = mutableMapOf<Long, String>()
    val newCandidatesMap = mutableMapOf<Long, Message>()
    val updateCandidates = mutableMapOf<Long, Pair<String, Flags>>()
    val labelsToBeUpdatedMap = mutableMapOf<Long, Pair<String, String>>()

    for (history in historyList) {
      history.messagesDeleted?.let { messagesDeleted ->
        for (historyMsgDeleted in messagesDeleted) {
          newCandidatesMap.remove(historyMsgDeleted.message.uid)
          updateCandidates.remove(historyMsgDeleted.message.uid)
          deleteCandidates[historyMsgDeleted.message.uid] = historyMsgDeleted.message.threadId
        }
      }

      history.messagesAdded?.let { messagesAdded ->
        for (historyMsgAdded in messagesAdded) {
          if (!accountEntity.useConversationMode) {
            if (LABEL_DRAFT in (historyMsgAdded.message.labelIds
                ?: emptyList()) && !localFolder.isDrafts
            ) {
              //skip adding drafts to non-Drafts folder
              continue
            }
          }
          deleteCandidates.remove(historyMsgAdded.message.uid)
          updateCandidates.remove(historyMsgAdded.message.uid)
          newCandidatesMap[historyMsgAdded.message.uid] = historyMsgAdded.message
        }
      }

      history.labelsRemoved?.let { labelsRemoved ->
        for (historyLabelRemoved in labelsRemoved) {
          val historyMessageLabelIds = historyLabelRemoved?.message?.labelIds ?: emptyList()
          labelsToBeUpdatedMap[historyLabelRemoved.message.uid] = Pair(
            historyLabelRemoved.message.threadId,
            historyMessageLabelIds.joinToString(LABEL_IDS_SEPARATOR)
          )

          if (localFolder.fullName in (historyLabelRemoved.labelIds ?: emptyList())) {
            newCandidatesMap.remove(historyLabelRemoved.message.uid)
            updateCandidates.remove(historyLabelRemoved.message.uid)
            deleteCandidates[historyLabelRemoved.message.uid] = historyLabelRemoved.message.threadId
            continue
          }

          if (LABEL_TRASH in (historyLabelRemoved.labelIds ?: emptyList())) {
            val message = historyLabelRemoved.message
            if (localFolder.fullName in historyMessageLabelIds) {
              deleteCandidates.remove(message.uid)
              updateCandidates.remove(message.uid)
              newCandidatesMap[message.uid] = message
              continue
            }
          }

          val existedFlags = labelsToImapFlags(historyMessageLabelIds)
          updateCandidates[historyLabelRemoved.message.uid] =
            Pair(historyLabelRemoved.message.threadId, existedFlags)
        }
      }

      history.labelsAdded?.let { labelsAdded ->
        for (historyLabelAdded in labelsAdded) {
          labelsToBeUpdatedMap[historyLabelAdded.message.uid] = Pair(
            historyLabelAdded.message.threadId,
            historyLabelAdded.message.labelIds.joinToString(LABEL_IDS_SEPARATOR)
          )
          if (localFolder.fullName in (historyLabelAdded.labelIds ?: emptyList())) {
            deleteCandidates.remove(historyLabelAdded.message.uid)
            updateCandidates.remove(historyLabelAdded.message.uid)
            newCandidatesMap[historyLabelAdded.message.uid] = historyLabelAdded.message
            continue
          }

          if ((historyLabelAdded.labelIds ?: emptyList()).contains(LABEL_TRASH)) {
            newCandidatesMap.remove(historyLabelAdded.message.uid)
            updateCandidates.remove(historyLabelAdded.message.uid)
            deleteCandidates[historyLabelAdded.message.uid] = historyLabelAdded.message.threadId
            continue
          }

          val existedFlags = labelsToImapFlags(historyLabelAdded.message.labelIds ?: emptyList())
          updateCandidates[historyLabelAdded.message.uid] =
            Pair(historyLabelAdded.message.threadId, existedFlags)
        }
      }
    }
    action.invoke(
      deleteCandidates,
      newCandidatesMap,
      updateCandidates,
      labelsToBeUpdatedMap
    )
  }
}