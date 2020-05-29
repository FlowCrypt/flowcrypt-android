/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model

import android.text.TextUtils

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * This is a POJO object which used to make a request to the API "https://flowcrypt.com/attester/lookup/email"
 *
 * @author DenBond7
 * Date: 24.04.2017
 * Time: 13:27
 * E-mail: DenBond7@gmail.com
 */
data class PostLookUpEmailModel(@SerializedName("email") @Expose var email: String) : RequestModel {
  init {
    if (!TextUtils.isEmpty(email)) {
      this.email = email.toLowerCase(Locale.getDefault())
    }
  }
}
