/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
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
  private val messageEntity: MessageEntity,
) : AccountViewModel(application) {

  private val controlledRunnerForChangingLabels = ControlledRunner<Result<Boolean>>()
  private val changeLabelsMutableStateFlow: MutableStateFlow<Result<Boolean?>> =
    MutableStateFlow(Result.none())
  val changeLabelsStateFlow: StateFlow<Result<Boolean?>> =
    changeLabelsMutableStateFlow.asStateFlow()

  @OptIn(ExperimentalCoroutinesApi::class)
  val labelsInfoFlow: Flow<List<LabelWithChoice>> =
    activeAccountLiveData.asFlow().mapLatest { account ->
      if (account?.isGoogleSignInAccount == true) {
        val labelEntities =
          roomDatabase.labelDao().getLabelsSuspend(account.email, account.accountType)
            .filter { it.isCustom || it.name == GmailApiHelper.LABEL_INBOX }
        val latestMessageEntityRecord = roomDatabase.msgDao().getMsgById(messageEntity.id ?: -1)
        val labelIds =
          latestMessageEntityRecord?.labelIds.orEmpty().split(MessageEntity.LABEL_IDS_SEPARATOR)
        val initialList = labelEntities.map { entity ->
          LabelWithChoice(
            name = entity.alias.orEmpty(),
            id = entity.name,
            entity.labelColor,
            entity.textColor,
            labelIds.any { it == entity.name }
          )
        }

        val inbox =
          (initialList.firstOrNull { it.id == GmailApiHelper.LABEL_INBOX } ?: LabelWithChoice(
            name = GmailApiHelper.LABEL_INBOX,
            id = GmailApiHelper.LABEL_INBOX,
            isChecked = false
          )).copy(name = GmailApiHelper.LABEL_INBOX.capitalize())
        val checkedLabels =
          initialList.filter { it.isChecked && it.id != GmailApiHelper.LABEL_INBOX }
            .sortedBy { it.name.lowercase() }
        val uncheckedLabels =
          initialList.filter { !it.isChecked && it.id != GmailApiHelper.LABEL_INBOX }
            .sortedBy { it.name.lowercase() }

        if (inbox.isChecked) {
          listOf(inbox) + checkedLabels + uncheckedLabels
        } else {
          checkedLabels + listOf(inbox) + uncheckedLabels
        }
      } else {
        emptyList()
      }
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

          val latestMessageEntityRecord = roomDatabase.msgDao().getMsgById(messageEntity.id ?: -1)
            ?: return@cancelPreviousThenRun Result.success(true)

          val cachedLabelIds = latestMessageEntityRecord.labelIds.orEmpty()
            .split(MessageEntity.LABEL_IDS_SEPARATOR).toSet()

          GmailApiHelper.changeLabels(
            context = getApplication(),
            accountEntity = activeAccount,
            ids = listOf(messageEntity.uidAsHEX),
            addLabelIds = (labelIds - cachedLabelIds).toList(),
            removeLabelIds = (cachedLabelIds - labelIds).toList()
          )

          //update the local cache
          roomDatabase.msgDao()
            .updateSuspend(latestMessageEntityRecord.copy(labelIds = labelIds.joinToString(" ")))

          Result.success(true)
        } catch (e: Exception) {
          Result.exception(e)
        }
      }
    }
  }
}