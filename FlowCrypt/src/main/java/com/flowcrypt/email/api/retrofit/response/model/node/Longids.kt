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
 * Date: 1/18/19
 * Time: 10:05 AM
 * E-mail: DenBond7@gmail.com
 */
data class Longids constructor(@Expose val message: List<String>?,
                               @Expose val matching: List<String>?,
                               @Expose val chosen: List<String>?,
                               @Expose val needPassphrase: List<String>?) : Parcelable {

  constructor(source: Parcel) : this(
      source.createStringArrayList(),
      source.createStringArrayList(),
      source.createStringArrayList(),
      source.createStringArrayList()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeStringList(message)
        writeStringList(matching)
        writeStringList(chosen)
        writeStringList(needPassphrase)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<Longids> = object : Parcelable.Creator<Longids> {
      override fun createFromParcel(source: Parcel): Longids = Longids(source)
      override fun newArray(size: Int): Array<Longids?> = arrayOfNulls(size)
    }
  }
}
