/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.api.retrofit.response.base.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * @author Denis Bondarenko
 *         Date: 9/2/22
 *         Time: 3:45 PM
 *         E-mail: DenBond7@gmail.com
 */
class DraftViewModel(
  application: Application,
  originalMsgText: String = "",
  originalMsgSubject: String = "",
  originalToRecipients: Set<String> = emptySet(),
  originalCcRecipients: Set<String> = emptySet(),
  originalBccRecipients: Set<String> = emptySet()
) : RoomBasicViewModel(application) {
  private var lastMsgText: String = originalMsgText
  private var lastMsgSubject: String = originalMsgSubject
  private val lastToRecipients: MutableSet<String> = originalToRecipients.toMutableSet()
  private val lastCcRecipients: MutableSet<String> = originalCcRecipients.toMutableSet()
  private val lastBccRecipients: MutableSet<String> = originalBccRecipients.toMutableSet()

  val draftRepeatableCheckingFlow: Flow<Long> = flow {
    while (viewModelScope.isActive) {
      delay(DELAY_TIMEOUT)
      emit(System.currentTimeMillis())
    }
  }.flowOn(Dispatchers.Default)

  private val draftMutableStateFlow = MutableStateFlow<Result<Boolean?>>(Result.none())
  val draftStateFlow = draftMutableStateFlow.asStateFlow()

  fun processDraft(currentTimeMillis: Long, currentOutgoingMessageInfo: OutgoingMessageInfo) {
    viewModelScope.launch {
      Log.d("DraftViewModel", "iteration = $currentTimeMillis")
      var isSavingDraftNeeded = false
      val currentToRecipients = currentOutgoingMessageInfo.toRecipients.map { internetAddress ->
        internetAddress.address.lowercase()
      }.toSet()
      val currentCcRecipients = currentOutgoingMessageInfo.ccRecipients?.map { internetAddress ->
        internetAddress.address.lowercase()
      }?.toSet() ?: emptySet()
      val currentBccRecipients =
        currentOutgoingMessageInfo.bccRecipients?.map { internetAddress ->
          internetAddress.address.lowercase()
        }?.toSet() ?: emptySet()

      if (currentOutgoingMessageInfo.msg != lastMsgText
        || currentOutgoingMessageInfo.subject != lastMsgSubject
        || currentToRecipients != lastToRecipients
        || currentCcRecipients != lastCcRecipients
        || currentBccRecipients != lastBccRecipients
      ) {
        isSavingDraftNeeded = true
      }

      lastMsgText = currentOutgoingMessageInfo.msg ?: ""
      lastMsgSubject = currentOutgoingMessageInfo.subject
      lastToRecipients.clear()
      lastToRecipients.addAll(currentToRecipients)
      lastCcRecipients.clear()
      lastCcRecipients.addAll(currentCcRecipients)
      lastBccRecipients.clear()
      lastBccRecipients.addAll(currentBccRecipients)

      if (isSavingDraftNeeded) {
        draftMutableStateFlow.value = Result.loading()
        draftMutableStateFlow.value = Result.success(true)
      }
    }
  }

  companion object {
    val DELAY_TIMEOUT = TimeUnit.SECONDS.toMillis(5)
  }
}
