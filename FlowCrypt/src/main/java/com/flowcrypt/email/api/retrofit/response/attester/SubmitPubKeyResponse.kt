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
 * This class describes a response from the https://flowcrypt.com/attester/pub API.
 *
 * @author Denis Bondarenko
 * Date: 15.01.2018
 * Time: 16:30
 * E-mail: DenBond7@gmail.com
 */
@Parcelize
data class SubmitPubKeyResponse constructor(
  @SerializedName("error") @Expose override val
  apiError: ApiError? = null
) : ApiResponse
