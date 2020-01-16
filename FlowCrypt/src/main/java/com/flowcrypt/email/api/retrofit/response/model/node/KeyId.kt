/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * @author Denis Bondarenko
 * Date: 2/11/19
 * Time: 1:38 PM
 * E-mail: DenBond7@gmail.com
 */
data class KeyId constructor(@Expose val fingerprint: String?,
                             @SerializedName("longid") @Expose val longId: String?,
                             @SerializedName("shortid") @Expose val shortId: String?,
                             @Expose val keywords: String?) : Parcelable {
  constructor(source: Parcel) : this(
      source.readString(),
      source.readString(),
      source.readString(),
      source.readString()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeString(fingerprint)
        writeString(longId)
        writeString(shortId)
        writeString(keywords)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<KeyId> = object : Parcelable.Creator<KeyId> {
      override fun createFromParcel(source: Parcel): KeyId = KeyId(source)
      override fun newArray(size: Int): Array<KeyId?> = arrayOfNulls(size)
    }
  }
}
