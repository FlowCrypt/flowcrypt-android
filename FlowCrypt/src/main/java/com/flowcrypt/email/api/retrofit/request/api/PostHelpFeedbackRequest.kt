/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
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
 * @author Denys Bondarenko
 */
//todo-denbond7 this class is redundant. We can refactor code and delete this
class PostHelpFeedbackRequest @JvmOverloads constructor(
  override val apiName: ApiName = ApiName.POST_HELP_FEEDBACK,
  override val requestModel: PostHelpFeedbackModel
) : BaseRequest<PostHelpFeedbackModel>
