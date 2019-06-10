/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.api

import com.flowcrypt.email.api.retrofit.ApiName
import com.flowcrypt.email.api.retrofit.request.BaseRequest
import com.flowcrypt.email.api.retrofit.request.model.PostHelpFeedbackModel

/**
 * This class describes the next request:
 *
 *
 * <pre>
 * `POST /help/feedback  {
 * "email" (<type></type>'str'>)  # user email or "unknown@cryptup.org"
 * "message" (<type></type>'str'>)  # user feedback message text
 * }
` *
</pre> *
 *
 * @author DenBond7
 * Date: 30.05.2017
 * Time: 12:32
 * E-mail: DenBond7@gmail.com
 */

class PostHelpFeedbackRequest @JvmOverloads constructor(override val apiName: ApiName = ApiName.POST_HELP_FEEDBACK,
                                                        override val requestModel: PostHelpFeedbackModel) : BaseRequest<PostHelpFeedbackModel>
