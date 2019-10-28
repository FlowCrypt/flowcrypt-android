/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
 * This class describes a response from the https://flowcrypt.com/api/account/login API.
 *
 * @author Denis Bondarenko
 *         Date: 10/23/19
 *         Time: 3:56 PM
 *         E-mail: DenBond7@gmail.com
 */
data class LoginResponse constructor(@SerializedName("error")
                                     @Expose override val apiError: ApiError?,
                                     @SerializedName("registered")
                                     @Expose val isRegistered: Boolean,
                                     @SerializedName("verified")
                                     @Expose val isVerified: Boolean) : ApiResponse {
  constructor(parcel: Parcel) : this(
      parcel.readParcelable(ApiError::class.java.classLoader),
      parcel.readByte() != 0.toByte(),
      parcel.readByte() != 0.toByte())

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    with(parcel) {
      writeParcelable(apiError, flags)
      writeInt((if (isRegistered) 1 else 0))
      writeInt((if (isVerified) 1 else 0))
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