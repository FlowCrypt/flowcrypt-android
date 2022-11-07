/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.base

import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * This POJO object describes a base error from the API.
 *
 * @author Denis Bondarenko
 * Date: 12.07.2017
 * Time: 9:26
 * E-mail: DenBond7@gmail.com
 */
@Parcelize
data class ApiError constructor(
  @Expose val code: Int? = null,
  @SerializedName("message") @Expose val msg: String? = null,
  @Expose val internal: String? = null,
  @Expose val stack: String? = null,
  @Expose val type: String? = null
) : Parcelable
