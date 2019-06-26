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
 * Using this class we can create a request to decrypt the given private key. The private key should be armored. We
 * can use one or more passphrases.
 *
 * @author Denis Bondarenko
 * Date: 2/12/19
 * Time: 4:34 PM
 * E-mail: DenBond7@gmail.com
 */
class DecryptKeyRequest(@Expose @SerializedName("armored") val armoredKey: String,
                        @Expose val passphrases: List<String>) : BaseNodeRequest() {

  override val endpoint: String = "decryptKey"

  override fun getResponse(nodeService: NodeService): Response<*> {
    return nodeService.decryptKey(this).execute()
  }
}
