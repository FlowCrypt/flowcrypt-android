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
 * This class describes a response from the https://flowcrypt.com/api/message/reply API.
 *
 *
 * `POST /message/reply
 * response(200): {
 * "sent" (True, False)  # successfully sent message
 * [voluntary] "error" (<type></type>'str'>)  # Encountered error if any
 * }`
 *
 * @author Denis Bondarenko
 * Date: 13.07.2017
 * Time: 16:33
 * E-mail: DenBond7@gmail.com
 */
@Parcelize
data class MessageReplyResponse constructor(
  @SerializedName("error") @Expose override val apiError: ApiError?,
  @Expose val isSent: Boolean
) : ApiResponse
