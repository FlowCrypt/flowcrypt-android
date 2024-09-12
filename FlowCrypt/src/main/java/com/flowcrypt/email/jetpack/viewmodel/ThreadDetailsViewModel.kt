/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.asFlow
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.getInReplyTo
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.getMessageId
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.isDraft
import com.flowcrypt.email.extensions.java.lang.printStackTraceIfDebugOnly
import com.flowcrypt.email.ui.adapter.GmailApiLabelsListAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge

/**
 * @author Denys Bondarenko
 */
class ThreadDetailsViewModel(
  private val messageEntityId: Long,
  application: Application
) : AccountViewModel(application) {

  val messageFlow: Flow<MessageEntity?> = roomDatabase.msgDao().getMessageByIdFlow(messageEntityId).distinctUntilChanged()

  val messagesInThreadFlow: Flow<List<MessageEntity>> =
    merge(
      flow {
        val initialMessageEntity = roomDatabase.msgDao().getMsgById(messageEntityId) ?: return@flow
        val activeAccount = getActiveAccountSuspend() ?: return@flow
        if (initialMessageEntity.threadId.isNullOrEmpty() || !activeAccount.isGoogleSignInAccount) {
          emit(listOf(initialMessageEntity))
        } else {
          try {
            val messagesInThread = GmailApiHelper.loadMessagesInThread(
              application,
              activeAccount,
              initialMessageEntity.threadId
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
              .updateSuspend(initialMessageEntity.copy(threadMessagesCount = messagesInThread.size))

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
              threadId = initialMessageEntity.threadId
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
              initialMessageEntity.threadId,
            )

            val finalList = messageEntities.map { fromServerMessageEntity ->
              fromServerMessageEntity.copy(id = cachedEntities.firstOrNull {
                it.uid == fromServerMessageEntity.uid
              }?.id)
            }

            emit(finalList)
          } catch (e: Exception) {
            e.printStackTraceIfDebugOnly()
            emit(listOf(initialMessageEntity))
          }
        }
      },
    )

  @OptIn(ExperimentalCoroutinesApi::class)
  val messageGmailApiLabelsFlow: Flow<List<GmailApiLabelsListAdapter.Label>> =
    merge(
      messageFlow.mapLatest { latestMessageEntityRecord ->
        val activeAccount = getActiveAccountSuspend()
        if (activeAccount?.isGoogleSignInAccount == true) {
          val labelEntities =
            roomDatabase.labelDao().getLabelsSuspend(activeAccount.email, activeAccount.accountType)
          MessageEntity.generateColoredLabels(
            latestMessageEntityRecord?.labelIds?.split(MessageEntity.LABEL_IDS_SEPARATOR),
            labelEntities
          )
        } else {
          emptyList()
        }
      },
      activeAccountLiveData.asFlow().mapLatest { account ->
        if (account?.isGoogleSignInAccount == true) {
          val labelEntities =
            roomDatabase.labelDao().getLabelsSuspend(account.email, account.accountType)
          val freshestMessageEntity = roomDatabase.msgDao().getMsgById(messageEntityId)
          val cachedLabelIds =
            freshestMessageEntity?.labelIds?.split(MessageEntity.LABEL_IDS_SEPARATOR)
          try {
            val latestLabelIds = GmailApiHelper.loadThreadInfo(
              context = getApplication(),
              accountEntity = account,
              threadId = freshestMessageEntity?.threadId ?: "",
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
            MessageEntity.generateColoredLabels(latestLabelIds, labelEntities)
          } catch (e: Exception) {
            MessageEntity.generateColoredLabels(cachedLabelIds, labelEntities)
          }
        } else {
          emptyList()
        }
      })
}