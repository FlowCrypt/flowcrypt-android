/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.api

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.flowcrypt.email.extensions.android.os.readParcelableViaExt
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * @author Denis Bondarenko
 *         Date: 7/23/21
 *         Time: 8:52 AM
 *         E-mail: DenBond7@gmail.com
 */
data class FesServerResponse constructor(
  @SerializedName("error")
  @Expose override val apiError: ApiError? = null,
  @Expose val vendor: String?,
  @Expose val service: String?,
  @Expose val orgId: String?,
  @Expose val version: String?,
  @Expose val endUserApiVersion: String?,
  @Expose val adminApiVersion: String?,
) : ApiResponse {
  constructor(parcel: Parcel) : this(
    parcel.readParcelableViaExt(ApiError::class.java),
    parcel.readString(),
    parcel.readString(),
    parcel.readString(),
    parcel.readString(),
    parcel.readString(),
    parcel.readString()
  )

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeParcelable(apiError, flags)
    parcel.writeString(vendor)
    parcel.writeString(service)
    parcel.writeString(orgId)
    parcel.writeString(version)
    parcel.writeString(endUserApiVersion)
    parcel.writeString(adminApiVersion)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<FesServerResponse> {
    override fun createFromParcel(parcel: Parcel): FesServerResponse = FesServerResponse(parcel)
    override fun newArray(size: Int): Array<FesServerResponse?> = arrayOfNulls(size)
  }
}
