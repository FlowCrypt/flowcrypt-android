/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node

import com.flowcrypt.email.api.retrofit.node.NodeService
import com.google.gson.annotations.Expose
import retrofit2.Response

/**
 * Using this class we can create a request to check is the passphrase is strong.
 *
 * @author Denis Bondarenko
 * Date: 4/1/19
 * Time: 3:06 PM
 * E-mail: DenBond7@gmail.com
 */
class ZxcvbnStrengthBarRequest(@Expose val guesses: Double) : BaseNodeRequest() {

  @Expose
  private val purpose: String = "passphrase"

  override val endpoint: String = "zxcvbnStrengthBar"

  override fun getResponse(nodeService: NodeService): Response<*> {
    return nodeService.zxcvbnStrengthBar(this).execute()
  }
}
