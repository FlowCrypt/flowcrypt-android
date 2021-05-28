/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.os.Parcel
import android.os.Parcelable

/**
 * @author Denis Bondarenko
 *         Date: 8/13/20
 *         Time: 4:57 PM
 *         E-mail: DenBond7@gmail.com
 */
data class AuthTokenInfo constructor(
  val email: String?,
  val accessToken: String? = null,
  val expiresAt: Long? = null,
  val refreshToken: String? = null
) : Parcelable {
  constructor(parcel: Parcel) : this(
    parcel.readString(),
    parcel.readString(),
    parcel.readValue(Long::class.java.classLoader) as? Long,
    parcel.readString()
  )

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeString(email)
    parcel.writeString(accessToken)
    parcel.writeValue(expiresAt)
    parcel.writeString(refreshToken)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<AuthTokenInfo> {
    override fun createFromParcel(parcel: Parcel): AuthTokenInfo {
      return AuthTokenInfo(parcel)
    }

    override fun newArray(size: Int): Array<AuthTokenInfo?> {
      return arrayOfNulls(size)
    }
  }
}