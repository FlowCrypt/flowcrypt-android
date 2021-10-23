/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.entity

import android.os.Parcel
import android.os.Parcelable
import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * @author Denis Bondarenko
 *         Date: 10/20/21
 *         Time: 11:00 AM
 *         E-mail: DenBond7@gmail.com
 */
@Entity(
  tableName = "public_keys",
  indices = [
    Index(
      name = "recipient_fingerprint_in_public_keys",
      value = ["recipient", "fingerprint"],
      unique = true
    ),
    Index(
      name = "fingerprint_in_public_keys",
      value = ["fingerprint"],
      unique = false
    )
  ],
  foreignKeys = [
    ForeignKey(
      entity = ContactEntity::class,
      parentColumns = ["email"],
      childColumns = ["recipient"],
      onDelete = ForeignKey.CASCADE
    )
  ]
)
class PublicKeyEntity(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) val id: Long? = null,
  @ColumnInfo(name = "recipient") val recipient: String,
  @ColumnInfo(name = "fingerprint") val fingerprint: String,
  @ColumnInfo(name = "public_key") val publicKey: ByteArray
) : Parcelable {
  constructor(parcel: Parcel) : this(
    parcel.readValue(Long::class.java.classLoader) as? Long,
    requireNotNull(parcel.readString()),
    requireNotNull(parcel.readString()),
    requireNotNull(parcel.createByteArray())
  )


  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeValue(id)
    parcel.writeString(recipient)
    parcel.writeString(fingerprint)
    parcel.writeByteArray(publicKey)
  }

  override fun describeContents(): Int {
    return 0
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as PublicKeyEntity

    if (id != other.id) return false
    if (recipient != other.recipient) return false
    if (fingerprint != other.fingerprint) return false
    if (!publicKey.contentEquals(other.publicKey)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id?.hashCode() ?: 0
    result = 31 * result + recipient.hashCode()
    result = 31 * result + fingerprint.hashCode()
    result = 31 * result + publicKey.contentHashCode()
    return result
  }

  companion object CREATOR : Parcelable.Creator<PublicKeyEntity> {
    override fun createFromParcel(parcel: Parcel): PublicKeyEntity = PublicKeyEntity(parcel)
    override fun newArray(size: Int): Array<PublicKeyEntity?> = arrayOfNulls(size)
  }
}
