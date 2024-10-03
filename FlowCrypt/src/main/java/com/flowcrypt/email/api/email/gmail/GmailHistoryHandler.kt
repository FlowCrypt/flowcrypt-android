/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.api.email.gmail

import android.content.Context
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.MsgsCacheManager
import com.flowcrypt.email.api.email.gmail.GmailApiHelper.Companion.LABEL_DRAFT
import com.flowcrypt.email.api.email.gmail.GmailApiHelper.Companion.LABEL_TRASH
import com.flowcrypt.email.api.email.gmail.GmailApiHelper.Companion.labelsToImapFlags
import com.flowcrypt.email.api.email.gmail.api.GmaiAPIMimeMessage
import com.flowcrypt.email.api.email.gmail.model.GmailThreadInfo
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.model.MessageFlag
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.database.entity.MessageEntity.Companion.LABEL_IDS_SEPARATOR
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.hasPgp
import com.flowcrypt.email.extensions.isAppForegrounded
import com.flowcrypt.email.extensions.kotlin.toHex
import com.flowcrypt.email.extensions.kotlin.toLongRadix16
import com.flowcrypt.email.extensions.threadIdAsLong
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
      deleteCandidatesUIDs: Set<Long>,
      newCandidatesMap: Map<Long, Message>,
      updateCandidatesMap: Map<Long, Pair<String, Flags>>,
      labelsToBeUpdatedMap: Map<Long, String>
    ) -> Unit = { _, _, _, _ -> }
  ) = withContext(Dispatchers.IO) {
    processHistory(accountEntity, localFolder, historyList) { deleteCandidatesUIDs,
                                               newCandidatesMap,
                                               updateCandidatesMap,
                                               labelsToBeUpdatedMap ->
      val applicationContext = context.applicationContext
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)

      if (accountEntity.isGoogleSignInAccount && accountEntity.useAPI && accountEntity.useConversationMode) {
        roomDatabase.msgDao()
          .deleteByUIDsSuspend(accountEntity.email, localFolder.fullName, deleteCandidatesUIDs.map {
            Long.MAX_VALUE - it
          })
      } else {
        roomDatabase.msgDao()
          .deleteByUIDsSuspend(accountEntity.email, localFolder.fullName, deleteCandidatesUIDs)
      }

      val folderType = FoldersManager.getFolderType(localFolder)
      if (folderType == FoldersManager.FolderType.INBOX) {
        val notificationManager = MessagesNotificationManager(applicationContext)
        for (uid in deleteCandidatesUIDs) {
          notificationManager.cancel(uid.toHex())
        }
      }

      val newCandidates = newCandidatesMap.values
      if (newCandidates.isNotEmpty()) {
        val gmailThreadInfoList: List<GmailThreadInfo>
        val msgs: List<Message>
        val existingThreads: List<MessageEntity>
        if (accountEntity.useConversationMode) {
          val uniqueThreadIdList = newCandidates.map { it.threadId }.toSet()
          existingThreads = roomDatabase.msgDao()
            .getThreadsByID(accountEntity.email, localFolder.fullName, uniqueThreadIdList)
          val existingThreadIds = existingThreads.mapNotNull { it.threadId }
          gmailThreadInfoList = GmailApiHelper.loadGmailThreadInfoInParallel(
            context = applicationContext,
            accountEntity = accountEntity,
            threads = uniqueThreadIdList.map { Thread().apply { id = it } },
            format = GmailApiHelper.RESPONSE_FORMAT_FULL,
            localFolder = localFolder
          )

          msgs = gmailThreadInfoList.map { it.lastMessage }
            .filter { it.threadIdAsLong !in existingThreadIds }
        } else {
          gmailThreadInfoList = emptyList()
          existingThreads = emptyList()
          msgs = GmailApiHelper.loadMsgsInParallel(
            applicationContext, accountEntity,
            newCandidates.toList(), localFolder
          ).run {
            if (accountEntity.showOnlyEncrypted == true) {
              filter { it.hasPgp() }
            } else this
          }
        }

        val draftIdsMap = if (localFolder.isDrafts) {
          val drafts =
            GmailApiHelper.loadBaseDraftInfoInParallel(applicationContext, accountEntity, msgs)
          val fetchedDraftIdsMap = drafts.associateBy({ it.message.id }, { it.id })
          if (fetchedDraftIdsMap.size != msgs.size) {
            throw IllegalStateException(
              (applicationContext as Context).getString(R.string.fetching_drafts_info_failed)
            )
          }
          fetchedDraftIdsMap
        } else emptyMap()

        val isNew = !context.isAppForegrounded() && folderType == FoldersManager.FolderType.INBOX

        val msgEntities = MessageEntity.genMessageEntities(
          context = applicationContext,
          account = accountEntity.email,
          accountType = accountEntity.accountType,
          label = localFolder.fullName,
          msgsList = msgs,
          isNew = isNew,
          onlyPgpModeEnabled = accountEntity.showOnlyEncrypted ?: false,
          draftIdsMap = draftIdsMap
        ) { message, messageEntity ->
          if (accountEntity.useConversationMode) {
            val thread = gmailThreadInfoList.firstOrNull { it.id == message.threadId }
            messageEntity.copy(
              subject = thread?.subject,
              threadMessagesCount = thread?.messagesCount,
              labelIds = thread?.labels?.joinToString(separator = LABEL_IDS_SEPARATOR),
              hasAttachments = thread?.hasAttachments,
              fromAddresses = InternetAddress.toString(
                thread?.recipients?.toTypedArray()
              ),
              hasPgp = thread?.hasPgpThings
            )
          } else {
            messageEntity
          }
        }

        roomDatabase.msgDao().insertWithReplaceSuspend(msgEntities)
        GmailApiHelper.identifyAttachments(
          msgEntities,
          msgs,
          accountEntity,
          localFolder,
          roomDatabase
        )

        if (accountEntity.useConversationMode && existingThreads.isNotEmpty()) {
          val threadsToBeUpdated = existingThreads.mapNotNull { threadMessageEntity ->
            val thread =
              gmailThreadInfoList.firstOrNull { it.id == threadMessageEntity.threadIdAsHEX }
            if (thread != null) {
              val updatedMessageEntity = MessageEntity.genMessageEntities(
                context = applicationContext,
                account = accountEntity.email,
                accountType = accountEntity.accountType,
                label = localFolder.fullName,
                msgsList = listOf(thread.lastMessage),
                isNew = isNew,
                onlyPgpModeEnabled = accountEntity.showOnlyEncrypted ?: false,
                draftIdsMap = draftIdsMap
              ).first()

              MsgsCacheManager.removeMessage(threadMessageEntity.id.toString())
              updatedMessageEntity.copy(
                id = threadMessageEntity.id,
                threadMessagesCount = thread.messagesCount,
                labelIds = thread.labels.joinToString(separator = LABEL_IDS_SEPARATOR),
                hasAttachments = thread.hasAttachments,
                fromAddresses = InternetAddress.toString(
                  thread.recipients.toTypedArray()
                ),
                hasPgp = thread.hasPgpThings,
                flags = if (thread.hasUnreadMessages) {
                  threadMessageEntity.flags?.replace(MessageFlag.SEEN.value, "")
                } else {
                  if (threadMessageEntity.flags?.contains(MessageFlag.SEEN.value) == true) {
                    threadMessageEntity.flags
                  } else {
                    threadMessageEntity.flags.plus("${MessageFlag.SEEN.value} ")
                  }
                }
              )
            } else null
          }
          roomDatabase.msgDao().updateSuspend(threadsToBeUpdated)
        }
      }

      if (accountEntity.isGoogleSignInAccount && accountEntity.useAPI && accountEntity.useConversationMode) {
        if (updateCandidatesMap.isNotEmpty()) {
          /*
           here we will try to keep the thread 'unread' state actual.
           As 'history' tracks messages, not threads, we need to check a thread
           if the last one has unread messages
           */
          val threadIds = updateCandidatesMap.entries.map { it.value.first }.toSet()
          val gmailThreadInfoList = GmailApiHelper.loadGmailThreadInfoInParallel(
            context = applicationContext,
            accountEntity = accountEntity,
            threads = threadIds.map { Thread().apply { id = it } },
            format = GmailApiHelper.RESPONSE_FORMAT_FULL,
            localFolder = localFolder
          )

          val flagsMap = gmailThreadInfoList.associateBy(
            { Long.MAX_VALUE - it.id.toLongRadix16 },
            { labelsToImapFlags(it.labels) })

          roomDatabase.msgDao()
            .updateFlagsSuspend(accountEntity.email, localFolder.fullName, flagsMap)
        }

        if (labelsToBeUpdatedMap.isNotEmpty()) {
          /*
           here we will try to keep the thread 'labels' actual
           */
          val m = labelsToBeUpdatedMap.keys.map { Long.MAX_VALUE - it }//messages ids
          val threadIds2 = roomDatabase.msgDao()
            .getMsgsByUidsSuspend(accountEntity.email, localFolder.fullName, msgsUID = m)
            .mapNotNull { it.threadIdAsHEX }.toSet()

          val gmailThreadInfoList2 = GmailApiHelper.loadGmailThreadInfoInParallel(
            context = applicationContext,
            accountEntity = accountEntity,
            threads = threadIds2.map { Thread().apply { id = it } },
            format = GmailApiHelper.RESPONSE_FORMAT_FULL,
            localFolder = localFolder
          )

          val msgs2 = gmailThreadInfoList2.map { it.lastMessage }
          val map2 = MessageEntity.genMessageEntities(
            context = applicationContext,
            account = accountEntity.email,
            accountType = accountEntity.accountType,
            label = localFolder.fullName,
            msgsList = msgs2,
            isNew = false,
            onlyPgpModeEnabled = accountEntity.showOnlyEncrypted ?: false,
            draftIdsMap = emptyMap()
          ) { message, messageEntity ->
            val thread = gmailThreadInfoList2.firstOrNull { it.id == message.threadId }
            messageEntity.copy(
              subject = thread?.subject,
              threadMessagesCount = thread?.messagesCount,
              labelIds = thread?.labels?.joinToString(separator = LABEL_IDS_SEPARATOR),
              hasAttachments = thread?.hasAttachments,
              fromAddresses = InternetAddress.toString(
                thread?.recipients?.toTypedArray()
              ),
              hasPgp = thread?.hasPgpThings
            )
          }.associateBy({ it.uid }, { it.labelIds ?: "" })

          roomDatabase.msgDao()
            .updateGmailLabels(accountEntity.email, localFolder.fullName, map2)
        }
      } else {
        roomDatabase.msgDao()
          .updateFlagsSuspend(
            accountEntity.email,
            localFolder.fullName,
            updateCandidatesMap.entries.associate { it.key to it.value.second })

        roomDatabase.msgDao()
          .updateGmailLabels(accountEntity.email, localFolder.fullName, labelsToBeUpdatedMap)
      }

      if (folderType == FoldersManager.FolderType.SENT) {
        val session = Session.getInstance(Properties())
        GeneralUtil.updateLocalContactsIfNeeded(
          context = applicationContext,
          messages = newCandidates
            .filter { it.labelIds?.contains(GmailApiHelper.LABEL_SENT) == true }
            .map { GmaiAPIMimeMessage(session, it) }.toTypedArray()
        )
      }

      action.invoke(
        deleteCandidatesUIDs,
        newCandidatesMap,
        updateCandidatesMap,
        labelsToBeUpdatedMap
      )
    }
  }

  private suspend fun processHistory(
    accountEntity: AccountEntity,
    localFolder: LocalFolder,
    historyList: List<History>,
    action: suspend (
      deleteCandidatesUIDs: Set<Long>,
      newCandidatesMap: Map<Long, Message>,
      updateCandidatesMap: Map<Long, Pair<String, Flags>>,
      labelsToBeUpdatedMap: Map<Long, String>
    ) -> Unit
  ) = withContext(Dispatchers.IO)
  {
    val deleteCandidatesUIDs = mutableSetOf<Long>()
    val newCandidatesMap = mutableMapOf<Long, Message>()
    val updateCandidates = mutableMapOf<Long, Pair<String, Flags>>()
    val labelsToBeUpdatedMap = mutableMapOf<Long, String>()
    val isDrafts = localFolder.isDrafts

    for (history in historyList) {
      history.messagesDeleted?.let { messagesDeleted ->
        for (historyMsgDeleted in messagesDeleted) {
          newCandidatesMap.remove(historyMsgDeleted.message.uid)
          updateCandidates.remove(historyMsgDeleted.message.uid)
          deleteCandidatesUIDs.add(historyMsgDeleted.message.uid)
        }
      }

      history.messagesAdded?.let { messagesAdded ->
        for (historyMsgAdded in messagesAdded) {
          if (LABEL_DRAFT in (historyMsgAdded.message.labelIds ?: emptyList()) && !isDrafts) {
            //skip adding drafts to non-Drafts folder
            continue
          }
          deleteCandidatesUIDs.remove(historyMsgAdded.message.uid)
          updateCandidates.remove(historyMsgAdded.message.uid)
          newCandidatesMap[historyMsgAdded.message.uid] = historyMsgAdded.message
        }
      }

      history.labelsRemoved?.let { labelsRemoved ->
        for (historyLabelRemoved in labelsRemoved) {
          val historyMessageLabelIds = historyLabelRemoved?.message?.labelIds ?: emptyList()
          labelsToBeUpdatedMap[historyLabelRemoved.message.uid] =
            historyMessageLabelIds.joinToString(LABEL_IDS_SEPARATOR)
          if (localFolder.fullName in (historyLabelRemoved.labelIds ?: emptyList())) {
            newCandidatesMap.remove(historyLabelRemoved.message.uid)
            updateCandidates.remove(historyLabelRemoved.message.uid)
            deleteCandidatesUIDs.add(historyLabelRemoved.message.uid)
            continue
          }

          if (LABEL_TRASH in (historyLabelRemoved.labelIds ?: emptyList())) {
            val message = historyLabelRemoved.message
            if (localFolder.fullName in historyMessageLabelIds) {
              deleteCandidatesUIDs.remove(message.uid)
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
          labelsToBeUpdatedMap[historyLabelAdded.message.uid] = historyLabelAdded.message
            .labelIds.joinToString(LABEL_IDS_SEPARATOR)
          if (localFolder.fullName in (historyLabelAdded.labelIds ?: emptyList())) {
            deleteCandidatesUIDs.remove(historyLabelAdded.message.uid)
            updateCandidates.remove(historyLabelAdded.message.uid)
            newCandidatesMap[historyLabelAdded.message.uid] = historyLabelAdded.message
            continue
          }

          if ((historyLabelAdded.labelIds ?: emptyList()).contains(LABEL_TRASH)) {
            newCandidatesMap.remove(historyLabelAdded.message.uid)
            updateCandidates.remove(historyLabelAdded.message.uid)
            deleteCandidatesUIDs.add(historyLabelAdded.message.uid)
            continue
          }

          val existedFlags = labelsToImapFlags(historyLabelAdded.message.labelIds ?: emptyList())
          updateCandidates[historyLabelAdded.message.uid] =
            Pair(historyLabelAdded.message.threadId, existedFlags)
        }
      }
    }
    action.invoke(
      deleteCandidatesUIDs,
      newCandidatesMap,
      updateCandidates,
      labelsToBeUpdatedMap
    )
  }
}