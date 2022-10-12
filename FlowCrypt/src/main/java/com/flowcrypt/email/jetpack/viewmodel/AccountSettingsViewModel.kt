/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.database.entity.AccountEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * @author Denis Bondarenko
 *         Date: 1/25/20
 *         Time: 10:21 AM
 *         E-mail: DenBond7@gmail.com
 */
open class AccountSettingsViewModel(application: Application) :
  AccountViewModel(application) {
  private val accountSettingsMutableStateFlow: MutableStateFlow<AccountEntity?> =
    MutableStateFlow(null)
  val accountSettingsStateFlow: StateFlow<AccountEntity?> =
    accountSettingsMutableStateFlow.asStateFlow()

  val cachedAccountEntity: AccountEntity?
    get() = accountSettingsStateFlow.value

  init {
    viewModelScope.launch {
      val accountEntity = getActiveAccountSuspend()
      accountSettingsMutableStateFlow.value = accountEntity?.copy(password = "", smtpPassword = "")
    }
  }

  fun updateCachedAccountSettings(accountEntity: AccountEntity) {
    accountSettingsMutableStateFlow.update {
      accountEntity
    }
  }
}
