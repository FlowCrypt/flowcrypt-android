/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.email.model.AuthCredentials
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
open class ChangeAuthCredentialsViewModel(application: Application) :
  AccountViewModel(application) {
  private val authCredentialsMutableStateFlow: MutableStateFlow<AuthCredentials?> =
    MutableStateFlow(null)
  val authCredentialsStateFlow: StateFlow<AuthCredentials?> =
    authCredentialsMutableStateFlow.asStateFlow()

  val authCredentials: AuthCredentials?
    get() = authCredentialsStateFlow.value

  init {
    viewModelScope.launch {
      val accountEntity = getActiveAccountSuspend()
      accountEntity?.let {
        authCredentialsMutableStateFlow.value =
          AuthCredentials.from(it).copy(password = "", smtpSignInPassword = "")
      }
    }
  }

  fun updateAuthCredentials(authCredentials: AuthCredentials) {
    authCredentialsMutableStateFlow.update { authCredentials }
  }
}
