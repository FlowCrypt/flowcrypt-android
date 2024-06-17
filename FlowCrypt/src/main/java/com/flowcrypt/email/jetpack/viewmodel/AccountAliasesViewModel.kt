/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.accounts.Account
import android.app.Application
import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.database.entity.AccountAliasesEntity
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.toAccountAliasesEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * @author Denys Bondarenko
 */
class AccountAliasesViewModel(application: Application) : AccountViewModel(application) {

  val accountAliasesLiveData: LiveData<List<AccountAliasesEntity>> =
    activeAccountLiveData.switchMap {
      roomDatabase.accountAliasesDao().getAliasesLD(it?.email ?: "", it?.accountType ?: "")
        .distinctUntilChanged()
    }

  private val freshAccountAliasesLiveData: LiveData<Collection<AccountAliasesEntity>> =
    activeAccountLiveData.switchMap { accountEntity ->
      liveData {
        val account = accountEntity?.account ?: return@liveData
        val context: Context = getApplication()
        val aliases: Collection<AccountAliasesEntity> = fetchAliases(context, account)
        emit(aliases)
      }
    }

  fun fetchUpdates(lifecycleOwner: LifecycleOwner) {
    freshAccountAliasesLiveData.observe(lifecycleOwner) { freshAliases ->
      viewModelScope.launch {
        roomDatabase.accountAliasesDao().updateAliases(activeAccountLiveData.value, freshAliases)
      }
    }
  }

  private suspend fun fetchAliases(context: Context, account: Account) =
    withContext(Dispatchers.IO) {
      try {
        val gmailService = GmailApiHelper.generateGmailApiService(context, account)
        val response =
          gmailService.users().settings().sendAs().list(GmailApiHelper.DEFAULT_USER_ID).execute()
        response.sendAs.map { it.toAccountAliasesEntity(account) }
      } catch (e: IOException) {
        e.printStackTrace()
        emptyList()
      }
    }
}
