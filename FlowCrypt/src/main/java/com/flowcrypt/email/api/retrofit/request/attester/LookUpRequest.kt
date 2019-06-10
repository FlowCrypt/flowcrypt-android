/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.attester

import com.flowcrypt.email.api.retrofit.ApiName
import com.flowcrypt.email.api.retrofit.request.BaseRequest
import com.flowcrypt.email.api.retrofit.request.model.RequestModel

/**
 * This class describes the request to the API "https://flowcrypt.com/attester/lookup"
 *
 * @author DenBond7
 * Date: 05.05.2018
 * Time: 14:46
 * E-mail: DenBond7@gmail.com
 */

class LookUpRequest @JvmOverloads constructor(override val apiName: ApiName = ApiName.GET_LOOKUP,
                                              val query: String) : BaseRequest<RequestModel> {
  override val requestModel: RequestModel = object : RequestModel {}
}

