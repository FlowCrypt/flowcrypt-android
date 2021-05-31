/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.api

import com.flowcrypt.email.api.retrofit.ApiName
import com.flowcrypt.email.api.retrofit.request.BaseRequest
import com.flowcrypt.email.api.retrofit.request.model.LoginModel

/**
 * This class describes a request to the https://flowcrypt.com/api/account/login.
 *
 * @author Denis Bondarenko
 *         Date: 10/23/19
 *         Time: 3:53 PM
 *         E-mail: DenBond7@gmail.com
 */
//todo-denbond7 this class is redundant. We can refactor code and delete this
class LoginRequest(
  override val apiName: ApiName = ApiName.POST_LOGIN,
  override val requestModel: LoginModel, val tokenId: String
) : BaseRequest<LoginModel>
