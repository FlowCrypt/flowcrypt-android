/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.api

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * @author Denis Bondarenko
 *         Date: 6/23/21
 *         Time: 9:50 AM
 *         E-mail: DenBond7@gmail.com
 */
class EkmPrivateKeysResponse constructor(
  @SerializedName("error")
  @Expose override val apiError: ApiError?
) : ApiResponse {
  constructor(parcel: Parcel) : this(
    parcel.readParcelable(ApiError::class.java.classLoader)
  )

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    with(parcel) {
      writeParcelable(apiError, flags)
    }
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<EkmPrivateKeysResponse> {
    override fun createFromParcel(parcel: Parcel): EkmPrivateKeysResponse {
      return EkmPrivateKeysResponse(parcel)
    }

    override fun newArray(size: Int): Array<EkmPrivateKeysResponse?> {
      return arrayOfNulls(size)
    }
  }
}
