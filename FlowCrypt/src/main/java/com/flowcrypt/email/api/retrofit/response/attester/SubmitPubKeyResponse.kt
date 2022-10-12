/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.attester

import android.os.Parcel
import android.os.Parcelable

import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * This class describes a response from the https://flowcrypt.com/attester/pub API.
 *
 * @author Denis Bondarenko
 * Date: 15.01.2018
 * Time: 16:30
 * E-mail: DenBond7@gmail.com
 */
data class SubmitPubKeyResponse constructor(
  @SerializedName("error") @Expose override val
  apiError: ApiError? = null
) : ApiResponse {
  constructor(source: Parcel) : this(
    source.readParcelable<ApiError>(ApiError::class.java.classLoader)
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
    with(dest) {
      writeParcelable(apiError, 0)
    }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<SubmitPubKeyResponse> =
      object : Parcelable.Creator<SubmitPubKeyResponse> {
        override fun createFromParcel(source: Parcel) = SubmitPubKeyResponse(source)
        override fun newArray(size: Int): Array<SubmitPubKeyResponse?> = arrayOfNulls(size)
      }
  }
}
