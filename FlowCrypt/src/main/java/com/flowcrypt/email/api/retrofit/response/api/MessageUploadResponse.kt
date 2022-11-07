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
 * @author Denis Bondarenko
 *         Date: 12/20/21
 *         Time: 12:34 PM
 *         E-mail: DenBond7@gmail.com
 */
@Parcelize
data class MessageUploadResponse(
  @SerializedName("error")
  @Expose override val apiError: ApiError? = null,
  @Expose val url: String? = null
) : ApiResponse
