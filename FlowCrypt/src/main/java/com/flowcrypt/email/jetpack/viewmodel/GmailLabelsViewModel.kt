/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.kotlin.capitalize
import com.flowcrypt.email.model.LabelWithChoice
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

/**
 * @author Denys Bondarenko
 */
class GmailLabelsViewModel(
  application: Application,
  private val messageEntityIds: LongArray,
) : AccountViewModel(application) {

  private val controlledRunnerForChangingLabels = ControlledRunner<Result<Boolean>>()
  private val changeLabelsMutableStateFlow: MutableStateFlow<Result<Boolean?>> =
    MutableStateFlow(Result.none())
  val changeLabelsStateFlow: StateFlow<Result<Boolean?>> =
    changeLabelsMutableStateFlow.asStateFlow()

  @OptIn(ExperimentalCoroutinesApi::class)
  val labelsInfoFlow: Flow<List<LabelWithChoice>> =
    activeAccountLiveData.asFlow().mapLatest { account ->
      account.takeIf { account?.isGoogleSignInAccount == true }?.let { accountEntity ->
        val labelEntities =
          roomDatabase.labelDao().getLabelsSuspend(accountEntity.email, accountEntity.accountType)
            .filter { it.isCustom || it.name == GmailApiHelper.LABEL_INBOX }
        val checkedStates = labelEntities.associateBy({ it.name }, { false }).toMutableMap()
        val messageEntities = roomDatabase.msgDao().getMessagesByIDs(messageEntityIds.toList())
        for (messageEntity in messageEntities) {
          val labelIds =
            messageEntity.labelIds.orEmpty().split(MessageEntity.LABEL_IDS_SEPARATOR)
          val uncheckedItems = checkedStates.filter { entry -> !entry.value }.keys
          if (uncheckedItems.isEmpty()) {
            break
          }

          val checkedItems = labelIds.filter { it in uncheckedItems }
          checkedStates.putAll(checkedItems.associateBy({ it }, { true }))
        }

        val resultsList = labelEntities.map { entity ->
          LabelWithChoice(
            name = entity.alias.orEmpty(),
            id = entity.name,
            backgroundColor = entity.labelColor,
            textColor = entity.textColor,
            isChecked = checkedStates.getOrDefault(entity.name, false)
          )
        }

        val inbox =
          (resultsList.firstOrNull { it.id == GmailApiHelper.LABEL_INBOX } ?: LabelWithChoice(
            name = GmailApiHelper.LABEL_INBOX,
            id = GmailApiHelper.LABEL_INBOX,
            isChecked = false
          )).copy(name = GmailApiHelper.LABEL_INBOX.capitalize())
        val checkedLabels =
          resultsList.filter { it.isChecked && it.id != GmailApiHelper.LABEL_INBOX }
            .sortedBy { it.name.lowercase() }
        val uncheckedLabels =
          resultsList.filter { !it.isChecked && it.id != GmailApiHelper.LABEL_INBOX }
            .sortedBy { it.name.lowercase() }

        if (inbox.isChecked) {
          listOf(inbox) + checkedLabels + uncheckedLabels
        } else {
          checkedLabels + listOf(inbox) + uncheckedLabels
        }
      } ?: emptyList()
    }

  fun changeLabels(labelIds: Set<String>) {
    viewModelScope.launch {
      changeLabelsMutableStateFlow.value = Result.loading()
      changeLabelsMutableStateFlow.value = controlledRunnerForChangingLabels.cancelPreviousThenRun {
        return@cancelPreviousThenRun try {
          val activeAccount =
            getActiveAccountSuspend() ?: return@cancelPreviousThenRun Result.exception<Boolean>(
              IllegalStateException("Account is not defined")
            )

          val labelsEntities = roomDatabase.labelDao().getLabelsSuspend(
            account = activeAccount.email,
            accountType = activeAccount.accountType
          )

          val protectedLabelIds = labelsEntities
            .filter { !it.isCustom && it.name != GmailApiHelper.LABEL_INBOX }
            .map { it.name }

          val latestMessageEntityRecord =
            roomDatabase.msgDao().getMsgById(messageEntityIds.firstOrNull() ?: -1)
            ?: return@cancelPreviousThenRun Result.success(true)

          val cachedLabelIds = latestMessageEntityRecord.labelIds.orEmpty()
            .split(MessageEntity.LABEL_IDS_SEPARATOR).toSet()

          val allowedToChangeLabelIds = cachedLabelIds.filter { it !in protectedLabelIds }.toSet()
          val addLabelIds = labelIds - allowedToChangeLabelIds
          val removeLabelIds = allowedToChangeLabelIds - labelIds

          GmailApiHelper.changeLabels(
            context = getApplication(),
            accountEntity = activeAccount,
            ids = listOf(latestMessageEntityRecord.uidAsHEX),
            addLabelIds = addLabelIds.toList(),
            removeLabelIds = removeLabelIds.toList()
          )

          //update the local cache
          val finalLabelIds = cachedLabelIds + addLabelIds - removeLabelIds
          val folderLabel = latestMessageEntityRecord.folder
          val foldersManager = FoldersManager.fromDatabaseSuspend(getApplication(), activeAccount)
          val folderType =
            foldersManager.getFolderByFullName(latestMessageEntityRecord.folder)?.getFolderType()

          when {
            folderType in setOf(FoldersManager.FolderType.TRASH, FoldersManager.FolderType.SPAM)
                && finalLabelIds.contains(GmailApiHelper.LABEL_INBOX) -> {
              roomDatabase.msgDao().deleteSuspend(latestMessageEntityRecord)
            }

            finalLabelIds.contains(folderLabel) || folderType in setOf(
              FoldersManager.FolderType.DRAFTS,
              FoldersManager.FolderType.All
            ) -> {
              roomDatabase.msgDao().updateSuspend(
                latestMessageEntityRecord.copy(
                  labelIds = finalLabelIds.joinToString(MessageEntity.LABEL_IDS_SEPARATOR)
                )
              )
            }

            else -> {
              roomDatabase.msgDao().deleteSuspend(latestMessageEntityRecord)
            }
          }

          Result.success(true)
        } catch (e: Exception) {
          Result.exception(e)
        }
      }
    }
  }
}