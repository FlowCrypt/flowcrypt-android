/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *  DenBond7
 *  Ivan Pizhenko
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
 * This class describes a response from the https://flowcrypt.com/api/account/login API.
 *
 * @author Denis Bondarenko
 *         Date: 10/23/19
 *         Time: 3:56 PM
 *         E-mail: DenBond7@gmail.com
 */
data class LoginResponse constructor(
  @SerializedName("error")
  @Expose override val apiError: ApiError?,
  @SerializedName("verified")
  @Expose val isVerified: Boolean?
) : ApiResponse {
  constructor(parcel: Parcel) : this(
    parcel.readParcelableViaExt(ApiError::class.java),
    parcel.readValue(Boolean::class.java.classLoader) as Boolean?
  )

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    with(parcel) {
      writeParcelable(apiError, flags)
      writeValue(isVerified)
    }
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<LoginResponse> {
    override fun createFromParcel(parcel: Parcel): LoginResponse {
      return LoginResponse(parcel)
    }

    override fun newArray(size: Int): Array<LoginResponse?> {
      return arrayOfNulls(size)
    }
  }
}
