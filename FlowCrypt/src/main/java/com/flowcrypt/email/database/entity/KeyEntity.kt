/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.entity

import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * @author Denis Bondarenko
 *         Date: 12/5/19
 *         Time: 4:12 PM
 *         E-mail: DenBond7@gmail.com
 */
@Entity(tableName = "keys",
    indices = [Index(name = "long_id_account_account_type_in_keys", value = ["long_id", "account", "account_type"], unique = true)],
    foreignKeys = [
      ForeignKey(entity = AccountEntity::class, parentColumns = ["email", "account_type"],
          childColumns = ["account", "account_type"], onDelete = ForeignKey.CASCADE)
    ]
)
data class KeyEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) val id: Long? = null,
    @ColumnInfo(name = "long_id") val longId: String,
    val account: String,
    @ColumnInfo(name = "account_type", defaultValue = "NULL") val accountType: String? = null,
    val source: String,
    @ColumnInfo(name = "public_key") val publicKey: ByteArray,
    @ColumnInfo(name = "private_key") val privateKey: ByteArray,
    @ColumnInfo(defaultValue = "NULL") val passphrase: String?) {

  @Ignore
  val privateKeyAsString = String(privateKey)

  @Ignore
  val publicKeyAsString = String(publicKey)

  override fun toString(): String {
    return "KeyEntity(id=$id," +
        " longId='$longId'," +
        " account='$account'," +
        " account_type='$accountType'," +
        " source='$source'," +
        " publicKey=${publicKey.contentToString()}," +
        " privateKey=${privateKey.contentToString()}," +
        " passphrase=(hidden))"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as KeyEntity

    if (id != other.id) return false
    if (longId != other.longId) return false
    if (account != other.account) return false
    if (accountType != other.accountType) return false
    if (source != other.source) return false
    if (!publicKey.contentEquals(other.publicKey)) return false
    if (!privateKey.contentEquals(other.privateKey)) return false
    if (passphrase != other.passphrase) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id?.hashCode() ?: 0
    result = 31 * result + longId.hashCode()
    result = 31 * result + account.hashCode()
    result = 31 * result + accountType.hashCode()
    result = 31 * result + source.hashCode()
    result = 31 * result + publicKey.contentHashCode()
    result = 31 * result + privateKey.contentHashCode()
    result = 31 * result + (passphrase?.hashCode() ?: 0)
    return result
  }
}