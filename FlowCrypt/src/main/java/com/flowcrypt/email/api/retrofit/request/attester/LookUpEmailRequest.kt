/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.attester

import com.flowcrypt.email.api.retrofit.ApiName
import com.flowcrypt.email.api.retrofit.request.BaseRequest
import com.flowcrypt.email.api.retrofit.request.model.PostLookUpEmailModel

/**
 * This class describes the request to the API "https://flowcrypt.com/attester/lookup/email"
 *
 * @author DenBond7
 * Date: 24.04.2017
 * Time: 13:24
 * E-mail: DenBond7@gmail.com
 */

class LookUpEmailRequest(override val apiName: ApiName = ApiName.POST_LOOKUP_EMAIL_SINGLE,
                         override val requestModel: PostLookUpEmailModel) : BaseRequest<PostLookUpEmailModel>
