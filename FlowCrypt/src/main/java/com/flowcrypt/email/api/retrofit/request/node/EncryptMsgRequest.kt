/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node

import com.flowcrypt.email.api.retrofit.node.NodeService
import com.google.gson.annotations.Expose
import retrofit2.Response
import java.io.IOException

/**
 * Using this class we can create a request to encrypt an input message using the given public keys.
 *
 * @author Denis Bondarenko
 * Date: 1/11/19
 * Time: 12:48 PM
 * E-mail: DenBond7@gmail.com
 */
class EncryptMsgRequest(private val msg: String?,
                        @Expose val pubKeys: List<String>) : BaseNodeRequest() {

  override val endpoint: String = "encryptMsg"

  override val data: ByteArray
    get() = msg?.toByteArray() ?: byteArrayOf()

  @Throws(IOException::class)
  override fun getResponse(nodeService: NodeService): Response<*> {
    return nodeService.encryptMsg(this).execute()
  }
}
