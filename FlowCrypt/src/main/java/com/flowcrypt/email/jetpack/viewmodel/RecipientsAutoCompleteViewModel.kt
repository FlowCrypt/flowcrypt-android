/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * @author Denis Bondarenko
 *         Date: 7/12/22
 *         Time: 9:22 AM
 *         E-mail: DenBond7@gmail.com
 */
class RecipientsAutoCompleteViewModel(application: Application) : RoomBasicViewModel(application) {
  private val controlledRunnerForAutoCompleteResult =
    ControlledRunner<Result<AutoCompleteResults>>()

  private val autoCompleteResultMutableStateFlow: MutableStateFlow<Result<AutoCompleteResults>> =
    MutableStateFlow(Result.none())
  val autoCompleteResultStateFlow: StateFlow<Result<AutoCompleteResults>> =
    autoCompleteResultMutableStateFlow.asStateFlow()

  fun updateAutoCompleteResults(email: String) {
    viewModelScope.launch {
      autoCompleteResultMutableStateFlow.value = Result.loading()
      autoCompleteResultMutableStateFlow.value =
        controlledRunnerForAutoCompleteResult.cancelPreviousThenRun {
          val autoCompleteResult = roomDatabase.recipientDao()
            .findMatchingRecipients(if (email.isEmpty()) "" else "%$email%")
          return@cancelPreviousThenRun Result.success(
            AutoCompleteResults(
              pattern = email,
              results = autoCompleteResult
            )
          )
        }
    }
  }

  data class AutoCompleteResults(val pattern: String, val results: List<RecipientWithPubKeys>)
}
