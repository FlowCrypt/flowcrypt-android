/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.api

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.flowcrypt.email.api.retrofit.response.model.OrgRules
import com.flowcrypt.email.extensions.android.os.readParcelableViaExt
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * @author Denis Bondarenko
 *         Date: 7/27/21
 *         Time: 4:16 PM
 *         E-mail: DenBond7@gmail.com
 */
data class ClientConfigurationResponse constructor(
  @SerializedName("error")
  @Expose override val apiError: ApiError? = null,
  @SerializedName("clientConfiguration")
  @Expose val orgRules: OrgRules?
) : ApiResponse {
  constructor(parcel: Parcel) : this(
    parcel.readParcelableViaExt(ApiError::class.java),
    parcel.readParcelableViaExt<OrgRules>(OrgRules::class.java)
  )

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    with(parcel) {
      writeParcelable(apiError, flags)
      writeParcelable(orgRules, flags)
    }
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<ClientConfigurationResponse> {
    override fun createFromParcel(parcel: Parcel) = ClientConfigurationResponse(parcel)
    override fun newArray(size: Int): Array<ClientConfigurationResponse?> = arrayOfNulls(size)
  }
}
