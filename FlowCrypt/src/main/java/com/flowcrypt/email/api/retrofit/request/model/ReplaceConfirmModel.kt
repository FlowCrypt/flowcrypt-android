/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * The request model for the https://flowcrypt.com/attester/replace/confirm API.
 *
 * @author Denis Bondarenko
 * Date: 13.07.2017
 * Time: 11:50
 * E-mail: DenBond7@gmail.com
 */

data class ReplaceConfirmModel(@SerializedName("signed_message") @Expose val signedMsg: String) : RequestModel
