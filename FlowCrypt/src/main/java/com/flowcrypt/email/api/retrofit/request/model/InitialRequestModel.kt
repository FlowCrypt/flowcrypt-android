/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * The request model for the https://flowcrypt.com/attester/initial/request API.
 *
 * @author Denis Bondarenko
 * Date: 12.07.2017
 * Time: 16:48
 * E-mail: DenBond7@gmail.com
 */

data class InitialRequestModel(@SerializedName("email") @Expose val email: String,
                               @SerializedName("pubkey") @Expose val pubKey: String,
                               @SerializedName("attest") @Expose val isAttest: Boolean = false) : RequestModel