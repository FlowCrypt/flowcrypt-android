/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * The request model for the https://flowcrypt.com/api/link/message API.
 *
 * @author Denis Bondarenko
 * Date: 13.07.2017
 * Time: 15:12
 * E-mail: DenBond7@gmail.com
 */
data class LinkMessageModel(@SerializedName("short") @Expose val shortValue: String) : RequestModel
