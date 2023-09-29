/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.asFlow
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.kotlin.capitalize
import com.flowcrypt.email.model.LabelWithChoice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

/**
 * @author Denys Bondarenko
 */
class GmailLabelsViewModel(
  application: Application,
  private val messageEntity: MessageEntity,
) : AccountViewModel(application) {

  @OptIn(ExperimentalCoroutinesApi::class)
  val labelsInfoFlow: Flow<List<LabelWithChoice>> =
    activeAccountLiveData.asFlow().mapLatest { account ->
      if (account?.isGoogleSignInAccount == true) {
        val labelEntities =
          roomDatabase.labelDao().getLabelsSuspend(account.email, account.accountType)
            .filter { it.isCustom || it.name == GmailApiHelper.LABEL_INBOX }
        val labelIds = messageEntity.labelIds.orEmpty().split(" ")
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
}