/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
 * Response from the API
 * "https://flowcrypt.com/attester/lookup/email"
 *
 * @author DenBond7
 * Date: 24.04.2017
 * Time: 13:20
 * E-mail: DenBond7@gmail.com
 */

data class LookUpEmailResponse constructor(@SerializedName("error") @Expose override val apiError: ApiError?,
                                           @SerializedName("has_cryptup") @Expose val hasCryptup: Boolean,
                                           @SerializedName("pubkey") @Expose val pubKey: String?,
                                           @Expose val email: String?,
                                           @SerializedName("longid") @Expose val longId: String?) : ApiResponse {
  fun hasCryptup(): Boolean {
    return hasCryptup
  }

  constructor(source: Parcel) : this(
      source.readParcelable<ApiError>(ApiError::class.java.classLoader),
      1 == source.readInt(),
      source.readString(),
      source.readString(),
      source.readString()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeParcelable(apiError, 0)
        writeInt((if (hasCryptup) 1 else 0))
        writeString(pubKey)
        writeString(email)
        writeString(longId)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<LookUpEmailResponse> = object : Parcelable.Creator<LookUpEmailResponse> {
      override fun createFromParcel(source: Parcel): LookUpEmailResponse = LookUpEmailResponse(source)
      override fun newArray(size: Int): Array<LookUpEmailResponse?> = arrayOfNulls(size)
    }
  }
}
