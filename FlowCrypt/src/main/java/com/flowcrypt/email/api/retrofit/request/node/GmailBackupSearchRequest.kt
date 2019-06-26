/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node

import com.flowcrypt.email.api.retrofit.node.NodeService
import com.google.gson.annotations.Expose
import retrofit2.Response

/**
 * Using this class we can create a request to receive a special search string for Gmail provider.
 *
 * @author Denis Bondarenko
 * Date: 2/11/19
 * Time: 9:53 AM
 * E-mail: DenBond7@gmail.com
 */
class GmailBackupSearchRequest(@Expose val acctEmail: String) : BaseNodeRequest() {

  override val endpoint: String = "gmailBackupSearch"


  override fun getResponse(nodeService: NodeService): Response<*> {
    return nodeService.gmailBackupSearch(this).execute()
  }
}
