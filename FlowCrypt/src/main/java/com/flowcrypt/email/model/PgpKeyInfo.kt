/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import android.os.Parcel
import android.os.Parcelable

data class PgpKeyInfo constructor(val longid: String,
                                  val private: String?,
                                  val pubKey: String) : Parcelable {
  constructor(source: Parcel) : this(
      source.readString()!!,
      source.readString(),
      source.readString()!!
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeString(longid)
        writeString(private)
        writeString(pubKey)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<PgpKeyInfo> = object : Parcelable.Creator<PgpKeyInfo> {
      override fun createFromParcel(source: Parcel): PgpKeyInfo = PgpKeyInfo(source)
      override fun newArray(size: Int): Array<PgpKeyInfo?> = arrayOfNulls(size)
    }
  }
}
