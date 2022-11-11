/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.api

import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.flowcrypt.email.api.retrofit.response.model.Key
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.google.gson.annotations.Expose
import kotlinx.parcelize.Parcelize

/**
 * @author Denis Bondarenko
 *         Date: 6/23/21
 *         Time: 9:50 AM
 *         E-mail: DenBond7@gmail.com
 */
@Parcelize
data class EkmPrivateKeysResponse constructor(
  @Expose val code: Int? = null,
  @Expose val message: String? = null,
  @Expose val privateKeys: List<Key>? = null,
  val pgpKeyDetailsList: List<PgpKeyDetails>? = null
) : ApiResponse {

  override val apiError: ApiError?
    get() = if (code != null) {
      ApiError(code = code, msg = message)
    } else null
}
