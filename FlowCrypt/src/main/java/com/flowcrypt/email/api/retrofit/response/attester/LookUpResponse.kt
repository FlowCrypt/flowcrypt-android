/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.attester

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.flowcrypt.email.api.retrofit.response.model.LookUpPublicKeyInfo
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * Response from the API
 * "https://flowcrypt.com/attester/lookup/{id or email}"
 *
 * @author Denis Bondarenko
 * Date: 05.05.2018
 * Time: 14:01
 * E-mail: DenBond7@gmail.com
 */

data class LookUpResponse constructor(@SerializedName("error") @Expose override val apiError: ApiError?,
                                      @Expose val results: ArrayList<LookUpPublicKeyInfo?>?,
                                      @Expose val query: String?) : ApiResponse {
  constructor(source: Parcel) : this(
      source.readParcelable<ApiError>(ApiError::class.java.classLoader),
      source.createTypedArrayList(LookUpPublicKeyInfo.CREATOR),
      source.readString()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeParcelable(apiError, 0)
        writeTypedList(results)
        writeString(query)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<LookUpResponse> = object : Parcelable.Creator<LookUpResponse> {
      override fun createFromParcel(source: Parcel): LookUpResponse = LookUpResponse(source)
      override fun newArray(size: Int): Array<LookUpResponse?> = arrayOfNulls(size)
    }
  }
}
