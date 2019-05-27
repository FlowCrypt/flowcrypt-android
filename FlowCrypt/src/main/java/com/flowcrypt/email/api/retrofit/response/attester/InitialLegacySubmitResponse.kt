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
 * This class describes a response from the https://flowcrypt.com/attester/initial/legacy_submit API.
 *
 *
 * `POST /initial/legacy_submit
 * response(200): {
 * "saved" (True, False)  # successfuly saved pubkey
 * [voluntary] "error" (<type></type>'str'>)  # error detail, if not saved
 * }`
 *
 * @author Denis Bondarenko
 * Date: 15.01.2018
 * Time: 16:30
 * E-mail: DenBond7@gmail.com
 */

class InitialLegacySubmitResponse constructor(@SerializedName("error") @Expose override val apiError: ApiError?,
                                              @Expose val isSaved: Boolean) : ApiResponse {
  constructor(source: Parcel) : this(
      source.readParcelable<ApiError>(ApiError::class.java.classLoader),
      1 == source.readInt()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeParcelable(apiError, 0)
        writeInt((if (isSaved) 1 else 0))
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<InitialLegacySubmitResponse> = object : Parcelable.Creator<InitialLegacySubmitResponse> {
      override fun createFromParcel(source: Parcel): InitialLegacySubmitResponse = InitialLegacySubmitResponse(source)
      override fun newArray(size: Int): Array<InitialLegacySubmitResponse?> = arrayOfNulls(size)
    }
  }
}
