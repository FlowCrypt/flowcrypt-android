/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.accounts.Account
import android.app.Application
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.retrofit.ApiRepository
import com.flowcrypt.email.api.retrofit.FlowcryptApiRepository
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.util.exception.ExceptionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException


/**
 * This [ViewModel] does job of receiving information about an array of public
 * keys from FlowCrypt Attester or WKD.
 *
 * @author Denys Bondarenko
 */
class AccountPublicKeyServersViewModel(application: Application) : AccountViewModel(application) {
  private val apiRepository: ApiRepository = FlowcryptApiRepository()
  val accountKeysInfoLiveData = MediatorLiveData<Result<List<Pair<String, PgpKeyDetails>>>>()
  private val initLiveData = activeAccountLiveData.switchMap { accountEntity ->
    liveData {
      emit(Result.loading())
      emit(getResult(accountEntity))
    }
  }
  private val refreshingLiveData = MutableLiveData<Result<List<Pair<String, PgpKeyDetails>>>>()

  init {
    accountKeysInfoLiveData.addSource(initLiveData) { accountKeysInfoLiveData.value = it }
    accountKeysInfoLiveData.addSource(refreshingLiveData) { accountKeysInfoLiveData.value = it }
  }

  /**
   * Get available Gmail aliases for an input [AccountEntity].
   *
   * @param account The [AccountEntity] object which contains information about an email account.
   * @return The list of available Gmail aliases.
   */
  private suspend fun getAvailableGmailAliases(account: Account): Collection<String> =
    withContext(Dispatchers.IO) {
      val emails = ArrayList<String>()

      try {
        val gmail = GmailApiHelper.generateGmailApiService(getApplication(), account)
        val aliases =
          gmail.users().settings().sendAs().list(GmailApiHelper.DEFAULT_USER_ID).execute()
        for (alias in aliases.sendAs) {
          if (alias.verificationStatus != null) {
            emails.add(alias.sendAsEmail)
          }
        }
      } catch (e: IOException) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
      }

      return@withContext emails
    }

  fun refreshData() {
    viewModelScope.launch {
      refreshingLiveData.value = Result.loading()
      val accountEntity = activeAccountLiveData.value
        ?: roomDatabase.accountDao().getActiveAccountSuspend()
      refreshingLiveData.value = getResult(accountEntity)
    }
  }

  private suspend fun getResult(accountEntity: AccountEntity?):
      Result<List<Pair<String, PgpKeyDetails>>> = withContext(Dispatchers.IO) {
    return@withContext if (accountEntity != null) {
      try {
        val results = mutableListOf<Pair<String, PgpKeyDetails>>()
        val emails = ArrayList<String>()
        emails.add(accountEntity.email)

        if (accountEntity.account.type == AccountEntity.ACCOUNT_TYPE_GOOGLE) {
          emails.addAll(getAvailableGmailAliases(accountEntity.account))
        }

        for (email in emails) {
          val normalizedEmail = email.lowercase()
          val pubResponseResult = apiRepository.pubLookup(
            context = getApplication(),
            email = normalizedEmail,
            clientConfiguration = accountEntity.clientConfiguration
          )
          pubResponseResult.data?.pubkey?.let { key ->
            results.addAll(PgpKey.parseKeys(key).pgpKeyDetailsList.map {
              Pair(normalizedEmail, it)
            })
          }
        }

        Result.success(results)
      } catch (e: Exception) {
        Result.exception(e)
      }
    } else {
      Result.exception(NullPointerException("AccountEntity is null!"))
    }
  }
}
