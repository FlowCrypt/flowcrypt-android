/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.Constants
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.security.pgp.PgpPwd
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import com.nulabinc.zxcvbn.Zxcvbn
import kotlinx.coroutines.launch
import java.util.*

/**
 * This [ViewModel] implementation can be used to check the passphrase strength
 *
 * @author Denis Bondarenko
 * Date: 4/2/19
 * Time: 11:11 AM
 * E-mail: DenBond7@gmail.com
 */
class PasswordStrengthViewModel(application: Application) : BaseAndroidViewModel(application) {
  private val zxcvbn: Zxcvbn = Zxcvbn()
  private val controlledRunnerForZxcvbn = ControlledRunner<Result<PgpPwd.PwdStrengthResult?>>()

  val pwdStrengthResultLiveData: MutableLiveData<Result<PgpPwd.PwdStrengthResult?>> =
    MutableLiveData()

  fun check(passphrase: String) {
    viewModelScope.launch {
      pwdStrengthResultLiveData.value = Result.loading()
      val measure = zxcvbn.measure(passphrase, arrayListOf(*Constants.PASSWORD_WEAK_WORDS)).guesses
      pwdStrengthResultLiveData.value = controlledRunnerForZxcvbn.cancelPreviousThenRun {
        return@cancelPreviousThenRun Result.success(PgpPwd.estimateStrength(measure.toBigDecimal()))
      }
    }
  }
}
