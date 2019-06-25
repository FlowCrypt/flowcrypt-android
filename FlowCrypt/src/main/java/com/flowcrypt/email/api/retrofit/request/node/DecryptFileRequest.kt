/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node

import com.flowcrypt.email.api.retrofit.node.NodeService
import com.flowcrypt.email.api.retrofit.request.model.node.PrivateKeyInfo
import com.flowcrypt.email.model.PgpKeyInfo
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import java.util.*

/**
 * Using this class we can create a request to decrypt an encrypted file using the given private keys.
 *
 * @author Denis Bondarenko
 * Date: 1/15/19
 * Time: 4:32 PM
 * E-mail: DenBond7@gmail.com
 */
class DecryptFileRequest(override val data: ByteArray,
                         prvKeys: List<PgpKeyInfo>,
                         @SerializedName("passphrases") @Expose val passphrases: List<String>) :
    BaseNodeRequest() {

  @SerializedName("keys")
  @Expose
  private val privateKeyInfoList: MutableList<PrivateKeyInfo>

  override val endpoint: String = "decryptFile"

  init {
    this.privateKeyInfoList = ArrayList()

    for ((longid, private) in prvKeys) {
      privateKeyInfoList.add(PrivateKeyInfo(private!!, longid))
    }
  }

  override fun getResponse(nodeService: NodeService): Response<*> {
    return nodeService.decryptFile(this).execute()
  }
}
