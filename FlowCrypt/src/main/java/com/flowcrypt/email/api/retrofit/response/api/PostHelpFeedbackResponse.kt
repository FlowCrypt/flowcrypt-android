/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.api

import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * @author Denys Bondarenko
 */
@Parcelize
data class PostHelpFeedbackResponse constructor(
  @SerializedName("error") @Expose override val apiError: ApiError? = null,
  @SerializedName("sent") @Expose val isSent: Boolean? = null,
  @Expose val text: String? = null
) : ApiResponse {
  @IgnoredOnParcel
  override val code: Int? = null

  @IgnoredOnParcel
  override val message: String? = null

  @IgnoredOnParcel
  override val details: String? = null
}
