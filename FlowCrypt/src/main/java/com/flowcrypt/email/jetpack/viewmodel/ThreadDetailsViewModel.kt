/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.asFlow
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.java.lang.printStackTraceIfDebugOnly
import com.flowcrypt.email.extensions.kotlin.toHex
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
            )

            val isOnlyPgpModeEnabled = activeAccount.showOnlyEncrypted ?: false
            val messageEntities = MessageEntity.genMessageEntities(
              context = getApplication(),
              account = activeAccount.email,
              accountType = activeAccount.accountType,
              label = "INBOX",
              msgsList = messagesInThread,
              isNew = false,
              onlyPgpModeEnabled = isOnlyPgpModeEnabled,
              draftIdsMap = emptyMap()
            ) { message, messageEntity ->
              messageEntity.copy(snippet = message.snippet)
            }

            emit(messageEntities)
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
            val message = GmailApiHelper.loadMsgInfoSuspend(
              context = getApplication(),
              accountEntity = account,
              msgId = messageEntityId.toHex(),
              fields = null,
              format = GmailApiHelper.MESSAGE_RESPONSE_FORMAT_MINIMAL
            )

            val latestLabelIds = message.labelIds
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