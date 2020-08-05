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
import com.google.gson.annotations.SerializedName

/**
 * @author Denis Bondarenko
 *         Date: 7/15/20
 *         Time: 2:52 PM
 *         E-mail: DenBond7@gmail.com
 */
data class MicrosoftOAuth2TokenResponse constructor(
    @SerializedName("access_token") @Expose val accessToken: String? = null,
    @SerializedName("token_type") @Expose val tokenType: String? = null,
    @SerializedName("expires_in") @Expose val expiresIn: Long? = null,
    @Expose val scope: String? = null,
    @SerializedName("refresh_token") @Expose val refreshToken: String? = null,
    @SerializedName("id_token") @Expose val idToken: String? = null
) : ApiResponse {
  constructor(parcel: Parcel) : this(
      parcel.readString(),
      parcel.readString(),
      parcel.readValue(Long::class.java.classLoader) as? Long,
      parcel.readString(),
      parcel.readString(),
      parcel.readString())

  override val apiError: ApiError? = null

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeString(accessToken)
    parcel.writeString(tokenType)
    parcel.writeValue(expiresIn)
    parcel.writeString(scope)
    parcel.writeString(refreshToken)
    parcel.writeString(idToken)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<MicrosoftOAuth2TokenResponse> {
    override fun createFromParcel(parcel: Parcel): MicrosoftOAuth2TokenResponse {
      return MicrosoftOAuth2TokenResponse(parcel)
    }

    override fun newArray(size: Int): Array<MicrosoftOAuth2TokenResponse?> {
      return arrayOfNulls(size)
    }
  }
}
