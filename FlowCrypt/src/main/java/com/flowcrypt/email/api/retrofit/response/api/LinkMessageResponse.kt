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
 * This class describes a response from the https://flowcrypt.com/shared-tenant-fes/link/message API.
 *
 *
 * `POST /initial/confirm
 * response(200): {
 * "url" (<type></type>'str'>, None)  # url of the message, or None if not found
 * "repliable" (True, False, None)  # this message may be available for a reply
 * }`
 *
 * @author Denis Bondarenko
 * Date: 13.07.2017
 * Time: 15:16
 * E-mail: DenBond7@gmail.com
 */
@Parcelize
data class LinkMessageResponse constructor(
  @SerializedName("error") @Expose override val apiError: ApiError?,
  @Expose val url: String?,
  @Expose val isDeleted: Boolean,
  @Expose val expire: String?,
  @Expose val isExpired: Boolean,
  @Expose val repliable: Boolean?
) : ApiResponse
