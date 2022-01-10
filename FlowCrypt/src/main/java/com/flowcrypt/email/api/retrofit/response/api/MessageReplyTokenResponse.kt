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
 *         Date: 12/20/21
 *         Time: 12:10 PM
 *         E-mail: DenBond7@gmail.com
 */
data class MessageReplyTokenResponse(
  @SerializedName("error")
  @Expose override val apiError: ApiError? = null,
  @Expose val replyToken: String? = null
) : ApiResponse {
  constructor(parcel: Parcel) : this(
    parcel.readParcelable(ApiError::class.java.classLoader),
    parcel.readString()
  )

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeParcelable(apiError, flags)
    parcel.writeString(replyToken)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<MessageReplyTokenResponse> {
    override fun createFromParcel(parcel: Parcel) = MessageReplyTokenResponse(parcel)
    override fun newArray(size: Int): Array<MessageReplyTokenResponse?> = arrayOfNulls(size)
  }
}
