/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.oauth2

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.google.gson.annotations.Expose

/**
 * @author Denis Bondarenko
 *         Date: 8/4/20
 *         Time: 2:39 PM
 *         E-mail: DenBond7@gmail.com
 */
data class MicrosoftAccountResponse constructor(
    @Expose val displayName: String? = null,
    @Expose val userPrincipalName: String? = null
) : ApiResponse {

  override val apiError: ApiError? = null

  constructor(parcel: Parcel) : this(
      parcel.readString(),
      parcel.readString())

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeString(displayName)
    parcel.writeString(userPrincipalName)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<MicrosoftAccountResponse> {
    override fun createFromParcel(parcel: Parcel): MicrosoftAccountResponse {
      return MicrosoftAccountResponse(parcel)
    }

    override fun newArray(size: Int): Array<MicrosoftAccountResponse?> {
      return arrayOfNulls(size)
    }
  }
}