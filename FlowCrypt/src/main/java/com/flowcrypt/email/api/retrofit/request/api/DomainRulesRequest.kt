/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.api

import com.flowcrypt.email.api.retrofit.ApiName
import com.flowcrypt.email.api.retrofit.request.BaseRequest
import com.flowcrypt.email.api.retrofit.request.model.LoginModel

/**
 * This class describes a request to the https://flowcrypt.com/api/account/get.
 *
 * @author Denis Bondarenko
 *         Date: 10/29/19
 *         Time: 11:21 AM
 *         E-mail: DenBond7@gmail.com
 */
class DomainRulesRequest(override val apiName: ApiName = ApiName.POST_GET_DOMAIN_RULES,
                         override val requestModel: LoginModel) : BaseRequest<LoginModel>