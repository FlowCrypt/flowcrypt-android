/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.node.NodeRepository
import com.flowcrypt.email.api.retrofit.node.PgpApiRepository
import com.flowcrypt.email.api.retrofit.request.node.ParseDecryptMsgRequest
import com.flowcrypt.email.security.KeysStorageImpl

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
  private val keysStorage: KeysStorageImpl = KeysStorageImpl.getInstance(application)
  private val apiRepository: PgpApiRepository = NodeRepository()
  private var rawMimeBytes: ByteArray? = null

  init {
    this.keysStorage.attachOnRefreshListener(this)
  }

  override fun onRefresh() {}

  fun decryptMessage(rawMimeBytes: ByteArray) {
    this.rawMimeBytes = rawMimeBytes

    val pgpKeyInfoList = keysStorage.getAllPgpPrivateKeys()

    apiRepository.parseDecryptMsg(R.id.live_data_id_parse_and_decrypt_msg, responsesLiveData,
        ParseDecryptMsgRequest(data = rawMimeBytes, pgpKeyInfos = pgpKeyInfoList, isEmail = true))
  }
}
