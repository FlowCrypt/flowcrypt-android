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
import com.flowcrypt.email.model.LabelWithChoice
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import com.google.android.material.checkbox.MaterialCheckBox
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
        val labelEntities = roomDatabase.labelDao().getLabelsSuspend(
          accountEntity.email, accountEntity.accountType
        ).filter { it.isCustom }

        val messageEntities = roomDatabase.msgDao().getMessagesByIDs(messageEntityIds.toList())
        val checkedStates =
          labelEntities.associateBy(
            { it.name },
            { labelEntity ->
              var count = 0

              messageEntities.forEach { messageEntity ->
                val labelIds =
                  messageEntity.labelIds.orEmpty().split(MessageEntity.LABEL_IDS_SEPARATOR)
                if (labelEntity.name in labelIds) {
                  count++
                }
              }

              when {
                count == messageEntities.size -> MaterialCheckBox.STATE_CHECKED
                count > 0 -> MaterialCheckBox.STATE_INDETERMINATE
                else -> MaterialCheckBox.STATE_UNCHECKED
              }
            })

        val resultsList = labelEntities.map { entity ->
          val initialState = checkedStates.getOrDefault(entity.name, MaterialCheckBox.STATE_UNCHECKED)
          LabelWithChoice(
            name = entity.alias.orEmpty(),
            id = entity.name,
            backgroundColor = entity.labelColor,
            textColor = entity.textColor,
            initialState = initialState,
            state = initialState
          )
        }

        val checkedLabels =
          resultsList.filter { it.state == MaterialCheckBox.STATE_CHECKED }
            .sortedBy { it.name.lowercase() }
        val indeterminateLabels =
          resultsList.filter { it.state == MaterialCheckBox.STATE_INDETERMINATE }
            .sortedBy { it.name.lowercase() }
        val uncheckedLabels =
          resultsList.filter { it.state == MaterialCheckBox.STATE_UNCHECKED }
            .sortedBy { it.name.lowercase() }

        checkedLabels + indeterminateLabels + uncheckedLabels
      } ?: emptyList()
    }

  fun changeLabels(labels: Set<LabelWithChoice>) {
    viewModelScope.launch {
      changeLabelsMutableStateFlow.value = Result.loading()
      changeLabelsMutableStateFlow.value = controlledRunnerForChangingLabels.cancelPreviousThenRun {
        return@cancelPreviousThenRun try {
          val activeAccount =
            getActiveAccountSuspend() ?: return@cancelPreviousThenRun Result.exception<Boolean>(
              IllegalStateException("Account is not defined")
            )

          val foldersManager = FoldersManager.fromDatabaseSuspend(getApplication(), activeAccount)

          val messageEntities = roomDatabase.msgDao().getMessagesByIDs(messageEntityIds.toList())
          if (messageEntities.isEmpty()) {
            return@cancelPreviousThenRun Result.success(true)
          }

          //update labels on server
          val labelsToBeAdded = labels.filter {
            it.initialState != MaterialCheckBox.STATE_CHECKED && it.state == MaterialCheckBox.STATE_CHECKED
          }
          val labelsToBeRemoved = labels.filter {
            it.initialState != MaterialCheckBox.STATE_UNCHECKED && it.state == MaterialCheckBox.STATE_UNCHECKED
          }

          GmailApiHelper.changeLabels(
            context = getApplication(),
            accountEntity = activeAccount,
            ids = messageEntities.map { it.uidAsHEX },
            addLabelIds = labelsToBeAdded.map { it.id },
            removeLabelIds = labelsToBeRemoved.map { it.id }
          )

          //update labels in the local cache
          val toBeDeletedMessageEntities = mutableListOf<MessageEntity>()
          val toBeUpdatedMessageEntities = mutableListOf<MessageEntity>()

          for (messageEntity in messageEntities) {
            val cachedLabelIds = messageEntity.labelIds.orEmpty()
              .split(MessageEntity.LABEL_IDS_SEPARATOR).toSet()

            val addLabelIds = labelsToBeAdded.map { it.id }.toSet()
            val removeLabelIds = labelsToBeRemoved.map { it.id }.toSet()

            val finalLabelIds = cachedLabelIds + addLabelIds - removeLabelIds
            val folderLabel = messageEntity.folder
            val folderType =
              foldersManager.getFolderByFullName(messageEntity.folder)?.getFolderType()

            when {
              folderType in setOf(FoldersManager.FolderType.TRASH, FoldersManager.FolderType.SPAM)
                  && finalLabelIds.contains(GmailApiHelper.LABEL_INBOX) -> {
                toBeDeletedMessageEntities.add(messageEntity)
              }

              finalLabelIds.contains(folderLabel) || folderType in setOf(
                FoldersManager.FolderType.DRAFTS,
                FoldersManager.FolderType.All
              ) -> {
                toBeUpdatedMessageEntities.add(
                  messageEntity.copy(
                    labelIds = finalLabelIds.joinToString(MessageEntity.LABEL_IDS_SEPARATOR)
                  )
                )
              }

              else -> toBeDeletedMessageEntities.add(messageEntity)
            }
          }
          roomDatabase.msgDao().deleteSuspend(toBeDeletedMessageEntities)
          roomDatabase.msgDao().updateSuspend(toBeUpdatedMessageEntities)

          Result.success(true)
        } catch (e: Exception) {
          Result.exception(e)
        }
      }
    }
  }
}