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
 * This class describes a response from the https://flowcrypt.com/api/link/message API.
 *
 *
 * `POST /initial/confirm
 * response(200): {
 * "url" (<type></type>'str'>, None)  # url of the message, or None if not found
 * "repliable" (True, False, None)  # this message may be available for a reply
 * }`
 *
 * @author Denis Bondarenko
 * Date: 13.07.2017
 * Time: 15:16
 * E-mail: DenBond7@gmail.com
 */
data class LinkMessageResponse constructor(
  @SerializedName("error") @Expose override val apiError: ApiError?,
  @Expose val url: String?,
  @Expose val isDeleted: Boolean,
  @Expose val expire: String?,
  @Expose val isExpired: Boolean,
  @Expose val repliable: Boolean?
) : ApiResponse {
  constructor(source: Parcel) : this(
    source.readParcelableViaExt<ApiError>(ApiError::class.java),
    source.readString(),
    1 == source.readInt(),
    source.readString(),
    1 == source.readInt(),
    source.readValue(Boolean::class.java.classLoader) as Boolean?
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
    with(dest) {
      writeParcelable(apiError, 0)
      writeString(url)
      writeInt((if (isDeleted) 1 else 0))
      writeString(expire)
      writeInt((if (isExpired) 1 else 0))
      writeValue(repliable)
    }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<LinkMessageResponse> =
      object : Parcelable.Creator<LinkMessageResponse> {
        override fun createFromParcel(source: Parcel): LinkMessageResponse =
          LinkMessageResponse(source)

        override fun newArray(size: Int): Array<LinkMessageResponse?> = arrayOfNulls(size)
      }
  }
}
