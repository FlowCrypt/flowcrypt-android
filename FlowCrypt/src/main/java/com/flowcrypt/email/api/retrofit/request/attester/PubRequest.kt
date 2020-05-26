/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.attester

import com.flowcrypt.email.api.retrofit.ApiName
import com.flowcrypt.email.api.retrofit.request.BaseRequest
import com.flowcrypt.email.api.retrofit.request.model.RequestModel

/**
 * This class describes the request to the API "https://flowcrypt.com/attester/pub"
 *
 * @author DenBond7
 * Date: 05.05.2018
 * Time: 14:46
 * E-mail: DenBond7@gmail.com
 */
//todo-denbond7 this class is redundant. We can refactor code and delete this
class PubRequest @JvmOverloads constructor(override val apiName: ApiName = ApiName.GET_PUB,
                                           val query: String) : BaseRequest<RequestModel> {
  override val requestModel: RequestModel = object : RequestModel {}
}

