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
import com.flowcrypt.email.api.retrofit.node.NodeRepository
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.node.ZxcvbnStrengthBarResult
import com.nulabinc.zxcvbn.Zxcvbn
import kotlinx.coroutines.launch

/**
 * This [ViewModel] implementation can be used to check the passphrase strength
 *
 * @author Denis Bondarenko
 * Date: 4/2/19
 * Time: 11:11 AM
 * E-mail: DenBond7@gmail.com
 */
class PasswordStrengthViewModel(application: Application) : BaseNodeApiViewModel(application) {
  private val zxcvbn: Zxcvbn = Zxcvbn()
  private val pgpApiRepository = NodeRepository()

  val zxcvbnStrengthBarResultLiveData: MutableLiveData<Result<ZxcvbnStrengthBarResult?>> = MutableLiveData()

  fun check(passphrase: String) {
    viewModelScope.launch {
      zxcvbnStrengthBarResultLiveData.value = Result.loading()
      val measure = zxcvbn.measure(passphrase, arrayListOf(*Constants.PASSWORD_WEAK_WORDS)).guesses
      zxcvbnStrengthBarResultLiveData.value = pgpApiRepository.zxcvbnStrengthBar(getApplication(), measure)
    }
  }
}
