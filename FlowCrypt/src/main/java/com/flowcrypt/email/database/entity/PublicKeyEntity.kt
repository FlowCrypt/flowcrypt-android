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
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flowcrypt.email.extensions.android.os.readParcelableViaExt
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

/**
 * @author Denys Bondarenko
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
      name = "recipient_in_public_keys",
      value = ["recipient"],
      unique = false
    ),
    Index(
      name = "fingerprint_in_public_keys",
      value = ["fingerprint"],
      unique = false
    )
  ],
  foreignKeys = [
    ForeignKey(
      entity = RecipientEntity::class,
      parentColumns = ["email"],
      childColumns = ["recipient"],
      onDelete = ForeignKey.CASCADE
    )
  ]
)
@Parcelize
data class PublicKeyEntity(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) val id: Long? = null,
  @ColumnInfo(name = "recipient") val recipient: String,
  @ColumnInfo(name = "fingerprint") val fingerprint: String,
  @ColumnInfo(name = "public_key") val publicKey: ByteArray
) : Parcelable {

  @Ignore
  var pgpKeyRingDetails: PgpKeyRingDetails? = null

  @Ignore
  var isNotUsable: Boolean? = null

  constructor(parcel: Parcel) : this(
    parcel.readValue(Long::class.java.classLoader) as? Long,
    requireNotNull(parcel.readString()),
    requireNotNull(parcel.readString()),
    requireNotNull(parcel.createByteArray())
  ) {
    pgpKeyRingDetails = parcel.readParcelableViaExt(PgpKeyRingDetails::class.java)
    isNotUsable = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as PublicKeyEntity

    if (id != other.id) return false
    if (recipient != other.recipient) return false
    if (fingerprint != other.fingerprint) return false
    if (!publicKey.contentEquals(other.publicKey)) return false
    if (pgpKeyRingDetails != other.pgpKeyRingDetails) return false
    if (isNotUsable != other.isNotUsable) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id?.hashCode() ?: 0
    result = 31 * result + recipient.hashCode()
    result = 31 * result + fingerprint.hashCode()
    result = 31 * result + publicKey.contentHashCode()
    result = 31 * result + (pgpKeyRingDetails?.hashCode() ?: 0)
    result = 31 * result + (isNotUsable?.hashCode() ?: 0)
    return result
  }

  companion object : Parceler<PublicKeyEntity> {
    override fun PublicKeyEntity.write(parcel: Parcel, flags: Int) {
      parcel.writeValue(id)
      parcel.writeString(recipient)
      parcel.writeString(fingerprint)
      parcel.writeByteArray(publicKey)
      parcel.writeParcelable(pgpKeyRingDetails, flags)
      parcel.writeValue(isNotUsable)
    }

    override fun create(parcel: Parcel): PublicKeyEntity = PublicKeyEntity(parcel)
  }
}
