/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.api

import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * The simple POJO object, which contains information about a post feedback result.
 *
 *
 * This class describes the next response:
 *
 *
 * <pre>
 * `POST
 * response(200): {
 * "sent" (True, False)  # True if message was sent successfully
 * "text" (<type></type>'str'>)  # User friendly success or error text
 * }
` *
</pre> *
 *
 * @author Denys Bondarenko
 */
@Parcelize
data class PostHelpFeedbackResponse constructor(
  @SerializedName("error") @Expose override val apiError: ApiError? = null,
  @SerializedName("sent") @Expose val isSent: Boolean? = null,
  @Expose val text: String? = null
) : ApiResponse
