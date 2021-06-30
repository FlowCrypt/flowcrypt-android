/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose

/**
 * @author Denis Bondarenko
 *         Date: 6/30/21
 *         Time: 2:48 PM
 *         E-mail: DenBond7@gmail.com
 */
data class Key constructor(@Expose val decryptedPrivateKey: String? = null) : Parcelable {
  constructor(parcel: Parcel) : this(parcel.readString())

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeString(decryptedPrivateKey)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<Key> {
    override fun createFromParcel(parcel: Parcel): Key {
      return Key(parcel)
    }

    override fun newArray(size: Int): Array<Key?> {
      return arrayOfNulls(size)
    }
  }
}
