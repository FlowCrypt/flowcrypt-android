/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.api

import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.flowcrypt.email.api.retrofit.response.model.Key
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.google.gson.annotations.Expose
import kotlinx.parcelize.Parcelize

/**
 * @author Denys Bondarenko
 */
@Parcelize
data class EkmPrivateKeysResponse constructor(
  @Expose override val code: Int? = null,
  @Expose override val message: String? = null,
  @Expose override val details: String? = null,
  @Expose val privateKeys: List<Key>? = null,
  val pgpKeyDetailsList: List<PgpKeyDetails>? = null
) : ApiResponse
