/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import android.os.Parcel
import android.os.Parcelable

/**
 * @author Denis Bondarenko
 *         Date: 8/8/22
 *         Time: 2:23 PM
 *         E-mail: DenBond7@gmail.com
 */
data class Screenshot(val byteArray: ByteArray) : Parcelable {
  constructor(parcel: Parcel) : this(parcel.createByteArray() ?: byteArrayOf())

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Screenshot

    if (!byteArray.contentEquals(other.byteArray)) return false

    return true
  }

  override fun hashCode(): Int {
    return byteArray.contentHashCode()
  }

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeByteArray(byteArray)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<Screenshot> {
    override fun createFromParcel(parcel: Parcel) = Screenshot(parcel)
    override fun newArray(size: Int): Array<Screenshot?> = arrayOfNulls(size)
  }
}
