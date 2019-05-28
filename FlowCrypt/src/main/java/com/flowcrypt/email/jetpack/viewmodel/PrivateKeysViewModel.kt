/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.node.PgpApiRepository
import com.flowcrypt.email.security.KeysStorageImpl
import com.google.android.gms.common.util.CollectionUtils

/**
 * This [ViewModel] implementation can be used to fetch details about imported keys.
 *
 * @author Denis Bondarenko
 * Date: 2/14/19
 * Time: 10:50 AM
 * E-mail: DenBond7@gmail.com
 */
class PrivateKeysViewModel(application: Application) : BaseNodeApiViewModel(application),
    KeysStorageImpl.OnRefreshListener {
  private var keysStorage: KeysStorageImpl? = null
  private var apiRepository: PgpApiRepository? = null

  override fun onRefresh() {
    checkAndFetchKeys()
  }

  fun init(apiRepository: PgpApiRepository) {
    this.apiRepository = apiRepository
    this.keysStorage = KeysStorageImpl.getInstance(getApplication())
    this.keysStorage!!.attachOnRefreshListener(this)
    checkAndFetchKeys()
  }

  private fun fetchKeys(rawKey: String?) {
    apiRepository!!.fetchKeyDetails(R.id.live_data_id_fetch_keys, responsesLiveData, rawKey!!)
  }

  private fun checkAndFetchKeys() {
    val pgpKeyInfoList = keysStorage!!.getAllPgpPrivateKeys()
    if (!CollectionUtils.isEmpty(pgpKeyInfoList)) {
      val builder = StringBuilder()
      for ((_, private) in pgpKeyInfoList) {
        builder.append(private).append("\n")
      }

      fetchKeys(builder.toString())
    } else {
      fetchKeys(null)
    }
  }
}
