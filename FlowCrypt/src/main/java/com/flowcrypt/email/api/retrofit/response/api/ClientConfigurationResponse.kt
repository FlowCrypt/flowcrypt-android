/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.api

import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * @author Denis Bondarenko
 *         Date: 7/27/21
 *         Time: 4:16 PM
 *         E-mail: DenBond7@gmail.com
 */
@Parcelize
data class ClientConfigurationResponse constructor(
  @SerializedName("error")
  @Expose override val apiError: ApiError? = null,
  @SerializedName("clientConfiguration", alternate = ["domain_org_rules"])
  @Expose val clientConfiguration: ClientConfiguration?
) : ApiResponse
