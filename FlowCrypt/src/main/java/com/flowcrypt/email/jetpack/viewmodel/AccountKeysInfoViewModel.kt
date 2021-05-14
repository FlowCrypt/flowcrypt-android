/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.accounts.Account
import android.app.Application
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
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
import java.util.*


/**
 * This [ViewModel] does job of receiving information about an array of public
 * keys from "https://flowcrypt.com/attester/lookup/email".
 *
 * @author Denis Bondarenko
 * Date: 13.11.2017
 * Time: 15:13
 * E-mail: DenBond7@gmail.com
 */

class AccountKeysInfoViewModel(application: Application) : AccountViewModel(application) {
  private val apiRepository: ApiRepository = FlowcryptApiRepository()
  val accountKeysInfoLiveData = MediatorLiveData<Result<List<PgpKeyDetails>>>()
  private val initLiveData = Transformations
      .switchMap(activeAccountLiveData) { accountEntity ->
        liveData {
          emit(Result.loading())
          emit(getResult(accountEntity))
        }
      }
  private val refreshingLiveData = MutableLiveData<Result<List<PgpKeyDetails>>>()

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
  private suspend fun getAvailableGmailAliases(account: Account): Collection<String> = withContext(Dispatchers.IO) {
    val emails = ArrayList<String>()

    try {
      val gmail = GmailApiHelper.generateGmailApiService(getApplication(), account)
      val aliases = gmail.users().settings().sendAs().list(GmailApiHelper.DEFAULT_USER_ID).execute()
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
      withContext(Dispatchers.IO) {
        refreshingLiveData.postValue(Result.loading())
        val accountEntity = activeAccountLiveData.value
            ?: roomDatabase.accountDao().getActiveAccountSuspend()
        refreshingLiveData.postValue(getResult(accountEntity))
      }
    }
  }

  private suspend fun getResult(accountEntity: AccountEntity?): Result<List<PgpKeyDetails>> {
    return if (accountEntity != null) {
      val results = mutableListOf<PgpKeyDetails>()
      val emails = ArrayList<String>()
      emails.add(accountEntity.email)

      if (accountEntity.account.type == AccountEntity.ACCOUNT_TYPE_GOOGLE) {
        emails.addAll(getAvailableGmailAliases(accountEntity.account))
      }

      for (email in emails) {
        val pubResponseResult = apiRepository.getPub(context = getApplication(), identData = email)
        pubResponseResult.data?.pubkey?.let { key ->
          results.addAll(PgpKey.parseKeys(key).toNodeKeyDetailsList())
        }
      }

      Result.success(results)
    } else {
      Result.exception(NullPointerException("AccountEntity is null!"))
    }
  }
}
