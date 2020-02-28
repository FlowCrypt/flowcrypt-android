/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose

/**
 * @author Denis Bondarenko
 *         Date: 7/12/19
 *         Time: 10:16 AM
 *         E-mail: DenBond7@gmail.com
 */
data class AttMeta(@Expose val name: String?,
                   @Expose var data: String?,
                   @Expose val length: Long,
                   @Expose val type: String?) : Parcelable {

  constructor(source: Parcel) : this(
      source.readString(),
      source.readString(),
      source.readLong(),
      source.readString()
  )

  override fun describeContents() = 0

  override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
    writeString(name)
    writeString(data)
    writeLong(length)
    writeString(type)
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<AttMeta> = object : Parcelable.Creator<AttMeta> {
      override fun createFromParcel(source: Parcel): AttMeta = AttMeta(source)
      override fun newArray(size: Int): Array<AttMeta?> = arrayOfNulls(size)
    }
  }
}