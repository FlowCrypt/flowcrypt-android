/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.entity

import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flowcrypt.email.database.dao.KeysDaoCompatibility

/**
 * @author Denis Bondarenko
 *         Date: 12/5/19
 *         Time: 4:12 PM
 *         E-mail: DenBond7@gmail.com
 */
@Entity(tableName = "keys",
    indices = [Index(name = "long_id_in_keys", value = ["long_id"], unique = true)]
)
data class KeyEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) val id: Long? = null,
    @ColumnInfo(name = "long_id") val longId: String,
    val source: String,
    @ColumnInfo(name = "public_key") val publicKey: ByteArray,
    @ColumnInfo(name = "private_key") val privateKey: ByteArray,
    @ColumnInfo(defaultValue = "NULL") val passphrase: String?) {

  @Ignore
  val privateKeyAsString = String(privateKey)

  @Ignore
  val publicKeyAsString = String(privateKey)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as KeyEntity

    if (id != other.id) return false
    if (longId != other.longId) return false
    if (source != other.source) return false
    if (!publicKey.contentEquals(other.publicKey)) return false
    if (!privateKey.contentEquals(other.privateKey)) return false
    if (passphrase != other.passphrase) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + longId.hashCode()
    result = 31 * result + source.hashCode()
    result = 31 * result + publicKey.contentHashCode()
    result = 31 * result + privateKey.contentHashCode()
    result = 31 * result + (passphrase?.hashCode() ?: 0)
    return result
  }

  override fun toString(): String {
    return "KeyEntity(id=$id, longId='$longId', source='$source', publicKey=${publicKey
        .contentToString()}, privateKey=${privateKey.contentToString()}, passphrase=(hidden))"
  }

  companion object {
    fun fromKeyDaoCompatibility(keysDaoCompatibility: KeysDaoCompatibility): KeyEntity {
      return KeyEntity(
          longId = keysDaoCompatibility.longId!!,
          source = keysDaoCompatibility.privateKeySourceType!!.toString(),
          publicKey = keysDaoCompatibility.publicKey?.toByteArray()
              ?: throw NullPointerException("keysDao.publicKey == null"),
          privateKey = keysDaoCompatibility.privateKey?.toByteArray()
              ?: throw NullPointerException("keysDao.privateKey == null"),
          passphrase = keysDaoCompatibility.passphrase)
    }
  }
}