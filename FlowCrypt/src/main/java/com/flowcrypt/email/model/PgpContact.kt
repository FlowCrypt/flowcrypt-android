/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.database.entity.ContactEntity
import com.flowcrypt.email.security.model.PgpKeyDetails
import java.util.ArrayList
import java.util.Locale
import javax.mail.internet.AddressException
import javax.mail.internet.InternetAddress

data class PgpContact constructor(
  var email: String,
  var name: String? = null,
  var pubkey: String? = null,
  var hasPgp: Boolean = false,
  var client: String? = null,
  var fingerprint: String? = null,
  var lastUse: Long = 0,
  var pgpKeyDetails: PgpKeyDetails? = null
) : Parcelable {

  constructor(source: Parcel) : this(
    source.readString()!!,
    source.readString(),
    source.readString(),
    source.readInt() == 1,
    source.readString(),
    source.readString(),
    source.readLong(),
    source.readParcelable(PgpKeyDetails::class.java.classLoader)
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
      writeLong(lastUse)
      writeParcelable(pgpKeyDetails, flags)
    }

  fun toContactEntity(): ContactEntity {
    return ContactEntity(
      email = email.toLowerCase(Locale.getDefault()),
      name = name,
      publicKey = pubkey?.toByteArray(),
      hasPgp = hasPgp,
      client = client,
      fingerprint = fingerprint,
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

    fun determinePgpContacts(users: List<String>): ArrayList<PgpContact> {
      val pgpContacts = ArrayList<PgpContact>()
      for (user in users) {
        try {
          val internetAddresses = InternetAddress.parse(user)

          for (internetAddress in internetAddresses) {
            val email = internetAddress.address.toLowerCase(Locale.US)
            val name = internetAddress.personal

            pgpContacts.add(PgpContact(email, name))
          }
        } catch (e: AddressException) {
          e.printStackTrace()
        }
      }

      return pgpContacts
    }
  }
}
