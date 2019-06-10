/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * @author DenBond7
 */

data class Error constructor(@SerializedName("message") @Expose val msg: String?,
                             @Expose val stack: String?,
                             @Expose val type: String?) : Parcelable {
  constructor(source: Parcel) : this(
      source.readString(),
      source.readString(),
      source.readString()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeString(msg)
        writeString(stack)
        writeString(type)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<Error> = object : Parcelable.Creator<Error> {
      override fun createFromParcel(source: Parcel): Error = Error(source)
      override fun newArray(size: Int): Array<Error?> = arrayOfNulls(size)
    }
  }
}
