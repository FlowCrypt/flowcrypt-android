/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.annotations.Expose

/**
 * @author Denis Bondarenko
 * Date: 2/11/19
 * Time: 1:42 PM
 * E-mail: DenBond7@gmail.com
 */
data class Algo constructor(@Expose val algorithm: String?,
                            @Expose val algorithmId: Int,
                            @Expose val bits: Int,
                            @Expose val curve: String?) : Parcelable {
  constructor(source: Parcel) : this(
      source.readString(),
      source.readInt(),
      source.readInt(),
      source.readString()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeString(algorithm)
        writeInt(algorithmId)
        writeInt(bits)
        writeString(curve)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<Algo> = object : Parcelable.Creator<Algo> {
      override fun createFromParcel(source: Parcel): Algo = Algo(source)
      override fun newArray(size: Int): Array<Algo?> = arrayOfNulls(size)
    }
  }
}
