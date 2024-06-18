/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.accounts.Account
import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountAliasesEntity
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.toAccountAliasesEntity
import com.flowcrypt.email.extensions.java.lang.printStackTraceIfDebugOnly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author Denys Bondarenko
 */
class AccountAliasesViewModel(application: Application) : AccountViewModel(application) {
  private val fetchFreshestAliasesMutableStateFlow: MutableStateFlow<Result<Boolean>> =
    MutableStateFlow(Result.none())
  val fetchFreshestAliasesStateFlow: StateFlow<Result<Boolean>> =
    fetchFreshestAliasesMutableStateFlow.asStateFlow()

  val accountAliasesLiveData: LiveData<List<AccountAliasesEntity>> =
    activeAccountLiveData.switchMap {
      roomDatabase.accountAliasesDao().getAliasesLD(it?.email ?: "", it?.accountType ?: "")
        .distinctUntilChanged()
    }

  fun fetchUpdates(monitorProgress: Boolean = false) {
    viewModelScope.launch {
      val accountEntity = getActiveAccountSuspend() ?: return@launch
      try {
        if (monitorProgress) {
          fetchFreshestAliasesMutableStateFlow.value = Result.loading()
        }
        val freshestAliases = fetchAliases(getApplication(), accountEntity.account)
        roomDatabase.accountAliasesDao().updateAliases(activeAccountLiveData.value, freshestAliases)
        if (monitorProgress) {
          fetchFreshestAliasesMutableStateFlow.value = Result.success(true)
        }
      } catch (e: Exception) {
        e.printStackTraceIfDebugOnly()
        fetchFreshestAliasesMutableStateFlow.value = Result.exception(e)
      }
    }
  }

  private suspend fun fetchAliases(context: Context, account: Account) =
    withContext(Dispatchers.IO) {
      val gmailService = GmailApiHelper.generateGmailApiService(context, account)
      val response =
        gmailService.users().settings().sendAs().list(GmailApiHelper.DEFAULT_USER_ID).execute()
      response.sendAs.map { it.toAccountAliasesEntity(account) }
    }
}
