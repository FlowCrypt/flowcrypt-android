/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * The request model for the https://flowcrypt.com/attester/test/welcome API.
 *
 * @author Denis Bondarenko
 * Date: 12.07.2017
 * Time: 16:40
 * E-mail: DenBond7@gmail.com
 */
data class WelcomeMessageModel(
  @SerializedName("email") @Expose val email: String,
  @SerializedName("pubkey") @Expose val pubKey: String
) : RequestModel
