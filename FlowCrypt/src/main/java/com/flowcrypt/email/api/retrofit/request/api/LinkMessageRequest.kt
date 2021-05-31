/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.api

import com.flowcrypt.email.api.retrofit.ApiName
import com.flowcrypt.email.api.retrofit.request.BaseRequest
import com.flowcrypt.email.api.retrofit.request.model.LinkMessageModel

/**
 * This class describes a request to the https://flowcrypt.com/api/link/message API.
 *
 *
 * `POST /link/message  {
 * "short" (<type></type>'str'>)  # short id of the message
 * }`
 *
 * @author Denis Bondarenko
 * Date: 13.07.2017
 * Time: 15:12
 * E-mail: DenBond7@gmail.com
 */
//todo-denbond7 this class is redundant. We can refactor code and delete this
class LinkMessageRequest(
  override val apiName: ApiName = ApiName.POST_LINK_MESSAGE,
  override val requestModel: LinkMessageModel
) : BaseRequest<LinkMessageModel>
