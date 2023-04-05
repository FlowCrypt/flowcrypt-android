/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.api

import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.google.gson.annotations.Expose
import kotlinx.parcelize.Parcelize

/**
 * @author Denys Bondarenko
 */
@Parcelize
data class FesServerResponse constructor(
  @Expose override val code: Int? = null,
  @Expose override val message: String? = null,
  @Expose override val details: String? = null,
  @Expose val vendor: String? = null,
  @Expose val service: String? = null,
  @Expose val orgId: String? = null,
  @Expose val version: String? = null,
  @Expose val endUserApiVersion: String? = null,
  @Expose val adminApiVersion: String? = null,
) : ApiResponse
