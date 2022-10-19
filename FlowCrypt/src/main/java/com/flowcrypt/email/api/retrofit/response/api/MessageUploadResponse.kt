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
 *         Date: 12/20/21
 *         Time: 12:34 PM
 *         E-mail: DenBond7@gmail.com
 */
data class MessageUploadResponse(
  @SerializedName("error")
  @Expose override val apiError: ApiError? = null,
  @Expose val url: String? = null
) : ApiResponse {
  constructor(parcel: Parcel) : this(
    parcel.readParcelableViaExt(ApiError::class.java),
    parcel.readString()
  )

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeParcelable(apiError, flags)
    parcel.writeString(url)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<MessageUploadResponse> {
    override fun createFromParcel(parcel: Parcel) = MessageUploadResponse(parcel)
    override fun newArray(size: Int): Array<MessageUploadResponse?> = arrayOfNulls(size)
  }
}
