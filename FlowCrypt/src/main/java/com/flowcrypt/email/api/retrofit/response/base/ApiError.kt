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
* @author Denys Bondarenko
 */
@Parcelize
data class ApiError constructor(
  @Expose val code: Int? = null,
  @Expose val message: String? = null,
  @SerializedName("details", alternate = ["internal"])
  @Expose val details: String? = null,
) : Parcelable
