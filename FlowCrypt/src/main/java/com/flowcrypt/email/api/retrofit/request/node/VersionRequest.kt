/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node

import com.flowcrypt.email.api.retrofit.node.NodeService
import retrofit2.Response

/**
 * This [request][NodeRequest] can be used to receive information about a version.
 *
 * @author Denis Bondarenko
 * Date: 1/10/19
 * Time: 5:46 PM
 * E-mail: DenBond7@gmail.com
 */
class VersionRequest : BaseNodeRequest() {

  override val endpoint: String = "version"

  override fun getResponse(nodeService: NodeService): Response<*> {
    return nodeService.getVersion(this).execute()
  }
}
