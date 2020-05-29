/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.node.PgpApiRepository
import com.flowcrypt.email.api.retrofit.request.node.ZxcvbnStrengthBarRequest
import com.nulabinc.zxcvbn.Zxcvbn

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
  private var apiRepository: PgpApiRepository? = null

  fun init(apiRepository: PgpApiRepository) {
    this.apiRepository = apiRepository
  }

  fun check(passphrase: String) {
    val measure = zxcvbn.measure(passphrase, arrayListOf(*Constants.PASSWORD_WEAK_WORDS)).guesses

    //todo-denbond7 need to change it to use the common approach with LiveData
    apiRepository!!.checkPassphraseStrength(R.id.live_data_id_check_passphrase_strength, responsesLiveData,
        ZxcvbnStrengthBarRequest(measure))
  }
}
