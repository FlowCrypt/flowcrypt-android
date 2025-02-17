/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.security.pgp.PgpPwd
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import com.flowcrypt.email.util.exception.IllegalTextForStrengthMeasuringException
import com.nulabinc.zxcvbn.Zxcvbn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This [ViewModel] implementation can be used to check the passphrase strength
 *
 * @author Denys Bondarenko
 */
class PasswordStrengthViewModel(application: Application) : BaseAndroidViewModel(application) {
  private val zxcvbn: Zxcvbn = Zxcvbn()
  private val controlledRunnerForZxcvbn = ControlledRunner<Result<PgpPwd.PwdStrengthResult?>>()

  private val pwdStrengthResultMutableStateFlow: MutableStateFlow<Result<PgpPwd.PwdStrengthResult?>> =
    MutableStateFlow(Result.none())
  val pwdStrengthResultStateFlow: StateFlow<Result<PgpPwd.PwdStrengthResult?>> =
    pwdStrengthResultMutableStateFlow.asStateFlow()

  fun check(passphrase: CharSequence) {
    viewModelScope.launch {
      val context: Context = getApplication()
      pwdStrengthResultMutableStateFlow.value = Result.loading()
      if (passphrase.isEmpty()) {
        pwdStrengthResultMutableStateFlow.value = Result.exception(
          IllegalTextForStrengthMeasuringException(context.getString(R.string.type_text_to_start_measuring))
        )
        return@launch
      }

      pwdStrengthResultMutableStateFlow.value = controlledRunnerForZxcvbn.cancelPreviousThenRun {
        val measure = withContext(Dispatchers.IO) {
          zxcvbn.measure(
            passphrase,
            arrayListOf(*Constants.PASSWORD_WEAK_WORDS)
          ).guesses
        }
        return@cancelPreviousThenRun Result.success(PgpPwd.estimateStrength(measure.toBigDecimal()))
      }
    }
  }
}
