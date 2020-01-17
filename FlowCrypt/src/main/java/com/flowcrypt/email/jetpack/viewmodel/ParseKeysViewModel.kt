/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.node.PgpApiRepository
import com.flowcrypt.email.security.KeysStorageImpl

/**
 * This [ViewModel] implementation can be used to fetch details about the given keys.
 *
 * @author Denis Bondarenko
 *         Date: 9/21/19
 *         Time: 2:24 PM
 *         E-mail: DenBond7@gmail.com
 */
class ParseKeysViewModel(application: Application) : BaseNodeApiViewModel(application) {
  private lateinit var keysStorage: KeysStorageImpl
  private lateinit var apiRepository: PgpApiRepository

  fun init(apiRepository: PgpApiRepository) {
    this.apiRepository = apiRepository
    this.keysStorage = KeysStorageImpl.getInstance(getApplication())
  }

  fun fetchKeys(rawKey: String?) {
    apiRepository.fetchKeyDetails(R.id.live_data_id_fetch_keys, responsesLiveData, rawKey)
  }
}