/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.api

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.flowcrypt.email.api.retrofit.response.model.DomainRules
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * This class describes a response from the https://flowcrypt.com/api/account/get API.
 *
 * @author Denis Bondarenko
 *         Date: 10/29/19
 *         Time: 11:25 AM
 *         E-mail: DenBond7@gmail.com
 */
data class DomainRulesResponse constructor(@SerializedName("error")
                                      @Expose override val apiError: ApiError?,
                                           @SerializedName("domain_org_rules")
                                      @Expose val domainRules: DomainRules?) : ApiResponse {
  constructor(parcel: Parcel) : this(
      parcel.readParcelable(ApiError::class.java.classLoader),
      parcel.readParcelable<DomainRules>(DomainRules::class.java.classLoader))

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    with(parcel) {
      writeParcelable(apiError, flags)
      writeParcelable(domainRules, flags)
    }
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<DomainRulesResponse> {
    override fun createFromParcel(parcel: Parcel): DomainRulesResponse {
      return DomainRulesResponse(parcel)
    }

    override fun newArray(size: Int): Array<DomainRulesResponse?> {
      return arrayOfNulls(size)
    }
  }
}