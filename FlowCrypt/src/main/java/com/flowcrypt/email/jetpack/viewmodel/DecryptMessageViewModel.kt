/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.node.PgpApiRepository
import com.flowcrypt.email.api.retrofit.request.node.ParseDecryptMsgRequest
import com.flowcrypt.email.security.KeysStorageImpl
import java.util.*

/**
 * This [ViewModel] implementation can be used to parse and decrypt (if needed) an incoming message.
 *
 * @author Denis Bondarenko
 * Date: 3/21/19
 * Time: 11:47 AM
 * E-mail: DenBond7@gmail.com
 */
class DecryptMessageViewModel(application: Application) : BaseNodeApiViewModel(application),
    KeysStorageImpl.OnRefreshListener {
  private var keysStorage: KeysStorageImpl? = null
  private var apiRepository: PgpApiRepository? = null
  private var rawMessage: String? = null

  override fun onRefresh() {}

  fun init(apiRepository: PgpApiRepository) {
    this.apiRepository = apiRepository
    this.keysStorage = KeysStorageImpl.getInstance(getApplication())
    this.keysStorage!!.attachOnRefreshListener(this)
  }

  fun decryptMessage(rawMessage: String) {
    this.rawMessage = rawMessage
    val passphrases = ArrayList<String>()

    val pgpKeyInfoList = keysStorage!!.getAllPgpPrivateKeys()

    for ((longid) in pgpKeyInfoList) {
      passphrases.add(keysStorage!!.getPassphrase(longid))
    }

    apiRepository!!.parseDecryptMsg(R.id.live_data_id_parse_and_decrypt_msg, responsesLiveData,
        ParseDecryptMsgRequest(rawMessage, pgpKeyInfoList, passphrases, true))
  }
}
