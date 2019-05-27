/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.attester

import android.os.Parcel
import android.os.Parcelable

import com.flowcrypt.email.api.retrofit.request.model.PostLookUpEmailsModel
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Response from the API
 * "https://flowcrypt.com/attester/lookup/email" for [PostLookUpEmailsModel]
 *
 * @author Denis Bondarenko
 * Date: 13.11.2017
 * Time: 15:15
 * E-mail: DenBond7@gmail.com
 */

data class LookUpEmailsResponse constructor(@SerializedName("error") @Expose override val apiError: ApiError?,
                                            @Expose val results: List<LookUpEmailResponse>?) : ApiResponse {
  constructor(source: Parcel) : this(
      source.readParcelable<ApiError>(ApiError::class.java.classLoader),
      source.createTypedArrayList(LookUpEmailResponse.CREATOR)
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeParcelable(apiError, 0)
        writeTypedList(results)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<LookUpEmailsResponse> = object : Parcelable.Creator<LookUpEmailsResponse> {
      override fun createFromParcel(source: Parcel): LookUpEmailsResponse = LookUpEmailsResponse(source)
      override fun newArray(size: Int): Array<LookUpEmailsResponse?> = arrayOfNulls(size)
    }
  }
}
