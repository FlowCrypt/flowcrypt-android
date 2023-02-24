/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * The request model for the https://flowcrypt.com/attester/initial/confirm API.
 *
 * @author Denys Bondarenko
 */
data class InitialConfirmModel(@SerializedName("signed_message") @Expose val signedMsg: String) :
  RequestModel
