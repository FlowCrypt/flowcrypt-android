/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node

import com.flowcrypt.email.api.retrofit.node.NodeService
import com.flowcrypt.email.api.retrofit.request.model.node.PrivateKeyInfo
import com.flowcrypt.email.model.PgpKeyInfo
import com.google.gson.annotations.Expose
import retrofit2.Response

/**
 * This class will be used for the message decryption.
 *
 * @author Denis Bondarenko
 * Date: 1/11/19
 * Time: 03:29 PM
 * E-mail: DenBond7@gmail.com
 */
class ParseDecryptMsgRequest @JvmOverloads constructor(override val data: ByteArray,
                                                       pgpKeyInfos: List<PgpKeyInfo>,
                                                       @Expose val isEmail: Boolean = false) : BaseNodeRequest() {

  @Expose
  private val keys: List<PrivateKeyInfo> = pgpKeyInfos.map { PrivateKeyInfo(it.private!!, it.longid, it.passphrase) }

  override val endpoint: String = "parseDecryptMsg"

  override fun getResponse(nodeService: NodeService): Response<*> {
    return nodeService.parseDecryptMsg(this).execute()
  }
}
