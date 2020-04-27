/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.database.entity.ContactEntity
import java.util.*

data class PgpContact constructor(var email: String,
                                  var name: String? = null,
                                  var pubkey: String? = null,
                                  var hasPgp: Boolean = false,
                                  var client: String? = null,
                                  var fingerprint: String? = null,
                                  var longid: String? = null,
                                  var keywords: String? = null,
                                  var lastUse: Long = 0) : Parcelable {

  constructor(source: Parcel) : this(
      source.readString()!!,
      source.readString(),
      source.readString(),
      source.readInt() == 1,
      source.readString(),
      source.readString(),
      source.readString(),
      source.readString(),
      source.readLong()
  )

  constructor(email: String, name: String?) : this(email) {
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
        writeLong(lastUse)
      }

  fun toContactEntity(): ContactEntity {
    return ContactEntity(
        email = email.toLowerCase(Locale.getDefault()),
        name = name,
        publicKey = pubkey?.toByteArray(),
        hasPgp = hasPgp,
        client = client,
        fingerprint = fingerprint,
        longId = longid,
        keywords = keywords,
        lastUse = lastUse,
        attested = false
    )
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<PgpContact> = object : Parcelable.Creator<PgpContact> {
      override fun createFromParcel(source: Parcel): PgpContact = PgpContact(source)
      override fun newArray(size: Int): Array<PgpContact?> = arrayOfNulls(size)
    }
  }
}
