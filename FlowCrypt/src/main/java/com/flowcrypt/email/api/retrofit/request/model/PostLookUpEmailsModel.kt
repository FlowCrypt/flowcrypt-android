/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * This is a POJO object which used to make a request to the API "https://flowcrypt.com/attester/lookup/email"
 * to retrieve info about an array of keys.
 *
 * @author Denis Bondarenko
 * Date: 13.11.2017
 * Time: 15:16
 * E-mail: DenBond7@gmail.com
 */

data class PostLookUpEmailsModel(@SerializedName("email") @Expose val emails: List<String>) : RequestModel
