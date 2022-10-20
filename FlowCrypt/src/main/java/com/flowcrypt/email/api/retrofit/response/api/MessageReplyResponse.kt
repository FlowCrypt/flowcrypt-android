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
 * This class describes a response from the https://flowcrypt.com/api/message/reply API.
 *
 *
 * `POST /message/reply
 * response(200): {
 * "sent" (True, False)  # successfully sent message
 * [voluntary] "error" (<type></type>'str'>)  # Encountered error if any
 * }`
 *
 * @author Denis Bondarenko
 * Date: 13.07.2017
 * Time: 16:33
 * E-mail: DenBond7@gmail.com
 */

data class MessageReplyResponse constructor(
  @SerializedName("error") @Expose override val apiError: ApiError?,
  @Expose val isSent: Boolean
) : ApiResponse {
  constructor(source: Parcel) : this(
    source.readParcelableViaExt(ApiError::class.java),
    1 == source.readInt()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
    with(dest) {
      writeParcelable(apiError, 0)
      writeInt((if (isSent) 1 else 0))
    }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<MessageReplyResponse> =
      object : Parcelable.Creator<MessageReplyResponse> {
        override fun createFromParcel(source: Parcel): MessageReplyResponse =
          MessageReplyResponse(source)

        override fun newArray(size: Int): Array<MessageReplyResponse?> = arrayOfNulls(size)
      }
  }
}
