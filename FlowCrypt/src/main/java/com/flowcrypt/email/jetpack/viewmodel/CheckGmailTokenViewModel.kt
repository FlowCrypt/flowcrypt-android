/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.google.android.gms.auth.UserRecoverableAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This [androidx.lifecycle.ViewModel] checks a token for a given Gmail account
 *
 * @author Denis Bondarenko
 *         Date: 11/27/19
 *         Time: 12:06 PM
 *         E-mail: DenBond7@gmail.com
 */
class CheckGmailTokenViewModel(application: Application) : BaseAndroidViewModel(application) {
  val tokenLiveData: MutableLiveData<Intent?> = MutableLiveData()

  fun checkToken(accountDao: AccountDao) {
    viewModelScope.launch {
      tokenLiveData.value = retrieveGmailToken(accountDao)
    }
  }

  private suspend fun retrieveGmailToken(accountDao: AccountDao): Intent? {
    return withContext(Dispatchers.IO) {
      try {
        EmailUtil.getGmailAccountToken(getApplication(), accountDao)
        null
      } catch (e: UserRecoverableAuthException) {
        e.printStackTrace()
        AccountDaoSource().updateAccountInformation(getApplication(), accountDao, ContentValues().apply {
          put(AccountDaoSource.COL_IS_RESTORE_ACCESS_REQUIRED, true)
        })
        e.intent
      } catch (e: Exception) {
        null
      }
    }
  }
}