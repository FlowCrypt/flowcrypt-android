/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.attester

import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * This class describes a response from the https://flowcrypt.com/attester/test/welcome API.
 *
 *
 * `POST /test/welcome
 * response(200): {
 * "sent" (True, False)  # successfuly sent email
 * [voluntary] "error" (<type></type>'str'>)  # error detail, if not saved
 * }`
 *
 * @author Denis Bondarenko
 * Date: 12.07.2017
 * Time: 14:38
 * E-mail: DenBond7@gmail.com
 */
@Parcelize
data class WelcomeMessageResponse constructor(
  @SerializedName("error") @Expose override val apiError: ApiError?
) : ApiResponse
