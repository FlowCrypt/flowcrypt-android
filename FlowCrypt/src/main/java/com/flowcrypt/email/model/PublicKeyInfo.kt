/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.database.entity.RecipientEntity

/**
 * This class describes information about some public key.
 *
 * @author Denis Bondarenko
 * Date: 13.05.2018
 * Time: 10:22
 * E-mail: DenBond7@gmail.com
 */
data class PublicKeyInfo constructor(
  val fingerprint: String,
  val keyOwner: String,
  var pgpContact: PgpContact? = null,
  val publicKey: String
) : Parcelable {
  val isUpdateEnabled: Boolean
    get() = pgpContact != null && (pgpContact!!.fingerprint == null || pgpContact!!.fingerprint != fingerprint)

  fun hasPgpContact(): Boolean {
    return pgpContact != null
  }

  constructor(source: Parcel) : this(
    source.readString()!!,
    source.readString()!!,
    source.readParcelable<PgpContact>(PgpContact::class.java.classLoader),
    source.readString()!!
  )

  override fun describeContents() = 0

  override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
    writeString(fingerprint)
    writeString(keyOwner)
    writeParcelable(pgpContact, flags)
    writeString(publicKey)
  }

  fun toRecipientEntity(): RecipientEntity {
    return RecipientEntity(
      email = keyOwner.lowercase()
    )
  }

  fun toPgpContact(): PgpContact {
    return PgpContact(
      email = keyOwner,
      name = null,
      pubkey = publicKey,
      hasPgp = true,
      client = null,
      fingerprint = fingerprint,
      lastUse = 0
    )
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<PublicKeyInfo> = object : Parcelable.Creator<PublicKeyInfo> {
      override fun createFromParcel(source: Parcel): PublicKeyInfo = PublicKeyInfo(source)
      override fun newArray(size: Int): Array<PublicKeyInfo?> = arrayOfNulls(size)
    }
  }
}
