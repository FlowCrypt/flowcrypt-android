/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * The request model for the https://flowcrypt.com/attester/replace/request API.
 *
 * @author Denys Bondarenko
 */
data class ReplaceRequestModel(
  @SerializedName("signed_message") @Expose val signedMsg: String,
  @SerializedName("new_pubkey") @Expose val newPubKey: String,
  @SerializedName("email") @Expose val email: String
) : RequestModel
