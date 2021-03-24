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
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.model.PgpContact

/**
 * @author Denis Bondarenko
 *         Date: 12/5/19
 *         Time: 4:22 PM
 *         E-mail: DenBond7@gmail.com
 */
@Entity(tableName = "contacts",
    indices = [
      Index(name = "has_pgp_in_contacts", value = ["has_pgp"]),
      Index(name = "name_in_contacts", value = ["name"]),
      Index(name = "long_id_in_contacts", value = ["long_id"]),
      Index(name = "last_use_in_contacts", value = ["last_use"]),
      Index(name = "email_in_contacts", value = ["email"], unique = true)
    ]
)
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) val id: Long? = null,
    val email: String,
    @ColumnInfo(defaultValue = "NULL") val name: String? = null,
    @ColumnInfo(name = "public_key", defaultValue = "NULL") val publicKey: ByteArray? = null,
    @ColumnInfo(name = "has_pgp") val hasPgp: Boolean,
    @ColumnInfo(defaultValue = "NULL") val client: String? = null,
    @ColumnInfo(defaultValue = "NULL") val attested: Boolean? = null,
    @ColumnInfo(defaultValue = "NULL") val fingerprint: String? = null,
    @ColumnInfo(name = "long_id", defaultValue = "NULL") val longId: String? = null,
    @Deprecated("Unused") @ColumnInfo(defaultValue = "NULL") val keywords: String? = null,
    @ColumnInfo(name = "last_use", defaultValue = "0") val lastUse: Long = 0
) : Parcelable {

  @Ignore
  var nodeKeyDetails: NodeKeyDetails? = null

  constructor(parcel: Parcel) : this(
      parcel.readValue(Long::class.java.classLoader) as? Long,
      parcel.readString() ?: throw IllegalArgumentException("Email can't be empty"),
      parcel.readString(),
      parcel.createByteArray(),
      parcel.readByte() != 0.toByte(),
      parcel.readString(),
      parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
      parcel.readString(),
      parcel.readString(),
      parcel.readString(),
      parcel.readLong())

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ContactEntity

    if (id != other.id) return false
    if (email != other.email) return false
    if (name != other.name) return false
    if (publicKey != null) {
      if (other.publicKey == null) return false
      if (!publicKey.contentEquals(other.publicKey)) return false
    } else if (other.publicKey != null) return false
    if (hasPgp != other.hasPgp) return false
    if (client != other.client) return false
    if (attested != other.attested) return false
    if (fingerprint != other.fingerprint) return false
    if (longId != other.longId) return false
    if (lastUse != other.lastUse) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + email.hashCode()
    result = 31 * result + (name?.hashCode() ?: 0)
    result = 31 * result + (publicKey?.contentHashCode() ?: 0)
    result = 31 * result + hasPgp.hashCode()
    result = 31 * result + (client?.hashCode() ?: 0)
    result = 31 * result + (attested?.hashCode() ?: 0)
    result = 31 * result + (fingerprint?.hashCode() ?: 0)
    result = 31 * result + (longId?.hashCode() ?: 0)
    result = 31 * result + lastUse.hashCode()
    return result
  }

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeValue(id)
    parcel.writeString(email)
    parcel.writeString(name)
    parcel.writeByteArray(publicKey)
    parcel.writeByte(if (hasPgp) 1 else 0)
    parcel.writeString(client)
    parcel.writeValue(attested)
    parcel.writeString(fingerprint)
    parcel.writeString(longId)
    parcel.writeString(null)
    parcel.writeLong(lastUse)
  }

  override fun describeContents(): Int {
    return 0
  }

  fun toPgpContact(): PgpContact {
    return PgpContact(
        email = email,
        name = name,
        pubkey = String(publicKey ?: byteArrayOf()),
        hasPgp = hasPgp,
        client = client,
        fingerprint = fingerprint,
        longid = longId,
        lastUse = lastUse,
        nodeKeyDetails = nodeKeyDetails)
  }

  companion object CREATOR : Parcelable.Creator<ContactEntity> {
    const val CLIENT_FLOWCRYPT = "flowcrypt"
    const val CLIENT_PGP = "pgp"

    override fun createFromParcel(parcel: Parcel): ContactEntity {
      return ContactEntity(parcel)
    }

    override fun newArray(size: Int): Array<ContactEntity?> {
      return arrayOfNulls(size)
    }
  }

  enum class Type {
    TO, CC, BCC
  }
}