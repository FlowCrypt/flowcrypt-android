/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.attester

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.flowcrypt.email.extensions.android.os.readParcelableViaExt
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Response from the API
 * "https://flowcrypt.com/attester/pub/{id or email}"
 *
 * @author Denis Bondarenko
 * Date: 05.05.2018
 * Time: 14:01
 * E-mail: DenBond7@gmail.com
 */
data class PubResponse constructor(
  @SerializedName("error")
  @Expose override val apiError: ApiError? = null,
  @Expose val pubkey: String?
) : ApiResponse {
  constructor(source: Parcel) : this(
    source.readParcelableViaExt<ApiError>(ApiError::class.java),
    source.readString()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
    with(dest) {
      writeParcelable(apiError, 0)
      writeString(pubkey)
    }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<PubResponse> = object : Parcelable.Creator<PubResponse> {
      override fun createFromParcel(source: Parcel): PubResponse = PubResponse(source)
      override fun newArray(size: Int): Array<PubResponse?> = arrayOfNulls(size)
    }
  }
}
