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
 * This class describes a response from the https://flowcrypt.com/api/account/get API.
 *
 * @author Denis Bondarenko
 *         Date: 10/29/19
 *         Time: 11:25 AM
 *         E-mail: DenBond7@gmail.com
 */
@Parcelize
data class DomainOrgRulesResponse constructor(
  @SerializedName("error")
  @Expose override val apiError: ApiError? = null,
  @SerializedName("domain_org_rules")
  @Expose val clientConfiguration: ClientConfiguration?
) : ApiResponse
