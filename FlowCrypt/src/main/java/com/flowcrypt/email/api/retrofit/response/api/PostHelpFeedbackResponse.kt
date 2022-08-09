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
 * The simple POJO object, which contains information about a post feedback result.
 *
 *
 * This class describes the next response:
 *
 *
 * <pre>
 * `POST
 * response(200): {
 * "sent" (True, False)  # True if message was sent successfully
 * "text" (<type></type>'str'>)  # User friendly success or error text
 * }
` *
</pre> *
 *
 * @author DenBond7
 * Date: 30.05.2017
 * Time: 12:34
 * E-mail: DenBond7@gmail.com
 */
data class PostHelpFeedbackResponse constructor(
  @SerializedName("error") @Expose override val apiError: ApiError? = null,
  @SerializedName("sent") @Expose val isSent: Boolean? = null,
  @Expose val text: String? = null
) : ApiResponse {
  constructor(source: Parcel) : this(
    source.readParcelable<ApiError>(ApiError::class.java.classLoader),
    1 == source.readInt(),
    source.readString()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
    with(dest) {
      writeParcelable(apiError, 0)
      writeInt((if (isSent == true) 1 else 0))
      writeString(text)
    }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<PostHelpFeedbackResponse> =
      object : Parcelable.Creator<PostHelpFeedbackResponse> {
        override fun createFromParcel(source: Parcel): PostHelpFeedbackResponse =
          PostHelpFeedbackResponse(source)

        override fun newArray(size: Int): Array<PostHelpFeedbackResponse?> = arrayOfNulls(size)
      }
  }
}
