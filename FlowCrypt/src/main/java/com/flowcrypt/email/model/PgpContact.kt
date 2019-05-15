/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import android.os.Parcel
import android.os.Parcelable

data class PgpContact constructor(var email: String,
                                  var name: String? = null,
                                  var pubkey: String? = null,
                                  var hasPgp: Boolean = false,
                                  var client: String? = null,
                                  var fingerprint: String? = null,
                                  var longid: String? = null,
                                  var keywords: String? = null,
                                  var lastUse: Int = 0) : Parcelable {

  constructor(source: Parcel) : this(
      source.readString()!!,
      source.readString(),
      source.readString(),
      source.readInt() == 1,
      source.readString(),
      source.readString(),
      source.readString(),
      source.readString(),
      source.readInt()
  )

  constructor(email: String, name: String) : this(email) {
    this.name = name
  }

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeString(email)
        writeString(name)
        writeString(pubkey)
        writeInt((if (hasPgp) 1 else 0))
        writeString(client)
        writeString(fingerprint)
        writeString(longid)
        writeString(keywords)
        writeInt(lastUse)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<PgpContact> = object : Parcelable.Creator<PgpContact> {
      override fun createFromParcel(source: Parcel): PgpContact = PgpContact(source)
      override fun newArray(size: Int): Array<PgpContact?> = arrayOfNulls(size)
    }
  }
}
