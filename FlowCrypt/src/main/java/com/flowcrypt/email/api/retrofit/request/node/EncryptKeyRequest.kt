/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node

import com.flowcrypt.email.api.retrofit.node.NodeService
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import retrofit2.Response

/**
 * Using this class we can create a request to encrypt a private key using the given passphrase. The private key
 * should be armored.
 *
 * @author Denis Bondarenko
 * Date: 2/18/19
 * Time: 9:26 AM
 * E-mail: DenBond7@gmail.com
 */
class EncryptKeyRequest(@Expose @SerializedName("armored") val armoredKey: String,
                        @Expose val passphrase: String) : BaseNodeRequest() {

  override val endpoint: String = "encryptKey"

  override fun getResponse(nodeService: NodeService): Response<*> {
    return nodeService.encryptKey(this).execute()
  }
}
