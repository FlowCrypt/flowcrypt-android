/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model.node

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * @author DenBond7
 */
data class PrivateKeyInfo(
  @SerializedName("private") @Expose val privateKey: String,
  @Expose val longid: String,
  @Expose val passphrase: String?
)
